package tn.piapp.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.piapp.model.User;
import tn.piapp.dao.ServiceUser;

public class GuestController {

    @FXML private Label         lblWelcome;
    @FXML private Label         lblRole;
    @FXML private Label         lblStatus;
    @FXML private TextField     tfUsername;
    @FXML private TextField     tfEmail;
    @FXML private TextField     tfPhone;
    @FXML private PasswordField pfPassword;
    @FXML private Label         lblMessage;
    @FXML private Button        btnDevenirHost;
    @FXML private Label lblBadge;

    private ServiceUser service     = new ServiceUser();
    private User        currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;

        lblWelcome.setText("👋 Bienvenue, " + user.getName());
        lblRole.setText("Rôle : " + user.getRole());
        lblStatus.setText("Statut : " + user.getStatus());
        tfUsername.setText(user.getName());
        tfEmail.setText(user.getEmail());
        tfPhone.setText(user.getPhone() != null ? user.getPhone() : "");

        if (user.getRole().equals("ROLE_HOST_PENDING")) {
            btnDevenirHost.setText("⏳ Demande en attente...");
            btnDevenirHost.setDisable(true);
            btnDevenirHost.setStyle(
                    "-fx-background-color: #95a5a6; -fx-text-fill: white;" +
                            "-fx-pref-width: 370px; -fx-pref-height: 42px;" +
                            "-fx-background-radius: 21;"
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
        lblWelcome.setText("👋 Bienvenue, " + currentUser.getName());
    }

    @FXML
    public void handleDevenirHost() {
        if (!tokenValide()) return;

        service.demanderHost(currentUser.getId());

        // Afficher badge immédiatement
        lblBadge.setText("⚠️ UNVERIFIED");
        lblBadge.setStyle(
                "-fx-font-size: 10px; -fx-text-fill: white;" +
                        "-fx-background-color: #e67e22;" +
                        "-fx-padding: 3 10 3 10; -fx-background-radius: 10;" +
                        "-fx-font-weight: bold;"
        );

        // Mettre à jour le rôle affiché
        lblRole.setText("Rôle : ROLE_HOST_PENDING");

        // Désactiver le bouton
        btnDevenirHost.setText("⏳ Demande en attente...");
        btnDevenirHost.setDisable(true);
        btnDevenirHost.setStyle(
                "-fx-background-color: #95a5a6; -fx-text-fill: white;" +
                        "-fx-pref-width: 370px; -fx-pref-height: 42px;" +
                        "-fx-background-radius: 21;"
        );

        showMessage("📩 Demande envoyée ! Badge UNVERIFIED actif.", true);
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