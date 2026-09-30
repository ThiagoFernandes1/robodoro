module com.thiagofernandes.robodoro {
    requires javafx.controls;
    // javax.sound (bipes sintetizados) e javax.imageio (gerar as capturas do README)
    requires java.desktop;

    exports com.thiagofernandes.robodoro.ui to javafx.graphics;
    // O JavaFX lê a folha de estilo que fica neste pacote.
    opens com.thiagofernandes.robodoro.ui to javafx.graphics;
}
