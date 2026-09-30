package com.thiagofernandes.robodoro.visual;

import com.thiagofernandes.robodoro.core.Configuracao;
import com.thiagofernandes.robodoro.core.Sessao;
import com.thiagofernandes.robodoro.visual.Aparencia.Boca;
import com.thiagofernandes.robodoro.visual.Aparencia.Olhos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AparenciaTest {

    private static final Configuracao CONFIG = new Configuracao(100, 20, 40, 4, 90, 50, 50, 100, 3, 10);

    private Sessao focando() {
        Sessao s = new Sessao(CONFIG);
        s.alternar();
        return s;
    }

    @Test
    void focadoTemOlhosConcentradosECorpoFrio() {
        Aparencia ap = Aparencia.de(focando(), false);

        assertEquals(Olhos.CONCENTRADOS, ap.olhos());
        assertEquals(Boca.GRADE, ap.boca());
        assertEquals(Aparencia.CORPO_FRIO, ap.corCorpo());
        assertEquals(Aparencia.CIANO, ap.corLed());
        assertEquals(5, ap.segmentosBateria());
        assertEquals(0, ap.quedaAntena());
        assertFalse(ap.caboConectado());
    }

    @Test
    void cansadoFicaSonolentoComAntenaCaindo() {
        Sessao s = focando();
        s.avancarTempo(100);

        Aparencia ap = Aparencia.de(s, false);

        assertEquals(Olhos.SONOLENTOS, ap.olhos());
        assertEquals(2, ap.segmentosBateria());
        assertEquals(Aparencia.AMARELO, ap.corBateria());
        assertEquals(0, ap.quedaAntena());
    }

    @Test
    void superaquecidoFicaVermelhoSoltandoFumacaEFaiscas() {
        Sessao s = focando();
        s.avancarTempo(100);
        s.avancarTempo(10);

        Aparencia ap = Aparencia.de(s, false);

        assertEquals(Olhos.ESPIRAIS, ap.olhos());
        assertEquals(Boca.ZIGUEZAGUE, ap.boca());
        assertEquals(Aparencia.CORPO_QUENTE, ap.corCorpo());
        assertEquals(1, ap.fumaca(), 1e-9);
        assertTrue(ap.faiscas());
        assertTrue(ap.quedaAntena() > 0);
    }

    @Test
    void naPausaVaiParaATomadaSemChama() {
        Sessao s = focando();
        s.avancarTempo(100);
        s.avancar();

        Aparencia ap = Aparencia.de(s, false);

        assertTrue(ap.caboConectado());
        assertEquals(0, ap.chama());
        assertEquals(Olhos.SONOLENTOS, ap.olhos());
    }

    @Test
    void recarregadoNaPausaFicaFeliz() {
        Sessao s = focando();
        s.avancarTempo(100);
        s.avancar();
        s.avancarTempo(15);

        assertEquals(Olhos.FELIZES, Aparencia.de(s, false).olhos());
    }

    @Test
    void comemoracaoForcaCaraFeliz() {
        Sessao s = focando();
        s.avancarTempo(100);
        s.avancarTempo(10);

        Aparencia ap = Aparencia.de(s, true);

        assertEquals(Olhos.FELIZES, ap.olhos());
        assertEquals(Boca.SORRISO, ap.boca());
    }

    @Test
    void semBateriaNenhumSegmentoAcende() {
        Configuracao config = new Configuracao(10, 5, 5, 4, 10, 100, 10, 10, 3, 1000);
        Sessao s = new Sessao(config);
        s.alternar();
        s.avancarTempo(10);

        Aparencia ap = Aparencia.de(s, false);

        assertEquals(0, ap.segmentosBateria());
        assertEquals(Olhos.PISCANDO, ap.olhos());
        assertEquals(1, ap.quedaAntena(), 1e-9);
        assertTrue(ap.faiscas());
    }

    @Test
    void misturaDeCores() {
        assertEquals(0x000000, Aparencia.misturar(0x000000, 0xFFFFFF, 0));
        assertEquals(0xFFFFFF, Aparencia.misturar(0x000000, 0xFFFFFF, 1));
        assertEquals(0x808080, Aparencia.misturar(0x000000, 0xFFFFFF, 0.5));
        assertEquals(0xFF0000, Aparencia.misturar(0xFF0000, 0x00FF00, -2));
    }
}
