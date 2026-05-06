package ch.qrscanner;

import javafx.application.Application;

/**
 * Separater Launcher – nötig damit der Fat-JAR ohne
 * JavaFX im Classpath korrekt startet (jpackage-Kompatibilität).
 */
public class Main {
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
