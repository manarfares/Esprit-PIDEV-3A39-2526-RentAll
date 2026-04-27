package tn.piapp.ui;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure JavaFX Canvas tile map — no WebView, no JavaScript, no browser quirks.
 * Fetches OpenStreetMap tiles directly as Image objects and draws them on Canvas.
 * Supports pan (drag), zoom (scroll), and listing pins with tooltips.
 */
public class MapCanvas extends Canvas {

    // ── Tile math constants ──────────────────────────────────────────────────
    private static final int    TILE_SIZE = 256;
    private static final int    MIN_ZOOM  = 2;
    private static final int    MAX_ZOOM  = 18;

    // ── Map state ────────────────────────────────────────────────────────────
    int    zoom    = 6;
    private double originX = 0;   // pixel offset of tile (0,0) from canvas top-left
    private double originY = 0;

    // ── Tile cache ───────────────────────────────────────────────────────────
    private final TileCache cache = new TileCache(this::redraw);

    // ── Listing pins ─────────────────────────────────────────────────────────
    public static class Pin {
        public final double lat, lng;
        public final String label;
        public final String detail;
        public Pin(double lat, double lng, String label, String detail) {
            this.lat = lat; this.lng = lng;
            this.label = label; this.detail = detail;
        }
    }

    private final List<Pin> pins      = new ArrayList<>();
    private       Pin       hoveredPin = null;

    // ── Drag state ───────────────────────────────────────────────────────────
    private double dragStartX, dragStartY;
    private double originXAtDrag, originYAtDrag;

    // ────────────────────────────────────────────────────────────────────────
    public MapCanvas(double width, double height) {
        super(width, height);
        centerOn(33.8869, 9.5375); // Tunisia

        widthProperty().addListener(e -> redraw());
        heightProperty().addListener(e -> redraw());

        setOnMousePressed(this::onMousePressed);
        setOnMouseDragged(this::onMouseDragged);
        setOnMouseReleased(e -> setCursor(javafx.scene.Cursor.DEFAULT));
        setOnScroll(this::onScroll);
        setOnMouseMoved(this::onMouseMoved);

        redraw();
    }

    // ── Public API ───────────────────────────────────────────────────────────
    public void addPin(double lat, double lng, String label, String detail) {
        pins.add(new Pin(lat, lng, label, detail));
        redraw();
    }

    public void clearPins() {
        pins.clear();
        hoveredPin = null;
        redraw();
    }

    public void centerOn(double lat, double lng) {
        double[] px = latLngToPixel(lat, lng, zoom);
        originX = getWidth()  / 2 - px[0];
        originY = getHeight() / 2 - px[1];
        redraw();
    }

    public void setZoom(int z) {
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, z));
        centerOn(33.8869, 9.5375);
    }

    // ── Drawing ──────────────────────────────────────────────────────────────
    public void redraw() {
        Platform.runLater(this::draw);
    }

    private void draw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        gc.setFill(Color.web("#e8e0d8"));
        gc.fillRect(0, 0, w, h);

        drawTiles(gc, w, h);
        drawPins(gc);
        drawAttribution(gc, w, h);
    }

    private void drawTiles(GraphicsContext gc, double w, double h) {
        int numTiles = (int) Math.pow(2, zoom);

        int tileXMin = (int) Math.floor(-originX / TILE_SIZE) - 1;
        int tileYMin = (int) Math.floor(-originY / TILE_SIZE) - 1;
        int tileXMax = (int) Math.ceil((w - originX) / TILE_SIZE) + 1;
        int tileYMax = (int) Math.ceil((h - originY) / TILE_SIZE) + 1;

        for (int tx = tileXMin; tx <= tileXMax; tx++) {
            for (int ty = tileYMin; ty <= tileYMax; ty++) {
                int wrappedX = ((tx % numTiles) + numTiles) % numTiles;
                if (ty < 0 || ty >= numTiles) continue;

                double drawX = originX + tx * TILE_SIZE;
                double drawY = originY + ty * TILE_SIZE;

                Image tile = cache.getTile(zoom, wrappedX, ty);
                if (tile != null && !tile.isError()) {
                    gc.drawImage(tile, drawX, drawY, TILE_SIZE, TILE_SIZE);
                } else {
                    gc.setFill(Color.web("#ddd8d0"));
                    gc.fillRect(drawX, drawY, TILE_SIZE, TILE_SIZE);
                    gc.setStroke(Color.web("#ccc6be"));
                    gc.setLineWidth(0.5);
                    gc.strokeRect(drawX, drawY, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }

    private void drawPins(GraphicsContext gc) {
        for (Pin pin : pins) {
            double[] px = latLngToPixel(pin.lat, pin.lng, zoom);
            double x = originX + px[0];
            double y = originY + px[1];
            boolean hovered = pin == hoveredPin;

            // Shadow
            gc.setFill(Color.rgb(0, 0, 0, 0.18));
            gc.fillOval(x - 9, y - 3, 18, 6);

            // Circle
            gc.setFill(hovered ? Color.web("#7F77DD") : Color.web("#534AB7"));
            gc.fillOval(x - 10, y - 22, 20, 20);

            // Point
            gc.beginPath();
            gc.moveTo(x - 5, y - 5);
            gc.lineTo(x + 5, y - 5);
            gc.lineTo(x, y);
            gc.closePath();
            gc.fill();

            // White dot
            gc.setFill(Color.WHITE);
            gc.fillOval(x - 4, y - 18, 8, 8);

            if (hovered) {
                drawTooltip(gc, x, y - 28, pin.label, pin.detail);
            }
        }
    }

    private void drawTooltip(GraphicsContext gc, double x, double y,
                              String title, String detail) {
        double padding    = 10;
        double lineHeight = 18;
        double tooltipW   = Math.max(title.length(), detail.length()) * 7.2 + padding * 2;
        double tooltipH   = detail.isEmpty()
            ? lineHeight + padding
            : lineHeight * 2 + padding;
        double tx = x - tooltipW / 2;
        double ty = y - tooltipH - 6;

        tx = Math.max(4, Math.min(getWidth()  - tooltipW - 4, tx));
        ty = Math.max(4, ty);

        gc.setFill(Color.WHITE);
        gc.fillRoundRect(tx, ty, tooltipW, tooltipH, 8, 8);
        gc.setStroke(Color.web("#534AB7", 0.4));
        gc.setLineWidth(1);
        gc.strokeRoundRect(tx, ty, tooltipW, tooltipH, 8, 8);

        gc.setFill(Color.web("#3C3489"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 13));
        gc.fillText(title, tx + padding, ty + padding + 12);

        if (!detail.isEmpty()) {
            gc.setFill(Color.web("#5F5E5A"));
            gc.setFont(Font.font("System", FontWeight.NORMAL, 12));
            gc.fillText(detail, tx + padding, ty + padding + 12 + lineHeight);
        }
    }

    private void drawAttribution(GraphicsContext gc, double w, double h) {
        String text = "© OpenStreetMap contributors";
        gc.setFill(Color.rgb(255, 255, 255, 0.85));
        gc.fillRoundRect(w - 210, h - 24, 206, 20, 4, 4);
        gc.setFill(Color.web("#444441"));
        gc.setFont(Font.font("System", FontWeight.NORMAL, 11));
        gc.fillText(text, w - 205, h - 10);
    }

    // ── Mouse events ─────────────────────────────────────────────────────────
    private void onMousePressed(MouseEvent e) {
        dragStartX    = e.getX();
        dragStartY    = e.getY();
        originXAtDrag = originX;
        originYAtDrag = originY;
        setCursor(javafx.scene.Cursor.CLOSED_HAND);
    }

    private void onMouseDragged(MouseEvent e) {
        originX = originXAtDrag + (e.getX() - dragStartX);
        originY = originYAtDrag + (e.getY() - dragStartY);
        redraw();
    }

    private void onMouseMoved(MouseEvent e) {
        Pin prev = hoveredPin;
        hoveredPin = null;
        for (Pin pin : pins) {
            double[] px  = latLngToPixel(pin.lat, pin.lng, zoom);
            double   px2 = originX + px[0];
            double   py2 = originY + px[1];
            if (Math.hypot(e.getX() - px2, e.getY() - (py2 - 11)) < 14) {
                hoveredPin = pin;
                setCursor(javafx.scene.Cursor.HAND);
                break;
            }
        }
        if (hoveredPin == null) setCursor(javafx.scene.Cursor.DEFAULT);
        if (hoveredPin != prev) redraw();
    }

    private void onScroll(ScrollEvent e) {
        double mouseX = e.getX();
        double mouseY = e.getY();

        double[] lngLat = pixelToLatLng(mouseX - originX, mouseY - originY, zoom);

        int newZoom = zoom + (e.getDeltaY() > 0 ? 1 : -1);
        newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
        if (newZoom == zoom) return;
        zoom = newZoom;

        double[] newPx = latLngToPixel(lngLat[0], lngLat[1], zoom);
        originX = mouseX - newPx[0];
        originY = mouseY - newPx[1];
        cache.evictZoom(zoom - 2);
        redraw();
    }

    // ── Tile math ────────────────────────────────────────────────────────────
    static double[] latLngToPixel(double lat, double lng, int zoom) {
        int    numTiles = 1 << zoom;
        double x        = (lng + 180.0) / 360.0 * numTiles * TILE_SIZE;
        double sinLat   = Math.sin(Math.toRadians(lat));
        double y        = (0.5 - Math.log((1 + sinLat) / (1 - sinLat)) / (4 * Math.PI))
                          * numTiles * TILE_SIZE;
        return new double[]{x, y};
    }

    static double[] pixelToLatLng(double x, double y, int zoom) {
        int    numTiles = 1 << zoom;
        double lng      = x / (numTiles * TILE_SIZE) * 360.0 - 180.0;
        double n        = Math.PI - 2 * Math.PI * y / (numTiles * TILE_SIZE);
        double lat      = Math.toDegrees(Math.atan(Math.sinh(n)));
        return new double[]{lat, lng};
    }
}
