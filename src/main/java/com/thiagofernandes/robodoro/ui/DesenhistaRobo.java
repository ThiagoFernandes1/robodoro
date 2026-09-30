package com.thiagofernandes.robodoro.ui;

import com.thiagofernandes.robodoro.visual.Aparencia;
import com.thiagofernandes.robodoro.visual.Aparencia.Boca;
import com.thiagofernandes.robodoro.visual.Aparencia.Olhos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Desenha o robô em pixel art numa grade lógica de {@value #LARGURA}x{@value #ALTURA}, ampliada
 * {@code escala} vezes. Nada de sprite em arquivo: cada pixel sai daqui, a partir da {@link Aparencia}.
 */
final class DesenhistaRobo {

    static final int LARGURA = 46;
    static final int ALTURA = 58;

    private static final int CONTORNO = 0x2A3440;
    private static final int VISOR = 0x141C27;
    private static final int SEGMENTO_APAGADO = 0x26313D;
    private static final int FUMACA = 0x9AA3AD;
    private static final int FAISCA = 0xFFF27A;
    private static final int CABO = 0x3A4452;

    /** Deslocamento do robô na grade: sobra espaço em cima para fumaça e à direita para o cabo. */
    private static final int OX = 3;
    private static final int OY = 11;

    /**
     * O que desenhar num quadro.
     *
     * @param t            tempo de animação em segundos
     * @param comemoracao  progresso da animação de início de pausa (0 a 1), ou negativo se não houver
     * @param alerta       se a fase acabou e o robô espera uma ação
     */
    record Quadro(Aparencia aparencia, double t, double comemoracao, boolean alerta) {
    }

    private final Canvas canvas;
    private final int escala;
    private GraphicsContext gc;
    private int dy;

    DesenhistaRobo(int escala) {
        this.escala = escala;
        this.canvas = new Canvas(LARGURA * escala, ALTURA * escala);
    }

    Canvas canvas() {
        return canvas;
    }

    void desenhar(Quadro q) {
        gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        Aparencia ap = q.aparencia();
        boolean comemorando = q.comemoracao() >= 0;

        dy = deslocamentoVertical(q, ap);

        int corpo = ap.corCorpo();
        int sombra = escurecer(corpo, 0.72);
        int luz = clarear(corpo, 0.3);

        if (ap.caboConectado()) {
            desenharCabo();
        } else {
            desenharChama(ap, q.t());
        }
        desenharAntena(ap, q.t());
        desenharCabeca(corpo, sombra, luz);
        desenharOlhos(ap, q.t());
        desenharBoca(ap.boca());
        desenharCorpo(corpo, sombra, luz);
        desenharBateria(ap, q.t());
        desenharBracos(sombra, comemorando);
        desenharPropulsor(sombra);

        if (ap.fumaca() > 0) {
            desenharFumaca(ap.fumaca(), q.t());
        }
        if (ap.faiscas()) {
            desenharFaiscas(q.t());
        }
        if (comemorando) {
            desenharRaios(q.comemoracao());
        } else if (ap.caboConectado()) {
            desenharRaioFlutuante(q.t());
        }
        if (q.alerta()) {
            desenharAlerta(q.t());
        }
    }

    /** Balanço de flutuação; com pouca bateria ele afunda, na tomada fica parado, comemorando ele pula. */
    private int deslocamentoVertical(Quadro q, Aparencia ap) {
        if (q.comemoracao() >= 0) {
            return (int) -Math.round(Math.abs(Math.sin(q.comemoracao() * Math.PI * 3)) * 4);
        }
        if (ap.caboConectado()) {
            return 2;
        }
        double velocidade = 1.5 + ap.chama() * 2;
        int balanco = (int) Math.round(Math.sin(q.t() * velocidade) * 1.2);
        int afundamento = ap.chama() < 0.4 ? 1 : 0;
        return balanco + afundamento;
    }

    // --- partes ---

    private void desenharAntena(Aparencia ap, double t) {
        double q = ap.quedaAntena();
        int xTopo = 20;
        int yTopo = 3;
        for (int k = 0; k <= 4; k++) {
            int x = 20 + (int) Math.round(q * k * 1.3);
            int y = 7 - k + (int) Math.round(q * k * 0.7);
            px(x, y, 2, 1, CONTORNO);
            xTopo = x;
            yTopo = y;
        }
        // LED da ponta pulsa devagar; em estado crítico pisca.
        double pulso = 0.6 + 0.4 * Math.sin(t * 3);
        int led = ap.olhos() == Olhos.PISCANDO && piscando(t) ? escurecer(ap.corLed(), 0.35) : misturarLed(ap.corLed(), pulso);
        px(xTopo - 1, yTopo - 3, 4, 3, CONTORNO);
        px(xTopo, yTopo - 3, 2, 3, led);
        px(xTopo - 1, yTopo - 2, 4, 1, led);
    }

    private void desenharCabeca(int corpo, int sombra, int luz) {
        // orelhas
        retanguloArredondado(4, 12, 3, 7, CONTORNO);
        px(5, 13, 1, 5, sombra);
        retanguloArredondado(33, 12, 3, 7, CONTORNO);
        px(34, 13, 1, 5, sombra);

        retanguloArredondado(6, 8, 28, 15, CONTORNO);
        retanguloArredondado(7, 9, 26, 13, corpo);
        px(8, 9, 24, 1, luz);
        px(8, 21, 24, 1, sombra);

        retanguloArredondado(9, 11, 22, 8, VISOR);
        px(10, 12, 2, 1, 0x2A3A4D);
    }

    private void desenharOlhos(Aparencia ap, double t) {
        int led = ap.corLed();
        switch (ap.olhos()) {
            case REDONDOS -> {
                for (int x : new int[]{12, 24}) {
                    retanguloArredondado(x, 12, 4, 5, led);
                    px(x + 1, 13, 1, 1, 0xFFFFFF);
                }
            }
            case CONCENTRADOS -> {
                for (int x : new int[]{11, 24}) {
                    px(x, 14, 5, 2, led);
                    px(x + (x < 20 ? 3 : 0), 13, 2, 1, led);
                }
            }
            case SONOLENTOS -> {
                for (int x : new int[]{12, 24}) {
                    px(x, 15, 4, 1, led);
                    px(x + 1, 16, 2, 1, escurecer(led, 0.6));
                }
            }
            case PISCANDO -> {
                // bateria acabando: olhos pesados, vermelhos, falhando
                int cor = piscando(t) ? escurecer(led, 0.3) : led;
                for (int x : new int[]{12, 24}) {
                    px(x, 14, 4, 1, escurecer(led, 0.5));
                    px(x, 15, 4, 2, cor);
                }
            }
            case ESPIRAIS -> {
                // superaquecido: "x" e "+" alternando dão a impressão de olhos girando
                boolean emX = ((int) (t * 5)) % 2 == 0;
                for (int x : new int[]{12, 24}) {
                    girando(x, 12, led, emX);
                }
            }
            case FELIZES -> {
                for (int x : new int[]{12, 24}) {
                    px(x, 15, 1, 1, led);
                    px(x + 1, 14, 2, 1, led);
                    px(x + 3, 15, 1, 1, led);
                }
            }
        }
    }

    private static final String[] OLHO_X = {"10001", "01010", "00100", "01010", "10001"};
    private static final String[] OLHO_MAIS = {"00100", "00100", "11111", "00100", "00100"};

    private void girando(int x, int y, int cor, boolean emX) {
        String[] linhas = emX ? OLHO_X : OLHO_MAIS;
        for (int i = 0; i < linhas.length; i++) {
            for (int j = 0; j < linhas[i].length(); j++) {
                if (linhas[i].charAt(j) == '1') {
                    px(x + j, y + i, 1, 1, cor);
                }
            }
        }
    }

    private void desenharBoca(Boca boca) {
        switch (boca) {
            case GRADE -> {
                for (int x = 16; x <= 24; x += 2) {
                    px(x, 20, 1, 1, CONTORNO);
                }
            }
            case SORRISO -> {
                px(16, 19, 1, 1, CONTORNO);
                px(17, 20, 7, 1, CONTORNO);
                px(24, 19, 1, 1, CONTORNO);
            }
            case RETA -> px(17, 20, 7, 1, CONTORNO);
            case ZIGUEZAGUE -> {
                for (int x = 15; x <= 25; x++) {
                    px(x, x % 2 == 0 ? 19 : 20, 1, 1, CONTORNO);
                }
            }
        }
    }

    private void desenharCorpo(int corpo, int sombra, int luz) {
        px(16, 23, 1, 2, CONTORNO);
        px(17, 23, 7, 2, sombra);
        px(24, 23, 1, 2, CONTORNO);

        retanguloArredondado(9, 25, 23, 12, CONTORNO);
        retanguloArredondado(10, 26, 21, 10, corpo);
        px(11, 26, 19, 1, luz);
        px(11, 35, 19, 1, sombra);
    }

    /** Bateria no peito: 5 segmentos. Na tomada, o segmento seguinte pisca "enchendo". */
    private void desenharBateria(Aparencia ap, double t) {
        px(12, 28, 16, 6, CONTORNO);
        px(13, 29, 14, 4, VISOR);
        px(28, 30, 1, 2, CONTORNO);

        int acesos = ap.segmentosBateria();
        int enchendo = -1;
        if (ap.caboConectado() && acesos < Aparencia.SEGMENTOS) {
            enchendo = acesos + ((int) (t * 3)) % (Aparencia.SEGMENTOS - acesos + 1);
        }
        for (int i = 0; i < Aparencia.SEGMENTOS; i++) {
            int cor = i < acesos || i < enchendo ? ap.corBateria() : SEGMENTO_APAGADO;
            px(13 + i * 3, 29, 2, 4, cor);
        }
    }

    private void desenharBracos(int sombra, boolean levantados) {
        int yBraco = levantados ? 19 : 26;
        for (int x : new int[]{5, 32}) {
            retanguloArredondado(x, yBraco, 4, 8, CONTORNO);
            px(x + 1, yBraco + 1, 2, 6, sombra);
            int yMao = levantados ? yBraco - 2 : yBraco + 8;
            retanguloArredondado(x - 1, yMao, 6, 3, CONTORNO);
        }
    }

    private void desenharPropulsor(int sombra) {
        retanguloArredondado(13, 37, 15, 3, CONTORNO);
        px(14, 37, 13, 2, sombra);
    }

    /** Chama azul do propulsor; fraca e engasgando quando a bateria está baixa. */
    private void desenharChama(Aparencia ap, double t) {
        boolean engasgando = ap.chama() < 0.35 && ((int) (t * 7)) % 3 == 0;
        if (engasgando) {
            return;
        }
        int comprimento = 1 + (int) Math.round(ap.chama() * 4 + Math.sin(t * 18) * 0.6);
        for (int i = 0; i < comprimento; i++) {
            int largura = Math.max(2, 9 - i * 2);
            int cor = i == 0 ? 0xE8FDFF : i < 3 ? Aparencia.CIANO : 0x2E7BD6;
            px(20 - largura / 2 + 1, 40 + i, largura, 1, cor);
        }
    }

    /**
     * Cabo saindo da mão direita até a tomada no chão. A ponta da mão acompanha o robô
     * (inclusive no pulo da comemoração); a tomada fica parada.
     */
    private void desenharCabo() {
        int yMao = 35 + dy;
        int chao = 44;
        pxFixo(37, yMao, 3, 1, CABO);
        pxFixo(39, Math.min(yMao, chao), 1, Math.abs(chao - yMao) + 1, CABO);
        pxFixo(38, chao, 3, 3, 0x55606E);
        pxFixo(38, chao + 3, 1, 1, CONTORNO);
        pxFixo(40, chao + 3, 1, 1, CONTORNO);
        // sombra no chão, já que ele está pousado
        pxFixo(11, 43, 19, 1, 0x000000, 0.25);
    }

    private void desenharFumaca(double intensidade, double t) {
        int nuvens = 1 + (int) Math.round(intensidade * 5);
        for (int k = 0; k < nuvens; k++) {
            double fase = (t * 0.7 + (double) k / nuvens) % 1;
            int x = 9 + (k * 7) % 24 + (int) Math.round(Math.sin(fase * 6 + k) * 2);
            int y = 6 - (int) Math.round(fase * 14);
            int tamanho = fase < 0.5 ? 2 : 3;
            px(x, y, tamanho, tamanho, FUMACA, (1 - fase) * 0.75 * intensidade + 0.1);
        }
    }

    private void desenharFaiscas(double t) {
        int semente = (int) (t * 9);
        int[][] pontos = {{3, 10}, {37, 11}, {2, 18}, {38, 20}, {24, 2}, {8, 6}};
        for (int k = 0; k < pontos.length; k++) {
            if ((semente + k * 5) % 4 == 0) {
                int x = pontos[k][0];
                int y = pontos[k][1];
                px(x, y, 1, 1, FAISCA);
                px(x - 1, y, 1, 1, FAISCA, 0.6);
                px(x + 1, y, 1, 1, FAISCA, 0.6);
                px(x, y - 1, 1, 1, FAISCA, 0.6);
                px(x, y + 1, 1, 1, FAISCA, 0.6);
            }
        }
    }

    /** Raios que saem voando do robô quando a pausa começa. */
    private void desenharRaios(double progresso) {
        // Começam já fora da cabeça (que tem 28 de largura) e se afastam enquanto somem.
        double[][] direcoes = {{-1, -0.5}, {1, -0.5}, {-1, 0.5}, {1, 0.5}};
        double alcance = 16 + progresso * 4;
        double alfa = 1 - progresso * 0.8;
        for (double[] d : direcoes) {
            int x = 19 + (int) Math.round(d[0] * alcance);
            int y = 20 + (int) Math.round(d[1] * alcance);
            raio(x, y, Aparencia.AMARELO, alfa);
        }
    }

    private void desenharRaioFlutuante(double t) {
        double fase = (t * 0.6) % 1;
        raio(38, 6 - (int) Math.round(fase * 5) - dy, Aparencia.AMARELO, 1 - fase * 0.7);
    }

    private static final String[] RAIO = {"0011", "0110", "1111", "0110", "1100"};

    private void raio(int x, int y, int cor, double alfa) {
        for (int i = 0; i < RAIO.length; i++) {
            for (int j = 0; j < RAIO[i].length(); j++) {
                if (RAIO[i].charAt(j) == '1') {
                    px(x + j, y + i, 1, 1, cor, alfa);
                }
            }
        }
    }

    /** Balão com "!" pulsando: a fase acabou e o robô espera você. */
    private void desenharAlerta(double t) {
        if (((int) (t * 3)) % 2 == 1) {
            return;
        }
        int x = 33;
        int y = 0;
        retanguloArredondado(x, y, 7, 9, CONTORNO);
        retanguloArredondado(x + 1, y + 1, 5, 7, 0xFFFFFF);
        px(x + 3, y + 2, 1, 3, Aparencia.VERMELHO);
        px(x + 3, y + 6, 1, 1, Aparencia.VERMELHO);
        px(x, y + 9, 2, 1, CONTORNO);
    }

    // --- primitivas ---

    private void retanguloArredondado(int x, int y, int l, int a, int cor) {
        px(x + 1, y, l - 2, a, cor);
        px(x, y + 1, l, a - 2, cor);
    }

    private void px(int x, int y, int l, int a, int cor) {
        px(x, y, l, a, cor, 1);
    }

    /** Pixel que acompanha o balanço do robô. */
    private void px(int x, int y, int l, int a, int cor, double alfa) {
        pxFixo(x, y + dy, l, a, cor, alfa);
    }

    private void pxFixo(int x, int y, int l, int a, int cor) {
        pxFixo(x, y, l, a, cor, 1);
    }

    /** Pixel preso à cena (chão, tomada), que não balança junto com o robô. */
    private void pxFixo(int x, int y, int l, int a, int cor, double alfa) {
        gc.setFill(Color.rgb((cor >> 16) & 0xFF, (cor >> 8) & 0xFF, cor & 0xFF, Math.clamp(alfa, 0, 1)));
        gc.fillRect((OX + x) * escala, (OY + y) * escala, l * escala, a * escala);
    }

    private static boolean piscando(double t) {
        return (t * 4) % 1 > 0.6;
    }

    private static int misturarLed(int led, double intensidade) {
        return Aparencia.misturar(escurecer(led, 0.55), led, intensidade);
    }

    private static int escurecer(int cor, double fator) {
        return Aparencia.misturar(0x000000, cor, fator);
    }

    private static int clarear(int cor, double fator) {
        return Aparencia.misturar(cor, 0xFFFFFF, fator);
    }
}
