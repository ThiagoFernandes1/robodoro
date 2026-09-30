package com.thiagofernandes.robodoro.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Máquina de estados de uma sessão Pomodoro com o robô.
 *
 * <pre>
 * FOCO rodando --acaba--> FOCO aguardando (conta hora extra, esquenta)
 *     --avancar()--> PAUSA rodando (recarrega, esfria) --acaba-->
 * PAUSA aguardando --avancar()--> FOCO rodando ...
 * </pre>
 *
 * A troca de fase é sempre manual. A diferença está no fim do foco: enquanto o usuário não
 * começa a pausa, o tempo corre como hora extra — a bateria drena mais rápido e o robô esquenta.
 * Ignorar a pausa tem consequência visível.
 */
public class Sessao {

    public static final double BATERIA_CRITICA = 15;
    public static final double BATERIA_BAIXA = 40;
    public static final double TEMPERATURA_ALTA = 0.5;

    private final Configuracao config;

    private Fase fase;
    private double restante;
    private boolean rodando;
    private boolean aguardando;
    private int ciclosConcluidos;
    private int ciclosNaRodada;
    private double bateria;
    /** 0 = frio, 1 = superaquecido. */
    private double temperatura;
    private double horaExtra;

    public Sessao(Configuracao config) {
        this.config = config;
        reiniciar();
    }

    // --- controles ---

    /** Nova sessão: foco, bateria inicial, frio, ciclos zerados. */
    public void reiniciar() {
        fase = Fase.FOCO;
        restante = config.focoSeg();
        rodando = false;
        aguardando = false;
        ciclosConcluidos = 0;
        ciclosNaRodada = 0;
        bateria = config.bateriaInicial();
        temperatura = 0;
        horaExtra = 0;
    }

    /**
     * Botão principal: inicia/pausa a contagem; se a fase acabou, começa a próxima.
     *
     * @return o evento da transição, se houve uma
     */
    public Evento alternar() {
        if (aguardando) {
            return avancar();
        }
        rodando = !rodando;
        return null;
    }

    /** Começa a próxima fase a partir do estado aguardando. */
    public Evento avancar() {
        if (!aguardando) {
            return null;
        }
        // Calculada antes de sair do "aguardando": proximaFase() usa esse estado para saber
        // se o foco atual já entrou na contagem de ciclos.
        Fase seguinte = proximaFase();
        aguardando = false;
        rodando = true;
        horaExtra = 0;
        if (fase == Fase.FOCO) {
            fase = seguinte;
            restante = config.duracaoDe(fase);
            return Evento.PAUSA_INICIOU;
        }
        if (fase == Fase.PAUSA_LONGA) {
            ciclosNaRodada = 0;
        }
        fase = Fase.FOCO;
        restante = config.focoSeg();
        return Evento.FOCO_INICIOU;
    }

    /**
     * Encerra a fase atual agora. Um foco pulado não conta como ciclo concluído: não entra no
     * histórico nem aproxima a pausa longa.
     */
    public Evento pular() {
        if (aguardando) {
            return null;
        }
        return encerrarFase(false);
    }

    // --- simulação ---

    /**
     * Avança {@code segundos} de tempo real.
     *
     * @return os eventos que ocorreram nesse intervalo, em ordem
     */
    public List<Evento> avancarTempo(double segundos) {
        List<Evento> eventos = new ArrayList<>();
        if (segundos <= 0) {
            return eventos;
        }
        if (aguardando) {
            if (fase == Fase.FOCO) {
                acumularHoraExtra(segundos, eventos);
            } else {
                esfriar(segundos);
            }
            return eventos;
        }
        if (!rodando) {
            return eventos;
        }

        double passo = Math.min(segundos, restante);
        ajustarBateria(config.variacaoPorSegundo(fase) * passo);
        esfriar(passo);
        restante -= passo;
        if (restante <= 0) {
            eventos.add(encerrarFase(true));
            // O que sobrou do intervalo depois do fim do foco já é hora extra.
            if (segundos > passo && fase == Fase.FOCO) {
                acumularHoraExtra(segundos - passo, eventos);
            }
        }
        return eventos;
    }

    private void acumularHoraExtra(double segundos, List<Evento> eventos) {
        horaExtra += segundos;
        ajustarBateria(config.variacaoPorSegundo(Fase.FOCO) * config.multiplicadorHoraExtra() * segundos);
        boolean estavaSuperaquecido = temperatura >= 1;
        temperatura = Math.min(1, temperatura + segundos / config.horaExtraAteSuperaquecer());
        if (!estavaSuperaquecido && temperatura >= 1) {
            eventos.add(Evento.SUPERAQUECEU);
        }
    }

    /** Esfria em meia pausa curta: uma pausa sempre resolve o superaquecimento. */
    private void esfriar(double segundos) {
        temperatura = Math.max(0, temperatura - segundos / (config.pausaCurtaSeg() / 2));
    }

    private void ajustarBateria(double delta) {
        bateria = Math.clamp(bateria + delta, 0, 100);
    }

    private Evento encerrarFase(boolean completa) {
        rodando = false;
        aguardando = true;
        restante = 0;
        if (fase == Fase.FOCO) {
            if (!completa) {
                return Evento.FOCO_PULADO;
            }
            ciclosConcluidos++;
            ciclosNaRodada++;
            return Evento.FOCO_TERMINOU;
        }
        return Evento.PAUSA_TERMINOU;
    }

    // --- consultas ---

    public EstadoRobo estado() {
        if (temperatura >= TEMPERATURA_ALTA) {
            return EstadoRobo.SUPERAQUECENDO;
        }
        if (fase.isPausa() && rodando) {
            return EstadoRobo.CARREGANDO;
        }
        if (bateria <= BATERIA_CRITICA) {
            return EstadoRobo.CRITICO;
        }
        if (bateria <= BATERIA_BAIXA) {
            return EstadoRobo.CANSADO;
        }
        return fase == Fase.FOCO ? EstadoRobo.FOCADO : EstadoRobo.DESCANSADO;
    }

    /** Fração já decorrida da fase atual (0 a 1). */
    public double progresso() {
        return Math.clamp(1 - restante / config.duracaoDe(fase), 0, 1);
    }

    /** Fase que vem depois desta, considerando a pausa longa. */
    public Fase proximaFase() {
        if (fase.isPausa()) {
            return Fase.FOCO;
        }
        int ciclosAoTerminar = aguardando ? ciclosNaRodada : ciclosNaRodada + 1;
        return ciclosAoTerminar >= config.ciclosAtePausaLonga() ? Fase.PAUSA_LONGA : Fase.PAUSA_CURTA;
    }

    /** Focos concluídos desde a última pausa longa (0 até ciclosAtePausaLonga). */
    public int ciclosNaRodada() {
        return ciclosNaRodada;
    }

    public Configuracao config() {
        return config;
    }

    public Fase fase() {
        return fase;
    }

    public double restante() {
        return restante;
    }

    public boolean rodando() {
        return rodando;
    }

    public boolean aguardando() {
        return aguardando;
    }

    public int ciclosConcluidos() {
        return ciclosConcluidos;
    }

    public double bateria() {
        return bateria;
    }

    public double temperatura() {
        return temperatura;
    }

    public double horaExtra() {
        return horaExtra;
    }
}
