package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.models.Guest;
import org.example.models.User;
import org.example.services.CaptchaService;
import org.example.services.EmailService;
import org.example.services.OAuthService;
import org.example.services.ServiceUser;
import org.example.utils.CaptchaWebView;
import org.example.utils.ValidationUtil;

import java.util.UUID;

public class RegisterController {

    @FXML private TextField     tfUsername;
    @FXML private TextField     tfEmail;
    @FXML private TextField     tfPhone;
    @FXML private PasswordField pfPassword;
    @FXML private PasswordField pfConfirm;
    @FXML private Label         lblMessage;
    @FXML private Label         lblErrUsername;
    @FXML private Label         lblErrEmail;
    @FXML private Label         lblErrPassword;
    @FXML private Label         lblErrConfirm;
    @FXML private Label         lblForce;

    private final ServiceUser    service  = new ServiceUser();
    private final CaptchaService captcha  = new CaptchaService();
    private final EmailService   emailSvc = new EmailService();
    private final OAuthService   oauth    = new OAuthService();

    @FXML
    public void initialize() {
        // Validation en temps réel username
        tfUsername.textProperty().addListener((obs, old, val) -> {
            String err = ValidationUtil.validerUsername(val);
            lblErrUsername.setText(err != null ? err : "✅");
            lblErrUsername.setStyle(err != null
                    ? "-fx-font-size: 10px; -fx-text-fill: #e74c3c;"
                    : "-fx-font-size: 10px; -fx-text-fill: #27ae60;");
        });

        // Validation en temps réel email
        tfEmail.textProperty().addListener((obs, old, val) -> {
            String err = ValidationUtil.validerEmail(val);
            lblErrEmail.setText(err != null ? err : "✅");
            lblErrEmail.setStyle(err != null
                    ? "-fx-font-size: 10px; -fx-text-fill: #e74c3c;"
                    : "-fx-font-size: 10px; -fx-text-fill: #27ae60;");
        });

        // Indicateur force mot de passe
        pfPassword.textProperty().addListener((obs, old, val) -> {
            int score = ValidationUtil.forcePassword(val);
            String label = ValidationUtil.labelForce(score);
            String color = ValidationUtil.colorForce(score);
            lblForce.setText(label);
            lblForce.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;" +
                    "-fx-text-fill: " + color + ";");
            String err = ValidationUtil.validerPassword(val);
            lblErrPassword.setText(err != null ? err : "✅");
            lblErrPassword.setStyle(err != null
                    ? "-fx-font-size: 10px; -fx-text-fill: #e74c3c;"
                    : "-fx-font-size: 10px; -fx-text-fill: #27ae60;");
        });

        // Confirmation password
        pfConfirm.textProperty().addListener((obs, old, val) -> {
            if (!val.equals(pfPassword.getText())) {
                lblErrConfirm.setText("⚠️ Les mots de passe ne correspondent pas.");
                lblErrConfirm.setStyle("-fx-font-size: 10px; -fx-text-fill: #e74c3c;");
            } else {
                lblErrConfirm.setText("✅");
                lblErrConfirm.setStyle("-fx-font-size: 10px; -fx-text-fill: #27ae60;");
            }
        });
    }

    @FXML
    public void handleRegister() {
        String username = tfUsername.getText().trim();
        String email    = tfEmail.getText().trim();
        String password = pfPassword.getText().trim();
        String confirm  = pfConfirm.getText().trim();

        // Validation finale
        String errUser = ValidationUtil.validerUsername(username);
        String errMail = ValidationUtil.validerEmail(email);
        String errPass = ValidationUtil.validerPassword(password);

        if (errUser != null) { showError(errUser); return; }
        if (errMail != null) { showError(errMail); return; }
        if (errPass != null) { showError(errPass); return; }

        if (!password.equals(confirm)) {
            showError("⚠️ Les mots de passe ne correspondent pas.");
            return;
        }

        if (service.findByUsername(username) != null) {
            showError("⚠️ Ce nom d'utilisateur est déjà pris.");
            return;
        }

        if (service.findByEmail(email) != null) {
            showError("⚠️ Cet email est déjà utilisé.");
            return;
        }

        // Ouverture du captcha JUSTE AVANT la soumission → token toujours frais
        var owner = lblMessage.getScene().getWindow();
        String freshToken = CaptchaWebView.openValidationDialog(owner);
        if (freshToken == null || freshToken.isBlank()) {
            showError("🛡️ Vérification anti-robot annulée. Réessayez.");
            return;
        }
        CaptchaService.VerifyResult captchaResult = captcha.verify(freshToken);
        if (!captchaResult.success) {
            String reason = captchaResult.errorMessage != null
                    ? captchaResult.errorMessage
                    : "Vérification échouée. Réessayez.";
            showError("❌ " + reason);
            return;
        }

        // Création du compte (inactif jusqu'à confirmation email)
        Guest guest = new Guest(username, email, password);
        if (tfPhone != null) guest.setPhone(tfPhone.getText().trim());
        service.ajouter(guest);
        User created = service.findByEmail(email);

        // Email de confirmation
        if (created != null) {
            String confirmToken = UUID.randomUUID().toString();
            service.saveConfirmationToken(created.getId(), confirmToken, 24);
            boolean sent = emailSvc.sendConfirmation(email, username, confirmToken);
            if (!sent) {
                lblMessage.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 12px;");
                lblMessage.setText("⚠️ Compte créé mais l'email n'a pas pu être envoyé.");
                return;
            }
        }

        lblMessage.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px;");
        lblMessage.setText("✅ Compte créé ! Vérifiez votre email pour l'activer.");

        new Thread(() -> {
            try {
                Thread.sleep(2500);
                javafx.application.Platform.runLater(this::handleGoLogin);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleGoLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) tfUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Inscription via Google OAuth (saute le captcha et l'email de confirmation). */
    @FXML
    public void handleGoogleSignup() {
        lblMessage.setStyle("-fx-text-fill: #2980b9; -fx-font-size: 12px;");
        lblMessage.setText("⏳ Ouverture de Google dans le navigateur...");

        new Thread(() -> {
            try {
                OAuthService.OAuthUserInfo info = oauth.loginWithGoogle();
                User user = service.findOrCreateOAuthUser(info.email, info.name, info.picture, info.provider);

                Platform.runLater(() -> {
                    if (user == null) {
                        lblMessage.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
                        lblMessage.setText("❌ Impossible de créer le compte Google (voir console).");
                        return;
                    }
                    lblMessage.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px;");
                    lblMessage.setText("✅ Connecté avec Google : " + info.email);
                    redirectToHome(user);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    lblMessage.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
                    lblMessage.setText("❌ OAuth Google : " + e.getMessage());
                });
            }
        }, "oauth-google-signup").start();
    }

    /** Inscription via Facebook OAuth. */
    @FXML
    public void handleFacebookSignup() {
        lblMessage.setStyle("-fx-text-fill: #2980b9; -fx-font-size: 12px;");
        lblMessage.setText("⏳ Ouverture de Facebook dans le navigateur...");

        new Thread(() -> {
            try {
                OAuthService.OAuthUserInfo info = oauth.loginWithFacebook();
                User user = service.findOrCreateOAuthUser(info.email, info.name, info.picture, info.provider);

                Platform.runLater(() -> {
                    if (user == null) {
                        lblMessage.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
                        lblMessage.setText("❌ Impossible de créer le compte Facebook.");
                        return;
                    }
                    lblMessage.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px;");
                    lblMessage.setText("✅ Connecté avec Facebook : " + info.email);
                    redirectToHome(user);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    lblMessage.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
                    lblMessage.setText("❌ OAuth Facebook : " + e.getMessage());
                });
            }
        }, "oauth-facebook-signup").start();
    }

    /** Redirection vers la home après inscription/connexion OAuth. */
    private void redirectToHome(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/home.fxml"));
            Parent root = loader.load();
            HomeController homeCtrl = loader.getController();
            homeCtrl.setCurrentUser(user);

            Stage stage = (Stage) tfUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            lblMessage.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
            lblMessage.setText("❌ Erreur redirection : " + e.getMessage());
        }
    }

    private void showError(String msg) {
        lblMessage.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        lblMessage.setText(msg);
    }
}
