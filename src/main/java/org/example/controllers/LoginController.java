package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.models.User;
import org.example.services.FaceVerificationService;
import org.example.services.OAuthService;
import org.example.services.ServiceUser;
import org.example.utils.FaceCaptureDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LoginController {

    @FXML private TextField     tfUsername;
    @FXML private PasswordField pfPassword;
    @FXML private Label         lblErreur;

    private final ServiceUser             service     = new ServiceUser();
    private final OAuthService            oauth       = new OAuthService();
    private final FaceVerificationService faceService = new FaceVerificationService();

    @FXML
    public void handleLogin() {
        String username = tfUsername.getText().trim();
        String password = pfPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            lblErreur.setText("⚠️ Veuillez remplir tous les champs.");
            return;
        }

        User user = service.login(username, password);

        if (user == null) {
            lblErreur.setText("❌ Identifiant ou mot de passe incorrect.");
            return;
        }

        finishLogin(user);
    }

    /** Connexion par reconnaissance faciale (selfie webcam ou upload). */
    @FXML
    public void handleFaceLogin() {
        var owner = lblErreur.getScene().getWindow();
        File probe = FaceCaptureDialog.show(owner, "Connexion faciale - RentAll");

        if (probe == null) {
            // Annulé par l'utilisateur
            return;
        }

        lblErreur.setStyle("-fx-text-fill: #2980b9; -fx-font-size: 11px;");
        lblErreur.setText("⏳ Analyse du visage en cours...");

        new Thread(() -> {
            try {
                // 1. Récupérer les candidats éligibles depuis la DB
                List<User> candidates = service.findFaceLoginCandidates();
                if (candidates.isEmpty()) {
                    Platform.runLater(() -> {
                        lblErreur.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
                        lblErreur.setText("❌ Aucun compte avec photo de référence enregistrée.");
                    });
                    return;
                }

                // 2. Construire la liste pour Python (résolution des chemins absolus)
                List<FaceVerificationService.FaceCandidate> payload = new ArrayList<>();
                for (User u : candidates) {
                    String selfieCol = service.getSelfieImagePath(u.getId());
                    String absPath   = faceService.resolveAbsolutePath(selfieCol);
                    if (absPath != null && new File(absPath).isFile()) {
                        payload.add(new FaceVerificationService.FaceCandidate(u.getId(), absPath));
                    }
                }
                if (payload.isEmpty()) {
                    Platform.runLater(() -> {
                        lblErreur.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
                        lblErreur.setText("❌ Photos de référence inaccessibles sur le disque.");
                    });
                    return;
                }

                // 3. Appel Python pour identifier le visage
                FaceVerificationService.IdentifyResult res = faceService.identifyUser(probe, payload);
                System.out.println("👤 Face identify → success=" + res.success
                        + ", matched=" + res.matched
                        + ", userId="  + res.userId
                        + ", distance=" + res.distance
                        + ", error=" + res.error);

                // 4. Cleanup du fichier temp si on l'a créé
                try { if (probe.getName().startsWith("face-capture-")) probe.delete(); } catch (Exception ignored) {}

                if (!res.success || !res.matched || res.userId == null) {
                    Platform.runLater(() -> {
                        lblErreur.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
                        lblErreur.setText("❌ " + (res.error != null
                                ? res.error
                                : "Visage non reconnu. Réessayez face à la caméra."));
                    });
                    return;
                }

                // 5. Connexion : récupérer le User + générer un session token
                User matched = service.findById(res.userId);
                if (matched == null) {
                    Platform.runLater(() -> {
                        lblErreur.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
                        lblErreur.setText("❌ Utilisateur introuvable en base.");
                    });
                    return;
                }

                String token = java.util.UUID.randomUUID().toString();
                service.saveTokenForUser(matched.getId(), token);
                matched.setSessionToken(token);

                Platform.runLater(() -> {
                    lblErreur.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 11px;");
                    lblErreur.setText("✅ Visage reconnu (distance " + res.distance + ") → "
                            + matched.getUsername());
                    finishLogin(matched);
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    lblErreur.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
                    lblErreur.setText("❌ Erreur reconnaissance : " + ex.getMessage());
                });
            }
        }, "face-login").start();
    }

    /** Action déclenchée par le bouton "Se connecter avec Google". */
    @FXML
    public void handleGoogleLogin() {
        lblErreur.setStyle("-fx-text-fill: #2980b9; -fx-font-size: 11px;");
        lblErreur.setText("⏳ Ouverture de Google dans le navigateur...");

        new Thread(() -> {
            try {
                OAuthService.OAuthUserInfo info = oauth.loginWithGoogle();
                User user = service.findOrCreateOAuthUser(info.email, info.name, info.picture, info.provider);

                Platform.runLater(() -> {
                    if (user == null) {
                        lblErreur.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
                        lblErreur.setText("❌ Compte Google refusé (banni ou erreur DB).");
                        return;
                    }
                    finishLogin(user);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    lblErreur.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
                    lblErreur.setText("❌ OAuth Google : " + e.getMessage());
                });
            }
        }, "oauth-google").start();
    }

    /** Action déclenchée par le bouton "Se connecter avec Facebook". */
    @FXML
    public void handleFacebookLogin() {
        lblErreur.setStyle("-fx-text-fill: #2980b9; -fx-font-size: 11px;");
        lblErreur.setText("⏳ Ouverture de Facebook dans le navigateur...");

        new Thread(() -> {
            try {
                OAuthService.OAuthUserInfo info = oauth.loginWithFacebook();
                User user = service.findOrCreateOAuthUser(info.email, info.name, info.picture, info.provider);

                Platform.runLater(() -> {
                    if (user == null) {
                        lblErreur.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
                        lblErreur.setText("❌ Compte Facebook refusé.");
                        return;
                    }
                    finishLogin(user);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    lblErreur.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
                    lblErreur.setText("❌ OAuth Facebook : " + e.getMessage());
                });
            }
        }, "oauth-facebook").start();
    }

    /** Étape commune après authentification (locale ou OAuth) : redirection vers la home. */
    private void finishLogin(User user) {
        if ("BANNED".equals(user.getStatus())) {
            lblErreur.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
            lblErreur.setText("🚫 Votre compte est banni.");
            return;
        }

        if ("INACTIVE".equals(user.getStatus())) {
            lblErreur.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
            lblErreur.setText("⚠️ Votre compte est inactif.");
            return;
        }

        if (!service.verifierToken(user.getId(), user.getSessionToken())) {
            lblErreur.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
            lblErreur.setText("❌ Erreur de session.");
            return;
        }

        try {
            String     fxml;
            FXMLLoader loader;
            Parent     root;

            switch (user.getRole()) {
                case "ROLE_ADMIN":
                    fxml   = "/dashboard_admin.fxml";
                    loader = new FXMLLoader(getClass().getResource(fxml));
                    root   = loader.load();
                    AdminController adminCtrl = loader.getController();
                    adminCtrl.setCurrentUser(user);
                    break;

                case "ROLE_HOST":
                case "ROLE_HOST_PENDING":
                    fxml   = "/home.fxml";
                    loader = new FXMLLoader(getClass().getResource(fxml));
                    root   = loader.load();
                    HomeController hostCtrl = loader.getController();
                    hostCtrl.setCurrentUser(user);
                    break;

                default:
                    fxml   = "/home.fxml";
                    loader = new FXMLLoader(getClass().getResource(fxml));
                    root   = loader.load();
                    HomeController guestCtrl = loader.getController();
                    guestCtrl.setCurrentUser(user);
                    break;
            }

            Stage stage = (Stage) tfUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            lblErreur.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
            lblErreur.setText("❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleGoRegister() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/register.fxml"));
            Stage stage = (Stage) tfUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleGoForgotPassword() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/forgot_password.fxml"));
            Stage stage = (Stage) tfUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
