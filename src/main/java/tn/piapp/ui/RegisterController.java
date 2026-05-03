package org.example.controllers;

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
import tn.piapp.model.Guest;
import tn.piapp.dao.ServiceUser;

public class RegisterController {

    @FXML private TextField     tfUsername;
    @FXML private TextField     tfEmail;
    @FXML private PasswordField pfPassword;
    @FXML private PasswordField pfConfirm;
    @FXML private Label         lblMessage;

    private ServiceUser service = new ServiceUser();

    @FXML
    public void handleRegister() {
        String username = tfUsername.getText().trim();
        String email    = tfEmail.getText().trim();
        String password = pfPassword.getText().trim();
        String confirm  = pfConfirm.getText().trim();

        if (name.isEmpty() || email.isEmpty() ||
                password.isEmpty() || confirm.isEmpty()) {
            showError("⚠️ Veuillez remplir tous les champs.");
            return;
        }
        // Validation finale
        String errUser = ValidationUtil.validerUsername(username);
        String errMail = ValidationUtil.validerEmail(email);
        String errPass = ValidationUtil.validerPassword(password);

        if (!email.contains("@") || !email.contains(".")) {
            showError("⚠️ Email invalide.");
            return;
        }

        if (password.length() < 6) {
            showError("⚠️ Mot de passe trop court (min 6 caractères).");
            return;
        }

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

        boolean success = service.ajouter(new Guest(name, email, password));
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

        if (success) {
            lblMessage.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px;");
            lblMessage.setText("✅ Compte créé avec succès !");
        lblMessage.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px;");
        lblMessage.setText("✅ Compte créé ! Vérifiez votre email pour l'activer.");

            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    javafx.application.Platform.runLater(this::handleGoLogin);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            showError("❌ Erreur système : création impossible (vérifiez les logs DB).");
        }
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
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        lblMessage.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        lblMessage.setText(msg);
    }
}