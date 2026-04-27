package tn.piapp.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Drop-in container for the Canvas tile map.
 * Add it programmatically to any layout — it's a standard Region.
 */
public class MapCanvasContainer extends StackPane {

    private final MapCanvas canvas;

    public MapCanvasContainer() {
        canvas = new MapCanvas(900, 600);

        // Canvas resizes with the container
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        // Zoom buttons
        Button zoomIn  = zoomButton("+");
        Button zoomOut = zoomButton("−");
        zoomIn.setOnAction(e  -> canvas.setZoom(canvas.zoom + 1));
        zoomOut.setOnAction(e -> canvas.setZoom(canvas.zoom - 1));

        VBox zoomControls = new VBox(4, zoomIn, zoomOut);
        zoomControls.setAlignment(Pos.TOP_LEFT);
        zoomControls.setPadding(new Insets(12));
        zoomControls.setPickOnBounds(false);

        getChildren().addAll(canvas, zoomControls);
        StackPane.setAlignment(zoomControls, Pos.TOP_LEFT);

        setMinSize(0, 0);
        setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }

    // ── Public API ────────────────────────────────────────────────────────────
    public void addPin(double lat, double lng, String label, String detail) {
        canvas.addPin(lat, lng, label, detail);
    }

    public void clearPins() {
        canvas.clearPins();
    }

    public void centerOn(double lat, double lng) {
        canvas.centerOn(lat, lng);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Button zoomButton(String text) {
        Button btn = new Button(text);
        btn.setPrefSize(32, 32);
        btn.setMinSize(32, 32);
        btn.setMaxSize(32, 32);
        String base = "-fx-background-color: white;" +
                      "-fx-border-color: rgba(0,0,0,0.2);" +
                      "-fx-border-width: 0.5px;" +
                      "-fx-border-radius: 4px;" +
                      "-fx-background-radius: 4px;" +
                      "-fx-font-size: 16px;" +
                      "-fx-cursor: hand;";
        String hover = "-fx-background-color: #f4f4f4;" +
                       "-fx-border-color: rgba(0,0,0,0.3);" +
                       "-fx-border-width: 0.5px;" +
                       "-fx-border-radius: 4px;" +
                       "-fx-background-radius: 4px;" +
                       "-fx-font-size: 16px;" +
                       "-fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }
}
