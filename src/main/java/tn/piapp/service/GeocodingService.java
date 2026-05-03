package tn.piapp.service;

import tn.piapp.db.DbConnection;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;

/**
 * Geocodes a location string to lat/lng using Nominatim (OpenStreetMap).
 *
 * Strategy:
 *  1. Check the DB cache — if any service or tool already has coordinates
 *     for the same location string, reuse them (no HTTP call).
 *  2. If not cached, call Nominatim once and return the result.
 *  3. If Nominatim fails or returns no results, return null gracefully.
 */
public class GeocodingService {

    private static final String NOMINATIM_URL =
        "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=";

    // Nominatim requires a User-Agent identifying your app
    private static final String USER_AGENT = "RentAll-JavaFX/1.0 (school project)";

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .build();

    public record LatLng(double lat, double lng) {}

    /**
     * Returns coordinates for the given location string.
     * Checks DB cache first, then calls Nominatim.
     * Returns null if geocoding fails or location is blank.
     */
    public LatLng geocode(String location) {
        if (location == null || location.isBlank()) return null;

        // 1. Check cache in DB
        LatLng cached = checkCache(location);
        if (cached != null) return cached;

        // 2. Call Nominatim
        return callNominatim(location);
    }

    // ── DB cache ───────────────────────────────────────────────────────────────
    private LatLng checkCache(String location) {
        String sql = "SELECT latitude, longitude FROM service " +
                     "WHERE location = ? AND latitude IS NOT NULL LIMIT 1";
        try {
            Connection conn = DbConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, location);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new LatLng(rs.getDouble("latitude"), rs.getDouble("longitude"));
                    }
                }
            }
            // Also check tool table
            sql = "SELECT latitude, longitude FROM tool " +
                  "WHERE location = ? AND latitude IS NOT NULL LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, location);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new LatLng(rs.getDouble("latitude"), rs.getDouble("longitude"));
                    }
                }
            }
        } catch (SQLException ignored) {}
        return null;
    }

    // ── Nominatim HTTP call ────────────────────────────────────────────────────
    private LatLng callNominatim(String location) {
        try {
            String encoded = URLEncoder.encode(location, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(NOMINATIM_URL + encoded))
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "en")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return null;

            String body = response.body();
            // Parse JSON manually — no external JSON lib needed
            // Response looks like: [{"lat":"36.8","lon":"10.18",...}]
            if (body == null || body.equals("[]") || !body.contains("\"lat\"")) return null;

            double lat = extractDouble(body, "\"lat\"");
            double lon = extractDouble(body, "\"lon\"");
            if (Double.isNaN(lat) || Double.isNaN(lon)) return null;

            return new LatLng(lat, lon);

        } catch (Exception e) {
            // Network error, timeout, parse error — fail gracefully
            return null;
        }
    }

    /** Extracts a double value after a given key in a JSON string. */
    private double extractDouble(String json, String key) {
        try {
            int idx = json.indexOf(key);
            if (idx < 0) return Double.NaN;
            int colon = json.indexOf(':', idx);
            int start = colon + 1;
            // Skip whitespace and quotes
            while (start < json.length() &&
                   (json.charAt(start) == ' ' || json.charAt(start) == '"')) start++;
            int end = start;
            while (end < json.length() &&
                   (Character.isDigit(json.charAt(end)) ||
                    json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
            return Double.parseDouble(json.substring(start, end));
        } catch (Exception e) {
            return Double.NaN;
        }
    }
}
