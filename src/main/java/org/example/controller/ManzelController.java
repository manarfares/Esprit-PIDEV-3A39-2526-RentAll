package org.example.controller;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.example.dao.ManzelCRUDManager;
import org.example.model.Manzel;
import org.example.utilis.DatabaseConnectionManager;
import org.example.utilis.PriceSuggestionEngine;
import org.example.utilis.TunisiaZoneClassifier;
import org.example.utilis.WeatherService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Single controller managing both the grid view and the form view
 * inside one StackPane. Views are built in Java — no sub-FXMLs needed.
 */
public class ManzelController {

    @FXML private StackPane mainContainer;

    private final ManzelCRUDManager crudManager =
            new ManzelCRUDManager(new DatabaseConnectionManager());

    // Background thread pool for weather API calls
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true); // don't block app shutdown
        return t;
    });

    private List<Manzel> allRecords = new ArrayList<>();
    private Manzel selectedRecord = null;

    // Grid view nodes
    private FlowPane cardGrid;
    private Label    lblStatus, lblSelected;
    private TextField tfSearch, tfMinPrice, tfMaxPrice, tfMinRooms;

    // Form view nodes
    private Label     lblFormTitle, lblFormStatus;
    private TextField tfFormName, tfFormAddress, tfFormPrice, tfFormRooms;
    private javafx.scene.control.TextArea taFormDescription;
    private String    selectedPhotoPath; // path chosen in the form
    private Manzel    editingRecord;

    @FXML
    public void initialize() {
        showGridView();
    }

    // ── Grid View ────────────────────────────────────────────────────────────

    private void showGridView() {
        selectedRecord = null;

        // Status bar
        lblStatus   = new Label();
        lblSelected = new Label("No record selected");
        lblSelected.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");

        // Search field
        tfSearch = new TextField();
        tfSearch.setPromptText("Search by name or address...");
        tfSearch.setOnKeyReleased(e -> applyFilters());

        // Filter fields
        tfMinPrice = new TextField(); tfMinPrice.setPromptText("0");   tfMinPrice.setPrefWidth(80);
        tfMaxPrice = new TextField(); tfMaxPrice.setPromptText("any"); tfMaxPrice.setPrefWidth(80);
        tfMinRooms = new TextField(); tfMinRooms.setPromptText("0");   tfMinRooms.setPrefWidth(60);
        tfMinPrice.setOnKeyReleased(e -> applyFilters());
        tfMaxPrice.setOnKeyReleased(e -> applyFilters());
        tfMinRooms.setOnKeyReleased(e -> applyFilters());

        Button btnClear = new Button("Clear");
        btnClear.setOnAction(e -> {
            tfSearch.clear(); tfMinPrice.clear(); tfMaxPrice.clear(); tfMinRooms.clear();
            applyFilters();
        });

        HBox filterRow = new HBox(8,
                new Label("Min price:"), tfMinPrice,
                new Label("Max price:"), tfMaxPrice,
                new Label("Min rooms:"), tfMinRooms,
                btnClear);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        VBox searchBox = new VBox(8, new Label("Search & Filter") {{
            setStyle("-fx-font-weight: bold;");
        }}, tfSearch, filterRow);
        searchBox.setPadding(new Insets(10));
        searchBox.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #ddd;" +
                           "-fx-border-radius: 6; -fx-background-radius: 6;");

        // Action buttons
        Button btnAdd     = new Button("+ Add");
        Button btnEdit    = new Button("✎ Edit");
        Button btnDelete  = new Button("✕ Delete");
        Button btnRefresh = new Button("↺ Refresh");

        btnAdd.setStyle("-fx-background-color: #3a7bd5; -fx-text-fill: white;");
        btnDelete.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white;");

        btnAdd.setOnAction(e -> showFormView(null));
        btnEdit.setOnAction(e -> {
            if (selectedRecord == null) { setStatus("Select a card first.", true); return; }
            showFormView(selectedRecord);
        });
        btnDelete.setOnAction(e -> handleDelete());
        btnRefresh.setOnAction(e -> loadAll());

        HBox topBar = new HBox(8, btnAdd, btnEdit, btnDelete, btnRefresh, lblStatus);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // Card grid
        cardGrid = new FlowPane();
        cardGrid.setHgap(12); cardGrid.setVgap(12);
        cardGrid.setPadding(new Insets(12));

        ScrollPane scroll = new ScrollPane(cardGrid);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox root = new VBox(8, topBar, searchBox, lblSelected, scroll);
        root.setPadding(new Insets(12));

        mainContainer.getChildren().setAll(root);
        // Sync zone counts from existing DB data on startup
        try {
            new org.example.dao.ZoneDAO(new org.example.utilis.DatabaseConnectionManager()).syncZoneCounts();
        } catch (Exception e) {
            System.err.println("Zone sync failed: " + e.getMessage());
        }
        // Apply automatic price reductions on every grid load
        try {
            int reduced = crudManager.applyPriceReductions();
            if (reduced > 0) System.out.println("Auto-reduced price for " + reduced + " properties.");
        } catch (SQLException e) {
            System.err.println("Price reduction check failed: " + e.getMessage());
        }
        loadAll();
    }

    // ── Form View ────────────────────────────────────────────────────────────

    private void showFormView(Manzel record) {
        editingRecord = record;
        selectedPhotoPath = record != null ? record.getPhotoPath() : null;

        lblFormTitle  = new Label(record == null ? "Add Record" : "Edit Record");
        lblFormTitle.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        lblFormStatus = new Label();

        tfFormName    = new TextField(); tfFormName.setPromptText("Name (max 30 chars)");
        tfFormAddress = new TextField(); tfFormAddress.setPromptText("Address (max 30 chars)");
        tfFormPrice   = new TextField(); tfFormPrice.setPromptText("Price (≥ 0)");
        tfFormRooms   = new TextField(); tfFormRooms.setPromptText("Rooms (≥ 0)");
        taFormDescription = new javafx.scene.control.TextArea();
        taFormDescription.setPromptText("Description (leave blank to auto-generate)");
        taFormDescription.setPrefRowCount(3);
        taFormDescription.setWrapText(true);

        if (record != null) {
            tfFormName.setText(record.getName());
            tfFormAddress.setText(record.getAddress());
            tfFormPrice.setText(String.valueOf(record.getPrice()));
            tfFormRooms.setText(String.valueOf(record.getRooms()));
            taFormDescription.setText(record.getRawDescription() != null ? record.getRawDescription() : "");
        }

        // Reject non-numeric and negative input live as user types
        addNonNegativeFilter(tfFormPrice);
        addNonNegativeFilter(tfFormRooms);

        // ── Photo picker ──────────────────────────────────────────────────
        Label lblPhotoName = new Label(selectedPhotoPath != null
                ? new File(selectedPhotoPath).getName() : "No photo selected");
        lblPhotoName.setStyle("-fx-text-fill: gray; -fx-font-size: 10;");

        ImageView photoPreview = new ImageView();
        photoPreview.setFitWidth(180);
        photoPreview.setFitHeight(120);
        photoPreview.setPreserveRatio(true);
        photoPreview.setStyle("-fx-border-color: #ccc; -fx-border-radius: 4;");
        if (selectedPhotoPath != null) {
            try {
                photoPreview.setImage(new Image(new File(selectedPhotoPath).toURI().toString()));
            } catch (Exception ignored) {}
        }

        Button btnPickPhoto = new Button("📷 Choose Photo");
        btnPickPhoto.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Property Photo");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"));
            File file = chooser.showOpenDialog(
                    ((Stage) mainContainer.getScene().getWindow()));
            if (file != null) {
                selectedPhotoPath = file.getAbsolutePath();
                lblPhotoName.setText(file.getName());
                lblPhotoName.setStyle("-fx-text-fill: #333; -fx-font-size: 10;");
                try {
                    photoPreview.setImage(new Image(file.toURI().toString()));
                } catch (Exception ignored) {}
            }
        });

        Button btnClearPhoto = new Button("✕ Remove");
        btnClearPhoto.setStyle("-fx-text-fill: #d9534f;");
        btnClearPhoto.setOnAction(e -> {
            selectedPhotoPath = null;
            photoPreview.setImage(null);
            lblPhotoName.setText("No photo selected");
            lblPhotoName.setStyle("-fx-text-fill: gray; -fx-font-size: 10;");
        });

        HBox photoButtons = new HBox(8, btnPickPhoto, btnClearPhoto);
        VBox photoBox = new VBox(6, photoButtons, lblPhotoName, photoPreview);
        // ─────────────────────────────────────────────────────────────────

        // Suggest price button
        Button btnSuggest = new Button("💡 Suggest Price");
        btnSuggest.setStyle("-fx-background-color: #f0a500; -fx-text-fill: white;");
        btnSuggest.setOnAction(e -> handleSuggestPrice());

        Label lblSuggestHint = new Label("Based on similar properties in the database");
        lblSuggestHint.setStyle("-fx-text-fill: gray; -fx-font-size: 10;");

        // Auto-generate description button
        Button btnGenDesc = new Button("✨ Auto-generate");
        btnGenDesc.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white;");
        btnGenDesc.setOnAction(e -> {
            String addr  = tfFormAddress.getText().trim();
            int    rooms = parseOrDefault(tfFormRooms.getText(), 0);
            int    price = parseOrDefault(tfFormPrice.getText(), 0);
            Manzel temp  = new Manzel("temp", addr.isEmpty() ? "unknown" : addr, price, rooms);
            taFormDescription.setText(temp.generateDescription());
        });

        Label lblDescHint = new Label("Or type your own description below");
        lblDescHint.setStyle("-fx-text-fill: gray; -fx-font-size: 10;");

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(12);
        form.addRow(0, new Label("Name:"),        tfFormName);
        form.addRow(1, new Label("Address:"),     tfFormAddress);
        form.addRow(2, new Label("Rooms:"),       tfFormRooms);
        form.addRow(3, new Label("Price:"),       tfFormPrice);
        form.addRow(4, new Label(""),             new VBox(4, btnSuggest, lblSuggestHint));
        form.addRow(5, new Label("Photo:"),       photoBox);
        form.addRow(6, new Label("Description:"), new VBox(4, new HBox(8, btnGenDesc, lblDescHint), taFormDescription));
        ColumnConstraints c1 = new ColumnConstraints(80);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(c1, c2);

        Button btnSave   = new Button("Save");
        Button btnCancel = new Button("Cancel");
        btnSave.setStyle("-fx-background-color: #3a7bd5; -fx-text-fill: white;");
        btnSave.setOnAction(e -> handleSave());
        btnCancel.setOnAction(e -> showGridView());

        HBox buttons = new HBox(8, btnSave, btnCancel);

        VBox root = new VBox(16, lblFormTitle, form, lblFormStatus, buttons);
        root.setPadding(new Insets(24));
        root.setMaxWidth(460);
        root.setAlignment(Pos.TOP_LEFT);

        mainContainer.getChildren().setAll(root);
    }

    /**
     * Restricts a TextField to non-negative integers only.
     * Rejects any input that would result in a negative number or non-digit.
     */
    private void addNonNegativeFilter(TextField tf) {
        tf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                tf.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
    }

    /** Calculates and fills in a suggested price based on address and rooms. */
    private void handleSuggestPrice() {
        String address = tfFormAddress.getText().trim();
        String roomsStr = tfFormRooms.getText().trim();

        if (address.isEmpty()) {
            setFormStatus("Enter an address first to get a suggestion.", true);
            return;
        }
        if (roomsStr.isEmpty()) {
            setFormStatus("Enter number of rooms first to get a suggestion.", true);
            return;
        }

        int rooms;
        try {
            rooms = Integer.parseInt(roomsStr);
        } catch (NumberFormatException e) {
            setFormStatus("Enter a valid room count first.", true);
            return;
        }

        int suggested = PriceSuggestionEngine.suggest(allRecords, address, rooms);

        if (suggested < 0) {
            setFormStatus("Not enough data to suggest a price yet.", true);
        } else {
            tfFormPrice.setText(String.valueOf(suggested));
            setFormStatus("Suggested price based on " + rooms + "-room properties near \"" +
                          address + "\".", false);
        }
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    private void handleDelete() {
        if (selectedRecord == null) { setStatus("Select a card first.", true); return; }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete property \"" + selectedRecord.getName() + "\"?");
        alert.setContentText("This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    crudManager.delete(selectedRecord.getId());
                    selectedRecord = null;
                    loadAll();
                    setStatus("Record deleted.", false);
                } catch (SQLException e) {
                    setStatus("Error: " + e.getMessage(), true);
                }
            }
        });
    }

    private void handleSave() {
        try {
            String name    = tfFormName.getText();
            String address = tfFormAddress.getText();
            int price      = Integer.parseInt(tfFormPrice.getText());
            int rooms      = Integer.parseInt(tfFormRooms.getText());
            String desc    = taFormDescription.getText().trim();

            if (price < 0)  { setFormStatus("Price cannot be negative.", true); return; }
            if (rooms < 0)  { setFormStatus("Rooms cannot be negative.", true); return; }

            if (editingRecord == null) {
                Manzel m = new Manzel(name, address, price, rooms);
                m.setDescription(desc.isEmpty() ? null : desc);
                m.setPhotoPath(selectedPhotoPath);
                crudManager.create(m);
            } else {
                Manzel m = new Manzel(editingRecord.getId(), name, address, price,
                        editingRecord.getOriginalPrice(), rooms, editingRecord.getListedDate(),
                        desc.isEmpty() ? null : desc);
                m.setPhotoPath(selectedPhotoPath);
                crudManager.update(m);
            }
            showGridView();

        } catch (NumberFormatException e) {
            setFormStatus("Price and Rooms must be valid numbers.", true);
        } catch (SQLException | IllegalArgumentException e) {
            setFormStatus("Error: " + e.getMessage(), true);
        }
    }

    // ── Data & Filtering ─────────────────────────────────────────────────────

    private void loadAll() {
        try {
            allRecords = crudManager.readAll();
            applyFilters();
        } catch (SQLException e) {
            setStatus("Error: " + e.getMessage(), true);
        }
    }

    private void applyFilters() {
        String search = tfSearch.getText().toLowerCase().trim();
        int minPrice  = parseOrDefault(tfMinPrice.getText(), Integer.MIN_VALUE);
        int maxPrice  = parseOrDefault(tfMaxPrice.getText(), Integer.MAX_VALUE);
        int minRooms  = parseOrDefault(tfMinRooms.getText(), Integer.MIN_VALUE);

        List<Manzel> filtered = new ArrayList<>();
        for (Manzel m : allRecords) {
            boolean ms = search.isEmpty()
                    || m.getName().toLowerCase().contains(search)
                    || m.getAddress().toLowerCase().contains(search);
            if (ms && m.getPrice() >= minPrice && m.getPrice() <= maxPrice
                   && m.getRooms() >= minRooms) {
                filtered.add(m);
            }
        }
        renderCards(filtered);
        setStatus("Showing " + filtered.size() + " of " + allRecords.size() + " records.", false);
    }

    private void renderCards(List<Manzel> list) {
        cardGrid.getChildren().clear();
        for (Manzel m : list) cardGrid.getChildren().add(createCard(m));
    }

    private VBox createCard(Manzel m) {
        // ── Photo at the top of the card ──────────────────────────────────
        VBox photoSection = new VBox();
        if (m.getPhotoPath() != null) {
            try {
                ImageView iv = new ImageView(new Image(new File(m.getPhotoPath()).toURI().toString()));
                iv.setFitWidth(176);
                iv.setFitHeight(110);
                iv.setPreserveRatio(true);
                iv.setStyle("-fx-background-radius: 6;");
                photoSection.getChildren().add(iv);
                photoSection.setStyle("-fx-alignment: center;");
            } catch (Exception ignored) {
                // Photo file missing or unreadable — skip silently
            }
        }
        // ─────────────────────────────────────────────────────────────────

        Label lName    = new Label("Name: "    + m.getName());
        Label lAddress = new Label("Address: " + m.getAddress());
        Label lRooms   = new Label("Rooms: "   + m.getRooms());
        Label lId      = new Label("ID: "      + m.getId());
        lName.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");
        lId.setStyle("-fx-text-fill: gray; -fx-font-size: 10;");

        // Price section — show both original and discounted when applicable
        VBox priceBox;
        if (m.isDiscounted()) {
            Label lDiscounted = new Label("Price: " + m.effectivePrice() + "  🏷 -15%");
            lDiscounted.setStyle("-fx-text-fill: #d9534f; -fx-font-weight: bold;");
            Label lOriginal = new Label("Was: " + m.getOriginalPrice());
            lOriginal.setStyle("-fx-text-fill: gray; -fx-font-size: 10; -fx-strikethrough: true;");
            Label lDays = new Label(m.daysListed() + " days listed");
            lDays.setStyle("-fx-font-size: 10; -fx-text-fill: gray;");
            priceBox = new VBox(2, lDiscounted, lOriginal, lDays);
        } else {
            Label lPrice = new Label("Price: " + m.getPrice());
            Label lDays  = new Label(m.daysListed() + " days listed");
            lDays.setStyle("-fx-font-size: 10; -fx-text-fill: gray;");
            priceBox = new VBox(2, lPrice, lDays);
        }

        // Weather section
        Label lWeatherTitle = new Label("🌤 Weather");
        lWeatherTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 10; -fx-padding: 6 0 2 0;");
        Label lWeather = new Label("Loading...");
        lWeather.setStyle("-fx-font-size: 10; -fx-text-fill: #555;");
        lWeather.setWrapText(true);

        // Description section
        Label lDesc = new Label(m.getDescription());
        lDesc.setStyle("-fx-font-size: 10; -fx-text-fill: #444; -fx-font-style: italic;");
        lDesc.setWrapText(true);

        VBox card = new VBox(6, photoSection, lName, lAddress, priceBox, lRooms, lId,
                             new Separator(), lDesc,
                             new Separator(), lWeatherTitle, lWeather);
        card.setPadding(new Insets(12));
        card.setPrefWidth(200);

        // Color card based on Tunisian zone
        TunisiaZoneClassifier.Zone zone = TunisiaZoneClassifier.classify(m.getAddress());
        String bg     = TunisiaZoneClassifier.zoneColor(zone);
        String border = TunisiaZoneClassifier.zoneBorderColor(zone);
        card.setStyle(normalStyle(bg, border));

        // Fetch weather on background thread
        executor.submit(() -> {
            List<WeatherService.DayWeather> weather = WeatherService.fetchWeather(m.getAddress());
            Platform.runLater(() -> {
                if (weather.isEmpty()) { lWeather.setText("Weather unavailable"); return; }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < weather.size(); i++) {
                    WeatherService.DayWeather d = weather.get(i);
                    sb.append(i == 0 ? "Today: " : d.date + ": ")
                      .append(d.description())
                      .append(" ↑").append((int) d.maxTemp)
                      .append("° ↓").append((int) d.minTemp).append("°\n");
                }
                lWeather.setText(sb.toString().trim());
            });
        });

        card.setOnMouseClicked(e -> {
            selectedRecord = m;
            lblSelected.setText("Selected: " + m.getName() + " (ID: " + m.getId() + ")");
            cardGrid.getChildren().forEach(n -> {
                // Reset each card to its own zone color
                if (n instanceof VBox v) {
                    Manzel rec = (Manzel) v.getUserData();
                    if (rec != null) {
                        TunisiaZoneClassifier.Zone z = TunisiaZoneClassifier.classify(rec.getAddress());
                        v.setStyle(normalStyle(TunisiaZoneClassifier.zoneColor(z),
                                               TunisiaZoneClassifier.zoneBorderColor(z)));
                    }
                }
            });
            card.setStyle(selectedStyle());
        });
        card.setUserData(m); // store record reference for reset on deselect
        return card;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void setStatus(String msg, boolean error) {
        lblStatus.setText(msg);
        lblStatus.setStyle(error ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }

    private void setFormStatus(String msg, boolean error) {
        lblFormStatus.setText(msg);
        lblFormStatus.setStyle(error ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }

    private int parseOrDefault(String s, int def) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return def; }
    }

    private String normalStyle(String bg, String border) {
        return "-fx-background-color: " + bg + "; -fx-border-color: " + border + ";" +
               "-fx-border-radius: 8; -fx-background-radius: 8;";
    }

    private String normalStyle() {
        return normalStyle("#f0f4ff", "#aab4d4");
    }

    private String selectedStyle() {
        return "-fx-background-color: #d0e4ff; -fx-border-color: #3a7bd5;" +
               "-fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;";
    }
}
