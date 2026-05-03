package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.models.User;
import org.example.services.ServiceUser;
import org.example.utils.IdleSessionManager;
import tn.piapp.model.User;
import tn.piapp.dao.ServiceUser;

public class HostController {

    @FXML private Label         lblWelcome;
    @FXML private Label         lblRole;
    @FXML private Label         lblStatus;
    @FXML private TextField     tfUsername;
    @FXML private TextField     tfEmail;
    @FXML private TextField     tfPhone;
    @FXML private PasswordField pfPassword;
    @FXML private Label         lblMessage;

    private ServiceUser service     = new ServiceUser();
    private User        currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;

        lblWelcome.setText("🏠 Bienvenue, " + user.getUsername());
        lblStatus.setText("Statut : " + user.getStatus());
        tfUsername.setText(user.getName());
        tfEmail.setText(user.getEmail());
        tfPhone.setText(user.getPhone() != null ? user.getPhone() : "");

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
    }

    private boolean tokenValide() {
        if (!service.verifierToken(currentUser.getId(),
                currentUser.getSessionToken())) {
            showMessage("❌ Session expirée. Reconnectez-vous.", false);
            return false;
        }
        return true;
    }

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
    public void handleUpdate() {
        if (!tokenValide()) return;

        String name = tfUsername.getText().trim();
        String email    = tfEmail.getText().trim();
        String phone    = tfPhone.getText().trim();
        String password = pfPassword.getText().trim();

        if (name.isEmpty() || email.isEmpty()) {
            showMessage("⚠️ Name et Email sont obligatoires.", false);
            return;
        }

        currentUser.setName(name);
        currentUser.setEmail(email);
        currentUser.setPhone(phone.isEmpty() ? null : phone);

        if (!password.isEmpty()) {
            if (password.length() < 6) {
                showMessage("⚠️ Mot de passe trop court (min 6).", false);
                return;
            }
            currentUser.setPassword(password);
        }

        service.modifier(currentUser);
        showMessage("✅ Profil mis à jour !", true);
        lblWelcome.setText("🏠 Bienvenue, " + currentUser.getName());
    }

    @FXML
    public void handleGererAnnonces() {
        if (!tokenValide()) return;
        showMessage("📋 Gestion des annonces — bientôt disponible !", true);
    }

    @FXML
    public void handleVoirReservations() {
        if (!tokenValide()) return;
        showMessage("📅 Réservations — bientôt disponible !", true);
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

    private void showMessage(String msg, boolean success) {
        lblMessage.setStyle(success
                ? "-fx-text-fill: #27ae60; -fx-font-size: 12px;"
                : "-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        lblMessage.setText(msg);
    }
}