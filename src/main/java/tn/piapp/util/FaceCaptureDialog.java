package org.example.utils;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Dialogue modal de capture webcam.
 * Utilise WebcamPanel (Swing) embarqué dans un SwingNode pour un flux fiable.
 */
public class FaceCaptureDialog {

    /** Affiche le dialog et retourne le fichier image capturé, ou null si annulé. */
    public static File show(Window owner, String title) {
        return new FaceCaptureDialog(owner, title).open();
    }

    private final Window owner;
    private final String title;
    private File         resultFile;
    private Webcam       webcam;
    private WebcamPanel  webcamPanel;
    private volatile boolean previewUsable;

    private FaceCaptureDialog(Window owner, String title) {
        this.owner = owner;
        this.title = title;
    }

    private File open() {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        if (owner != null) stage.initOwner(owner);
        stage.setTitle(title);
        stage.setOnCloseRequest(e -> stopWebcam());

        // ── Sélecteur de caméra (HD Camera / DroidCam / OBS / etc.) ─────
        ComboBox<Webcam> cameraPicker = new ComboBox<>();
        cameraPicker.setPromptText("Choisissez une caméra...");
        cameraPicker.setPrefWidth(320);
        cameraPicker.setStyle("-fx-font-size: 12px;");
        // Affiche le nom de la webcam dans la cellule
        cameraPicker.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(Webcam w, boolean empty) {
                super.updateItem(w, empty);
                setText(empty || w == null ? "" : w.getName());
            }
        });
        cameraPicker.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(Webcam w, boolean empty) {
                super.updateItem(w, empty);
                setText(empty || w == null ? "Choisissez une caméra..." : "📹 " + w.getName());
            }
        });

        Label cameraLabel = new Label("Caméra :");
        cameraLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        HBox pickerRow = new HBox(10, cameraLabel, cameraPicker);
        pickerRow.setAlignment(Pos.CENTER_LEFT);
        pickerRow.setPadding(new Insets(0, 0, 8, 0));

        // ── Conteneur du flux webcam (haut) ─────────────────
        SwingNode swingNode = new SwingNode();

        StackPane previewBox = new StackPane(swingNode);
        previewBox.setPrefSize(560, 380);
        previewBox.setStyle("-fx-background-color: black;");

        Label statusLabel = new Label("⏳ Recherche des caméras disponibles...");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

        VBox topBox = new VBox(8, pickerRow, previewBox, statusLabel);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(15));
        topBox.setStyle("-fx-background-color: #f5f7fa;");

        // ── Boutons (bas) ───────────────────────────────────
        Button btnCapture = new Button("📷  Prendre la photo");
        btnCapture.setStyle("-fx-background-color: linear-gradient(to right, #6C63FF, #9B59B6);" +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;" +
                "-fx-pref-width: 280px; -fx-pref-height: 44px; -fx-background-radius: 22;" +
                "-fx-cursor: hand;");
        btnCapture.setDisable(true);

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c;" +
                "-fx-font-size: 12px; -fx-cursor: hand;");

        VBox bottomBox = new VBox(8, btnCapture, btnCancel);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets(15, 20, 20, 20));

        BorderPane root = new BorderPane();
        root.setCenter(topBox);
        root.setBottom(bottomBox);
        root.setStyle("-fx-background-color: white;");

        Scene scene = new Scene(root, 620, 580);
        stage.setScene(scene);
        stage.setResizable(false);

        // ── Actions ─────────────────────────────────────────
        btnCancel.setOnAction(e -> {
            stopWebcam();
            stage.close();
        });

        btnCapture.setOnAction(e -> {
            if (webcam == null || !webcam.isOpen()) {
                statusLabel.setText("⚠️ Webcam non disponible");
                return;
            }
            if (!previewUsable) {
                statusLabel.setText("⚠️ Flux caméra indisponible (image noire/masquée). Fermez Teams/Zoom/OBS et réessayez.");
                statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
                return;
            }
            try {
                BufferedImage frame = webcam.getImage();
                if (frame == null) {
                    statusLabel.setText("⚠️ Impossible de capturer le frame, réessayez");
                    return;
                }
                if (isMostlyBlack(frame)) {
                    statusLabel.setText("⚠️ Image noire capturée. Vérifiez l'obturateur caméra/permissions Windows.");
                    statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
                    return;
                }
                File temp = File.createTempFile("face-capture-" + UUID.randomUUID(), ".jpg");
                ImageIO.write(frame, "jpg", temp);
                resultFile = temp;
                System.out.println("📸 Photo capturée : " + temp.getAbsolutePath());
                stopWebcam();
                stage.close();
            } catch (IOException ex) {
                statusLabel.setText("❌ Erreur écriture : " + ex.getMessage());
            }
        });

        // Lambda d'ouverture d'une caméra spécifique (utilisée à l'init et au changement)
        java.util.function.Consumer<Webcam> openCamera = (w) -> {
            if (w == null) return;
            SwingUtilities.invokeLater(() -> {
                try {
                    // Stop la précédente si besoin
                    if (webcamPanel != null) {
                        try { webcamPanel.stop(); } catch (Exception ignored) {}
                    }
                    if (webcam != null && webcam.isOpen() && webcam != w) {
                        try { webcam.close(); } catch (Exception ignored) {}
                    }

                    webcam = w;
                    webcam.setViewSize(WebcamResolution.VGA.getSize());
                    if (!webcam.isOpen() && !webcam.open(true)) {
                        Platform.runLater(() -> {
                            statusLabel.setText("❌ Impossible d'ouvrir " + w.getName()
                                    + ". Fermez Teams/Zoom/DroidCam et réessayez.");
                            statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
                            btnCapture.setDisable(true);
                        });
                        return;
                    }

                    webcamPanel = new WebcamPanel(webcam, false);
                    webcamPanel.setFPSDisplayed(false);
                    webcamPanel.setMirrored(true);
                    webcamPanel.setDrawMode(WebcamPanel.DrawMode.FIT);
                    webcamPanel.setBackground(java.awt.Color.BLACK);
                    webcamPanel.start();

                    final WebcamPanel finalPanel = webcamPanel;
                    Platform.runLater(() -> {
                        swingNode.setContent(finalPanel);
                        statusLabel.setText("⏳ Vérification du flux vidéo...");
                        statusLabel.setStyle("-fx-text-fill: #2980b9; -fx-font-size: 12px;");
                        btnCapture.setDisable(true);
                    });
                    System.out.println("✅ Webcam ouverte : " + w.getName());

                    Thread.sleep(400);
                    BufferedImage probe = webcam.getImage();
                    boolean usable = probe != null && !isMostlyBlack(probe);
                    previewUsable = usable;
                    Platform.runLater(() -> {
                        if (usable) {
                            statusLabel.setText("✅ " + w.getName() + " prête. Cliquez \"Prendre la photo\".");
                            statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px;");
                            btnCapture.setDisable(false);
                        } else {
                            statusLabel.setText("⚠️ Caméra détectée mais image noire/masquée. Vérifiez le cache caméra, permissions Windows, puis fermez les apps qui utilisent la webcam.");
                            statusLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 12px;");
                            btnCapture.setDisable(true);
                        }
                    });

                } catch (Throwable ex) {
                    ex.printStackTrace();
                    previewUsable = false;
                    Platform.runLater(() -> {
                        statusLabel.setText("❌ Erreur : " + ex.getMessage());
                        statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
                    });
                }
            });
        };

        // ── Initialisation : lister toutes les webcams + sélectionner la première ──
        SwingUtilities.invokeLater(() -> {
            try {
                List<Webcam> all = Webcam.getWebcams();
                System.out.println("🎥 Webcams détectées : " + all.size());
                for (int i = 0; i < all.size(); i++) {
                    System.out.println("   [" + i + "] " + all.get(i).getName());
                }

                Platform.runLater(() -> {
                    cameraPicker.setItems(FXCollections.observableArrayList(all));
                    if (all.isEmpty()) {
                        statusLabel.setText("❌ Aucune webcam détectée. Branchez DroidCam ou activez votre caméra.");
                        statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
                        return;
                    }
                    cameraPicker.getSelectionModel().selectFirst();
                });

                // Ouvre la première caméra automatiquement
                if (!all.isEmpty()) {
                    openCamera.accept(all.get(0));
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("❌ Webcam indisponible : " + ex.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
                });
            }
        });

        // Changement de caméra dans le ComboBox
        cameraPicker.setOnAction(e -> {
            Webcam selected = cameraPicker.getValue();
            if (selected != null && selected != webcam) {
                statusLabel.setText("⏳ Changement de caméra...");
                statusLabel.setStyle("-fx-text-fill: #2980b9; -fx-font-size: 12px;");
                openCamera.accept(selected);
            }
        });

        stage.showAndWait();
        return resultFile;
    }

    private void stopWebcam() {
        try {
            if (webcamPanel != null) {
                SwingUtilities.invokeLater(() -> {
                    try { webcamPanel.stop(); } catch (Exception ignored) {}
                });
            }
        } catch (Exception ignored) {}
        try {
            if (webcam != null && webcam.isOpen()) webcam.close();
        } catch (Exception ignored) {}
    }

    /**
     * Détecte un flux "noir" (caméra bloquée, cache fermé, frame vide).
     */
    private static boolean isMostlyBlack(BufferedImage img) {
        if (img == null) return true;
        int w = img.getWidth();
        int h = img.getHeight();
        if (w <= 0 || h <= 0) return true;

        long total = 0;
        long dark = 0;
        int stepX = Math.max(1, w / 40);
        int stepY = Math.max(1, h / 40);
        for (int y = 0; y < h; y += stepY) {
            for (int x = 0; x < w; x += stepX) {
                int rgb = img.getRGB(x, y);
                Color c = new Color(rgb);
                int lum = (int) (0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue());
                if (lum < 20) dark++;
                total++;
            }
        }
        if (total == 0) return true;
        return ((double) dark / (double) total) > 0.95d;
    }
}
