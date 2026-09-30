package com.thiagofernandes.robodoro.ui;

import com.thiagofernandes.robodoro.core.ArquivoHistorico;
import com.thiagofernandes.robodoro.core.Configuracao;
import com.thiagofernandes.robodoro.core.Evento;
import com.thiagofernandes.robodoro.core.Fase;
import com.thiagofernandes.robodoro.core.FormatoTempo;
import com.thiagofernandes.robodoro.core.Historico;
import com.thiagofernandes.robodoro.core.Sessao;
import com.thiagofernandes.robodoro.ui.Bipador.Melodia;
import com.thiagofernandes.robodoro.visual.Aparencia;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Janela flutuante, sem moldura e com fundo transparente, sempre por cima das outras.
 * Em repouso mostra só o robô e o tempo; com o mouse em cima, abre o painel de controle.
 *
 * <p>Argumentos: {@code --demo} (fases de segundos) e {@code --capturas=<pasta>} (gera os PNGs do README e sai).
 */
public class RobodoroApp extends Application {

    private static final int ESCALA = 4;
    private static final double DURACAO_COMEMORACAO = 1.8;

    private Sessao sessao;
    private Historico historico;
    private ArquivoHistorico arquivoHistorico;
    private final Bipador bipador = new Bipador();

    private DesenhistaRobo desenhista;
    private double tempoAnimacao;
    private double comemoracaoRestante = -1;

    private final Label tempoCompacto = new Label();
    private final Label faseBadge = new Label();
    private final Label status = new Label();
    private final Label tempoGrande = new Label();
    private final Label rotuloBateria = new Label();
    private final ProgressBar barraBateria = new ProgressBar();
    private final Label rotuloTemperatura = new Label();
    private final ProgressBar barraTemperatura = new ProgressBar();
    private final HBox bolinhasCiclos = new HBox(4);
    private final Label rotuloHoje = new Label();
    private final Button botaoPrincipal = new Button();
    private final Button botaoSom = new Button();
    private VBox painel;

    private double arrastoX;
    private double arrastoY;
    private boolean arrastou;

    @Override
    public void start(Stage stage) {
        List<String> args = getParameters().getRaw();
        boolean demo = args.contains("--demo");
        String capturas = args.stream().filter(a -> a.startsWith("--capturas=")).findFirst()
                .map(a -> a.substring("--capturas=".length())).orElse(null);

        sessao = new Sessao(demo ? Configuracao.DEMO : Configuracao.PADRAO);
        arquivoHistorico = ArquivoHistorico.padrao();
        historico = capturas != null ? new Historico() : carregarHistorico();

        HBox raiz = montarInterface();
        Scene cena = new Scene(raiz, Color.TRANSPARENT);
        cena.getStylesheets().add(getClass().getResource("robodoro.css").toExternalForm());

        if (capturas != null) {
            new Capturas(this, raiz).gerar(Path.of(capturas));
            Platform.exit();
            return;
        }

        configurarTeclado(cena);
        configurarJanela(stage, cena, raiz);
        iniciarLoop();
    }

    // --- montagem ---

    private HBox montarInterface() {
        desenhista = new DesenhistaRobo(ESCALA);
        tempoCompacto.getStyleClass().add("tempo-compacto");

        VBox roboBox = new VBox(-6, desenhista.canvas(), tempoCompacto);
        roboBox.setAlignment(Pos.TOP_CENTER);
        roboBox.getStyleClass().add("robo");

        faseBadge.getStyleClass().add("badge");
        status.getStyleClass().add("status");
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox topo = new HBox(8, faseBadge, espaco, status);
        topo.setAlignment(Pos.CENTER_LEFT);

        tempoGrande.getStyleClass().add("tempo-grande");
        rotuloBateria.getStyleClass().add("rotulo");
        rotuloTemperatura.getStyleClass().add("rotulo");
        barraBateria.setMaxWidth(Double.MAX_VALUE);
        barraTemperatura.setMaxWidth(Double.MAX_VALUE);
        barraTemperatura.getStyleClass().add("barra-temperatura");
        rotuloHoje.getStyleClass().add("rotulo");

        HBox linhaCiclos = new HBox(8, bolinhasCiclos, rotuloHoje);
        linhaCiclos.setAlignment(Pos.CENTER_LEFT);

        botaoPrincipal.getStyleClass().add("principal");
        botaoPrincipal.setMaxWidth(Double.MAX_VALUE);
        botaoPrincipal.setOnAction(e -> tratar(sessao.alternar()));

        Button pular = botao("pular", () -> tratar(sessao.pular()));
        Button reiniciar = botao("reiniciar", this::reiniciar);
        botaoSom.setOnAction(e -> alternarSom());
        botaoSom.getStyleClass().add("secundario");
        Button sair = botao("sair", Platform::exit);
        HBox secundarios = new HBox(4, pular, reiniciar, botaoSom, sair);

        painel = new VBox(6, topo, tempoGrande, rotuloBateria, barraBateria, rotuloTemperatura, barraTemperatura,
                linhaCiclos, botaoPrincipal, secundarios);
        painel.getStyleClass().add("painel");
        painel.setPrefWidth(250);

        HBox raiz = new HBox(4, roboBox, painel);
        raiz.setAlignment(Pos.CENTER_LEFT);
        raiz.getStyleClass().add("raiz");
        atualizarInterface();
        return raiz;
    }

    private Button botao(String texto, Runnable acao) {
        Button b = new Button(texto);
        b.getStyleClass().add("secundario");
        b.setOnAction(e -> acao.run());
        return b;
    }

    private void configurarJanela(Stage stage, Scene cena, HBox raiz) {
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        stage.setTitle("Robodoro");
        stage.setScene(cena);

        // Painel começa escondido; aparece com o mouse em cima e some pouco depois de sair.
        painel.setOpacity(0);
        painel.setMouseTransparent(true);
        PauseTransition atrasoParaEsconder = new PauseTransition(Duration.millis(700));
        atrasoParaEsconder.setOnFinished(e -> mostrarPainel(false));
        raiz.setOnMouseEntered(e -> {
            atrasoParaEsconder.stop();
            mostrarPainel(true);
        });
        raiz.setOnMouseExited(e -> atrasoParaEsconder.playFromStart());

        configurarArrasto(stage, (Region) desenhista.canvas().getParent());
        desenhista.canvas().setOnContextMenuRequested(e ->
                menuDeContexto().show(desenhista.canvas(), e.getScreenX(), e.getScreenY()));

        stage.show();
        Rectangle2D tela = Screen.getPrimary().getVisualBounds();
        stage.setX(tela.getMaxX() - stage.getWidth() - 24);
        stage.setY(tela.getMaxY() - stage.getHeight() - 24);
        stage.setOnCloseRequest(e -> Platform.exit());
    }

    private void mostrarPainel(boolean visivel) {
        painel.setMouseTransparent(!visivel);
        FadeTransition fade = new FadeTransition(Duration.millis(180), painel);
        fade.setToValue(visivel ? 1 : 0);
        fade.play();
    }

    /** Arrastar o robô move a janela; clicar sem arrastar é o botão principal. */
    private void configurarArrasto(Stage stage, Region alvo) {
        alvo.setOnMousePressed(e -> {
            arrastoX = stage.getX() - e.getScreenX();
            arrastoY = stage.getY() - e.getScreenY();
            arrastou = false;
        });
        alvo.setOnMouseDragged(e -> {
            double novoX = e.getScreenX() + arrastoX;
            double novoY = e.getScreenY() + arrastoY;
            if (Math.abs(novoX - stage.getX()) + Math.abs(novoY - stage.getY()) > 3) {
                arrastou = true;
            }
            stage.setX(novoX);
            stage.setY(novoY);
        });
        alvo.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && !arrastou) {
                tratar(sessao.alternar());
            }
        });
    }

    private ContextMenu menuDeContexto() {
        MenuItem principal = new MenuItem(botaoPrincipal.getText());
        principal.setOnAction(e -> tratar(sessao.alternar()));
        MenuItem pular = new MenuItem("Pular fase");
        pular.setOnAction(e -> tratar(sessao.pular()));
        MenuItem reiniciar = new MenuItem("Reiniciar sessão");
        reiniciar.setOnAction(e -> reiniciar());
        MenuItem som = new MenuItem(bipador.mudo() ? "Ligar som" : "Desligar som");
        som.setOnAction(e -> alternarSom());
        MenuItem sair = new MenuItem("Sair");
        sair.setOnAction(e -> Platform.exit());
        return new ContextMenu(principal, pular, reiniciar, som, new SeparatorMenuItem(), sair);
    }

    private void configurarTeclado(Scene cena) {
        cena.setOnKeyPressed(e -> {
            KeyCode tecla = e.getCode();
            switch (tecla) {
                case SPACE, ENTER -> tratar(sessao.alternar());
                case S -> tratar(sessao.pular());
                case R -> reiniciar();
                case M -> alternarSom();
                case ESCAPE -> Platform.exit();
                default -> {
                }
            }
        });
    }

    // --- ciclo de vida ---

    private void iniciarLoop() {
        new AnimationTimer() {
            private long anterior = -1;

            @Override
            public void handle(long agora) {
                double dt = anterior < 0 ? 0 : (agora - anterior) / 1e9;
                anterior = agora;
                sessao.avancarTempo(dt).forEach(RobodoroApp.this::tratar);
                avancarAnimacao(dt);
                atualizarInterface();
            }
        }.start();
    }

    void avancarAnimacao(double dt) {
        tempoAnimacao += dt;
        if (comemoracaoRestante >= 0) {
            comemoracaoRestante -= dt;
            if (comemoracaoRestante < 0) {
                comemoracaoRestante = -1;
            }
        }
    }

    private void tratar(Evento evento) {
        if (evento == null) {
            atualizarInterface();
            return;
        }
        switch (evento) {
            case FOCO_TERMINOU -> {
                historico.registrarCiclo(LocalDate.now());
                salvarHistorico();
                bipador.tocar(Melodia.FIM_DO_FOCO);
            }
            case FOCO_PULADO -> bipador.tocar(Melodia.FIM_DA_PAUSA);
            case PAUSA_INICIOU -> {
                comemoracaoRestante = DURACAO_COMEMORACAO;
                bipador.tocar(Melodia.INICIO_DA_PAUSA);
            }
            case PAUSA_TERMINOU -> bipador.tocar(Melodia.FIM_DA_PAUSA);
            case FOCO_INICIOU -> bipador.tocar(Melodia.INICIO_DO_FOCO);
            case SUPERAQUECEU -> bipador.tocar(Melodia.SUPERAQUECEU);
        }
        atualizarInterface();
    }

    private void reiniciar() {
        sessao.reiniciar();
        comemoracaoRestante = -1;
        atualizarInterface();
    }

    private void alternarSom() {
        bipador.alternarMudo();
        atualizarInterface();
    }

    private Historico carregarHistorico() {
        try {
            return arquivoHistorico.carregar();
        } catch (UncheckedIOException e) {
            System.err.println("Histórico indisponível, começando do zero: " + e.getMessage());
            return new Historico();
        }
    }

    private void salvarHistorico() {
        try {
            arquivoHistorico.salvar(historico);
        } catch (UncheckedIOException e) {
            System.err.println("Não foi possível salvar o histórico: " + e.getMessage());
        }
    }

    // --- desenho ---

    void atualizarInterface() {
        boolean comemorando = comemoracaoRestante >= 0;
        double progressoComemoracao = comemorando ? 1 - comemoracaoRestante / DURACAO_COMEMORACAO : -1;
        Aparencia aparencia = Aparencia.de(sessao, comemorando);
        desenhista.desenhar(new DesenhistaRobo.Quadro(aparencia, tempoAnimacao, progressoComemoracao, sessao.aguardando()));

        boolean horaExtra = sessao.aguardando() && sessao.fase() == Fase.FOCO;
        String tempo = horaExtra ? FormatoTempo.decorrido(sessao.horaExtra()) : FormatoTempo.mmss(sessao.restante());
        tempoCompacto.setText(tempo);
        tempoGrande.setText(tempo);
        alternarClasse(tempoCompacto, "hora-extra", horaExtra);
        alternarClasse(tempoGrande, "hora-extra", horaExtra);

        faseBadge.setText(sessao.fase().rotulo());
        faseBadge.getStyleClass().removeAll("foco", "pausa");
        faseBadge.getStyleClass().add(sessao.fase().isPausa() ? "pausa" : "foco");
        status.setText(textoStatus(horaExtra));

        rotuloBateria.setText("bateria %d%%".formatted(Math.round(sessao.bateria())));
        barraBateria.setProgress(sessao.bateria() / 100);
        barraBateria.setStyle("-fx-accent: %s;".formatted(css(aparencia.corBateria())));
        rotuloTemperatura.setText("temperatura %s".formatted(textoTemperatura()));
        barraTemperatura.setProgress(sessao.temperatura());

        atualizarBolinhas();
        int hoje = historico.ciclosEm(LocalDate.now());
        int sequencia = historico.sequenciaDeDias(LocalDate.now());
        rotuloHoje.setText(sequencia > 1 ? "hoje %d · %d dias seguidos".formatted(hoje, sequencia) : "hoje %d".formatted(hoje));

        botaoPrincipal.setText(textoBotaoPrincipal());
        alternarClasse(botaoPrincipal, "chamativo", sessao.aguardando());
        botaoSom.setText(bipador.mudo() ? "mudo" : "som");
    }

    private void atualizarBolinhas() {
        int total = sessao.config().ciclosAtePausaLonga();
        if (bolinhasCiclos.getChildren().size() != total) {
            bolinhasCiclos.getChildren().clear();
            for (int i = 0; i < total; i++) {
                Region bolinha = new Region();
                bolinha.getStyleClass().add("bolinha");
                bolinhasCiclos.getChildren().add(bolinha);
            }
        }
        for (int i = 0; i < total; i++) {
            alternarClasse(bolinhasCiclos.getChildren().get(i), "cheia", i < sessao.ciclosNaRodada());
        }
    }

    private String textoStatus(boolean horaExtra) {
        if (horaExtra) {
            return sessao.horaExtra() < 1 ? "fim do foco!" : "hora extra!";
        }
        if (sessao.aguardando()) {
            return "pausa acabou";
        }
        if (sessao.rodando()) {
            return sessao.fase().isPausa() ? "carregando" : "trabalhando";
        }
        return "pausado";
    }

    private String textoTemperatura() {
        double t = sessao.temperatura();
        if (t >= 1) {
            return "SUPERAQUECIDO";
        }
        if (t >= Sessao.TEMPERATURA_ALTA) {
            return "quente";
        }
        return t > 0 ? "morna" : "normal";
    }

    private String textoBotaoPrincipal() {
        if (sessao.aguardando()) {
            if (sessao.fase() == Fase.FOCO) {
                return sessao.proximaFase() == Fase.PAUSA_LONGA ? "começar pausa longa" : "começar pausa";
            }
            return "voltar ao foco";
        }
        if (sessao.rodando()) {
            return "pausar";
        }
        return sessao.fase() == Fase.FOCO && sessao.progresso() == 0 ? "iniciar foco" : "retomar";
    }

    private static void alternarClasse(javafx.scene.Node no, String classe, boolean ativa) {
        if (ativa && !no.getStyleClass().contains(classe)) {
            no.getStyleClass().add(classe);
        } else if (!ativa) {
            no.getStyleClass().remove(classe);
        }
    }

    private static String css(int rgb) {
        return "#%06x".formatted(rgb);
    }

    // --- acesso para o gerador de capturas ---

    Sessao sessao() {
        return sessao;
    }

    VBox painel() {
        return painel;
    }

    void definirTempoAnimacao(double t) {
        tempoAnimacao = t;
    }

    void definirComemoracao(double restante) {
        comemoracaoRestante = restante;
    }

    Historico historico() {
        return historico;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
