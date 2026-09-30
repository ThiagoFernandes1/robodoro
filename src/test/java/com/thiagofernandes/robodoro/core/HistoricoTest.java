package com.thiagofernandes.robodoro.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HistoricoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 9, 30);

    @Test
    void contaCiclosPorDia() {
        Historico h = new Historico();
        h.registrarCiclo(HOJE);
        h.registrarCiclo(HOJE);
        h.registrarCiclo(HOJE.minusDays(1));

        assertEquals(2, h.ciclosEm(HOJE));
        assertEquals(1, h.ciclosEm(HOJE.minusDays(1)));
        assertEquals(0, h.ciclosEm(HOJE.minusDays(2)));
    }

    @Test
    void sequenciaContaDiasSeguidos() {
        Historico h = new Historico();
        h.registrarCiclo(HOJE);
        h.registrarCiclo(HOJE.minusDays(1));
        h.registrarCiclo(HOJE.minusDays(2));
        h.registrarCiclo(HOJE.minusDays(4));

        assertEquals(3, h.sequenciaDeDias(HOJE));
    }

    @Test
    void diaDeHojeSemFocoAindaNaoQuebraASequencia() {
        Historico h = new Historico();
        h.registrarCiclo(HOJE.minusDays(1));
        h.registrarCiclo(HOJE.minusDays(2));

        assertEquals(2, h.sequenciaDeDias(HOJE));
    }

    @Test
    void semHistoricoASequenciaEhZero() {
        assertEquals(0, new Historico().sequenciaDeDias(HOJE));
    }

    @Test
    void salvaECarregaDoArquivo(@TempDir Path pasta) {
        ArquivoHistorico arquivo = new ArquivoHistorico(pasta.resolve("sub/historico.txt"));
        Historico h = new Historico();
        h.registrarCiclo(HOJE);
        h.registrarCiclo(HOJE);
        h.registrarCiclo(HOJE.minusDays(3));

        arquivo.salvar(h);
        Historico lido = arquivo.carregar();

        assertEquals(2, lido.ciclosEm(HOJE));
        assertEquals(1, lido.ciclosEm(HOJE.minusDays(3)));
    }

    @Test
    void arquivoInexistenteViraHistoricoVazio(@TempDir Path pasta) {
        Historico h = new ArquivoHistorico(pasta.resolve("nao-existe.txt")).carregar();

        assertEquals(0, h.ciclosEm(HOJE));
    }

    @Test
    void linhasCorrompidasSaoIgnoradas(@TempDir Path pasta) throws IOException {
        Path caminho = pasta.resolve("historico.txt");
        Files.write(caminho, List.of("2026-09-30;3", "lixo", "2026-13-40;2", "2026-09-29;abc", "", "2026-09-28;1"));

        Historico h = new ArquivoHistorico(caminho).carregar();

        assertEquals(3, h.ciclosEm(HOJE));
        assertEquals(1, h.ciclosEm(LocalDate.of(2026, 9, 28)));
        assertEquals(0, h.ciclosEm(LocalDate.of(2026, 9, 29)));
    }
}
