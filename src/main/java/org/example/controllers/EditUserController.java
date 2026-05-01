package org.example.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.models.User;
import org.example.services.FaceVerificationService;
import org.example.services.ServiceUser;
import org.example.utils.FaceCaptureDialog;
import org.example.utils.ValidationUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class EditUserController {

    @FXML private TextField        tfUsername;
    @FXML private TextField        tfEmail;
    @FXML private TextField        tfPhone;
    @FXML private ComboBox<String> cbRole;
    @FXML private ComboBox<String> cbStatus;
    @FXML private Label            lblMessage;
    @FXML private Label            lblErrUsername;
    @FXML private Label            lblErrEmail;
    @FXML private Label            lblErrPhone;
    @FXML private Label            lblVerifStatus;
    @FXML private Label            lblSelfieFile;
    @FXML private Label            lblCinFile;
    @FXML private Button           btnPickSelfie;
    @FXML private Button           btnCaptureSelfie;
    @FXML private Button           btnPickCin;

    private ServiceUser             service;
    private AdminController         adminController;
    private User                    selectedUser;
    private File                    selectedSelfie;
    private File                    selectedCin;
    private final FaceVerificationService faceService = new FaceVerificationService();

    @FXML
    public void initialize() {
        if (tfUsername != null) {
            tfUsername.textProperty().addListener((obs, old, val) -> {
                String err = ValidationUtil.validerUsername(val);
                if (lblErrUsername != null) {
                    lblErrUsername.setText(err != null ? err : "✅");
                    lblErrUsername.setStyle(err != null
                            ? "-fx-font-size: 10px; -fx-text-fill: #e74c3c;"
                            : "-fx-font-size: 10px; -fx-text-fill: #27ae60;");
                }
            });
        }
        if (tfEmail != null) {
            tfEmail.textProperty().addListener((obs, old, val) -> {
                String err = ValidationUtil.validerEmail(val);
                if (lblErrEmail != null) {
                    lblErrEmail.setText(err != null ? err : "✅");
                    lblErrEmail.setStyle(err != null
                            ? "-fx-font-size: 10px; -fx-text-fill: #e74c3c;"
                            : "-fx-font-size: 10px; -fx-text-fill: #27ae60;");
                }
            });
        }
        if (tfPhone != null) {
            tfPhone.textProperty().addListener((obs, old, val) -> {
                String err = ValidationUtil.validerTelephone(val);
                if (lblErrPhone != null) {
                    lblErrPhone.setText(err != null ? err : (val.isEmpty() ? "" : "✅"));
                    lblErrPhone.setStyle(err != null
                            ? "-fx-font-size: 10px; -fx-text-fill: #e74c3c;"
                            : "-fx-font-size: 10px; -fx-text-fill: #27ae60;");
                }
            });
        }
    }

    // ── Init Admin ────────────────────────────
    public void init(ServiceUser service, AdminController adminController, User selectedUser) {
        this.service          = service;
        this.adminController  = adminController;
        this.selectedUser     = selectedUser;

        tfUsername.setText(selectedUser.getUsername());
        tfEmail.setText(selectedUser.getEmail());
        tfPhone.setText(selectedUser.getPhone() != null ? selectedUser.getPhone() : "");

        cbRole.setItems(FXCollections.observableArrayList(
                "ROLE_GUEST", "ROLE_HOST", "ROLE_HOST_PENDING", "ROLE_ADMIN"));
        cbRole.setValue(selectedUser.getRole());
        cbRole.setVisible(true);
        cbRole.setManaged(true);

        cbStatus.setItems(FXCollections.observableArrayList(
                "ACTIVE", "INACTIVE", "BANNED", "UNVERIFIED"));
        cbStatus.setValue(selectedUser.getStatus());
        cbStatus.setVisible(true);
        cbStatus.setManaged(true);

        refreshVerifStatus();
    }

    // ── Init Profil personnel ─────────────────
    public void initProfil(ServiceUser service, User selectedUser) {
        this.service      = service;
        this.selectedUser = selectedUser;

        tfUsername.setText(selectedUser.getUsername());
        tfEmail.setText(selectedUser.getEmail());
        tfPhone.setText(selectedUser.getPhone() != null ? selectedUser.getPhone() : "");

        refreshVerifStatus();
    }

    /** Met à jour le badge "vérifié / non vérifié" selon l'état du compte. */
    private void refreshVerifStatus() {
        if (lblVerifStatus == null || selectedUser == null) return;
        String currentSelfie = service.getSelfieImagePath(selectedUser.getId());
        if (currentSelfie != null && !currentSelfie.isBlank()) {
            lblVerifStatus.setText("✅ Compte vérifié — selfie de référence enregistré.");
            lblVerifStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else {
            lblVerifStatus.setText("⚠️ Compte non vérifié — fournissez selfie + CIN pour activer la connexion faciale.");
            lblVerifStatus.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 11px; -fx-font-weight: bold;");
        }
    }

    // ── Choisir un fichier selfie ───────────────
    @FXML
    public void handlePickSelfie() {
        File f = pickImage("Choisissez votre selfie");
        if (f != null) {
            selectedSelfie = f;
            lblSelfieFile.setText("✅ " + f.getName());
            lblSelfieFile.setStyle("-fx-font-size: 10px; -fx-text-fill: #27ae60;");
        }
    }

    // ── Capturer le selfie via webcam ───────────
    @FXML
    public void handleCaptureSelfie() {
        var owner = tfUsername.getScene().getWindow();
        File captured = FaceCaptureDialog.show(owner, "Capturez votre selfie");
        if (captured != null) {
            selectedSelfie = captured;
            lblSelfieFile.setText("📷 " + captured.getName());
            lblSelfieFile.setStyle("-fx-font-size: 10px; -fx-text-fill: #16a085;");
        }
    }

    // ── Choisir un fichier CIN ──────────────────
    @FXML
    public void handlePickCin() {
        File f = pickImage("Choisissez la photo de votre CIN");
        if (f != null) {
            selectedCin = f;
            lblCinFile.setText("✅ " + f.getName());
            lblCinFile.setStyle("-fx-font-size: 10px; -fx-text-fill: #27ae60;");
        }
    }

    private File pickImage(String title) {
        FileChooser ch = new FileChooser();
        ch.setTitle(title);
        ch.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.bmp"));
        return ch.showOpenDialog(tfUsername.getScene().getWindow());
    }

    // ── Enregistrer ───────────────────────────
    @FXML
    public void handleModifier() {
        String username = tfUsername.getText().trim();
        String email    = tfEmail.getText().trim();
        String phone    = tfPhone.getText().trim();

        String errUser  = ValidationUtil.validerUsername(username);
        String errMail  = ValidationUtil.validerEmail(email);
        String errPhone = ValidationUtil.validerTelephone(phone);

        if (errUser != null)  { showMessage(errUser,  false); return; }
        if (errMail != null)  { showMessage(errMail,  false); return; }
        if (errPhone != null) { showMessage(errPhone, false); return; }

        selectedUser.setUsername(username);
        selectedUser.setEmail(email);
        selectedUser.setPhone(phone.isEmpty() ? null : phone);

        // Mode Admin : rôle + statut
        if (cbRole != null && cbRole.isVisible() && cbRole.getValue() != null) {
            String role   = cbRole.getValue();
            String status = cbStatus.getValue();
            selectedUser.setStatus(status);
            service.modifier(selectedUser);
            if (!selectedUser.getRole().equals(role)) {
                service.updateRole(selectedUser.getId(), role);
            }
            if (adminController != null) adminController.filtrerUsers();
        } else {
            service.modifier(selectedUser);
        }

        // ═══════════ Vérification faciale (si selfie + CIN fournis) ═══════════
        if (selectedSelfie != null && selectedCin != null) {
            showMessage("⏳ Vérification du visage en cours (selfie ↔ CIN)...", true);

            new Thread(() -> {
                FaceVerificationService.VerifyResult res = faceService.verifyFaces(selectedSelfie, selectedCin);
                System.out.println("👤 Face verify → success=" + res.success
                        + ", match=" + res.match + ", distance=" + res.distance + ", error=" + res.error);

                Platform.runLater(() -> {
                    if (!res.success) {
                        showMessage("❌ Vérification impossible : "
                                + (res.error != null ? res.error : "moteur facial indisponible"), false);
                        return;
                    }
                    if (!res.match) {
                        showMessage("❌ Le visage du selfie ne correspond pas à celui de la CIN"
                                + (res.distance != null ? " (distance " + res.distance + ")" : "") + ".", false);
                        return;
                    }

                    // ✅ Match → on copie les fichiers dans le dossier Symfony
                    try {
                        String selfieRel = persistImage(selectedSelfie, "verification/selfies");
                        String cinRel    = persistImage(selectedCin,    "verification/identity");
                        service.updateFaceVerification(selectedUser.getId(), selfieRel, cinRel);

                        showMessage("✅ Vérification réussie ! Connexion faciale activée (distance "
                                + res.distance + ").", true);
                        refreshVerifStatus();

                        // Cleanup tmp file si capture webcam
                        if (selectedSelfie.getName().startsWith("face-capture-")) {
                            try { selectedSelfie.delete(); } catch (Exception ignored) {}
                        }
                        selectedSelfie = null;
                        selectedCin    = null;
                        lblSelfieFile.setText("Aucun fichier sélectionné");
                        lblSelfieFile.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");
                        lblCinFile.setText("Aucun fichier sélectionné");
                        lblCinFile.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");

                        scheduleClose();

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showMessage("❌ Erreur sauvegarde des photos : " + ex.getMessage(), false);
                    }
                });
            }, "face-verify").start();
        } else if (selectedSelfie != null || selectedCin != null) {
            showMessage("⚠️ Sélectionnez les DEUX fichiers (selfie + CIN) pour activer la vérification.", false);
        } else {
            showMessage("✅ Modifications enregistrées !", true);
            scheduleClose();
        }
    }

    /**
     * Copie l'image dans face.symfony.dir/public/uploads/{subdir}/ et retourne le
     * chemin relatif à stocker en DB (ex : "/uploads/verification/selfies/abc.jpg").
     */
    private String persistImage(File source, String subDir) throws Exception {
        String symfonyDir = org.example.services.AppConfig.get("face.symfony.dir");
        if (symfonyDir == null || symfonyDir.isBlank())
            throw new IllegalStateException("face.symfony.dir non configuré");

        Path destDir = Paths.get(symfonyDir, "public", "uploads", subDir);
        if (!Files.isDirectory(destDir)) Files.createDirectories(destDir);

        String ext = ".jpg";
        String name = source.getName();
        int dot = name.lastIndexOf('.');
        if (dot != -1) ext = name.substring(dot);

        String filename = "javafx-" + UUID.randomUUID() + ext;
        Path dest = destDir.resolve(filename);
        Files.copy(source.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/" + subDir + "/" + filename;
    }

    private void scheduleClose() {
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                Platform.runLater(this::handleFermer);
            } catch (Exception ignored) {}
        }).start();
    }

    @FXML
    public void handleFermer() {
        Stage stage = (Stage) tfUsername.getScene().getWindow();
        stage.close();
    }

    private void showMessage(String msg, boolean success) {
        lblMessage.setStyle(success
                ? "-fx-text-fill: #27ae60; -fx-font-size: 12px;"
                : "-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        lblMessage.setText(msg);
    }
}
