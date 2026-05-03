package tn.piapp.ui;

import javafx.application.Platform;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Async tile cache. Fetches OSM tiles via HttpClient with a proper User-Agent
 * (required by OpenStreetMap tile usage policy), caches them as Image objects.
 * LRU eviction keeps memory bounded to MAX_TILES entries.
 */
public class TileCache {

    private static final int    MAX_TILES  = 256;
    private static final String TILE_URL   = "https://a.basemaps.cartocdn.com/rastertiles/voyager/%d/%d/%d.png";
    private static final String USER_AGENT = "RentAll-JavaFX/1.0 (school project)";

    // LRU cache: key = "zoom/x/y"
    private final Map<String, Image> cache = new LinkedHashMap<>(MAX_TILES, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
            return size() > MAX_TILES;
        }
    };

    // Tracks in-flight requests so we don't double-fetch
    private final Set<String> inflight = ConcurrentHashMap.newKeySet();

    // Background thread pool for submitting requests
    private final ExecutorService executor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "tile-submit");
        t.setDaemon(true);
        return t;
    });

    private final Runnable onTileLoaded;

    public TileCache(Runnable onTileLoaded) {
        this.onTileLoaded = onTileLoaded;
    }

    /**
     * Returns the tile image if cached, otherwise triggers async fetch and returns null.
     * MapCanvas will call redraw() once the tile arrives via onTileLoaded.
     */
    public Image getTile(int zoom, int x, int y) {
        String key = zoom + "/" + x + "/" + y;
        synchronized (cache) {
            Image cached = cache.get(key);
            if (cached != null) return cached;
        }
        if (inflight.add(key)) {
            executor.submit(() -> fetchTile(zoom, x, y, key));
        }
        return null;
    }

    /** Evict tiles from a specific zoom level to free memory after zoom change. */
    public void evictZoom(int zoom) {
        if (zoom < 0) return;
        String prefix = zoom + "/";
        synchronized (cache) {
            cache.keySet().removeIf(k -> k.startsWith(prefix));
        }
    }

    private void fetchTile(int zoom, int x, int y, String key) {
        try {
            String url = String.format(TILE_URL, zoom, x, y);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "image/png")
                .GET()
                .build();

            // Blocking send — fine because we're already on a background thread
            HttpResponse<byte[]> response = client.send(
                request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                byte[] bytes = response.body();
                // Image decoding must happen on the JavaFX thread when using InputStream
                Platform.runLater(() -> {
                    try {
                        Image img = new Image(new ByteArrayInputStream(bytes));
                        synchronized (cache) { cache.put(key, img); }
                        inflight.remove(key);
                        onTileLoaded.run();
                    } catch (Exception ex) {
                        inflight.remove(key);
                        System.err.println("Image parse failed: " + key);
                    }
                });
            } else {
                inflight.remove(key);
                System.err.println("Tile HTTP " + response.statusCode() + ": " + key);
            }
        } catch (Exception e) {
            inflight.remove(key);
            System.err.println("Tile fetch failed: " + key + " — " + e.getMessage());
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
