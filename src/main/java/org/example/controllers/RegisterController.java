package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.models.Guest;
import org.example.services.ServiceUser;

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

        if (username.isEmpty() || email.isEmpty() ||
                password.isEmpty() || confirm.isEmpty()) {
            showError("⚠️ Veuillez remplir tous les champs.");
            return;
        }

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

        service.ajouter(new Guest(username, email, password));

        lblMessage.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px;");
        lblMessage.setText("✅ Compte créé avec succès !");

        new Thread(() -> {
            try {
                Thread.sleep(1500);
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