package com.thiagofernandes.robodoro.ui;

import com.thiagofernandes.robodoro.core.Sessao;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Gera as imagens do README simulando cada situação na sessão real — a mesma lógica e o mesmo
 * desenho do app, sem montagem manual. Rode com {@code --capturas=docs}.
 */
final class Capturas {

    private static final double MINUTO = 60;

    private final RobodoroApp app;
    private final HBox raiz;

    Capturas(RobodoroApp app, HBox raiz) {
        this.app = app;
        this.raiz = raiz;
    }

    void gerar(Path pasta) {
        app.painel().setOpacity(1);
        app.historico().registrarCiclo(LocalDate.now().minusDays(1));
        Sessao s = app.sessao();

        // 1. Começo do foco: cheio de energia, concentrado.
        s.reiniciar();
        s.alternar();
        s.avancarTempo(3 * MINUTO);
        salvar(pasta, "1-focado.png", 0.4);

        // 2. Fim do foco: bateria baixa, cansado, esperando você começar a pausa.
        s.avancarTempo(22 * MINUTO);
        app.historico().registrarCiclo(LocalDate.now());
        salvar(pasta, "2-fim-do-foco.png", 0.1);

        // 3. Ignorou a pausa: hora extra, superaquecido.
        s.avancarTempo(5 * MINUTO);
        salvar(pasta, "3-superaquecido.png", 1.35);

        // 4. Na tomada: pausa recarregando.
        s.reiniciar();
        s.alternar();
        s.avancarTempo(25 * MINUTO);
        s.avancar();
        s.avancarTempo(4 * MINUTO);
        salvar(pasta, "4-carregando.png", 0.2);

        // 5. Comemoração ao começar a pausa (braços para cima, raios).
        s.reiniciar();
        s.alternar();
        s.avancarTempo(25 * MINUTO);
        s.avancar();
        app.definirComemoracao(0.9);
        salvar(pasta, "5-comemorando.png", 0.5);
        app.definirComemoracao(-1);

        // Só o robô, para o topo do README.
        app.painel().setVisible(false);
        app.painel().setManaged(false);
        s.reiniciar();
        s.alternar();
        s.avancarTempo(MINUTO);
        salvar(pasta, "robo.png", 0.4);
    }

    private void salvar(Path pasta, String nome, double tempoAnimacao) {
        app.definirTempoAnimacao(tempoAnimacao);
        app.atualizarInterface();
        raiz.applyCss();
        raiz.layout();

        SnapshotParameters parametros = new SnapshotParameters();
        parametros.setFill(Color.TRANSPARENT);
        WritableImage imagem = raiz.snapshot(parametros, null);

        try {
            Files.createDirectories(pasta);
            ImageIO.write(paraBufferedImage(imagem), "png", pasta.resolve(nome).toFile());
            System.out.println("gerado: " + pasta.resolve(nome));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Conversão manual para não depender do módulo javafx.swing só por causa disso. */
    private static BufferedImage paraBufferedImage(WritableImage imagem) {
        int largura = (int) imagem.getWidth();
        int altura = (int) imagem.getHeight();
        BufferedImage saida = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        PixelReader leitor = imagem.getPixelReader();
        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                saida.setRGB(x, y, leitor.getArgb(x, y));
            }
        }
        return saida;
    }
}
