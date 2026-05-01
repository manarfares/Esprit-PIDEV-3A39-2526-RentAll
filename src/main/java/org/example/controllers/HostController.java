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

public class HostController {

    @FXML private Label lblWelcome;
    @FXML private Label lblRole;
    @FXML private Label lblStatus;

    private ServiceUser service     = new ServiceUser();
    private User        currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;

        lblWelcome.setText("🏠 Bienvenue, " + user.getUsername());
        lblStatus.setText("Statut : " + user.getStatus());

        // Badge vérifié / non vérifié
        if (user.getRole().equals("ROLE_HOST_PENDING")) {
            lblRole.setText("⚠️ UNVERIFIED");
            lblRole.setStyle(
                    "-fx-font-size: 12px; -fx-font-weight: bold;" +
                            "-fx-text-fill: white; -fx-background-color: #e67e22;" +
                            "-fx-padding: 3 10 3 10; -fx-background-radius: 10;"
            );
        } else {
            lblRole.setText("✅ Host Vérifié");
            lblRole.setStyle(
                    "-fx-font-size: 12px; -fx-font-weight: bold;" +
                            "-fx-text-fill: white; -fx-background-color: #6C63FF;" +
                            "-fx-padding: 3 10 3 10; -fx-background-radius: 10;"
            );
        }

        // Active la déconnexion automatique sur cette scene
        Platform.runLater(() -> {
            Scene scene = lblWelcome.getScene();
            if (scene != null) {
                IdleSessionManager.attachToScene(scene, user);
            }
        });
    }

    // ── Ouvrir fenêtre Modifier ────────────────
    @FXML
    public void handleOuvrirModifier() {
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

    @FXML
    public void handleGererAnnonces() {
        System.out.println("📋 Gestion des annonces — bientôt disponible !");
    }

    @FXML
    public void handleVoirReservations() {
        System.out.println("📅 Réservations — bientôt disponible !");
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