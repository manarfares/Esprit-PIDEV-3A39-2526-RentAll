package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.models.User;
import org.example.services.ServiceUser;

public class DeleteUserController {

    @FXML private Label lblConfirmation;
    @FXML private Label lblMessage;

    private ServiceUser     service;
    private AdminController adminController;
    private User            selectedUser;

    public void init(ServiceUser service, AdminController adminController,
                     User selectedUser) {
        this.service         = service;
        this.adminController = adminController;
        this.selectedUser    = selectedUser;

        lblConfirmation.setText(
                "⚠️ Voulez-vous vraiment supprimer :\n\n" +
                        "👤 " + selectedUser.getUsername() + "\n" +
                        "📧 " + selectedUser.getEmail() + "\n" +
                        "🎭 " + selectedUser.getRole()
        );
    }

    @FXML
    public void handleConfirmer() {
        if (selectedUser.getRole().equals("ROLE_ADMIN")) {
            showMessage("⚠️ Impossible de supprimer un Admin.", false);
            return;
        }

        service.supprimer(selectedUser);
        showMessage("✅ Utilisateur supprimé !", true);

        // Rafraîchir table admin
        adminController.filtrerUsers();

        // Fermer après 1s
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                javafx.application.Platform.runLater(this::handleAnnuler);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleAnnuler() {
        Stage stage = (Stage) lblConfirmation.getScene().getWindow();
        stage.close();
    }

    private void showMessage(String msg, boolean success) {
        lblMessage.setStyle(success
                ? "-fx-text-fill: #27ae60; -fx-font-size: 12px;"
                : "-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        lblMessage.setText(msg);
    }
}