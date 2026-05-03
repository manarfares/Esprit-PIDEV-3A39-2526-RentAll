package tn.piapp.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.piapp.model.User;
import tn.piapp.dao.ServiceUser;
import tn.piapp.util.SessionManager;

public class HomeController {

    @FXML private BorderPane rootPane;
    @FXML private Label  lblUsername;
    @FXML private Label  lblRoleBadge;
    @FXML private Button btnProfil;
    @FXML private Label  lblSearchResult;
    @FXML private Button btnToolsServices;
    @FXML private Button btnReservations;
    @FXML private Button btnAvis;

    private ServiceUser service     = new ServiceUser();
    private User        currentUser;
    private Node        staysContent;

    @FXML
    public void initialize() {
        staysContent = rootPane.getCenter();
    }

    @FXML
    public void handleShowStays() {
        rootPane.setCenter(staysContent);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        SessionManager.getInstance().setCurrentUser(user);

        String initiales = user.getName().substring(0, 1).toUpperCase();
        btnProfil.setText(initiales);
        lblUsername.setText(user.getName());

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
        service.logout(currentUser.getId());
        currentUser.logout();
        SessionManager.getInstance().logout();
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

    // ── Ouvrir Tools & Services ────────────────
    @FXML
    public void handleOpenToolsServices() {
        try {
            String role = currentUser.getRole();

            if (role.equals("ROLE_GUEST") || role.equals("ROLE_HOST_PENDING")) {
                // Guest / unverified host → card-based read-only browse view
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/tn/piapp/ui/browse_guest.fxml"));
                Parent root = loader.load();
                GuestBrowseController ctrl = loader.getController();
                ctrl.setCurrentUser(currentUser);
                Stage stage = (Stage) btnProfil.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setMaximized(true);
                stage.show();
            } else {
                // ROLE_HOST or ROLE_ADMIN → table-based management view
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/tn/piapp/ui/main.fxml"));
                Parent root = loader.load();
                MainController ctrl = loader.getController();
                ctrl.setCurrentUser(currentUser);
                Stage stage = (Stage) btnProfil.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setMaximized(true);
                stage.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Could not open Tools & Services");
            alert.setContentText(e.getClass().getSimpleName() + ": " + e.getMessage()
                    + (e.getCause() != null ? "\nCause: " + e.getCause().getMessage() : ""));
            alert.showAndWait();
        }
    }

    // ── Ouvrir Réservations ────────────────
    @FXML
    public void handleOpenReservations() {
        rootPane.setCenter(new IntegratedReservationView(currentUser));
    }

    // ── Ouvrir Avis ────────────────
    @FXML
    public void handleOpenAvis() {
        rootPane.setCenter(new IntegratedAvisView(currentUser));
    }
}
