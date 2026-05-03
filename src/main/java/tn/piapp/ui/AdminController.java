package tn.piapp.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.piapp.model.User;
import tn.piapp.dao.ServiceUser;
import tn.piapp.util.SessionManager;
public class AdminController {

    @FXML private BorderPane                  rootPane;
    @FXML private VBox                        usersContent;
    @FXML private Label                      lblWelcome;
    @FXML private TableView<User>            tableUsers;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String>  colUsername;
    @FXML private TableColumn<User, String>  colEmail;
    @FXML private TableColumn<User, String>  colRole;
    @FXML private TableColumn<User, String>  colStatus;
    @FXML private Label                      lblMessage;
    @FXML private ComboBox<String>           cbFilter;

    private ServiceUser          service = new ServiceUser();
    private User                 currentUser;
    private ObservableList<User> data    = FXCollections.observableArrayList();
    private Node                 defaultUsersContent;

    @FXML
    public void initialize() {
        defaultUsersContent = usersContent;

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tableUsers.setItems(data);

        cbFilter.setItems(FXCollections.observableArrayList(
                "TOUS", "ROLE_GUEST", "ROLE_HOST",
                "ROLE_HOST_PENDING", "ROLE_ADMIN"
        ));
        cbFilter.setValue("TOUS");
        cbFilter.setOnAction(e -> filtrerUsers());
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        SessionManager.getInstance().setCurrentUser(user);
        lblWelcome.setText("👑 Bienvenue, " + user.getName());
        chargerUsers();
    }

    @FXML
    public void handleShowUsers() {
        rootPane.setCenter(defaultUsersContent);
        filtrerUsers();
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
        data.clear();
        data.addAll(service.recuperer());
    }

    @FXML
    public void filtrerUsers() {
        String filtre = cbFilter.getValue();
        data.clear();
        if (filtre.equals("TOUS")) {
            data.addAll(service.recuperer());
        } else {
            for (User u : service.recuperer()) {
                if (u.getRole().equals(filtre)) data.add(u);
            }
        }
    }

    // ── Ouvrir fenêtre Ajouter ─────────────────
    @FXML
    public void handleAjouter() {
        if (!tokenValide()) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/add_user.fxml"));
            Parent root = loader.load();

            AddUserController ctrl = loader.getController();
            ctrl.init(service, this);

            Stage stage = new Stage();
            stage.setTitle("➕ Ajouter un utilisateur");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            showMessage("❌ Erreur : " + e.getMessage(), false);
            e.printStackTrace();
        }
    }

    // ── Ouvrir fenêtre Modifier ────────────────
    @FXML
    public void handleModifier() {
        if (!tokenValide()) return;
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("⚠️ Sélectionnez un utilisateur.", false);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/edit_user.fxml"));
            Parent root = loader.load();

            EditUserController ctrl = loader.getController();
            ctrl.init(service, this, selected);

            Stage stage = new Stage();
            stage.setTitle("✏️ Modifier l'utilisateur");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            showMessage("❌ Erreur : " + e.getMessage(), false);
            e.printStackTrace();
        }
    }

    // ── Ouvrir fenêtre Supprimer ───────────────
    @FXML
    public void handleSupprimer() {
        if (!tokenValide()) return;
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("⚠️ Sélectionnez un utilisateur.", false);
            return;
        }
        if (selected.getRole().equals("ROLE_ADMIN")) {
            showMessage("⚠️ Impossible de supprimer un Admin.", false);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/confirm_delete.fxml"));
            Parent root = loader.load();

            DeleteUserController ctrl = loader.getController();
            ctrl.init(service, this, selected);

            Stage stage = new Stage();
            stage.setTitle("🗑️ Confirmer la suppression");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            showMessage("❌ Erreur : " + e.getMessage(), false);
            e.printStackTrace();
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
        // Passage de ROLE_HOST_PENDING → ROLE_HOST
        service.updateRole(selected.getId(), "ROLE_HOST");
        showMessage("✅ " + selected.getName() +
                " est maintenant Host ✅", true);
        filtrerUsers();
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
        showMessage("❌ Demande refusée → " + selected.getName(), false);
        filtrerUsers();
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
        showMessage("🔨 Utilisateur banni → " + selected.getName(), false);
        filtrerUsers();
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
        service.updateStatus(selected.getId(), "ACTIVE");
        showMessage("✅ Utilisateur activé → " + selected.getName(), true);
        filtrerUsers();
    }

    // ── Categories ─────────────────────────────
    @FXML
    public void handleManageCategories() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/piapp/ui/category_admin.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("🗂️ Manage Categories");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            showMessage("❌ Erreur : " + e.getMessage(), false);
            e.printStackTrace();
        }
    }

    // ── Tools & Services ───────────────────────
    @FXML
    public void handleOpenToolsServices() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/piapp/ui/main.fxml"));
            Parent root = loader.load();
            MainController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            Stage stage = (Stage) lblWelcome.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("❌ Erreur ouverture Tools & Services : " + e.getMessage(), false);
        }
    }

    // ── Déconnexion ────────────────────────────
    @FXML
    public void handleOpenReservations() {
        rootPane.setCenter(new IntegratedReservationView(currentUser));
    }

    @FXML
    public void handleOpenAvis() {
        rootPane.setCenter(new IntegratedAvisView(currentUser));
    }

    @FXML
    public void handleLogout() {        service.logout(currentUser.getId());
        currentUser.logout();
        SessionManager.getInstance().logout();
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
    }
}
