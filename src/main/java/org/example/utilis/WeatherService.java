package org.example.utilis;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Fetches weather data from Open-Meteo API.
 * Uses the Open-Meteo geocoding API to resolve address → coordinates,
 * then fetches today's weather + 3-day forecast.
 */
public class WeatherService {

    /** Represents one day's weather summary. */
    public static class DayWeather {
        public final String date;
        public final double maxTemp;
        public final double minTemp;
        public final int    weatherCode;

        public DayWeather(String date, double maxTemp, double minTemp, int weatherCode) {
            this.date        = date;
            this.maxTemp     = maxTemp;
            this.minTemp     = minTemp;
            this.weatherCode = weatherCode;
        }

        /** Returns a human-readable weather description from WMO code. */
        public String description() {
            if (weatherCode == 0)              return "☀ Clear";
            if (weatherCode <= 3)              return "⛅ Partly cloudy";
            if (weatherCode <= 48)             return "🌫 Foggy";
            if (weatherCode <= 67)             return "🌧 Rain";
            if (weatherCode <= 77)             return "❄ Snow";
            if (weatherCode <= 82)             return "🌦 Showers";
            if (weatherCode <= 99)             return "⛈ Thunderstorm";
            return "🌡 Unknown";
        }

        @Override
        public String toString() {
            return date + ": " + description() +
                   " ↑" + (int) maxTemp + "° ↓" + (int) minTemp + "°";
        }
    }

    /**
     * Fetches today + next 3 days weather for the given address.
     * Returns an empty list if the address can't be geocoded or the API fails.
     */
    public static List<DayWeather> fetchWeather(String address) {
        List<DayWeather> result = new ArrayList<>();
        try {
            // Step 1: geocode the address
            double[] coords = geocode(address);
            if (coords == null) return result;

            double lat = coords[0];
            double lon = coords[1];

            // Step 2: fetch 4-day forecast from Open-Meteo
            String url = String.format(
                "https://api.open-meteo.com/v1/forecast" +
                "?latitude=%.4f&longitude=%.4f" +
                "&daily=weathercode,temperature_2m_max,temperature_2m_min" +
                "&forecast_days=4&timezone=auto",
                lat, lon
            );

            String json = httpGet(url);
            if (json == null) return result;

            JSONObject root    = new JSONObject(json);
            JSONObject daily   = root.getJSONObject("daily");
            JSONArray  dates   = daily.getJSONArray("time");
            JSONArray  maxT    = daily.getJSONArray("temperature_2m_max");
            JSONArray  minT    = daily.getJSONArray("temperature_2m_min");
            JSONArray  codes   = daily.getJSONArray("weathercode");

            for (int i = 0; i < Math.min(4, dates.length()); i++) {
                result.add(new DayWeather(
                        dates.getString(i),
                        maxT.getDouble(i),
                        minT.getDouble(i),
                        codes.getInt(i)
                ));
            }

        } catch (Exception e) {
            // Silently fail — weather is optional
        }
        return result;
    }

    /**
     * Converts an address string to [lat, lon] using Open-Meteo geocoding API.
     * Strips leading numbers and common street words to extract the location name.
     * e.g. "123 Main Street, Algiers" → "Algiers"
     *      "45 Oak Ave Tunis"         → "Tunis"
     * Returns null if no result found.
     */
    private static double[] geocode(String address) {
        try {
            String location = extractLocation(address);
            if (location.isEmpty()) return null;

            String encoded = URLEncoder.encode(location, StandardCharsets.UTF_8);
            String url = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                         encoded + "&count=1&language=en&format=json";

            String json = httpGet(url);
            if (json == null) return null;

            JSONObject root = new JSONObject(json);
            if (!root.has("results")) return null;

            JSONArray results = root.getJSONArray("results");
            if (results.isEmpty()) return null;

            JSONObject first = results.getJSONObject(0);
            return new double[]{first.getDouble("latitude"), first.getDouble("longitude")};

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts the most likely location name from a raw address string.
     * Strategy:
     *  1. Split by spaces and commas
     *  2. Skip tokens that are: pure numbers, single chars, or known street words
     *  3. Return the last meaningful token (usually the city/area at the end)
     *     or the first meaningful token if nothing better is found
     *
     * Examples:
     *   "123 Main St Algiers"     → "Algiers"
     *   "45 Oak Avenue, Tunis"    → "Tunis"
     *   "Rue Didouche Mourad"     → "Mourad"  (falls back gracefully)
     *   "Cairo"                   → "Cairo"
     */
    static String extractLocation(String address) {
        if (address == null || address.isBlank()) return "";

        // Street-type words to skip
        java.util.Set<String> streetWords = new java.util.HashSet<>(java.util.Arrays.asList(
            "street", "st", "avenue", "ave", "road", "rd", "boulevard", "blvd",
            "lane", "ln", "drive", "dr", "court", "ct", "place", "pl",
            "way", "terrace", "ter", "rue", "شارع", "طريق"
        ));

        String[] tokens = address.split("[\\s,]+");
        String first = null;
        String last  = null;

        for (String token : tokens) {
            String t = token.trim();
            if (t.isEmpty()) continue;
            if (t.matches("\\d+"))                          continue; // pure number
            if (t.length() <= 1)                            continue; // single char
            if (streetWords.contains(t.toLowerCase()))      continue; // street word

            if (first == null) first = t;
            last = t;
        }

        // Prefer the last meaningful token (usually the city)
        // but if first == last there's only one candidate — use it
        if (last != null) return last;
        if (first != null) return first;
        return "";
    }

    /** Simple HTTP GET — returns response body as String, or null on failure. */
    private static String httpGet(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) return null;

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            return sb.toString();

        } catch (Exception e) {
            return null;
        }
    }
}
