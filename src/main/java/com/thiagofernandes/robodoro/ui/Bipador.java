package com.thiagofernandes.robodoro.ui;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bipes de robô sintetizados na hora (onda quadrada, estilo 8 bits). Sem arquivos de áudio.
 * Toca numa thread própria para não travar a animação; se não houver saída de som, fica em silêncio.
 */
final class Bipador {

    /** Sequências de {frequência em Hz, duração em ms}; frequência 0 é silêncio. */
    enum Melodia {
        FIM_DO_FOCO(new double[][]{{660, 90}, {0, 40}, {660, 90}, {0, 40}, {990, 160}}),
        INICIO_DA_PAUSA(new double[][]{{523, 70}, {659, 70}, {784, 70}, {1047, 180}}),
        FIM_DA_PAUSA(new double[][]{{880, 80}, {0, 60}, {880, 80}}),
        INICIO_DO_FOCO(new double[][]{{784, 60}, {1175, 90}}),
        SUPERAQUECEU(new double[][]{{300, 120}, {600, 120}, {300, 120}, {600, 120}, {300, 200}});

        private final double[][] notas;

        Melodia(double[][] notas) {
            this.notas = notas;
        }
    }

    private static final float TAXA = 22_050f;
    private static final double VOLUME = 0.12;

    private final ExecutorService fila = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "robodoro-som");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean mudo;

    void tocar(Melodia melodia) {
        if (!mudo) {
            fila.submit(() -> reproduzir(melodia));
        }
    }

    boolean alternarMudo() {
        mudo = !mudo;
        return mudo;
    }

    boolean mudo() {
        return mudo;
    }

    private static void reproduzir(Melodia melodia) {
        AudioFormat formato = new AudioFormat(TAXA, 8, 1, true, false);
        try (SourceDataLine linha = AudioSystem.getSourceDataLine(formato)) {
            linha.open(formato);
            linha.start();
            for (double[] nota : melodia.notas) {
                byte[] amostras = onda(nota[0], nota[1]);
                linha.write(amostras, 0, amostras.length);
            }
            linha.drain();
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException e) {
            // sem dispositivo de som: o robô fica mudo, o resto continua
        }
    }

    private static byte[] onda(double frequencia, double ms) {
        int total = (int) (TAXA * ms / 1000);
        byte[] amostras = new byte[total];
        if (frequencia <= 0) {
            return amostras;
        }
        double periodo = TAXA / frequencia;
        for (int i = 0; i < total; i++) {
            // envelope curto nas pontas evita o "clique" ao ligar e desligar a nota
            double envelope = Math.min(1, Math.min(i, total - i) / (TAXA * 0.005));
            double quadrada = (i % periodo) < periodo / 2 ? 1 : -1;
            amostras[i] = (byte) (quadrada * VOLUME * envelope * 127);
        }
        return amostras;
    }
}
