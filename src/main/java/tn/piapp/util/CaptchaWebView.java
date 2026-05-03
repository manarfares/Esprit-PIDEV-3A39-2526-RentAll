package org.example.utils;

import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.example.services.AppConfig;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * WebView JavaFX hébergeant le widget Google reCAPTCHA v3 (invisible).
 *
 * reCAPTCHA v3 génère un token automatiquement, sans interaction utilisateur.
 * Le token est récupéré via JS puis vérifié côté serveur par {@link org.example.services.CaptchaService}.
 *
 * Usage :
 *   CaptchaWebView.TokenResult r = CaptchaWebView.getToken(ownerWindow);
 *   if (r.success) { // utiliser r.token }
 */
public class CaptchaWebView {

    // Délai max d'attente du token JS (ms)
    private static final int TOKEN_TIMEOUT_MS = 12_000;
    // Intervalle de polling JS (ms)
    private static final int POLL_INTERVAL_MS = 300;

    private static HttpServer sharedServer;
    private static int        sharedPort;
    private static String     cachedHtml;

    // =========================================================================
    // API publique principale
    // =========================================================================

    /**
     * Ouvre une boîte de dialogue modale avec le widget reCAPTCHA v3 (invisible).
     * L'analyse démarre automatiquement. L'utilisateur clique "Continuer" une fois
     * la vérification terminée (ou "Annuler").
     *
     * @param owner fenêtre parente (peut être null)
     * @return le token reCAPTCHA si succès, null si annulé ou erreur
     */
    public static String openValidationDialog(Window owner) {
        final String[] result = {null};

        // ---- WebView ----
        WebView webView = new WebView();
        webView.setPrefSize(360, 280);
        webView.setMinSize(360, 280);
        webView.setMaxSize(360, 280);
        webView.setContextMenuEnabled(false);

        // ---- Dialog ----
        Stage dialog = new Stage();
        if (owner != null) dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Vérification de sécurité");
        dialog.setResizable(false);

        // ---- En-tête ----
        Label lblTitle = new Label("Vérification anti-robot");
        lblTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label lblHint = new Label("Analyse automatique en cours. Aucune action requise.");
        lblHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        lblHint.setWrapText(true);

        VBox header = new VBox(5, lblTitle, lblHint);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(18, 20, 10, 20));
        header.setStyle("-fx-background-color: white;");

        // ---- Statut ----
        Label lblStatus = new Label("");
        lblStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        lblStatus.setWrapText(true);
        lblStatus.setMaxWidth(380);

        // ---- Boutons ----
        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle(
                "-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50;" +
                "-fx-font-size: 13px; -fx-pref-width: 130px; -fx-pref-height: 38px;" +
                "-fx-background-radius: 20; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> {
            result[0] = null;
            dialog.close();
        });

        Button btnContinue = new Button("Continuer");
        btnContinue.setDisable(true);
        btnContinue.setStyle(
                "-fx-background-color: linear-gradient(to right, #6C63FF, #9B59B6);" +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;" +
                "-fx-pref-width: 130px; -fx-pref-height: 38px;" +
                "-fx-background-radius: 20; -fx-cursor: hand;");
        btnContinue.setOnAction(e -> {
            try {
                Object tokenObj = webView.getEngine().executeScript("getCaptchaToken()");
                String token = tokenObj == null ? "" : tokenObj.toString().trim();
                if (token.isEmpty()) {
                    lblStatus.setText("⏳ Vérification encore en cours, patientez…");
                    return;
                }
                result[0] = token;
                dialog.close();
            } catch (Exception ex) {
                lblStatus.setText("❌ Erreur JS : " + ex.getMessage());
            }
        });

        HBox buttons = new HBox(14, btnCancel, btnContinue);
        buttons.setAlignment(Pos.CENTER);

        VBox footer = new VBox(8, lblStatus, buttons);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(12, 20, 18, 20));
        footer.setStyle("-fx-background-color: white; -fx-border-color: #ecf0f1; -fx-border-width: 1 0 0 0;");

        // ---- Layout ----
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: white;");
        root.setTop(header);
        root.setCenter(webView);
        root.setBottom(footer);

        // ---- Chargement WebView ----
        WebEngine engine = webView.getEngine();
        installPopupHandler(engine);

        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                System.out.println("✅ reCAPTCHA v3 chargé");
                // Polling : activer le bouton dès que le token est prêt
                startTokenPolling(engine, btnContinue, lblStatus);
            } else if (state == Worker.State.FAILED) {
                Platform.runLater(() ->
                    lblStatus.setText("❌ Impossible de charger reCAPTCHA. Vérifiez votre connexion."));
            }
        });

        try {
            ensureServerStarted();
            engine.load("http://localhost:" + sharedPort + "/recaptcha.html");
        } catch (Exception ex) {
            System.err.println("❌ Erreur démarrage serveur captcha : " + ex.getMessage());
            return null;
        }

        Scene scene = new Scene(root, 400, 420);
        dialog.setScene(scene);
        dialog.showAndWait();
        return result[0];
    }

    // =========================================================================
    // Polling JS pour détecter quand le token est prêt
    // =========================================================================

    private static void startTokenPolling(WebEngine engine, Button btnContinue, Label lblStatus) {
        long start = System.currentTimeMillis();

        Thread poller = new Thread(() -> {
            while (true) {
                try { Thread.sleep(POLL_INTERVAL_MS); } catch (InterruptedException e) { return; }

                long elapsed = System.currentTimeMillis() - start;

                Platform.runLater(() -> {
                    try {
                        Object readyObj = engine.executeScript("getCaptchaReady()");
                        boolean ready   = Boolean.TRUE.equals(readyObj)
                                || "true".equalsIgnoreCase(String.valueOf(readyObj));

                        Object errorObj = engine.executeScript("getCaptchaError()");
                        String error    = errorObj == null ? "" : errorObj.toString().trim();

                        if (ready) {
                            btnContinue.setDisable(false);
                            btnContinue.setStyle(
                                    "-fx-background-color: linear-gradient(to right, #6C63FF, #9B59B6);" +
                                    "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;" +
                                    "-fx-pref-width: 130px; -fx-pref-height: 38px;" +
                                    "-fx-background-radius: 20; -fx-cursor: hand; -fx-opacity: 1;");
                            lblStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
                            lblStatus.setText("✅ Vérification réussie. Cliquez sur Continuer.");
                        } else if (!error.isEmpty()) {
                            lblStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            lblStatus.setText("❌ Erreur reCAPTCHA : " + error);
                        } else if (elapsed > TOKEN_TIMEOUT_MS) {
                            lblStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #e67e22; -fx-font-weight: bold;");
                            lblStatus.setText("⚠️ Délai dépassé. Fermez et réessayez.");
                        }
                    } catch (Exception ignored) {}
                });

                // Arrêt du polling si prêt ou timeout
                try {
                    Object readyObj = engine.executeScript("getCaptchaReady()");
                    boolean ready   = Boolean.TRUE.equals(readyObj)
                            || "true".equalsIgnoreCase(String.valueOf(readyObj));
                    if (ready || System.currentTimeMillis() - start > TOKEN_TIMEOUT_MS) return;
                } catch (Exception ignored) {}
            }
        }, "captcha-poller");
        poller.setDaemon(true);
        poller.start();
    }

    // =========================================================================
    // Mini serveur HTTP local (requis pour que Google accepte "localhost")
    // =========================================================================

    private static synchronized void ensureServerStarted() throws Exception {
        if (sharedServer != null) return;

        InputStream in = CaptchaWebView.class.getResourceAsStream("/recaptcha.html");
        if (in == null) throw new IllegalStateException("recaptcha.html introuvable dans resources");

        String html     = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        String siteKey  = AppConfig.get("recaptcha.site.key");
        if (siteKey == null || siteKey.isBlank())
            throw new IllegalStateException("recaptcha.site.key vide dans config.properties");

        cachedHtml = html.replace("__SITE_KEY__", siteKey.trim());

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/recaptcha.html", ex -> {
            byte[] bytes = cachedHtml.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            ex.getResponseHeaders().set("Cache-Control", "no-cache");
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
        });
        server.setExecutor(null);
        server.start();
        sharedServer = server;
        sharedPort   = server.getAddress().getPort();
        System.out.println("🌐 Serveur captcha démarré → http://localhost:" + sharedPort);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { server.stop(0); } catch (Exception ignored) {}
        }));
    }

    // =========================================================================
    // Popup handler (pour les éventuels défis supplémentaires)
    // =========================================================================

    private static void installPopupHandler(WebEngine engine) {
        engine.setCreatePopupHandler(features -> {
            WebView popup = new WebView();
            popup.setContextMenuEnabled(false);
            installPopupHandler(popup.getEngine());

            Stage stage = new Stage();
            stage.setTitle("reCAPTCHA");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(popup, 520, 600));
            stage.setResizable(true);
            popup.getEngine().setOnVisibilityChanged(ev -> {
                if (!ev.getData()) stage.close();
            });
            stage.show();
            return popup.getEngine();
        });
    }
}
