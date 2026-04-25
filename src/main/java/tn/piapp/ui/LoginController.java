package tn.piapp.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.piapp.model.User;
import tn.piapp.dao.ServiceUser;

public class LoginController {

    @FXML private TextField     tfUsername;
    @FXML private PasswordField pfPassword;
    @FXML private Label         lblErreur;

    private ServiceUser service = new ServiceUser();

    @FXML
    public void handleLogin() {
        String name = tfUsername.getText().trim();
        String password = pfPassword.getText().trim();

        if (name.isEmpty() || password.isEmpty()) {
            lblErreur.setText("⚠️ Veuillez remplir tous les champs.");
            return;
        }

        User user = service.login(name, password);

        if (user == null) {
            lblErreur.setText("❌ Identifiant ou mot de passe incorrect.");
            return;
        }

        if (user.getStatus().equals("BANNED")) {
            lblErreur.setText("🚫 Votre compte est banni.");
            return;
        }

        if (user.getStatus().equals("INACTIVE")) {
            lblErreur.setText("⚠️ Votre compte est inactif.");
            return;
        }

        if (!service.verifierToken(user.getId(), user.getSessionToken())) {
            lblErreur.setText("❌ Erreur de session.");
            return;
        }

        try {
            String fxml;
            FXMLLoader loader;
            Parent root;

            switch (user.getRole()) {
                case "ROLE_ADMIN":
                    fxml   = "/dashboard_admin.fxml";
                    loader = new FXMLLoader(getClass().getResource(fxml));
                    root   = loader.load();
                    AdminController adminCtrl = loader.getController();
                    adminCtrl.setCurrentUser(user);
                    break;

                case "ROLE_HOST":
                case "ROLE_HOST_PENDING":
                    // Host vérifié ET non vérifié → même accueil
                    fxml   = "/home.fxml";
                    loader = new FXMLLoader(getClass().getResource(fxml));
                    root   = loader.load();
                    HomeController hostCtrl = loader.getController();
                    hostCtrl.setCurrentUser(user);
                    break;

                default:
                    // Guest → accueil
                    fxml   = "/home.fxml";
                    loader = new FXMLLoader(getClass().getResource(fxml));
                    root   = loader.load();
                    HomeController guestCtrl = loader.getController();
                    guestCtrl.setCurrentUser(user);
                    break;
            }

            Stage stage = (Stage) tfUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            lblErreur.setText("❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleGoRegister() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/register.fxml"));
            Stage stage = (Stage) tfUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}