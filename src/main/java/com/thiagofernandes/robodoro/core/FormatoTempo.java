package com.thiagofernandes.robodoro.core;

public final class FormatoTempo {

    private FormatoTempo() {
    }

    /** Segundos restantes como MM:SS, arredondando para cima (59.2s mostra 01:00, não 00:59). */
    public static String mmss(double segundos) {
        long total = (long) Math.ceil(Math.max(0, segundos));
        return "%02d:%02d".formatted(total / 60, total % 60);
    }

    /** Tempo decorrido (hora extra) como +MM:SS. */
    public static String decorrido(double segundos) {
        long total = (long) Math.floor(Math.max(0, segundos));
        return "+%02d:%02d".formatted(total / 60, total % 60);
    }
}
