package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.services.ServiceUser;
import org.example.utils.ValidationUtil;

public class ResetPasswordController {

    @FXML private TextField     tfCode;
    @FXML private PasswordField pfNewPassword;
    @FXML private PasswordField pfConfirm;
    @FXML private Label         lblMessage;

    private final ServiceUser service = new ServiceUser();
    private int userId;

    public void setUserId(int id) { this.userId = id; }

    @FXML
    public void handleReset() {
        String code    = tfCode.getText().trim();
        String newPwd  = pfNewPassword.getText();
        String confirm = pfConfirm.getText();

        if (code.isEmpty() || newPwd.isEmpty() || confirm.isEmpty()) {
            show("⚠️ Tous les champs sont obligatoires.", "#e74c3c"); return;
        }
        if (!newPwd.equals(confirm)) {
            show("⚠️ Les mots de passe ne correspondent pas.", "#e74c3c"); return;
        }
        String err = ValidationUtil.validerPassword(newPwd);
        if (err != null) { show(err, "#e74c3c"); return; }

        boolean ok = service.verifyResetCodeAndUpdatePassword(userId, code, newPwd);
        if (!ok) { show("❌ Code invalide ou expiré.", "#e74c3c"); return; }

        show("✅ Mot de passe mis à jour !", "#27ae60");

        new Thread(() -> {
            try {
                Thread.sleep(1500);
                javafx.application.Platform.runLater(this::goLogin);
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void goLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) tfCode.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void show(String msg, String color) {
        lblMessage.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
        lblMessage.setText(msg);
    }
}
