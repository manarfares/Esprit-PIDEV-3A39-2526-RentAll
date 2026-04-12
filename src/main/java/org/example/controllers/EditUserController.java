package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.models.User;
import org.example.services.ServiceUser;

public class EditUserController {

    @FXML private TextField     tfUsername;
    @FXML private TextField     tfEmail;
    @FXML private TextField     tfPhone;
    @FXML private ComboBox<String> cbRole;
    @FXML private ComboBox<String> cbStatus;
    @FXML private Label         lblMessage;

    private ServiceUser     service;
    private AdminController adminController;
    private User            selectedUser;

    public void init(ServiceUser service, AdminController adminController,
                     User selectedUser) {
        this.service         = service;
        this.adminController = adminController;
        this.selectedUser    = selectedUser;

        // Remplir les champs
        tfUsername.setText(selectedUser.getUsername());
        tfEmail.setText(selectedUser.getEmail());
        tfPhone.setText(selectedUser.getPhone() != null
                ? selectedUser.getPhone() : "");

        // Rôles
        cbRole.setItems(FXCollections.observableArrayList(
                "ROLE_GUEST", "ROLE_HOST",
                "ROLE_HOST_PENDING", "ROLE_ADMIN"
        ));
        cbRole.setValue(selectedUser.getRole());

        // Statuts
        cbStatus.setItems(FXCollections.observableArrayList(
                "ACTIVE", "INACTIVE", "BANNED"
        ));
        cbStatus.setValue(selectedUser.getStatus());
    }

    @FXML
    public void handleModifier() {
        String username = tfUsername.getText().trim();
        String email    = tfEmail.getText().trim();
        String phone    = tfPhone.getText().trim();
        String role     = cbRole.getValue();
        String status   = cbStatus.getValue();

        // Validation
        if (username.isEmpty() || email.isEmpty()) {
            showMessage("⚠️ Username et Email sont obligatoires.", false);
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            showMessage("⚠️ Email invalide.", false);
            return;
        }

        // Mettre à jour l'objet
        selectedUser.setUsername(username);
        selectedUser.setEmail(email);
        selectedUser.setPhone(phone.isEmpty() ? null : phone);
        selectedUser.setStatus(status);

        // Modifier profil
        service.modifier(selectedUser);

        // Modifier rôle si changé
        if (!selectedUser.getRole().equals(role)) {
            service.updateRole(selectedUser.getId(), role);
        }

        showMessage("✅ Utilisateur modifié !", true);

        // Rafraîchir table admin
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