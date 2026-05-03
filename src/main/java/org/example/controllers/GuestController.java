package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.dao.ManzelCRUDManager;
import org.example.model.Manzel;
import org.example.models.User;
import org.example.services.ServiceUser;
import org.example.utilis.DatabaseConnectionManager;
import org.example.utilis.TunisiaZoneClassifier;
import org.example.utilis.WeatherService;
import org.example.utils.IdleSessionManager;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GuestController {

    @FXML private Label    lblWelcome;
    @FXML private Label    lblRole;
    @FXML private Label    lblStatus;
    @FXML private Label    lblBadge;
    @FXML private Button   btnDevenirHost;

    // Browse section
    @FXML private FlowPane cardGrid;
    @FXML private TextField tfSearch;
    @FXML private TextField tfMinPrice;
    @FXML private TextField tfMaxPrice;
    @FXML private TextField tfMinRooms;
    @FXML private Label     lblBrowseStatus;

    private final ManzelCRUDManager crudManager =
            new ManzelCRUDManager(new DatabaseConnectionManager());
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });
    private List<Manzel> allProperties = new ArrayList<>();

    private ServiceUser service     = new ServiceUser();
    private User        currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;

        lblWelcome.setText("👋 Bienvenue, " + user.getUsername());
        lblRole.setText("Rôle : " + user.getRole());
        lblStatus.setText("Statut : " + user.getStatus());

        // Badge UNVERIFIED si HOST_PENDING
        if (user.getRole().equals("ROLE_HOST_PENDING")) {
            lblBadge.setText("⚠️ UNVERIFIED");
            lblBadge.setStyle(
                    "-fx-font-size: 10px; -fx-text-fill: white;" +
                            "-fx-background-color: #e67e22;" +
                            "-fx-padding: 3 10 3 10; -fx-background-radius: 10;" +
                            "-fx-font-weight: bold;"
            );
            btnDevenirHost.setText("⏳ Demande en attente...");
            btnDevenirHost.setDisable(true);
            btnDevenirHost.setStyle(
                    "-fx-background-color: #95a5a6; -fx-text-fill: white;" +
                            "-fx-pref-width: 370px; -fx-pref-height: 42px;" +
                            "-fx-background-radius: 21;"
            );
        } else {
            lblBadge.setText("");
        }

        // Active la déconnexion automatique sur cette scene
        Platform.runLater(() -> {
            Scene scene = lblWelcome.getScene();
            if (scene != null) {
                IdleSessionManager.attachToScene(scene, user);
            }
        });

        // Load properties for browsing
        loadProperties();
    }

    // ── Browse Properties ──────────────────────
    @FXML
    public void initialize() {
        if (tfSearch != null) {
            tfSearch.setOnKeyReleased(e -> applyFilters());
        }
        if (tfMinPrice != null) tfMinPrice.setOnKeyReleased(e -> applyFilters());
        if (tfMaxPrice != null) tfMaxPrice.setOnKeyReleased(e -> applyFilters());
        if (tfMinRooms != null) tfMinRooms.setOnKeyReleased(e -> applyFilters());
    }

    private void loadProperties() {
        try {
            allProperties = crudManager.readAll();
            applyFilters();
        } catch (SQLException e) {
            if (lblBrowseStatus != null)
                lblBrowseStatus.setText("Could not load properties: " + e.getMessage());
        }
    }

    @FXML
    public void handleClearFilters() {
        if (tfSearch   != null) tfSearch.clear();
        if (tfMinPrice != null) tfMinPrice.clear();
        if (tfMaxPrice != null) tfMaxPrice.clear();
        if (tfMinRooms != null) tfMinRooms.clear();
        applyFilters();
    }

    private void applyFilters() {
        if (cardGrid == null) return;
        String search  = tfSearch   != null ? tfSearch.getText().toLowerCase().trim() : "";
        int minPrice   = parseOrDefault(tfMinPrice != null ? tfMinPrice.getText() : "", Integer.MIN_VALUE);
        int maxPrice   = parseOrDefault(tfMaxPrice != null ? tfMaxPrice.getText() : "", Integer.MAX_VALUE);
        int minRooms   = parseOrDefault(tfMinRooms != null ? tfMinRooms.getText() : "", Integer.MIN_VALUE);

        List<Manzel> filtered = new ArrayList<>();
        for (Manzel m : allProperties) {
            boolean matchSearch = search.isEmpty()
                    || m.getName().toLowerCase().contains(search)
                    || m.getAddress().toLowerCase().contains(search);
            if (matchSearch && m.getPrice() >= minPrice && m.getPrice() <= maxPrice
                    && m.getRooms() >= minRooms) {
                filtered.add(m);
            }
        }
        renderCards(filtered);
        if (lblBrowseStatus != null)
            lblBrowseStatus.setText("Showing " + filtered.size() + " of " + allProperties.size() + " properties");
    }

    private void renderCards(List<Manzel> list) {
        cardGrid.getChildren().clear();
        for (Manzel m : list) cardGrid.getChildren().add(createPropertyCard(m));
    }

    private VBox createPropertyCard(Manzel m) {
        // Photo
        VBox photoSection = new VBox();
        if (m.getPhotoPath() != null) {
            try {
                ImageView iv = new ImageView(new Image(new File(m.getPhotoPath()).toURI().toString()));
                iv.setFitWidth(200);
                iv.setFitHeight(120);
                iv.setPreserveRatio(true);
                photoSection.getChildren().add(iv);
                photoSection.setStyle("-fx-alignment: center;");
            } catch (Exception ignored) { }
        }

        Label lName    = new Label(m.getName());
        Label lAddress = new Label("📍 " + m.getAddress());
        Label lRooms   = new Label("🛏 " + m.getRooms() + " rooms");
        lName.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        lAddress.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        lRooms.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");

        // Price
        VBox priceBox;
        if (m.isDiscounted()) {
            Label lDiscounted = new Label("💰 " + m.effectivePrice() + " TND  🏷 -15%");
            lDiscounted.setStyle("-fx-text-fill: #d9534f; -fx-font-weight: bold; -fx-font-size: 13px;");
            Label lOriginal = new Label("Was: " + m.getOriginalPrice() + " TND");
            lOriginal.setStyle("-fx-text-fill: gray; -fx-font-size: 10px; -fx-strikethrough: true;");
            priceBox = new VBox(2, lDiscounted, lOriginal);
        } else {
            Label lPrice = new Label("💰 " + m.getPrice() + " TND");
            lPrice.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            priceBox = new VBox(2, lPrice);
        }

        // Description
        Label lDesc = new Label(m.getDescription());
        lDesc.setStyle("-fx-font-size: 10px; -fx-text-fill: #444; -fx-font-style: italic;");
        lDesc.setWrapText(true);
        lDesc.setMaxWidth(210);

        // Weather
        Label lWeatherTitle = new Label("🌤 Weather");
        lWeatherTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 0 2 0;");
        Label lWeather = new Label("Loading...");
        lWeather.setStyle("-fx-font-size: 10px; -fx-text-fill: #555;");
        lWeather.setWrapText(true);
        lWeather.setMaxWidth(210);

        VBox card = new VBox(6, photoSection, lName, lAddress, priceBox, lRooms,
                new Separator(), lDesc, new Separator(), lWeatherTitle, lWeather);
        card.setPadding(new Insets(12));
        card.setPrefWidth(230);

        TunisiaZoneClassifier.Zone zone = TunisiaZoneClassifier.classify(m.getAddress());
        String bg     = TunisiaZoneClassifier.zoneColor(zone);
        String border = TunisiaZoneClassifier.zoneBorderColor(zone);
        card.setStyle("-fx-background-color: " + bg + "; -fx-border-color: " + border + ";" +
                "-fx-border-radius: 10; -fx-background-radius: 10;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

        // Fetch weather in background
        executor.submit(() -> {
            List<WeatherService.DayWeather> weather = WeatherService.fetchWeather(m.getAddress());
            Platform.runLater(() -> {
                if (weather.isEmpty()) { lWeather.setText("Weather unavailable"); return; }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(weather.size(), 3); i++) {
                    WeatherService.DayWeather d = weather.get(i);
                    sb.append(i == 0 ? "Today: " : d.date + ": ")
                      .append(d.description())
                      .append(" ↑").append((int) d.maxTemp)
                      .append("° ↓").append((int) d.minTemp).append("°\n");
                }
                lWeather.setText(sb.toString().trim());
            });
        });

        return card;
    }

    private int parseOrDefault(String s, int def) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return def; }
    }

    private boolean tokenValide() {
        if (!service.verifierToken(currentUser.getId(),
                currentUser.getSessionToken())) {
            return false;
        }
        return true;
    }

    // ── Ouvrir fenêtre Modifier ────────────────
    @FXML
    public void handleOuvrirModifier() {
        if (!tokenValide()) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/edit_profil.fxml"));
            Parent root = loader.load();
            EditUserController ctrl = loader.getController();
            ctrl.initProfil(service, currentUser);
            Stage stage = new Stage();
            stage.setTitle("✏️ Modifier mon profil");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Devenir Host ───────────────────────────
    @FXML
    public void handleDevenirHost() {
        if (!tokenValide()) return;

        service.demanderHost(currentUser.getId());

        lblRole.setText("Rôle : ROLE_HOST_PENDING");
        lblBadge.setText("⚠️ UNVERIFIED");
        lblBadge.setStyle(
                "-fx-font-size: 10px; -fx-text-fill: white;" +
                        "-fx-background-color: #e67e22;" +
                        "-fx-padding: 3 10 3 10; -fx-background-radius: 10;" +
                        "-fx-font-weight: bold;"
        );
        btnDevenirHost.setText("⏳ Demande en attente...");
        btnDevenirHost.setDisable(true);
        btnDevenirHost.setStyle(
                "-fx-background-color: #95a5a6; -fx-text-fill: white;" +
                        "-fx-pref-width: 370px; -fx-pref-height: 42px;" +
                        "-fx-background-radius: 21;"
        );
    }

    // ── Retour Accueil ─────────────────────────
    @FXML
    public void handleRetourAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/home.fxml"));
            Parent root = loader.load();
            HomeController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            Stage stage = (Stage) lblWelcome.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            stage.setMaximized(false);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}