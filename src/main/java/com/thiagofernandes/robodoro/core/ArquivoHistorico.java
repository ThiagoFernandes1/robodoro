package com.thiagofernandes.robodoro.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Guarda o histórico num arquivo texto simples, uma linha por dia: {@code 2026-09-30;4}.
 * Legível e editável à mão, sem dependência de biblioteca de JSON.
 */
public class ArquivoHistorico {

    private final Path arquivo;

    public ArquivoHistorico(Path arquivo) {
        this.arquivo = arquivo;
    }

    public static ArquivoHistorico padrao() {
        return new ArquivoHistorico(Path.of(System.getProperty("user.home"), ".robodoro", "historico.txt"));
    }

    /** Linhas corrompidas são ignoradas: perder um dia é melhor que o app não abrir. */
    public Historico carregar() {
        if (!Files.exists(arquivo)) {
            return new Historico();
        }
        String conteudo;
        try {
            // new String(...) troca bytes inválidos por U+FFFD em vez de lançar exceção como
            // readAllLines: um byte estragado custa só a linha dele
            conteudo = new String(Files.readAllBytes(arquivo), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler " + arquivo, e);
        }
        // o Bloco de Notas pode gravar um BOM no início, que grudaria na data da primeira linha
        if (conteudo.startsWith("﻿")) {
            conteudo = conteudo.substring(1);
        }

        Map<LocalDate, Integer> ciclos = new HashMap<>();
        for (String linha : conteudo.split("\\R")) {
            String[] partes = linha.strip().split(";");
            if (partes.length != 2) {
                continue;
            }
            try {
                int quantidade = Integer.parseInt(partes[1]);
                if (quantidade > 0) {
                    ciclos.merge(LocalDate.parse(partes[0]), quantidade, Integer::sum);
                }
            } catch (DateTimeParseException | NumberFormatException e) {
                // linha inválida: ignora
            }
        }
        return new Historico(ciclos);
    }

    public void salvar(Historico historico) {
        List<String> linhas = historico.comoMapa().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + ";" + e.getValue())
                .toList();
        try {
            Files.createDirectories(arquivo.toAbsolutePath().getParent());
            Path temporario = arquivo.resolveSibling(arquivo.getFileName() + ".tmp");
            Files.write(temporario, linhas, StandardCharsets.UTF_8);
            // Escreve num temporário e troca: se o app cair no meio, o arquivo antigo continua inteiro.
            Files.move(temporario, arquivo, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível salvar " + arquivo, e);
        }
    }
}
