package com.thiagofernandes.robodoro.core;

/**
 * Parâmetros da sessão. Gasto e recarga de bateria são dados em pontos por fase completa,
 * então encurtar ou alongar as fases não desequilibra a mecânica.
 *
 * @param focoSeg                  duração do foco
 * @param pausaCurtaSeg            duração da pausa curta
 * @param pausaLongaSeg            duração da pausa longa
 * @param ciclosAtePausaLonga      a cada quantos focos concluídos vem uma pausa longa
 * @param bateriaInicial           carga no início da sessão (0 a 100)
 * @param gastoPorFoco             carga gasta num foco completo
 * @param recargaPorPausaCurta     carga recuperada numa pausa curta completa
 * @param recargaPorPausaLonga     carga recuperada numa pausa longa completa
 * @param multiplicadorHoraExtra   quantas vezes mais rápido a bateria drena na hora extra
 * @param horaExtraAteSuperaquecer segundos de hora extra até o robô superaquecer
 */
public record Configuracao(
        double focoSeg,
        double pausaCurtaSeg,
        double pausaLongaSeg,
        int ciclosAtePausaLonga,
        double bateriaInicial,
        double gastoPorFoco,
        double recargaPorPausaCurta,
        double recargaPorPausaLonga,
        double multiplicadorHoraExtra,
        double horaExtraAteSuperaquecer) {

    public static final Configuracao PADRAO =
            new Configuracao(25 * 60, 5 * 60, 15 * 60, 4, 90, 50, 50, 100, 3, 5 * 60);

    /** Fases de segundos, para ver o ciclo inteiro (inclusive o superaquecimento) rapidinho. */
    public static final Configuracao DEMO =
            new Configuracao(12, 6, 10, 4, 90, 50, 50, 100, 3, 4);

    public Configuracao {
        if (focoSeg <= 0 || pausaCurtaSeg <= 0 || pausaLongaSeg <= 0 || horaExtraAteSuperaquecer <= 0) {
            throw new IllegalArgumentException("Durações precisam ser positivas");
        }
        if (ciclosAtePausaLonga < 1) {
            throw new IllegalArgumentException("ciclosAtePausaLonga precisa ser pelo menos 1");
        }
    }

    public double duracaoDe(Fase fase) {
        return switch (fase) {
            case FOCO -> focoSeg;
            case PAUSA_CURTA -> pausaCurtaSeg;
            case PAUSA_LONGA -> pausaLongaSeg;
        };
    }

    /** Variação de bateria por segundo rodando na fase (negativa no foco). */
    double variacaoPorSegundo(Fase fase) {
        return switch (fase) {
            case FOCO -> -gastoPorFoco / focoSeg;
            case PAUSA_CURTA -> recargaPorPausaCurta / pausaCurtaSeg;
            case PAUSA_LONGA -> recargaPorPausaLonga / pausaLongaSeg;
        };
    }
}
