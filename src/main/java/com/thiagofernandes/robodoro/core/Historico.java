package com.thiagofernandes.robodoro.core;

import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;

/** Focos concluídos por dia, para mostrar "hoje" e a sequência de dias seguidos. */
public class Historico {

    private final TreeMap<LocalDate, Integer> ciclosPorDia;

    public Historico() {
        this(Map.of());
    }

    public Historico(Map<LocalDate, Integer> ciclosPorDia) {
        this.ciclosPorDia = new TreeMap<>(ciclosPorDia);
    }

    public void registrarCiclo(LocalDate dia) {
        ciclosPorDia.merge(dia, 1, Integer::sum);
    }

    public int ciclosEm(LocalDate dia) {
        return ciclosPorDia.getOrDefault(dia, 0);
    }

    /**
     * Dias seguidos com pelo menos um foco, contando até hoje. Se hoje ainda não teve foco,
     * a sequência que terminou ontem continua valendo (o dia ainda não acabou).
     */
    public int sequenciaDeDias(LocalDate hoje) {
        LocalDate dia = ciclosEm(hoje) > 0 ? hoje : hoje.minusDays(1);
        int sequencia = 0;
        while (ciclosEm(dia) > 0) {
            sequencia++;
            dia = dia.minusDays(1);
        }
        return sequencia;
    }

    public Map<LocalDate, Integer> comoMapa() {
        return Map.copyOf(ciclosPorDia);
    }
}
