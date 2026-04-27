package tn.piapp.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import tn.piapp.model.User;

public class MainController {

    @FXML private ServiceController serviceController;
    @FXML private ToolController    toolController;
    @FXML private Label             lblConnected;
    @FXML private Label             lblContext;

    private User currentUser;

    @FXML
    public void initialize() {
        // Sub-controllers are injected via fx:include.
        // setCurrentUser() is called after initialize() by the opener.
    }

    /**
     * Called by HomeController (or any opener) after the FXML is loaded.
     * Propagates the logged-in user to both sub-controllers so they can
     * apply role-based restrictions and load the correct data scope.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;

        // Connected indicator
        if (lblConnected != null) {
            lblConnected.setText("● " + user.getName());
        }

        // Context label — role-aware title
        if (lblContext != null) {
            String ctx = switch (user.getRole()) {
                case "ROLE_ADMIN"        -> "⚙️ Admin Panel";
                case "ROLE_HOST"         -> "🏠 My Listings — " + user.getName();
                case "ROLE_HOST_PENDING" -> "⏳ My Listings (Pending) — " + user.getName();
                default                  -> "🔧 Tools & Services";
            };
            lblContext.setText(ctx);
        }

        if (serviceController != null) serviceController.setCurrentUser(user);
        if (toolController    != null) toolController.setCurrentUser(user);
    }

    /** Navigates back — admin goes to dashboard, others go to home. */
    @FXML
    public void handleRetourAccueil() {
        try {
            Stage stage = (Stage) lblConnected.getScene().getWindow();

            if (currentUser != null && currentUser.getRole().equals("ROLE_ADMIN")) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/dashboard_admin.fxml"));
                Parent root = loader.load();
                AdminController ctrl = loader.getController();
                ctrl.setCurrentUser(currentUser);
                stage.setScene(new Scene(root));
            } else {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/home.fxml"));
                Parent root = loader.load();
                HomeController ctrl = loader.getController();
                ctrl.setCurrentUser(currentUser);
                stage.setScene(new Scene(root));
            }

            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
