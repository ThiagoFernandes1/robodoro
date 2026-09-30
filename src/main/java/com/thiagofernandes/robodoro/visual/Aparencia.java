package com.thiagofernandes.robodoro.visual;

import com.thiagofernandes.robodoro.core.EstadoRobo;
import com.thiagofernandes.robodoro.core.Sessao;

/**
 * Tudo o que o desenho do robô precisa saber, calculado a partir da sessão. Fica fora do JavaFX
 * (cores são inteiros RGB) para que "qual cara o robô faz em cada situação" seja testável.
 *
 * @param olhos             formato dos olhos no visor
 * @param boca              formato da grade de voz
 * @param corCorpo          lataria: aço frio até vermelho quente, conforme a temperatura
 * @param corLed            olhos e ponta da antena
 * @param segmentosBateria  quantos dos 5 segmentos do peito estão acesos
 * @param corBateria        cor dos segmentos acesos
 * @param quedaAntena       0 = antena em pé, 1 = totalmente caída
 * @param fumaca            intensidade da fumaça (0 a 1)
 * @param faiscas           se soltam faíscas
 * @param caboConectado     se está na tomada
 * @param chama             força do propulsor que o mantém flutuando (0 a 1)
 */
public record Aparencia(
        Olhos olhos,
        Boca boca,
        int corCorpo,
        int corLed,
        int segmentosBateria,
        int corBateria,
        double quedaAntena,
        double fumaca,
        boolean faiscas,
        boolean caboConectado,
        double chama) {

    public enum Olhos { REDONDOS, CONCENTRADOS, SONOLENTOS, PISCANDO, ESPIRAIS, FELIZES }

    public enum Boca { GRADE, SORRISO, RETA, ZIGUEZAGUE }

    public static final int CORPO_FRIO = 0x8FA3B8;
    public static final int CORPO_QUENTE = 0xD9573B;

    public static final int CIANO = 0x4DE3FF;
    public static final int VERDE = 0x5DFF8A;
    public static final int AMARELO = 0xFFD23D;
    public static final int LARANJA = 0xFF8C1A;
    public static final int VERMELHO = 0xFF4D4D;

    public static final int SEGMENTOS = 5;

    /**
     * @param comemorando se a animação de início de pausa está rodando (força a cara feliz)
     */
    public static Aparencia de(Sessao sessao, boolean comemorando) {
        EstadoRobo estado = sessao.estado();
        double bateria = sessao.bateria();
        double temperatura = sessao.temperatura();
        boolean carregando = estado == EstadoRobo.CARREGANDO;

        Olhos olhos = comemorando ? Olhos.FELIZES : switch (estado) {
            case CARREGANDO -> bateria >= 70 ? Olhos.FELIZES : Olhos.SONOLENTOS;
            case SUPERAQUECENDO -> Olhos.ESPIRAIS;
            case CRITICO -> Olhos.PISCANDO;
            case CANSADO -> Olhos.SONOLENTOS;
            case FOCADO -> Olhos.CONCENTRADOS;
            case DESCANSADO -> Olhos.REDONDOS;
        };

        Boca boca = comemorando ? Boca.SORRISO : switch (estado) {
            case CARREGANDO -> bateria >= 70 ? Boca.SORRISO : Boca.RETA;
            case SUPERAQUECENDO, CRITICO -> Boca.ZIGUEZAGUE;
            case CANSADO -> Boca.RETA;
            case FOCADO -> Boca.GRADE;
            case DESCANSADO -> Boca.SORRISO;
        };

        int corLed = switch (estado) {
            case FOCADO -> CIANO;
            case DESCANSADO, CARREGANDO -> VERDE;
            case CANSADO -> AMARELO;
            case CRITICO -> VERMELHO;
            case SUPERAQUECENDO -> LARANJA;
        };

        int segmentos = (int) Math.clamp(Math.ceil(bateria / (100.0 / SEGMENTOS)), 0, SEGMENTOS);
        int corBateria = bateria > 60 ? VERDE : bateria > 30 ? AMARELO : VERMELHO;

        double quedaAntena = bateria < Sessao.BATERIA_BAIXA ? (Sessao.BATERIA_BAIXA - bateria) / Sessao.BATERIA_BAIXA : 0;
        double fumaca = Math.clamp((temperatura - 0.3) / 0.7, 0, 1);
        boolean faiscas = temperatura >= 0.8 || estado == EstadoRobo.CRITICO;
        double chama = carregando ? 0 : Math.clamp(bateria / 100, 0.15, 1);

        return new Aparencia(olhos, boca, misturar(CORPO_FRIO, CORPO_QUENTE, temperatura), corLed,
                segmentos, corBateria, quedaAntena, fumaca, faiscas, carregando, chama);
    }

    /** Interpola duas cores RGB. */
    public static int misturar(int de, int para, double t) {
        t = Math.clamp(t, 0, 1);
        int r = (int) Math.round(((de >> 16) & 0xFF) + (((para >> 16) & 0xFF) - ((de >> 16) & 0xFF)) * t);
        int g = (int) Math.round(((de >> 8) & 0xFF) + (((para >> 8) & 0xFF) - ((de >> 8) & 0xFF)) * t);
        int b = (int) Math.round((de & 0xFF) + ((para & 0xFF) - (de & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }
}
