package tn.piapp.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.piapp.dao.ServiceDao;
import tn.piapp.dao.ToolDao;
import tn.piapp.model.Service;
import tn.piapp.model.Tool;
import tn.piapp.model.User;
import tn.piapp.service.QualityScoreResult;
import tn.piapp.service.QualityScoreService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Read-only card-based browse view for Guest / ROLE_HOST_PENDING users.
 * Shows only isActive = true services and tools.
 */
public class GuestBrowseController {

    @FXML private ToggleButton tbServices;
    @FXML private ToggleButton tbTools;
    @FXML private TextField    tfSearch;
    @FXML private TextField    tfLocation;
    @FXML private TextField    tfMinPrice;
    @FXML private TextField    tfMaxPrice;
    @FXML private Label        lblCount;
    @FXML private Label        lblSectionTitle;
    @FXML private FlowPane     cardGrid;
    @FXML private VBox         emptyState;
    @FXML private Label        lblConnected;

    private final ServiceDao          serviceDao          = new ServiceDao();
    private final ToolDao             toolDao             = new ToolDao();
    private final QualityScoreService qualityScoreService = new QualityScoreService();

    private final ObservableList<Service> allServices = FXCollections.observableArrayList();
    private final ObservableList<Tool>    allTools    = FXCollections.observableArrayList();

    private User currentUser;
    private boolean showingServices = true; // default tab

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
        // Toggle group for Services / Tools tabs
        ToggleGroup tg = new ToggleGroup();
        tbServices.setToggleGroup(tg);
        tbTools.setToggleGroup(tg);
        tbServices.setSelected(true);
        styleToggle(tbServices, true);
        styleToggle(tbTools, false);

        tg.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) { tg.selectToggle(oldVal); return; } // prevent deselect
            showingServices = (newVal == tbServices);
            styleToggle(tbServices, showingServices);
            styleToggle(tbTools, !showingServices);
            updateSectionTitle();
            renderCards();
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

        QualityScoreResult quality = qualityScoreService.score(s);

        VBox card = new VBox(8);
        card.setPrefWidth(230);
        card.setStyle("-fx-background-color: white;" +
                      "-fx-border-radius: 12; -fx-background-radius: 12;" +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);" +
                      "-fx-cursor: hand;");

        // Image area
        StackPane imgPane = new StackPane();
        imgPane.setPrefHeight(150);
        imgPane.setStyle("-fx-background-color: " + gradient + ";" +
                         "-fx-background-radius: 12 12 0 0;");

        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 46px;");

        Label typeBadge = new Label("Service");
        typeBadge.setStyle("-fx-background-color: rgba(0,0,0,0.4); -fx-text-fill: white;" +
                           "-fx-font-size: 10px; -fx-padding: 3 7 3 7; -fx-background-radius: 5;");
        StackPane.setAlignment(typeBadge, javafx.geometry.Pos.TOP_LEFT);
        StackPane.setMargin(typeBadge, new Insets(8, 0, 0, 8));

        // Quality badge
        Label qualityBadge = new Label(quality.getScore() + "% " + quality.getRating());
        qualityBadge.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-text-fill: white;" +
                              "-fx-font-size: 9px; -fx-padding: 3 7 3 7; -fx-background-radius: 5;");
        StackPane.setAlignment(qualityBadge, javafx.geometry.Pos.TOP_RIGHT);
        StackPane.setMargin(qualityBadge, new Insets(8, 8, 0, 0));

        imgPane.getChildren().addAll(emojiLbl, typeBadge, qualityBadge);

        // Info area
        VBox info = new VBox(4);
        info.setPadding(new Insets(10, 12, 12, 12));

        Label name = new Label(s.getName() != null ? s.getName() : "—");
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        name.setWrapText(true);

        Label location = new Label("📍 " + (s.getLocation() != null ? s.getLocation() : "—"));
        location.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");

        String priceText = s.getBasePrice() != null
            ? s.getBasePrice().toPlainString() + " DT / séance"
            : "Prix non défini";
        Label price = new Label(priceText);
        price.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #6C63FF;");

        if (s.getDurationMinutes() > 0) {
            Label duration = new Label("⏱ " + s.getDurationMinutes() + " min");
            duration.setStyle("-fx-font-size: 10px; -fx-text-fill: #b2bec3;");
            info.getChildren().addAll(name, location, price, duration);
        } else {
            info.getChildren().addAll(name, location, price);
        }

        card.getChildren().addAll(imgPane, info);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() +
            "-fx-effect: dropshadow(gaussian, rgba(108,99,255,0.25), 18, 0, 0, 4);"));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-radius: 12; -fx-background-radius: 12;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);" +
            "-fx-cursor: hand;"));

        return card;
    }

    private VBox buildToolCard(Tool t, int index) {
        String gradient = TOOL_GRADIENTS[index % TOOL_GRADIENTS.length];
        String emoji    = TOOL_EMOJIS[index % TOOL_EMOJIS.length];

        QualityScoreResult quality = qualityScoreService.score(t);

        VBox card = new VBox(8);
        card.setPrefWidth(230);
        card.setStyle("-fx-background-color: white;" +
                      "-fx-border-radius: 12; -fx-background-radius: 12;" +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);" +
                      "-fx-cursor: hand;");

        // Image area
        StackPane imgPane = new StackPane();
        imgPane.setPrefHeight(150);
        imgPane.setStyle("-fx-background-color: " + gradient + ";" +
                         "-fx-background-radius: 12 12 0 0;");

        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 46px;");

        Label typeBadge = new Label("Outil");
        typeBadge.setStyle("-fx-background-color: rgba(0,0,0,0.4); -fx-text-fill: white;" +
                           "-fx-font-size: 10px; -fx-padding: 3 7 3 7; -fx-background-radius: 5;");
        StackPane.setAlignment(typeBadge, javafx.geometry.Pos.TOP_LEFT);
        StackPane.setMargin(typeBadge, new Insets(8, 0, 0, 8));

        Label qualityBadge = new Label(quality.getScore() + "% " + quality.getRating());
        qualityBadge.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-text-fill: white;" +
                              "-fx-font-size: 9px; -fx-padding: 3 7 3 7; -fx-background-radius: 5;");
        StackPane.setAlignment(qualityBadge, javafx.geometry.Pos.TOP_RIGHT);
        StackPane.setMargin(qualityBadge, new Insets(8, 8, 0, 0));

        imgPane.getChildren().addAll(emojiLbl, typeBadge, qualityBadge);

        // Info area
        VBox info = new VBox(4);
        info.setPadding(new Insets(10, 12, 12, 12));

        Label name = new Label(t.getName() != null ? t.getName() : "—");
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        name.setWrapText(true);

        Label location = new Label("📍 " + (t.getLocation() != null ? t.getLocation() : "—"));
        location.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");

        String priceText = t.getPricePerDay() != null
            ? t.getPricePerDay().toPlainString() + " DT / jour"
            : "Prix non défini";
        Label price = new Label(priceText);
        price.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #6C63FF;");

        Label stock = new Label("📦 Stock : " + t.getStockQuantity());
        stock.setStyle("-fx-font-size: 10px; -fx-text-fill: #b2bec3;");

        info.getChildren().addAll(name, location, price, stock);
        card.getChildren().addAll(imgPane, info);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() +
            "-fx-effect: dropshadow(gaussian, rgba(108,99,255,0.25), 18, 0, 0, 4);"));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-radius: 12; -fx-background-radius: 12;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);" +
            "-fx-cursor: hand;"));

        return card;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
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
