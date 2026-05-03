package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.models.User;
import org.example.services.ServiceUser;
import org.example.utils.IdleSessionManager;

public class HomeController {

    @FXML private Label  lblUsername;
    @FXML private Label  lblRoleBadge;
    @FXML private Button btnProfil;
    @FXML private Label  lblSearchResult;

    private ServiceUser service     = new ServiceUser();
    private User        currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;

        String initiales = user.getUsername().substring(0, 1).toUpperCase();
        btnProfil.setText(initiales);
        lblUsername.setText(user.getUsername());

        switch (user.getRole()) {
            case "ROLE_HOST":
                // Host vérifié
                lblRoleBadge.setText("✅ Host");
                lblRoleBadge.setStyle(
                        "-fx-background-color: #6C63FF; -fx-text-fill: white;" +
                                "-fx-padding: 3 10 3 10; -fx-background-radius: 10;" +
                                "-fx-font-size: 11px; -fx-font-weight: bold;");
                break;

            case "ROLE_HOST_PENDING":
                // Host non vérifié → même accès mais badge UNVERIFIED
                lblRoleBadge.setText("⚠️ UNVERIFIED");
                lblRoleBadge.setStyle(
                        "-fx-background-color: #e67e22; -fx-text-fill: white;" +
                                "-fx-padding: 3 10 3 10; -fx-background-radius: 10;" +
                                "-fx-font-size: 11px; -fx-font-weight: bold;");
                break;

            default:
                // Guest
                lblRoleBadge.setText("Guest");
                lblRoleBadge.setStyle(
                        "-fx-background-color: #27ae60; -fx-text-fill: white;" +
                                "-fx-padding: 3 10 3 10; -fx-background-radius: 10;" +
                                "-fx-font-size: 11px; -fx-font-weight: bold;");
                break;
        }

        // Active la déconnexion automatique après inactivité
        // (le scene n'est pas encore attaché à ce stade → différé)
        Platform.runLater(() -> {
            Scene scene = btnProfil.getScene();
            if (scene != null) {
                IdleSessionManager.attachToScene(scene, user);
            }
        });
    }

    @FXML
    public void handleOuvrirProfil() {
        try {
            String fxml;
            FXMLLoader loader;
            Parent root;

            if (currentUser.getRole().equals("ROLE_HOST") ||
                    currentUser.getRole().equals("ROLE_HOST_PENDING")) {
                // Host vérifié ET non vérifié → même profil Host
                fxml   = "/profile_host.fxml";
                loader = new FXMLLoader(getClass().getResource(fxml));
                root   = loader.load();
                HostController ctrl = loader.getController();
                ctrl.setCurrentUser(currentUser);
            } else {
                // Guest
                fxml   = "/profile_guest.fxml";
                loader = new FXMLLoader(getClass().getResource(fxml));
                root   = loader.load();
                GuestController ctrl = loader.getController();
                ctrl.setCurrentUser(currentUser);
            }

            Stage stage = (Stage) btnProfil.getScene().getWindow();
            stage.setScene(new Scene(root));
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
            Stage stage = (Stage) btnProfil.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Recherche ──────────────────────────────
    @FXML
    public void handleRechercher() {
        lblSearchResult.setText("🔍 Recherche en cours...");
    }
}