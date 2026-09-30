package com.thiagofernandes.robodoro.core;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatoTempoTest {

    @ParameterizedTest
    @CsvSource({
            "1500, 25:00",
            "59.2, 01:00",
            "59.0, 00:59",
            "0.4,  00:01",
            "0,    00:00",
            "-3,   00:00"
    })
    void tempoRestanteArredondaParaCima(double segundos, String esperado) {
        assertEquals(esperado, FormatoTempo.mmss(segundos));
    }

    @ParameterizedTest
    @CsvSource({
            "0.9, +00:00",
            "65,  +01:05",
            "300, +05:00"
    })
    void horaExtraArredondaParaBaixo(double segundos, String esperado) {
        assertEquals(esperado, FormatoTempo.decorrido(segundos));
    }
}
