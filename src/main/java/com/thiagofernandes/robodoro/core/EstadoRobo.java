package com.thiagofernandes.robodoro.core;

/** Como o robô está se sentindo — decide olhos, boca, cores e efeitos. */
public enum EstadoRobo {
    /** Na tomada, recarregando. */
    CARREGANDO,
    /** Aquecido pela hora extra: fumaça, faíscas, olhos em espiral. */
    SUPERAQUECENDO,
    /** Bateria quase no fim: olhos vermelhos piscando. */
    CRITICO,
    /** Bateria baixa: olhos semicerrados, antena caída. */
    CANSADO,
    /** Trabalhando com carga boa. */
    FOCADO,
    /** Carga boa fora do foco. */
    DESCANSADO
}
