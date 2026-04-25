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
        if (lblConnected != null) {
            lblConnected.setText("● " + user.getName() + " (" + user.getRole() + ")");
        }
        if (serviceController != null) serviceController.setCurrentUser(user);
        if (toolController    != null) toolController.setCurrentUser(user);
    }

    /** Navigates back to home.fxml, passing the current user. */
    @FXML
    public void handleRetourAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/home.fxml"));
            Parent root = loader.load();
            HomeController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);

            Stage stage = (Stage) lblConnected.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
