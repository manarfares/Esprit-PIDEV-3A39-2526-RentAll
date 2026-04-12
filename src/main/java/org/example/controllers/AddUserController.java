package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.models.*;
import org.example.services.ServiceUser;

public class AddUserController {

    @FXML private TextField     tfUsername;
    @FXML private TextField     tfEmail;
    @FXML private PasswordField pfPassword;
    @FXML private TextField     tfPhone;
    @FXML private ComboBox<String> cbRole;
    @FXML private Label         lblMessage;

    private ServiceUser          service;
    private AdminController      adminController;

    public void init(ServiceUser service, AdminController adminController) {
        this.service         = service;
        this.adminController = adminController;

        cbRole.setItems(FXCollections.observableArrayList(
                "ROLE_GUEST", "ROLE_HOST", "ROLE_ADMIN"
        ));
        cbRole.setValue("ROLE_GUEST");
    }

    @FXML
    public void handleAjouter() {
        String username = tfUsername.getText().trim();
        String email    = tfEmail.getText().trim();
        String password = pfPassword.getText().trim();
        String phone    = tfPhone.getText().trim();
        String role     = cbRole.getValue();

        // Validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showMessage("⚠️ Username, Email et Password sont obligatoires.", false);
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            showMessage("⚠️ Email invalide.", false);
            return;
        }

        if (password.length() < 6) {
            showMessage("⚠️ Mot de passe trop court (min 6).", false);
            return;
        }

        if (service.findByUsername(username) != null) {
            showMessage("⚠️ Username déjà pris.", false);
            return;
        }

        if (service.findByEmail(email) != null) {
            showMessage("⚠️ Email déjà utilisé.", false);
            return;
        }

        // Créer selon rôle
        User user;
        switch (role) {
            case "ROLE_ADMIN":
                user = new Admin(username, email, password);
                break;
            case "ROLE_HOST":
                user = new Host(username, email, password);
                break;
            default:
                user = new Guest(username, email, password);
                break;
        }

        if (!phone.isEmpty()) user.setPhone(phone);

        service.ajouter(user);
        showMessage("✅ Utilisateur ajouté !", true);

        // Rafraîchir la table admin
        adminController.filtrerUsers();

        // Fermer après 1s
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                javafx.application.Platform.runLater(this::handleFermer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleFermer() {
        Stage stage = (Stage) tfUsername.getScene().getWindow();
        stage.close();
    }

    private void showMessage(String msg, boolean success) {
        lblMessage.setStyle(success
                ? "-fx-text-fill: #27ae60; -fx-font-size: 12px;"
                : "-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        lblMessage.setText(msg);
    }
}