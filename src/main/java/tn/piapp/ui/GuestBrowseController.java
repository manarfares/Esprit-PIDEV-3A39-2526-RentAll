package tn.piapp.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.piapp.dao.ServiceDao;
import tn.piapp.dao.ToolDao;
import tn.piapp.model.Service;
import tn.piapp.model.Tool;
import tn.piapp.model.User;
import tn.piapp.service.QualityScoreResult;
import tn.piapp.service.QualityScoreService;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only card-based browse view for Guest / ROLE_HOST_PENDING users.
 * Shows only isActive = true services and tools.
 */
public class GuestBrowseController {

    @FXML private ToggleButton tbServices;
    @FXML private ToggleButton tbTools;
    @FXML private ToggleButton tbMap;
    @FXML private TextField    tfSearch;
    @FXML private TextField    tfLocation;
    @FXML private TextField    tfMinPrice;
    @FXML private TextField    tfMaxPrice;
    @FXML private Label        lblCount;
    @FXML private Label        lblSectionTitle;
    @FXML private FlowPane     cardGrid;
    @FXML private VBox         emptyState;
    @FXML private Label        lblConnected;
    @FXML private ScrollPane   scrollCards;
    @FXML private BorderPane   rootPane;

    // WebView created per-window in openMapWindow() — not a field


    private final ServiceDao          serviceDao          = new ServiceDao();
    private final ToolDao             toolDao             = new ToolDao();
    private final QualityScoreService qualityScoreService = new QualityScoreService();

    private final ObservableList<Service> allServices = FXCollections.observableArrayList();
    private final ObservableList<Tool>    allTools    = FXCollections.observableArrayList();

    private User currentUser;
    private boolean showingServices = true;
    private boolean showingMap      = false;

    // ── Card colour palettes ───────────────────────────────────────────────────
    private static final String[] SERVICE_GRADIENTS = {
        "linear-gradient(to bottom right, #a29bfe, #6C63FF)",
        "linear-gradient(to bottom right, #fd79a8, #e84393)",
        "linear-gradient(to bottom right, #55efc4, #00b894)",
        "linear-gradient(to bottom right, #fdcb6e, #e17055)",
        "linear-gradient(to bottom right, #74b9ff, #0984e3)",
        "linear-gradient(to bottom right, #fab1a0, #e17055)"
    };
    private static final String[] TOOL_GRADIENTS = {
        "linear-gradient(to bottom right, #636e72, #2d3436)",
        "linear-gradient(to bottom right, #00cec9, #00b894)",
        "linear-gradient(to bottom right, #fdcb6e, #e17055)",
        "linear-gradient(to bottom right, #6c5ce7, #a29bfe)",
        "linear-gradient(to bottom right, #fd79a8, #e84393)",
        "linear-gradient(to bottom right, #55efc4, #00b894)"
    };
    private static final String[] SERVICE_EMOJIS = {"🔧", "✂️", "🧹", "🚿", "🔌", "🎨"};
    private static final String[] TOOL_EMOJIS    = {"🔨", "🪚", "🔩", "🪛", "⚙️", "🛠️"};

    // ── Init ───────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        // Toggle group for Services / Tools / Map tabs
        ToggleGroup tg = new ToggleGroup();
        tbServices.setToggleGroup(tg);
        tbTools.setToggleGroup(tg);
        tbMap.setToggleGroup(tg);
        tbServices.setSelected(true);
        styleToggle(tbServices, true);
        styleToggle(tbTools, false);
        styleToggle(tbMap, false);

        tg.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) { tg.selectToggle(oldVal); return; }
            showingMap      = (newVal == tbMap);
            showingServices = (newVal == tbServices);
            styleToggle(tbServices, newVal == tbServices);
            styleToggle(tbTools,    newVal == tbTools);
            styleToggle(tbMap,      newVal == tbMap);

            if (showingMap) {
                // Open map in a dedicated Stage — avoids all WebView layout issues
                openMapWindow();
                // Deselect the Map tab immediately — it's a launcher, not a persistent view
                tg.selectToggle(showingServices ? tbServices : tbTools);
                styleToggle(tbServices, showingServices);
                styleToggle(tbTools, !showingServices);
                styleToggle(tbMap, false);
                showingMap = false;
            } else {
                // Swap back to card scroll view
                rootPane.setCenter(scrollCards);
                updateSectionTitle();
                renderCards();
            }
        });

        // Filter listeners
        tfSearch.textProperty().addListener((obs, o, n) -> renderCards());
        tfLocation.textProperty().addListener((obs, o, n) -> renderCards());
        tfMinPrice.textProperty().addListener((obs, o, n) -> renderCards());
        tfMaxPrice.textProperty().addListener((obs, o, n) -> renderCards());

        loadAllData();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            lblConnected.setText("● " + user.getName() + " (" + user.getRole() + ")");
        }
    }

    // ── Data loading ───────────────────────────────────────────────────────────
    private void loadAllData() {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                List<Service> services = serviceDao.findAllActive();
                List<Tool>    tools    = toolDao.findAllActive();
                javafx.application.Platform.runLater(() -> {
                    allServices.setAll(services);
                    allTools.setAll(tools);
                    renderCards();
                });
                return null;
            }
        };
        task.setOnFailed(e -> {
            lblCount.setText("Failed to load data.");
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // ── Rendering ──────────────────────────────────────────────────────────────
    private void renderCards() {
        cardGrid.getChildren().clear();

        String search  = tfSearch.getText().toLowerCase().trim();
        String loc     = tfLocation.getText().toLowerCase().trim();
        BigDecimal min = parseBD(tfMinPrice.getText());
        BigDecimal max = parseBD(tfMaxPrice.getText());

        if (showingServices) {
            List<Service> filtered = allServices.stream()
                .filter(s -> matchesText(s.getName(), s.getDescription(), s.getLocation(), search))
                .filter(s -> loc.isEmpty() || (s.getLocation() != null && s.getLocation().toLowerCase().contains(loc)))
                .filter(s -> min == null || (s.getBasePrice() != null && s.getBasePrice().compareTo(min) >= 0))
                .filter(s -> max == null || (s.getBasePrice() != null && s.getBasePrice().compareTo(max) <= 0))
                .toList();

            for (int i = 0; i < filtered.size(); i++) {
                cardGrid.getChildren().add(buildServiceCard(filtered.get(i), i));
            }
            lblCount.setText(filtered.size() + " service" + (filtered.size() != 1 ? "s" : ""));
            setEmptyState(filtered.isEmpty());
        } else {
            List<Tool> filtered = allTools.stream()
                .filter(t -> matchesText(t.getName(), t.getDescription(), t.getLocation(), search))
                .filter(t -> loc.isEmpty() || (t.getLocation() != null && t.getLocation().toLowerCase().contains(loc)))
                .filter(t -> min == null || (t.getPricePerDay() != null && t.getPricePerDay().compareTo(min) >= 0))
                .filter(t -> max == null || (t.getPricePerDay() != null && t.getPricePerDay().compareTo(max) <= 0))
                .toList();

            for (int i = 0; i < filtered.size(); i++) {
                cardGrid.getChildren().add(buildToolCard(filtered.get(i), i));
            }
            lblCount.setText(filtered.size() + " outil" + (filtered.size() != 1 ? "s" : ""));
            setEmptyState(filtered.isEmpty());
        }
    }

    // ── Card builders ──────────────────────────────────────────────────────────
    private VBox buildServiceCard(Service s, int index) {
        String gradient = SERVICE_GRADIENTS[index % SERVICE_GRADIENTS.length];
        String emoji    = SERVICE_EMOJIS[index % SERVICE_EMOJIS.length];

        VBox card = new VBox(0);
        card.setPrefWidth(230);
        card.setStyle(cardStyle(false));

        // ── Image / gradient area ──
        StackPane imgPane = new StackPane();
        imgPane.setPrefHeight(150);
        imgPane.setStyle("-fx-background-color: " + gradient + "; -fx-background-radius: 12 12 0 0;");

        // Try to load real image; fall back to emoji
        Image img = loadImage(s.getImageName());
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(230);
            iv.setFitHeight(150);
            iv.setPreserveRatio(false);
            iv.setSmooth(true);
            imgPane.getChildren().add(iv);
        } else {
            Label emojiLbl = new Label(emoji);
            emojiLbl.setStyle("-fx-font-size: 46px;");
            imgPane.getChildren().add(emojiLbl);
        }

        // Badges on top of image
        Label typeBadge = new Label("Service");
        typeBadge.setStyle("-fx-background-color: rgba(0,0,0,0.45); -fx-text-fill: white;" +
                           "-fx-font-size: 10px; -fx-padding: 3 7 3 7; -fx-background-radius: 5;");
        StackPane.setAlignment(typeBadge, Pos.TOP_LEFT);
        StackPane.setMargin(typeBadge, new Insets(8, 0, 0, 8));

        imgPane.getChildren().add(typeBadge);

        // ── Info area ──
        VBox info = new VBox(4);
        info.setPadding(new Insets(10, 12, 12, 12));

        Label name = new Label(s.getName() != null ? s.getName() : "—");
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        name.setWrapText(true);

        Label location = new Label("📍 " + (s.getLocation() != null ? s.getLocation() : "—"));
        location.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");

        Label price = new Label(s.getBasePrice() != null
            ? s.getBasePrice().toPlainString() + " DT / séance" : "Prix non défini");
        price.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #6C63FF;");

        info.getChildren().addAll(name, location, price);
        if (s.getDurationMinutes() > 0) {
            Label dur = new Label("⏱ " + s.getDurationMinutes() + " min");
            dur.setStyle("-fx-font-size: 10px; -fx-text-fill: #b2bec3;");
            info.getChildren().add(dur);
        }

        card.getChildren().addAll(imgPane, info);

        // Hover + click
        card.setOnMouseEntered(e -> card.setStyle(cardStyle(true)));
        card.setOnMouseExited(e  -> card.setStyle(cardStyle(false)));
        card.setOnMouseClicked(e -> showServiceDetail(s, gradient, emoji));

        return card;
    }

    private VBox buildToolCard(Tool t, int index) {
        String gradient = TOOL_GRADIENTS[index % TOOL_GRADIENTS.length];
        String emoji    = TOOL_EMOJIS[index % TOOL_EMOJIS.length];

        VBox card = new VBox(0);
        card.setPrefWidth(230);
        card.setStyle(cardStyle(false));

        // ── Image / gradient area ──
        StackPane imgPane = new StackPane();
        imgPane.setPrefHeight(150);
        imgPane.setStyle("-fx-background-color: " + gradient + "; -fx-background-radius: 12 12 0 0;");

        Image img = loadImage(t.getImageName());
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(230);
            iv.setFitHeight(150);
            iv.setPreserveRatio(false);
            iv.setSmooth(true);
            imgPane.getChildren().add(iv);
        } else {
            Label emojiLbl = new Label(emoji);
            emojiLbl.setStyle("-fx-font-size: 46px;");
            imgPane.getChildren().add(emojiLbl);
        }

        Label typeBadge = new Label("Outil");
        typeBadge.setStyle("-fx-background-color: rgba(0,0,0,0.45); -fx-text-fill: white;" +
                           "-fx-font-size: 10px; -fx-padding: 3 7 3 7; -fx-background-radius: 5;");
        StackPane.setAlignment(typeBadge, Pos.TOP_LEFT);
        StackPane.setMargin(typeBadge, new Insets(8, 0, 0, 8));

        imgPane.getChildren().add(typeBadge);

        // ── Info area ──
        VBox info = new VBox(4);
        info.setPadding(new Insets(10, 12, 12, 12));

        Label name = new Label(t.getName() != null ? t.getName() : "—");
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        name.setWrapText(true);

        Label location = new Label("📍 " + (t.getLocation() != null ? t.getLocation() : "—"));
        location.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");

        Label price = new Label(t.getPricePerDay() != null
            ? t.getPricePerDay().toPlainString() + " DT / jour" : "Prix non défini");
        price.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #6C63FF;");

        Label stock = new Label("📦 Stock : " + t.getStockQuantity());
        stock.setStyle("-fx-font-size: 10px; -fx-text-fill: #b2bec3;");

        info.getChildren().addAll(name, location, price, stock);
        card.getChildren().addAll(imgPane, info);

        card.setOnMouseEntered(e -> card.setStyle(cardStyle(true)));
        card.setOnMouseExited(e  -> card.setStyle(cardStyle(false)));
        card.setOnMouseClicked(e -> showToolDetail(t, gradient, emoji));

        return card;
    }

    // ── Detail popups ──────────────────────────────────────────────────────────
    private void showServiceDetail(Service s, String gradient, String emoji) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle(s.getName() != null ? s.getName() : "Service Detail");
        popup.setResizable(false);

        VBox root = buildDetailRoot(
            s.getImageName(), gradient, emoji,
            s.getName(),
            "Service",
            s.getDescription(),
            s.getBasePrice() != null ? s.getBasePrice().toPlainString() + " DT / séance" : "—",
            s.getLocation(),
            s.getDurationMinutes() > 0 ? "⏱ " + s.getDurationMinutes() + " min" : null,
            null,
            popup
        );

        popup.setScene(new Scene(root, 460, 580));
        popup.show();
    }

    private void showToolDetail(Tool t, String gradient, String emoji) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle(t.getName() != null ? t.getName() : "Tool Detail");
        popup.setResizable(false);

        VBox root = buildDetailRoot(
            t.getImageName(), gradient, emoji,
            t.getName(),
            "Outil",
            t.getDescription(),
            t.getPricePerDay() != null ? t.getPricePerDay().toPlainString() + " DT / jour" : "—",
            t.getLocation(),
            null,
            "📦 Stock : " + t.getStockQuantity(),
            popup
        );

        popup.setScene(new Scene(root, 460, 580));
        popup.show();
    }

    /**
     * Builds the detail popup layout shared by both service and tool.
     * Quality score and suggestions are intentionally excluded — those are
     * internal host/admin tools, not guest-facing information.
     */
    private VBox buildDetailRoot(
            String imageName, String gradient, String emoji,
            String name, String type, String description,
            String priceText, String location,
            String extraLine1, String extraLine2,
            Stage popup) {

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: white;");

        // ── Hero image / gradient ──
        StackPane hero = new StackPane();
        hero.setPrefHeight(200);
        hero.setStyle("-fx-background-color: " + gradient + ";");

        Image img = loadImage(imageName);
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(460);
            iv.setFitHeight(200);
            iv.setPreserveRatio(false);
            iv.setSmooth(true);
            hero.getChildren().add(iv);
        } else {
            Label emojiLbl = new Label(emoji);
            emojiLbl.setStyle("-fx-font-size: 64px;");
            hero.getChildren().add(emojiLbl);
        }

        // Type badge over hero
        Label typeBadge = new Label(type);
        typeBadge.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-text-fill: white;" +
                           "-fx-font-size: 11px; -fx-padding: 4 10 4 10; -fx-background-radius: 6;");
        StackPane.setAlignment(typeBadge, Pos.BOTTOM_LEFT);
        StackPane.setMargin(typeBadge, new Insets(0, 0, 12, 12));
        hero.getChildren().add(typeBadge);

        // ── Content area ──
        VBox content = new VBox(12);
        content.setPadding(new Insets(20, 24, 8, 24));

        // Name
        Label lblName = new Label(name != null ? name : "—");
        lblName.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        lblName.setWrapText(true);

        Separator sep1 = new Separator();

        // Price
        HBox priceRow = new HBox(8);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        Label pIcon = new Label("💰");
        pIcon.setStyle("-fx-font-size: 16px;");
        Label pLabel = new Label(priceText);
        pLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #6C63FF;");
        priceRow.getChildren().addAll(pIcon, pLabel);

        // Location
        HBox locRow = new HBox(8);
        locRow.setAlignment(Pos.CENTER_LEFT);
        Label locIcon = new Label("📍");
        locIcon.setStyle("-fx-font-size: 14px;");
        Label locLabel = new Label(location != null ? location : "—");
        locLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");
        locRow.getChildren().addAll(locIcon, locLabel);

        content.getChildren().addAll(lblName, sep1, priceRow, locRow);

        // Extra lines (duration / stock)
        if (extraLine1 != null) {
            Label el = new Label(extraLine1);
            el.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");
            content.getChildren().add(el);
        }
        if (extraLine2 != null) {
            Label el = new Label(extraLine2);
            el.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");
            content.getChildren().add(el);
        }

        // Description
        if (description != null && !description.isBlank()) {
            Separator sep2 = new Separator();
            Label descTitle = new Label("Description");
            descTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #7f8c8d;");
            Label descText = new Label(description);
            descText.setStyle("-fx-font-size: 13px; -fx-text-fill: #2c3e50;");
            descText.setWrapText(true);
            descText.setMaxWidth(412);
            content.getChildren().addAll(sep2, descTitle, descText);
        }

        // ── Close button ──
        Button btnClose = new Button("✕  Close");
        btnClose.setStyle("-fx-background-color: linear-gradient(to right, #6C63FF, #9B59B6);" +
                          "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
                          "-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 10 30 10 30;");
        btnClose.setOnAction(e -> popup.close());

        HBox btnRow = new HBox(btnClose);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setPadding(new Insets(12, 24, 20, 24));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;" +
                        "-fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(hero, scroll, btnRow);
        return root;
    }

    // ── Map window ─────────────────────────────────────────────────────────────
    private boolean mapLoaded = false;

    // ── Map window (Canvas-based — no WebView) ────────────────────────────────
    private void openMapWindow() {
        MapCanvasContainer mapContainer = new MapCanvasContainer();

        // Load pins
        for (Service s : allServices) {
            if (s.getLatitude() != null && s.getLongitude() != null) {
                String price = s.getBasePrice() != null
                    ? s.getBasePrice().toPlainString() + " DT/séance" : "—";
                mapContainer.addPin(s.getLatitude(), s.getLongitude(),
                    s.getName() != null ? s.getName() : "—", price);
            }
        }
        for (Tool t : allTools) {
            if (t.getLatitude() != null && t.getLongitude() != null) {
                String price = t.getPricePerDay() != null
                    ? t.getPricePerDay().toPlainString() + " DT/jour" : "—";
                mapContainer.addPin(t.getLatitude(), t.getLongitude(),
                    t.getName() != null ? t.getName() : "—", price);
            }
        }

        Stage mapStage = new Stage();
        mapStage.setTitle("RentAll — Map View");
        Scene scene = new Scene(mapContainer, 1000, 700);
        mapStage.setScene(scene);
        mapStage.show();
    }

    // ── Image loading ──────────────────────────────────────────────────────────
    /**
     * Tries to load an image by name.
     * First checks the filesystem path (src/main/resources/images/) so images
     * added at runtime are found without recompiling.
     * Falls back to classpath resource if the file isn't on disk.
     * Returns null if the image name is blank or loading fails.
     */
    private Image loadImage(String imageName) {
        if (imageName == null || imageName.isBlank()) return null;
        try {
            // 1. Filesystem — works for images added at runtime
            File f = new File("src/main/resources/images/" + imageName);
            if (f.exists()) {
                return new Image(f.toURI().toString(), 460, 200, false, true);
            }
            // 2. Classpath — works for images bundled at compile time
            var url = getClass().getResource("/images/" + imageName);
            if (url != null) {
                return new Image(url.toExternalForm(), 460, 200, false, true);
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private String cardStyle(boolean hovered) {
        String shadow = hovered
            ? "dropshadow(gaussian, rgba(108,99,255,0.28), 20, 0, 0, 6)"
            : "dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2)";
        return "-fx-background-color: white;" +
               "-fx-border-radius: 12; -fx-background-radius: 12;" +
               "-fx-effect: " + shadow + ";" +
               "-fx-cursor: hand;";
    }

    private String qualityColor(String rating) {
        return switch (rating) {
            case "Excellent" -> "#27ae60";
            case "Good"      -> "#f39c12";
            default          -> "#e74c3c";
        };
    }

    private boolean matchesText(String name, String desc, String loc, String search) {
        if (search.isEmpty()) return true;
        return (name != null && name.toLowerCase().contains(search))
            || (desc != null && desc.toLowerCase().contains(search))
            || (loc  != null && loc.toLowerCase().contains(search));
    }

    private BigDecimal parseBD(String text) {
        try { return new BigDecimal(text.trim()); } catch (Exception e) { return null; }
    }

    private void setEmptyState(boolean empty) {
        emptyState.setVisible(empty);
        emptyState.setManaged(empty);
        cardGrid.setVisible(!empty);
        cardGrid.setManaged(!empty);
    }

    private void updateSectionTitle() {
        lblSectionTitle.setText(showingServices
            ? "🔧 Services disponibles"
            : "🛠️ Outils disponibles");
    }

    private void styleToggle(ToggleButton btn, boolean active) {
        if (active) {
            btn.setStyle("-fx-background-color: linear-gradient(to right, #6C63FF, #9B59B6);" +
                         "-fx-text-fill: white; -fx-background-radius: 20;" +
                         "-fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 6 16 6 16;");
        } else {
            btn.setStyle("-fx-background-color: #f0eeff; -fx-text-fill: #6C63FF;" +
                         "-fx-background-radius: 20; -fx-cursor: hand;" +
                         "-fx-font-size: 12px; -fx-padding: 6 16 6 16;");
        }
    }

    // ── Navigation ─────────────────────────────────────────────────────────────
    @FXML
    public void handleRetourAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/home.fxml"));
            Parent root = loader.load();
            HomeController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            Stage stage = (Stage) cardGrid.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
