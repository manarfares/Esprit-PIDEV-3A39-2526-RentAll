package org.example.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.models.User;
import org.example.services.PdfReportService;
import org.example.services.ServiceUser;
import org.example.utils.IdleSessionManager;

import java.awt.Desktop;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class AdminController {

    @FXML private Label lblWelcome;
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private Label lblMessage;
    @FXML private ComboBox<String> cbFilter;
    @FXML private TextField searchField;
    @FXML private Label lblPageInfo;
    @FXML private ComboBox<String> cbPageSize;

    // Statistiques
    @FXML private Label    lblStatTotal;
    @FXML private Label    lblStatAdmin;
    @FXML private Label    lblStatHost;
    @FXML private Label    lblStatHostPending;
    @FXML private Label    lblStatGuest;
    @FXML private Label    lblStatActive;
    @FXML private Label    lblStatBanned;
    @FXML private Label    lblStatVerified;
    @FXML private Label    lblStatFace;
    @FXML private PieChart pieChartRoles;

    private final PdfReportService pdfService = new PdfReportService();

    private ServiceUser service = new ServiceUser();
    private User currentUser;
    private ObservableList<User> masterData = FXCollections.observableArrayList();
    private FilteredList<User> filteredData;

    // Pagination variables
    private int currentPage = 0;
    private int pageSize = 20;
    private int totalPages = 0;


    @FXML
    public void initialize() {
        // Setup table columns
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Custom cell factory for status with colors
        colStatus.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "ACTIVE":
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                            break;
                        case "BANNED":
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("-fx-text-fill: #7f8c8d;");
                    }
                }
            }
        });

        // Custom cell factory for roles with badges
        colRole.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String displayRole = switch (item) {
                        case "ROLE_ADMIN" -> "👑 Admin";
                        case "ROLE_HOST" -> "🏠 Host";
                        case "ROLE_HOST_PENDING" -> "⏳ Host (en attente)";
                        default -> "👤 Guest";
                    };
                    setText(displayRole);

                    switch (item) {
                        case "ROLE_ADMIN":
                            setStyle("-fx-text-fill: #9B59B6; -fx-font-weight: bold;");
                            break;
                        case "ROLE_HOST":
                            setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                            break;
                        case "ROLE_HOST_PENDING":
                            setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("-fx-text-fill: #7f8c8d;");
                    }
                }
            }
        });

        // Setup filter combo box - ROLE_ADMIN supprimé
        cbFilter.setItems(FXCollections.observableArrayList(
                "TOUS", "ROLE_GUEST", "ROLE_HOST",
                "ROLE_HOST_PENDING"  // ← ROLE_ADMIN enlevé
        ));
        cbFilter.setValue("TOUS");
        cbFilter.setOnAction(e -> filtrerUsers());

        // Setup page size
        cbPageSize.setItems(FXCollections.observableArrayList("10", "20", "50", "100"));
        cbPageSize.setValue("20");
        cbPageSize.setOnAction(e -> {
            pageSize = Integer.parseInt(cbPageSize.getValue());
            currentPage = 0;
            updatePagination();
        });

        // Setup search field
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            currentPage = 0;
            filtrerUsers();
        });

        tableUsers.setPlaceholder(new Label("📭 Aucun utilisateur trouvé"));
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        // Texte court adapté à la sidebar
        lblWelcome.setText("👑 " + user.getUsername());
        chargerUsers();
        chargerStatistiques();

        // Active la déconnexion automatique après inactivité
        Platform.runLater(() -> {
            Scene scene = lblWelcome.getScene();
            if (scene != null) {
                IdleSessionManager.attachToScene(scene, user);
            }
        });
    }

    /** Charge / met à jour les cartes statistiques + le PieChart. */
    public void chargerStatistiques() {
        Map<String, Integer> s = service.getUserStats();

        if (lblStatTotal       != null) lblStatTotal.setText(String.valueOf(s.getOrDefault("total", 0)));
        if (lblStatAdmin       != null) lblStatAdmin.setText(String.valueOf(s.getOrDefault("role_admin", 0)));
        if (lblStatHost        != null) lblStatHost.setText(String.valueOf(s.getOrDefault("role_host", 0)));
        if (lblStatHostPending != null) lblStatHostPending.setText(String.valueOf(s.getOrDefault("role_host_pending", 0)));
        if (lblStatGuest       != null) lblStatGuest.setText(String.valueOf(s.getOrDefault("role_guest", 0)));
        if (lblStatActive      != null) lblStatActive.setText(String.valueOf(s.getOrDefault("status_active", 0)));
        if (lblStatBanned      != null) lblStatBanned.setText(String.valueOf(s.getOrDefault("status_banned", 0)));
        if (lblStatVerified    != null) lblStatVerified.setText(String.valueOf(s.getOrDefault("verified", 0)));
        if (lblStatFace        != null) lblStatFace.setText(String.valueOf(s.getOrDefault("face_enabled", 0)));

        if (pieChartRoles != null) {
            ObservableList<PieChart.Data> data = FXCollections.observableArrayList(
                    new PieChart.Data("Admin ("           + s.getOrDefault("role_admin", 0)        + ")", s.getOrDefault("role_admin", 0)),
                    new PieChart.Data("Host vérifiés ("    + s.getOrDefault("role_host", 0)         + ")", s.getOrDefault("role_host", 0)),
                    new PieChart.Data("Host en attente (" + s.getOrDefault("role_host_pending", 0) + ")", s.getOrDefault("role_host_pending", 0)),
                    new PieChart.Data("Guest ("           + s.getOrDefault("role_guest", 0)        + ")", s.getOrDefault("role_guest", 0))
            );
            pieChartRoles.setData(data);
        }
    }

    @FXML
    public void handleRefreshStats() {
        chargerUsers();
        chargerStatistiques();
        showInfo("✅ Statistiques actualisées.");
    }

    @FXML
    public void handleExportPdf() {
        // Suggérer un nom par défaut
        String defaultName = "rentall-rapport-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + ".pdf";

        FileChooser ch = new FileChooser();
        ch.setTitle("Enregistrer le rapport PDF");
        ch.setInitialFileName(defaultName);
        ch.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File target = ch.showSaveDialog(lblWelcome.getScene().getWindow());
        if (target == null) return;

        // Génération en arrière-plan
        showInfo("⏳ Génération du PDF...");
        new Thread(() -> {
            try {
                Map<String, Integer> stats = service.getUserStats();
                java.util.List<User> users = service.recuperer();
                pdfService.generateUserReport(target, stats, users);

                Platform.runLater(() -> {
                    showInfo("✅ PDF généré : " + target.getName());

                    // Proposer d'ouvrir
                    Alert ok = new Alert(AlertType.INFORMATION);
                    ok.setTitle("PDF généré");
                    ok.setHeaderText("✅ Rapport sauvegardé");
                    ok.setContentText("Fichier : " + target.getAbsolutePath()
                            + "\n\nOuvrir le fichier maintenant ?");
                    ButtonType btnOpen   = new ButtonType("📄 Ouvrir le PDF");
                    ButtonType btnClose  = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
                    ok.getButtonTypes().setAll(btnOpen, btnClose);
                    ok.showAndWait().ifPresent(bt -> {
                        if (bt == btnOpen) {
                            try { Desktop.getDesktop().open(target); }
                            catch (Exception ignored) {}
                        }
                    });
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() ->
                        showError("❌ Échec génération PDF : " + ex.getMessage()));
            }
        }, "pdf-export").start();
    }

    private void showInfo(String msg) {
        if (lblMessage != null) {
            lblMessage.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px;");
            lblMessage.setText(msg);
        }
    }
    private void showError(String msg) {
        if (lblMessage != null) {
            lblMessage.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
            lblMessage.setText(msg);
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

    public void chargerUsers() {
        masterData.clear();
        masterData.addAll(service.recuperer());
        setupFilteredData();
    }

    private void setupFilteredData() {
        filteredData = new FilteredList<>(masterData, user -> true);
        filtrerUsers();
    }

    public void filtrerUsers() {
        String filtre = cbFilter.getValue();
        String searchText = searchField.getText();

        filteredData.setPredicate(user -> {
            // Role filter
            if (filtre != null && !filtre.equals("TOUS")) {
                if (!user.getRole().equals(filtre)) {
                    return false;
                }
            }

            // Search filter
            if (searchText != null && !searchText.isEmpty()) {
                String lowerSearch = searchText.toLowerCase();
                return user.getUsername().toLowerCase().contains(lowerSearch) ||
                        user.getEmail().toLowerCase().contains(lowerSearch);
            }

            return true;
        });

        updatePagination();
    }

    private void updatePagination() {
        int filteredSize = filteredData.size();
        totalPages = (int) Math.ceil((double) filteredSize / pageSize);

        if (totalPages == 0) totalPages = 1;
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        int fromIndex = currentPage * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, filteredSize);

        if (filteredSize > 0 && fromIndex < filteredSize) {
            ObservableList<User> pageData = FXCollections.observableArrayList(
                    filteredData.subList(fromIndex, toIndex)
            );
            tableUsers.setItems(pageData);
        } else {
            tableUsers.setItems(FXCollections.observableArrayList());
        }

        lblPageInfo.setText(String.format("Page %d / %d (%d utilisateur%s)",
                currentPage + 1, totalPages, filteredSize,
                filteredSize > 1 ? "s" : ""));
    }

    @FXML
    public void handleFirstPage() {
        if (totalPages > 0 && currentPage != 0) {
            currentPage = 0;
            updatePagination();
        }
    }

    @FXML
    public void handlePreviousPage() {
        if (currentPage > 0) {
            currentPage--;
            updatePagination();
        }
    }

    @FXML
    public void handleNextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            updatePagination();
        }
    }

    @FXML
    public void handleLastPage() {
        if (totalPages > 0 && currentPage != totalPages - 1) {
            currentPage = totalPages - 1;
            updatePagination();
        }
    }

    // ── Valider Host ───────────────────────────
    @FXML
    public void handleValiderHost() {
        if (!tokenValide()) return;
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("⚠️ Sélectionnez un utilisateur.", false);
            return;
        }
        if (!selected.getRole().equals("ROLE_HOST_PENDING")) {
            showMessage("⚠️ Cet utilisateur n'a pas de demande Host.", false);
            return;
        }
        service.updateRole(selected.getId(), "ROLE_HOST");
        showMessage("✅ " + selected.getUsername() + " est maintenant Host ✅", true);
        refreshAfterAction();
    }

    // ── Refuser Host ───────────────────────────
    @FXML
    public void handleRefuserHost() {
        if (!tokenValide()) return;
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("⚠️ Sélectionnez un utilisateur.", false);
            return;
        }
        if (!selected.getRole().equals("ROLE_HOST_PENDING")) {
            showMessage("⚠️ Cet utilisateur n'a pas de demande Host.", false);
            return;
        }
        service.updateRole(selected.getId(), "ROLE_GUEST");
        showMessage("❌ Demande refusée → " + selected.getUsername(), false);
        refreshAfterAction();
    }

    // ── Bannir ─────────────────────────────────
    @FXML
    public void handleBannir() {
        if (!tokenValide()) return;
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("⚠️ Sélectionnez un utilisateur.", false);
            return;
        }
        if (selected.getRole().equals("ROLE_ADMIN")) {
            showMessage("⚠️ Impossible de bannir un Admin.", false);
            return;
        }
        service.updateStatus(selected.getId(), "BANNED");
        showMessage("🔨 Utilisateur banni → " + selected.getUsername(), false);
        refreshAfterAction();
    }

    // ── Activer ────────────────────────────────
    @FXML
    public void handleActiver() {
        if (!tokenValide()) return;
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("⚠️ Sélectionnez un utilisateur.", false);
            return;
        }
        if (selected.getStatus().equals("ACTIVE")) {
            showMessage("⚠️ Cet utilisateur est déjà actif.", false);
            return;
        }
        service.updateStatus(selected.getId(), "ACTIVE");
        showMessage("✅ Utilisateur activé → " + selected.getUsername(), true);
        refreshAfterAction();
    }

    private void refreshAfterAction() {
        chargerUsers();
        currentPage = 0;
        filtrerUsers();
        chargerStatistiques();
    }

    // ── Déconnexion ────────────────────────────
    @FXML
    public void handleLogout() {
        IdleSessionManager.detachCurrent();
        service.logout(currentUser.getId());
        currentUser.logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) lblWelcome.getScene().getWindow();
            stage.setScene(new Scene(root));
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

        // Auto-clear message after 3 seconds
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                javafx.application.Platform.runLater(() -> {
                    if (lblMessage.getText().equals(msg)) {
                        lblMessage.setText("");
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}