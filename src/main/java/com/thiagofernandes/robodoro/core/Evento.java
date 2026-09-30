package com.thiagofernandes.robodoro.core;

/** O que aconteceu numa transição da sessão. A interface reage a isso com som e animação. */
public enum Evento {
    /** Foco completado até o fim: conta como ciclo. */
    FOCO_TERMINOU,
    /** Foco encerrado antes da hora: não conta como ciclo. */
    FOCO_PULADO,
    PAUSA_TERMINOU,
    PAUSA_INICIOU,
    FOCO_INICIOU,
    /** A hora extra aqueceu o robô até o limite. */
    SUPERAQUECEU
}
