package tn.piapp.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.piapp.dao.CategoryDao;
import tn.piapp.model.Category;
import tn.piapp.util.Alerts;

import java.sql.SQLException;
import java.util.List;

public class CategoryAdminController {

    @FXML private TableView<Category>               tableView;
    @FXML private TableColumn<Category, Integer>    colId;
    @FXML private TableColumn<Category, String>     colName;
    @FXML private TableColumn<Category, String>     colType;
    @FXML private Button                            btnAdd;
    @FXML private Button                            btnEdit;
    @FXML private Button                            btnDelete;
    @FXML private Label                             lblStatus;
    @FXML private Label                             lblMessage;
    @FXML private ComboBox<String>                  cbTypeFilter;

    private final CategoryDao dao = new CategoryDao();
    private final ObservableList<Category> masterList = FXCollections.observableArrayList();
    private FilteredList<Category> filteredList;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));

        // Type badge cell
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) { setGraphic(null); return; }
                Label badge = new Label(value);
                String color = "service".equals(value) ? "#6C63FF" : "#00b894";
                badge.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white;" +
                               "-fx-padding: 2 8 2 8; -fx-background-radius: 10;" +
                               "-fx-font-size: 10px; -fx-font-weight: bold;");
                HBox box = new HBox(badge);
                box.setStyle("-fx-alignment: center-left; -fx-padding: 4 0 4 0;");
                setGraphic(box); setText(null);
            }
        });

        // Filter ComboBox
        cbTypeFilter.getItems().setAll("All", "service", "tool");
        cbTypeFilter.setValue("All");
        cbTypeFilter.valueProperty().addListener((obs, o, n) -> applyFilter());

        // FilteredList
        filteredList = new FilteredList<>(masterList, p -> true);
        tableView.setItems(filteredList);

        // Disable Edit/Delete when nothing selected
        btnEdit.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());
        btnDelete.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());

        loadData();
    }

    // ── Data ───────────────────────────────────────────────────────────────────
    private void loadData() {
        try {
            List<Category> all = dao.findAll();
            masterList.setAll(all);
            applyFilter();
            lblStatus.setText(all.size() + " categories total");
        } catch (SQLException e) {
            showMessage("❌ Load error: " + e.getMessage(), false);
        }
    }

    private void applyFilter() {
        String type = cbTypeFilter.getValue();
        filteredList.setPredicate(c ->
            "All".equals(type) || type == null || type.equals(c.getType()));
        lblStatus.setText(filteredList.size() + " shown");
    }

    // ── Add ────────────────────────────────────────────────────────────────────
    @FXML private void onAdd() {
        showCategoryDialog(null);
    }

    // ── Edit ───────────────────────────────────────────────────────────────────
    @FXML private void onEdit() {
        Category selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        showCategoryDialog(selected);
    }

    // ── Delete ─────────────────────────────────────────────────────────────────
    @FXML private void onDelete() {
        Category selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        boolean confirmed = Alerts.showConfirmation(
            "Delete Category",
            "Delete \"" + selected.getName() + "\"? This may affect existing listings.");
        if (!confirmed) return;
        try {
            dao.delete(selected.getId());
            loadData();
            showMessage("✅ Category deleted.", true);
        } catch (SQLException e) {
            showMessage("❌ Delete error: " + e.getMessage(), false);
        }
    }

    // ── Inline add/edit dialog ─────────────────────────────────────────────────
    private void showCategoryDialog(Category existing) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existing == null ? "Add Category" : "Edit Category");
        dialog.setResizable(false);

        TextField tfName = new TextField(existing != null ? existing.getName() : "");
        tfName.setPromptText("Category name");
        tfName.setPrefWidth(220);

        ComboBox<String> cbType = new ComboBox<>();
        cbType.getItems().setAll("service", "tool");
        cbType.setValue(existing != null ? existing.getType() : "service");
        cbType.setPrefWidth(120);

        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");

        Button btnSave   = new Button("Save");
        Button btnCancel = new Button("Cancel");
        btnSave.setStyle("-fx-background-color: linear-gradient(to right, #6C63FF, #9B59B6);" +
                         "-fx-text-fill: white; -fx-font-weight: bold;" +
                         "-fx-background-radius: 15; -fx-padding: 6 18 6 18; -fx-cursor: hand;");
        btnCancel.setStyle("-fx-background-color: #f0eeff; -fx-text-fill: #6C63FF;" +
                           "-fx-background-radius: 15; -fx-padding: 6 18 6 18; -fx-cursor: hand;");

        btnCancel.setOnAction(e -> dialog.close());
        btnSave.setOnAction(e -> {
            String name = tfName.getText().trim();
            String type = cbType.getValue();
            if (name.isEmpty()) { lblErr.setText("Name is required."); return; }
            if (type == null)   { lblErr.setText("Type is required."); return; }
            try {
                if (existing == null) {
                    Category c = new Category();
                    c.setName(name);
                    c.setType(type);
                    dao.insert(c);
                    showMessage("✅ Category added.", true);
                } else {
                    existing.setName(name);
                    existing.setType(type);
                    dao.update(existing);
                    showMessage("✅ Category updated.", true);
                }
                loadData();
                dialog.close();
            } catch (SQLException ex) {
                lblErr.setText("Error: " + ex.getMessage());
            }
        });

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(20));
        Label lName = new Label("Name"); lName.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        Label lType = new Label("Type"); lType.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        grid.add(lName, 0, 0); grid.add(tfName, 1, 0);
        grid.add(lType, 0, 1); grid.add(cbType, 1, 1);

        HBox btnRow = new HBox(10, btnCancel, btnSave);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(0, 20, 16, 20));

        // Title strip
        Pane strip = new Pane();
        strip.setStyle("-fx-background-color: linear-gradient(to right, #6C63FF, #9B59B6);");
        strip.setMinHeight(4); strip.setMaxHeight(4);

        Label title = new Label(existing == null ? "➕ Add Category" : "✏️ Edit Category");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;" +
                       "-fx-padding: 12 20 4 20;");

        VBox root = new VBox(strip, title, grid, lblErr, btnRow);
        root.setStyle("-fx-background-color: white;");
        VBox.setMargin(lblErr, new Insets(0, 20, 0, 20));

        dialog.setScene(new Scene(root, 360, 200));
        dialog.showAndWait();
    }

    private void showMessage(String msg, boolean success) {
        lblMessage.setStyle(success
            ? "-fx-text-fill: #27ae60; -fx-font-size: 11px;"
            : "-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
        lblMessage.setText(msg);
    }
}
