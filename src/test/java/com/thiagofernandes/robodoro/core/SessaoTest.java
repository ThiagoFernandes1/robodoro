package com.thiagofernandes.robodoro.core;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessaoTest {

    /** Foco 100s, pausas 20s/40s, pausa longa a cada 2, bateria 90, gasto 50, hora extra superaquece em 10s. */
    private static final Configuracao CONFIG = new Configuracao(100, 20, 40, 2, 90, 50, 50, 100, 3, 10);

    private static final double DELTA = 1e-9;

    private Sessao iniciada() {
        Sessao s = new Sessao(CONFIG);
        s.alternar();
        return s;
    }

    @Test
    void comecaParadaNoFocoComBateriaInicial() {
        Sessao s = new Sessao(CONFIG);

        assertEquals(Fase.FOCO, s.fase());
        assertFalse(s.rodando());
        assertFalse(s.aguardando());
        assertEquals(90, s.bateria(), DELTA);
        assertEquals(100, s.restante(), DELTA);
    }

    @Test
    void paradaOTempoNaoPassa() {
        Sessao s = new Sessao(CONFIG);

        s.avancarTempo(30);

        assertEquals(100, s.restante(), DELTA);
        assertEquals(90, s.bateria(), DELTA);
    }

    @Nested
    class Bateria {

        @Test
        void focoCompletoGastaOValorConfigurado() {
            Sessao s = iniciada();

            s.avancarTempo(100);

            assertEquals(40, s.bateria(), DELTA);
        }

        @Test
        void gastoEhProporcionalAoTempo() {
            Sessao s = iniciada();

            s.avancarTempo(50);

            assertEquals(65, s.bateria(), DELTA);
        }

        @Test
        void pausaCurtaRecarregaOValorConfigurado() {
            Sessao s = iniciada();
            s.avancarTempo(100);
            s.avancar();

            s.avancarTempo(20);

            assertEquals(90, s.bateria(), DELTA);
        }

        @Test
        void bateriaNuncaFicaNegativa() {
            Configuracao gastona = new Configuracao(10, 5, 5, 4, 20, 500, 10, 10, 3, 10);
            Sessao s = new Sessao(gastona);
            s.alternar();

            s.avancarTempo(10);

            assertEquals(0, s.bateria(), DELTA);
        }
    }

    @Nested
    class TrocaDeFase {

        @Test
        void fimDoFocoNaoComecaAPausaSozinho() {
            Sessao s = iniciada();

            List<Evento> eventos = s.avancarTempo(100);

            assertEquals(List.of(Evento.FOCO_TERMINOU), eventos);
            assertEquals(Fase.FOCO, s.fase());
            assertTrue(s.aguardando());
            assertFalse(s.rodando());
            assertEquals(1, s.ciclosConcluidos());
        }

        @Test
        void botaoPrincipalAguardandoComecaAPausa() {
            Sessao s = iniciada();
            s.avancarTempo(100);

            Evento evento = s.alternar();

            assertEquals(Evento.PAUSA_INICIOU, evento);
            assertEquals(Fase.PAUSA_CURTA, s.fase());
            assertTrue(s.rodando());
        }

        @Test
        void fimDaPausaEsperaParaVoltarAoFoco() {
            Sessao s = iniciada();
            s.avancarTempo(100);
            s.avancar();

            assertEquals(List.of(Evento.PAUSA_TERMINOU), s.avancarTempo(20));
            assertEquals(Evento.FOCO_INICIOU, s.avancar());
            assertEquals(Fase.FOCO, s.fase());
            assertEquals(100, s.restante(), DELTA);
        }

        @Test
        void avancarSemEstarAguardandoNaoFazNada() {
            Sessao s = iniciada();

            assertNull(s.avancar());
            assertEquals(Fase.FOCO, s.fase());
        }

        @Test
        void alternarPausaERetoma() {
            Sessao s = iniciada();

            s.alternar();
            assertFalse(s.rodando());
            s.alternar();
            assertTrue(s.rodando());
        }
    }

    @Nested
    class PausaLonga {

        @Test
        void vemDepoisDoNumeroConfiguradoDeFocos() {
            Sessao s = iniciada();
            completarFoco(s);
            s.avancar();
            assertEquals(Fase.PAUSA_CURTA, s.fase());
            s.avancarTempo(20);
            s.avancar();

            completarFoco(s);
            assertEquals(Fase.PAUSA_LONGA, s.proximaFase());
            s.avancar();

            assertEquals(Fase.PAUSA_LONGA, s.fase());
            assertEquals(40, s.restante(), DELTA);
            assertEquals(2, s.ciclosNaRodada());
        }

        @Test
        void rodadaRecomecaDepoisDaPausaLonga() {
            Sessao s = iniciada();
            for (int i = 0; i < 2; i++) {
                completarFoco(s);
                s.avancar();
                s.avancarTempo(40);
                s.avancar();
            }

            assertEquals(0, s.ciclosNaRodada());
            assertEquals(Fase.PAUSA_CURTA, s.proximaFase());
        }

        @Test
        void pausaLongaRecarregaMais() {
            Configuracao config = new Configuracao(100, 20, 40, 1, 90, 80, 10, 100, 3, 10);
            Sessao s = new Sessao(config);
            s.alternar();
            s.avancarTempo(100);
            s.avancar();

            s.avancarTempo(40);

            assertEquals(100, s.bateria(), DELTA);
        }
    }

    @Nested
    class HoraExtra {

        @Test
        void ignorarOFimDoFocoDrenaMaisRapido() {
            Sessao s = iniciada();
            s.avancarTempo(100);

            s.avancarTempo(5);

            // gasto normal 0.5/s, na hora extra 3x: 1.5/s * 5s
            assertEquals(40 - 7.5, s.bateria(), DELTA);
            assertEquals(5, s.horaExtra(), DELTA);
        }

        @Test
        void aqueceAteSuperaquecerEAvisaUmaVezSo() {
            Sessao s = iniciada();
            s.avancarTempo(100);

            assertEquals(List.of(), s.avancarTempo(5));
            assertEquals(0.5, s.temperatura(), DELTA);
            assertEquals(List.of(Evento.SUPERAQUECEU), s.avancarTempo(5));
            assertEquals(List.of(), s.avancarTempo(5));
            assertEquals(1, s.temperatura(), DELTA);
        }

        @Test
        void sobraDoIntervaloDepoisDoFimDoFocoJaContaComoHoraExtra() {
            Sessao s = iniciada();

            List<Evento> eventos = s.avancarTempo(104);

            assertEquals(List.of(Evento.FOCO_TERMINOU), eventos);
            assertEquals(4, s.horaExtra(), DELTA);
        }

        @Test
        void pausaEsfriaEZeraAHoraExtra() {
            Sessao s = iniciada();
            s.avancarTempo(100);
            s.avancarTempo(10);

            s.avancar();
            assertEquals(0, s.horaExtra(), DELTA);
            s.avancarTempo(10);

            assertEquals(0, s.temperatura(), DELTA);
        }

        @Test
        void fimDaPausaNaoContaHoraExtra() {
            Sessao s = iniciada();
            s.avancarTempo(100);
            s.avancar();
            s.avancarTempo(20);

            s.avancarTempo(30);

            assertEquals(0, s.horaExtra(), DELTA);
            assertEquals(0, s.temperatura(), DELTA);
        }
    }

    @Nested
    class Pular {

        @Test
        void focoPuladoNaoContaComoCiclo() {
            Sessao s = iniciada();

            assertEquals(Evento.FOCO_PULADO, s.pular());

            assertTrue(s.aguardando());
            assertEquals(0, s.ciclosConcluidos());
            assertEquals(0, s.ciclosNaRodada());
        }

        @Test
        void pularAPausaEncerraAPausa() {
            Sessao s = iniciada();
            completarFoco(s);
            s.avancar();

            assertEquals(Evento.PAUSA_TERMINOU, s.pular());
        }

        @Test
        void pularAguardandoNaoFazNada() {
            Sessao s = iniciada();
            completarFoco(s);

            assertNull(s.pular());
        }
    }

    @Nested
    class Estado {

        @Test
        void focadoComBateriaBoa() {
            assertEquals(EstadoRobo.FOCADO, iniciada().estado());
        }

        @Test
        void cansadoComBateriaBaixa() {
            Sessao s = iniciada();
            s.avancarTempo(100);

            assertEquals(EstadoRobo.CANSADO, s.estado());
        }

        @Test
        void horaExtraCurtaAindaNaoSuperaquece() {
            Sessao s = iniciada();
            s.avancarTempo(100);
            s.avancarTempo(4); // temperatura 0.4, abaixo do limite de 0.5

            assertEquals(EstadoRobo.CANSADO, s.estado());
        }

        @Test
        void superaquecimentoTemPrioridadeSobreOResto() {
            Sessao s = iniciada();
            s.avancarTempo(100);
            s.avancarTempo(6);

            assertEquals(EstadoRobo.SUPERAQUECENDO, s.estado());
        }

        @Test
        void carregandoDuranteAPausa() {
            Sessao s = iniciada();
            s.avancarTempo(100);
            s.avancar();

            assertEquals(EstadoRobo.CARREGANDO, s.estado());
        }

        @Test
        void criticoQuandoABateriaAcaba() {
            Configuracao config = new Configuracao(100, 20, 40, 4, 20, 10, 10, 10, 3, 1000);
            Sessao s = new Sessao(config);
            s.alternar();
            s.avancarTempo(100);

            assertEquals(10, s.bateria(), DELTA);
            assertEquals(EstadoRobo.CRITICO, s.estado());
        }
    }

    @Test
    void progressoDaFase() {
        Sessao s = iniciada();
        s.avancarTempo(25);

        assertEquals(0.25, s.progresso(), DELTA);
    }

    @Test
    void reiniciarVoltaTudoAoComeco() {
        Sessao s = iniciada();
        s.avancarTempo(100);
        s.avancarTempo(10);

        s.reiniciar();

        assertEquals(Fase.FOCO, s.fase());
        assertEquals(90, s.bateria(), DELTA);
        assertEquals(0, s.temperatura(), DELTA);
        assertEquals(0, s.ciclosConcluidos());
        assertFalse(s.aguardando());
    }

    @Test
    void configuracaoInvalidaEhRecusada() {
        assertThrows(IllegalArgumentException.class, () -> new Configuracao(0, 5, 5, 4, 90, 50, 50, 100, 3, 10));
        assertThrows(IllegalArgumentException.class, () -> new Configuracao(10, 5, 5, 0, 90, 50, 50, 100, 3, 10));
    }

    private static void completarFoco(Sessao s) {
        s.avancarTempo(s.restante());
    }
}
