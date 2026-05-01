package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.models.User;
import org.example.services.ServiceUser;
import org.example.utils.IdleSessionManager;

public class GuestController {

    @FXML private Label  lblWelcome;
    @FXML private Label  lblRole;
    @FXML private Label  lblStatus;
    @FXML private Label  lblBadge;
    @FXML private Button btnDevenirHost;

    private ServiceUser service     = new ServiceUser();
    private User        currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;

        lblWelcome.setText("👋 Bienvenue, " + user.getUsername());
        lblRole.setText("Rôle : " + user.getRole());
        lblStatus.setText("Statut : " + user.getStatus());

        // Badge UNVERIFIED si HOST_PENDING
        if (user.getRole().equals("ROLE_HOST_PENDING")) {
            lblBadge.setText("⚠️ UNVERIFIED");
            lblBadge.setStyle(
                    "-fx-font-size: 10px; -fx-text-fill: white;" +
                            "-fx-background-color: #e67e22;" +
                            "-fx-padding: 3 10 3 10; -fx-background-radius: 10;" +
                            "-fx-font-weight: bold;"
            );
            btnDevenirHost.setText("⏳ Demande en attente...");
            btnDevenirHost.setDisable(true);
            btnDevenirHost.setStyle(
                    "-fx-background-color: #95a5a6; -fx-text-fill: white;" +
                            "-fx-pref-width: 370px; -fx-pref-height: 42px;" +
                            "-fx-background-radius: 21;"
            );
        } else {
            lblBadge.setText("");
        }

        // Active la déconnexion automatique sur cette scene
        Platform.runLater(() -> {
            Scene scene = lblWelcome.getScene();
            if (scene != null) {
                IdleSessionManager.attachToScene(scene, user);
            }
        });
    }

    private boolean tokenValide() {
        if (!service.verifierToken(currentUser.getId(),
                currentUser.getSessionToken())) {
            return false;
        }
        return true;
    }

    // ── Ouvrir fenêtre Modifier ────────────────
    @FXML
    public void handleOuvrirModifier() {
        if (!tokenValide()) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/edit_profil.fxml"));
            Parent root = loader.load();
            EditUserController ctrl = loader.getController();
            ctrl.initProfil(service, currentUser);
            Stage stage = new Stage();
            stage.setTitle("✏️ Modifier mon profil");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Devenir Host ───────────────────────────
    @FXML
    public void handleDevenirHost() {
        if (!tokenValide()) return;

        service.demanderHost(currentUser.getId());

        lblRole.setText("Rôle : ROLE_HOST_PENDING");
        lblBadge.setText("⚠️ UNVERIFIED");
        lblBadge.setStyle(
                "-fx-font-size: 10px; -fx-text-fill: white;" +
                        "-fx-background-color: #e67e22;" +
                        "-fx-padding: 3 10 3 10; -fx-background-radius: 10;" +
                        "-fx-font-weight: bold;"
        );
        btnDevenirHost.setText("⏳ Demande en attente...");
        btnDevenirHost.setDisable(true);
        btnDevenirHost.setStyle(
                "-fx-background-color: #95a5a6; -fx-text-fill: white;" +
                        "-fx-pref-width: 370px; -fx-pref-height: 42px;" +
                        "-fx-background-radius: 21;"
        );
    }

    // ── Retour Accueil ─────────────────────────
    @FXML
    public void handleRetourAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/home.fxml"));
            Parent root = loader.load();
            HomeController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            Stage stage = (Stage) lblWelcome.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Déconnexion ────────────────────────────
    @FXML
    public void handleLogout() {
        IdleSessionManager.detachCurrent();
        service.logout(currentUser.getId());
        currentUser.logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) lblWelcome.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(false);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}