package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.models.User;
import org.example.services.EmailService;
import org.example.services.ServiceUser;
import org.example.services.SmsService;

public class ForgotPasswordController {

    @FXML private TextField tfPhone;
    @FXML private Label     lblMessage;

    private final ServiceUser  service  = new ServiceUser();
    private final SmsService   smsSvc   = new SmsService();
    private final EmailService emailSvc = new EmailService();

    @FXML
    public void handleSendCode() {
        String phone = tfPhone.getText().trim();
        if (phone.isEmpty()) { show("⚠️ Saisissez votre numéro de téléphone.", "#e74c3c"); return; }

        User user = service.findByPhone(phone);
        if (user == null) { show("❌ Aucun compte avec ce numéro.", "#e74c3c"); return; }

        String code = SmsService.generateCode();
        service.saveResetSmsCode(user.getId(), code, 10); // 10 min

        boolean smsOk = smsSvc.sendResetCode(phone, code);
        if (!smsOk) { show("❌ Impossible d'envoyer le code WhatsApp (config Twilio ?)", "#e74c3c"); return; }

        // Email informatif (optionnel mais recommandé)
        if (user.getEmail() != null) emailSvc.sendPasswordResetInfo(user.getEmail(), user.getUsername());

        show("✅ Code envoyé par WhatsApp.", "#27ae60");

        // Redirection vers l'écran de vérification du code
        new Thread(() -> {
            try {
                Thread.sleep(1200);
                javafx.application.Platform.runLater(() -> openResetScreen(user.getId()));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void openResetScreen(int userId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/reset_password.fxml"));
            Parent root = loader.load();
            ResetPasswordController ctrl = loader.getController();
            ctrl.setUserId(userId);
            Stage stage = (Stage) tfPhone.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void handleBackToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) tfPhone.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void show(String msg, String color) {
        lblMessage.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
        lblMessage.setText(msg);
    }
}
