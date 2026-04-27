package tn.piapp.ui;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.piapp.dao.CategoryDao;
import tn.piapp.dao.ToolDao;
import tn.piapp.model.Category;
import tn.piapp.model.Tool;
import tn.piapp.service.CategorySuggestionResult;
import tn.piapp.service.CategorySuggestionService;
import tn.piapp.service.GeocodingService;
import tn.piapp.service.PriceSuggestionResult;
import tn.piapp.service.PriceSuggestionService;
import tn.piapp.util.Validation;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ToolFormDialog {

    private final Stage stage;
    private Tool result;

    private final TextField tfName        = new TextField();
    private final TextArea  taDescription = new TextArea();
    private final TextField tfPricePerDay = new TextField();
    private final TextField tfStockQuantity = new TextField();
    private final TextField tfLocation    = new TextField();
    private final TextField tfImageName   = new TextField();
    private final ComboBox<Category> cbCategory = new ComboBox<>();
    private final Label lblError          = new Label();
    private final Label lblPriceSuggestion = new Label();

    // Image picker
    private final ImageView imgPreview   = new ImageView();
    private final Button    btnPickImage = new Button("Choose Image");
    private File selectedImageFile = null;

    // Geocoding
    private final GeocodingService geocodingService = new GeocodingService();
    private Double geocodedLat = null;
    private Double geocodedLng = null;

    private final ToolDao toolDao = new ToolDao();
    private final PriceSuggestionService priceSuggestionService = new PriceSuggestionService();
    private final CategorySuggestionService categorySuggestionService = new CategorySuggestionService();

    // Loaded categories — kept for category suggestion
    private List<Category> loadedCategories = List.of();

    // Last category suggestion — used by Apply button
    private CategorySuggestionResult lastSuggestion = null;

    public ToolFormDialog(Tool existing) {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.setTitle(existing == null ? "Add Tool" : "Edit Tool");

        String css = getClass().getResource("/tn/piapp/ui/styles.css").toExternalForm();

        // Style fields
        tfName.getStyleClass().add("dialog-text-field");
        taDescription.getStyleClass().add("dialog-text-area");
        taDescription.setPrefRowCount(3);
        taDescription.setWrapText(true);
        tfPricePerDay.getStyleClass().add("dialog-text-field");
        tfStockQuantity.getStyleClass().add("dialog-text-field");
        tfLocation.getStyleClass().add("dialog-text-field");
        tfImageName.getStyleClass().add("dialog-text-field");
        cbCategory.getStyleClass().add("dialog-text-field");
        cbCategory.setMaxWidth(Double.MAX_VALUE);

        lblError.getStyleClass().add("dialog-error-label");
        lblError.setVisible(false);
        lblError.setManaged(false);

        // Load categories
        loadCategories(existing);

        // Pre-populate fields in edit mode
        if (existing != null) {
            tfName.setText(existing.getName() != null ? existing.getName() : "");
            taDescription.setText(existing.getDescription() != null ? existing.getDescription() : "");
            tfPricePerDay.setText(existing.getPricePerDay() != null ? existing.getPricePerDay().toPlainString() : "");
            tfStockQuantity.setText(String.valueOf(existing.getStockQuantity()));
            tfLocation.setText(existing.getLocation() != null ? existing.getLocation() : "");
            tfImageName.setText(existing.getImageName() != null ? existing.getImageName() : "");
        }

        // Form grid — fixed column widths so labels are always visible
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16, 20, 8, 20));

        // Column 0: label (fixed 110px), Column 1: field (fills remaining space)
        ColumnConstraints col0 = new ColumnConstraints(110);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        col1.setFillWidth(true);
        grid.getColumnConstraints().addAll(col0, col1);

        addRow(grid, 0, "Name",           tfName);
        addRow(grid, 1, "Description",    taDescription);
        addRow(grid, 2, "Price / Day",    tfPricePerDay);
        addRow(grid, 3, "Stock Quantity", tfStockQuantity);
        addRow(grid, 4, "Location",       tfLocation);

        // Verify location button + status label
        Button btnVerifyLocation = new Button("Verify Location");
        btnVerifyLocation.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white;" +
                                   "-fx-font-size: 11px; -fx-background-radius: 12;" +
                                   "-fx-cursor: hand; -fx-padding: 4 12 4 12;");
        Label lblGeoStatus = new Label();
        lblGeoStatus.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");
        HBox geoRow = new HBox(8, btnVerifyLocation, lblGeoStatus);
        geoRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        grid.add(geoRow, 1, 5);

        if (existing != null && existing.getLatitude() != null) {
            geocodedLat = existing.getLatitude();
            geocodedLng = existing.getLongitude();
            lblGeoStatus.setText(String.format("%.4f, %.4f", geocodedLat, geocodedLng));
            lblGeoStatus.setStyle("-fx-font-size: 10px; -fx-text-fill: #27ae60;");
        }

        btnVerifyLocation.setOnAction(e -> {
            String loc = tfLocation.getText().trim();
            if (loc.isEmpty()) { lblGeoStatus.setText("Enter a location first."); return; }
            lblGeoStatus.setText("Searching...");
            lblGeoStatus.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");
            btnVerifyLocation.setDisable(true);
            javafx.concurrent.Task<GeocodingService.LatLng> geoTask = new javafx.concurrent.Task<>() {
                @Override protected GeocodingService.LatLng call() {
                    return geocodingService.geocode(loc);
                }
            };
            geoTask.setOnSucceeded(ev -> {
                btnVerifyLocation.setDisable(false);
                GeocodingService.LatLng result = geoTask.getValue();
                if (result != null) {
                    geocodedLat = result.lat();
                    geocodedLng = result.lng();
                    lblGeoStatus.setText(String.format("%.4f, %.4f", geocodedLat, geocodedLng));
                    lblGeoStatus.setStyle("-fx-font-size: 10px; -fx-text-fill: #27ae60;");
                } else {
                    geocodedLat = null; geocodedLng = null;
                    lblGeoStatus.setText("Location not found — will save without coordinates.");
                    lblGeoStatus.setStyle("-fx-font-size: 10px; -fx-text-fill: #e74c3c;");
                }
            });
            geoTask.setOnFailed(ev -> {
                btnVerifyLocation.setDisable(false);
                lblGeoStatus.setText("Geocoding failed.");
                lblGeoStatus.setStyle("-fx-font-size: 10px; -fx-text-fill: #e74c3c;");
            });
            Thread gt = new Thread(geoTask); gt.setDaemon(true); gt.start();
        });

        addRow(grid, 6, "Category",       cbCategory);

        // Image picker row
        imgPreview.setFitWidth(80);
        imgPreview.setFitHeight(60);
        imgPreview.setPreserveRatio(true);
        imgPreview.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 4;");
        btnPickImage.getStyleClass().add("btn-refresh");
        btnPickImage.setOnAction(e -> pickImage());

        if (existing != null && existing.getImageName() != null && !existing.getImageName().isBlank()) {
            tfImageName.setText(existing.getImageName());
            loadPreview(existing.getImageName());
        }

        HBox imageRow = new HBox(8, btnPickImage, imgPreview, tfImageName);
        imageRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        tfImageName.setEditable(false);
        tfImageName.setPrefWidth(140);
        tfImageName.getStyleClass().add("dialog-text-field");
        Label imgLabel = new Label("Image");
        imgLabel.setMinWidth(110);
        imgLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #7f8c8d;");
        grid.add(imgLabel, 0, 7);
        grid.add(imageRow, 1, 7);

        // Price suggestion label (below category row)
        lblPriceSuggestion.getStyleClass().add("price-suggestion-label");
        lblPriceSuggestion.setVisible(false);
        lblPriceSuggestion.setManaged(false);
        lblPriceSuggestion.setWrapText(true);
        grid.add(lblPriceSuggestion, 1, 9);

        // Wire price suggestion on category selection
        cbCategory.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                lblPriceSuggestion.setVisible(false);
                lblPriceSuggestion.setManaged(false);
                return;
            }
            Task<java.util.List<java.math.BigDecimal>> priceTask = new Task<>() {
                @Override protected java.util.List<java.math.BigDecimal> call() throws Exception {
                    return toolDao.getPricesByCategory(newVal.getId());
                }
            };
            priceTask.setOnSucceeded(e -> {
                java.util.List<java.math.BigDecimal> prices = priceTask.getValue();
                if (prices.isEmpty()) {
                    lblPriceSuggestion.setText("No price data available for this category.");
                } else {
                    PriceSuggestionResult r = priceSuggestionService.suggest(prices);
                    lblPriceSuggestion.setText(String.format(
                        "Suggested: %.2f  (min %.2f · max %.2f · avg %.2f · %d listings)",
                        r.getSuggested(), r.getMin(), r.getMax(), r.getMean(), r.getCount()));
                }
                lblPriceSuggestion.setVisible(true);
                lblPriceSuggestion.setManaged(true);
            });
            priceTask.setOnFailed(e -> {
                lblPriceSuggestion.setText("Could not load price data.");
                lblPriceSuggestion.setVisible(true);
                lblPriceSuggestion.setManaged(true);
            });
            Thread t = new Thread(priceTask);
            t.setDaemon(true);
            t.start();
        });

        // Category suggestion label + Apply button
        Label lblCategorySuggestion = new Label();
        lblCategorySuggestion.getStyleClass().add("category-suggestion-label");
        lblCategorySuggestion.setVisible(false);
        lblCategorySuggestion.setManaged(false);

        Button btnApplySuggestion = new Button("Apply");
        btnApplySuggestion.getStyleClass().add("btn-apply-suggestion");
        btnApplySuggestion.setVisible(false);
        btnApplySuggestion.setManaged(false);
        btnApplySuggestion.setOnAction(e -> {
            if (lastSuggestion != null && lastSuggestion.hasMatch()) {
                cbCategory.setValue(lastSuggestion.getCategory());
            }
        });

        HBox suggestionRow = new HBox(6, lblCategorySuggestion, btnApplySuggestion);
        suggestionRow.setStyle("-fx-alignment: center-left;");
        grid.add(suggestionRow, 1, 10);

        // Wire category suggestion on name/description typing
        javafx.beans.value.ChangeListener<String> suggestionListener = (obs, o, n) -> {
            String combined = tfName.getText() + " " + taDescription.getText();
            lastSuggestion = categorySuggestionService.suggest(combined, loadedCategories);
            if (lastSuggestion.hasMatch()) {
                lblCategorySuggestion.setText(
                    "Suggested: " + lastSuggestion.getCategory().getName()
                    + " (" + lastSuggestion.getConfidence() + "% match)");
                lblCategorySuggestion.setVisible(true);
                lblCategorySuggestion.setManaged(true);
                btnApplySuggestion.setVisible(true);
                btnApplySuggestion.setManaged(true);
            } else {
                lblCategorySuggestion.setVisible(false);
                lblCategorySuggestion.setManaged(false);
                btnApplySuggestion.setVisible(false);
                btnApplySuggestion.setManaged(false);
            }
        };
        tfName.textProperty().addListener(suggestionListener);
        taDescription.textProperty().addListener(suggestionListener);
        Button btnSave   = new Button("Save");
        Button btnCancel = new Button("Cancel");
        btnSave.getStyleClass().add("btn-save");
        btnCancel.getStyleClass().add("btn-cancel");
        btnSave.setOnAction(e -> onSave(existing));
        btnCancel.setOnAction(e -> stage.close());

        HBox btnRow = new HBox(10, btnCancel, btnSave);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(8, 20, 16, 20));

        // Title strip
        Label title = new Label(existing == null ? "Add Tool" : "Edit Tool");
        title.getStyleClass().add("dialog-title-label");

        Pane strip = new Pane();
        strip.getStyleClass().add("dialog-gradient-strip");
        strip.setMinHeight(4);
        strip.setMaxHeight(4);

        VBox root = new VBox(strip, title, grid, lblError, btnRow);
        root.getStyleClass().add("dialog-root");

        Scene scene = new Scene(root, 520, 620);
        scene.getStylesheets().add(css);
        stage.setScene(scene);
    }

    private void pickImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Image");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            selectedImageFile = file;
            tfImageName.setText(file.getName());
            imgPreview.setImage(new Image(file.toURI().toString(), 80, 60, true, true));
        }
    }

    private void loadPreview(String imageName) {
        try {
            var url = getClass().getResource("/images/" + imageName);
            if (url != null) {
                imgPreview.setImage(new Image(url.toExternalForm(), 80, 60, true, true));
            }
        } catch (Exception ignored) {}
    }

    private String copyImageToResources(File src) throws IOException {
        Path imagesDir = Paths.get("src/main/resources/images");
        Files.createDirectories(imagesDir);
        Path dest = imagesDir.resolve(src.getName());
        Files.copy(src.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        return src.getName();
    }

    private void loadCategories(Tool existing) {
        try {
            loadedCategories = new CategoryDao().findByType("tool");
            cbCategory.getItems().setAll(loadedCategories);

            // Pre-select matching category in edit mode
            if (existing != null && existing.getCategoryId() != null) {
                loadedCategories.stream()
                        .filter(c -> c.getId() == existing.getCategoryId())
                        .findFirst()
                        .ifPresent(cbCategory::setValue);
            }
        } catch (SQLException e) {
            // Leave ComboBox empty on error — not a blocking failure
            lblError.setText("Could not load categories.");
            lblError.setVisible(true);
            lblError.setManaged(true);
        }
    }

    private void addRow(GridPane grid, int row, String labelText, Control field) {
        Label lbl = new Label(labelText);
        lbl.setMinWidth(110);
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #7f8c8d;");
        grid.add(lbl, 0, row);
        grid.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
        GridPane.setFillWidth(field, true);
    }

    private void onSave(Tool existing) {
        BigDecimal pricePerDay;
        int stockQuantity;

        try {
            pricePerDay = new BigDecimal(tfPricePerDay.getText().trim());
        } catch (NumberFormatException e) {
            showError("Price per day must be a valid number.");
            return;
        }
        try {
            stockQuantity = Integer.parseInt(tfStockQuantity.getText().trim());
        } catch (NumberFormatException e) {
            showError("Stock quantity must be a valid integer.");
            return;
        }

        Tool t = new Tool();
        if (existing != null) {
            t.setId(existing.getId());
            t.setCreatedAt(existing.getCreatedAt());
            t.setHostId(existing.getHostId());
            t.setActive(existing.isActive()); // preserve existing active state on edit
        }
        t.setName(tfName.getText().trim());
        t.setDescription(taDescription.getText().trim());
        t.setPricePerDay(pricePerDay);
        t.setStockQuantity(stockQuantity);
        t.setLocation(tfLocation.getText().trim());
        t.setImageName(tfImageName.getText().trim());
        t.setCategoryId(cbCategory.getValue() != null ? cbCategory.getValue().getId() : null);

        String error = Validation.validateTool(t);
        if (error != null) {
            showError(error);
            return;
        }

        // Copy selected image file to resources/images/
        if (selectedImageFile != null) {
            try {
                String savedName = copyImageToResources(selectedImageFile);
                t.setImageName(savedName);
            } catch (IOException e) {
                showError("Could not save image: " + e.getMessage());
                return;
            }
        }

        // Store geocoded coordinates
        t.setLatitude(geocodedLat);
        t.setLongitude(geocodedLng);

        result = t;
        stage.close();
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    /** Exposes loaded categories for use by category suggestion (added in a later task). */
    public List<Category> getLoadedCategories() {
        return loadedCategories;
    }

    public Optional<Tool> showAndWait() {
        stage.showAndWait();
        return Optional.ofNullable(result);
    }

    public Tool getResult() {
        return result;
    }
}
