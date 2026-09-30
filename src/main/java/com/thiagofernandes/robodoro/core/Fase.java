package com.thiagofernandes.robodoro.core;

public enum Fase {
    FOCO("FOCO"),
    PAUSA_CURTA("PAUSA"),
    PAUSA_LONGA("PAUSA LONGA");

    private final String rotulo;

    Fase(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    public boolean isPausa() {
        return this != FOCO;
    }
}
