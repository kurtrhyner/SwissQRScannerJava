package ch.qrscanner;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Executors;

public class App extends Application {

    private Button  btnScan, btnCopy;
    private Label   clipPreview, statusLabel;
    private TextArea dataDisplay;
    private ImageView imageView;

    private SwissQRData lastData = null;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Swiss QR-Rechnung Scanner");
        stage.setMinWidth(820);
        stage.setMinHeight(600);
        stage.setWidth(980);
        stage.setHeight(700);

        // ── Kopfzeile ────────────────────────────────────────────────────
        Label title = new Label("🇨🇭  Swiss QR-Rechnung Scanner");
        title.setStyle("-fx-font-size:16px; -fx-font-weight:bold;");

        btnScan = new Button("📷  Bereich auswählen");
        btnScan.setStyle(btnStyle());
        btnScan.setPrefHeight(38);
        btnScan.setOnAction(e -> startSelection(stage));

        btnCopy = new Button("📋  In Zwischenablage");
        btnCopy.setStyle(btnStyle());
        btnCopy.setPrefHeight(38);
        btnCopy.setDisable(true);
        btnCopy.setOnAction(e -> copyToClipboard());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(12, title, spacer, btnScan, btnCopy);
        header.setPadding(new Insets(0, 0, 8, 0));

        Separator sep = new Separator();

        // ── Clipboard-Vorschau ───────────────────────────────────────────
        Label clipLabel = new Label("Zwischenablage-Ausgabe:");
        clipLabel.setStyle("-fx-font-size:11px;");

        clipPreview = new Label("—");
        clipPreview.setStyle(
            "-fx-font-family:'Consolas','Courier New',monospace;" +
            "-fx-font-size:11px;" +
            "-fx-background-color:#f0f4fa;" +
            "-fx-border-color:#c0c8d8;" +
            "-fx-border-radius:4;" +
            "-fx-background-radius:4;" +
            "-fx-padding:6;"
        );
        clipPreview.setMaxWidth(Double.MAX_VALUE);
        clipPreview.setWrapText(true);

        // ── Splitter: Bild | Daten ───────────────────────────────────────
        // Links: Bildvorschau
        Label imgLabel = new Label("QR-Bildvorschau:");
        imgLabel.setStyle("-fx-font-size:11px;");

        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(260);
        imageView.setFitHeight(260);

        StackPane imgPane = new StackPane(imageView);
        imgPane.setStyle("-fx-background-color:#1a1a2e; -fx-border-color:#555; -fx-border-radius:4; -fx-background-radius:4;");
        imgPane.setMinSize(220, 220);
        VBox.setVgrow(imgPane, Priority.ALWAYS);

        VBox leftBox = new VBox(6, imgLabel, imgPane);
        VBox.setVgrow(leftBox, Priority.ALWAYS);

        // Rechts: Dateanzeige
        Label dataLabel = new Label("QR-Daten (vollständig):");
        dataLabel.setStyle("-fx-font-size:11px;");

        dataDisplay = new TextArea();
        dataDisplay.setEditable(false);
        dataDisplay.setStyle("-fx-font-family:'Consolas','Courier New',monospace; -fx-font-size:11px;");
        dataDisplay.setPromptText("Hier erscheinen alle Daten der QR-Rechnung …");
        VBox.setVgrow(dataDisplay, Priority.ALWAYS);

        VBox rightBox = new VBox(6, dataLabel, dataDisplay);
        VBox.setVgrow(rightBox, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane(leftBox, rightBox);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.28);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // ── Statusleiste ─────────────────────────────────────────────────
        statusLabel = new Label("Bereit. Klicke auf 'Bereich auswählen'.");
        statusLabel.setStyle("-fx-font-size:11px;");

        // ── Hauptlayout ──────────────────────────────────────────────────
        VBox root = new VBox(10,
            header, sep,
            clipLabel, clipPreview,
            splitPane,
            statusLabel
        );
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color:#f8f9fb;");

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    // ── Bereichsauswahl starten ──────────────────────────────────────────

    private void startSelection(Stage stage) {
        statusLabel.setText("Fenster minimiert – Bereich auf dem Bildschirm aufziehen …");
        stage.setIconified(true);

        // Kurze Pause damit das Fenster wirklich weg ist
        Executors.newSingleThreadExecutor().execute(() -> {
            try { Thread.sleep(350); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> openSelector(stage));
        });
    }

    private void openSelector(Stage stage) {
        ScreenSelector selector = new ScreenSelector(
            // onSelected
            crop -> Platform.runLater(() -> {
                stage.setIconified(false);
                stage.toFront();
                processImage(crop);
            }),
            // onCancelled
            () -> Platform.runLater(() -> {
                stage.setIconified(false);
                statusLabel.setText("Auswahl abgebrochen.");
            })
        );
        selector.start();
    }

    // ── QR-Code verarbeiten ──────────────────────────────────────────────

    private void processImage(BufferedImage img) {
        // Vorschaubild anzeigen
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            Image fxImg = new Image(new ByteArrayInputStream(baos.toByteArray()));
            imageView.setImage(fxImg);
        } catch (Exception ignored) {}

        statusLabel.setText("Dekodiere QR-Code …");

        // QR im Hintergrund dekodieren
        Executors.newSingleThreadExecutor().execute(() -> {
            String rawText = QRDecoder.decode(img);
            Platform.runLater(() -> {
                if (rawText == null) {
                    statusLabel.setText(
                        "❌ Kein QR-Code erkannt. Bitte grösseren Bereich wählen.");
                    dataDisplay.setText(
                        "Kein QR-Code gefunden.\n\n" +
                        "Tipps:\n" +
                        "• Bereich grosszügiger aufziehen\n" +
                        "• PDF-Zoom erhöhen und nochmal versuchen\n" +
                        "• Etwas Rand um den QR-Code lassen"
                    );
                    clipPreview.setText("—");
                    btnCopy.setDisable(true);
                    lastData = null;
                    return;
                }

                lastData = SwissQRData.parse(rawText);
                dataDisplay.setText(lastData.fullDisplay());

                String clip = lastData.valid ? lastData.clipboardLine() : rawText;
                clipPreview.setText(clip);
                btnCopy.setDisable(false);

                if (lastData.valid) {
                    copyToClipboard();
                    statusLabel.setText(
                        "✅ QR gelesen & kopiert!  IBAN: " + lastData.iban +
                        "  |  " + lastData.amount + " " + lastData.currency
                    );
                } else {
                    statusLabel.setText(
                        "⚠️  QR-Code gelesen, aber kein Swiss-QR-Format erkannt.");
                }
            });
        });
    }

    // ── Zwischenablage ───────────────────────────────────────────────────

    private void copyToClipboard() {
        if (lastData == null) return;
        String text = lastData.valid ? lastData.clipboardLine() : lastData.raw;
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
        statusLabel.setText("✅ In Zwischenablage kopiert!");
    }

    private String btnStyle() {
        return "-fx-background-color:#0078d4; -fx-text-fill:white; " +
               "-fx-border-radius:5; -fx-background-radius:5; -fx-padding:0 16;";
    }
}
