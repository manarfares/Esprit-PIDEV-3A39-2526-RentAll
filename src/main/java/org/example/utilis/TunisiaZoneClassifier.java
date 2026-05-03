package org.example.utilis;

import java.util.HashMap;
import java.util.Map;

/**
 * Classifies a Tunisian address into one of 5 geographic zones.
 */
public class TunisiaZoneClassifier {

    public enum Zone {
        GrandTunis, CapBon, NordOuest, Centre, Sud, Unknown
    }

    private static final Map<String, Zone> ZONE_MAP = new HashMap<>();

    static {
        // Grand Tunis
        for (String k : new String[]{
            "tunis", "ariana", "ben arous", "manouba", "la marsa", "carthage",
            "sidi bou said", "la goulette", "hammam lif", "rades", "megrine",
            "mourouj", "bardo", "ettadhamen", "mnihla", "kalaat andalous"
        }) ZONE_MAP.put(k, Zone.GrandTunis);

        // Cap Bon
        for (String k : new String[]{
            "nabeul", "hammamet", "kelibia", "menzel temime", "korba",
            "soliman", "grombalia", "beni khalled", "cap bon", "takelsa",
            "menzel bouzelfa", "bou argoub", "haouaria"
        }) ZONE_MAP.put(k, Zone.CapBon);

        // Nord Ouest
        for (String k : new String[]{
            "bizerte", "beja", "jendouba", "kef", "siliana", "tabarka",
            "ain draham", "ghardimaou", "bulla regia", "nefza", "mateur",
            "menzel bourguiba", "ras jebel", "sejnane"
        }) ZONE_MAP.put(k, Zone.NordOuest);

        // Centre
        for (String k : new String[]{
            "sousse", "monastir", "mahdia", "kairouan", "kasserine", "sidi bouzid",
            "sfax", "el jem", "msaken", "enfidha", "hergla", "chebba",
            "sbeitla", "thala", "feriana", "skhira"
        }) ZONE_MAP.put(k, Zone.Centre);

        // Sud
        for (String k : new String[]{
            "gabes", "medenine", "tataouine", "gafsa", "tozeur", "kebili",
            "djerba", "zarzis", "houmt souk", "douz", "nefta", "el hamma",
            "matmata", "beni gardane", "remada", "redeyef"
        }) ZONE_MAP.put(k, Zone.Sud);
    }

    /**
     * Classifies an address string into a Tunisian zone.
     */
    public static Zone classify(String address) {
        if (address == null || address.isBlank()) return Zone.Unknown;
        String lower = address.toLowerCase();
        for (Map.Entry<String, Zone> entry : ZONE_MAP.entrySet()) {
            if (lower.contains(entry.getKey())) return entry.getValue();
        }
        return Zone.Unknown;
    }

    /**
     * Returns the DB column name for a given zone.
     */
    public static String toColumnName(Zone zone) {
        return switch (zone) {
            case GrandTunis -> "GrandTunis";
            case CapBon     -> "CapBon";
            case NordOuest  -> "NordOuest";
            case Centre     -> "Centre";
            case Sud        -> "Sud";
            default         -> null;
        };
    }

    /**
     * Returns the background color hex for a given zone.
     * GrandTunis = gray, CapBon = orange, NordOuest = green,
     * Centre = olive, Sud = sahara yellow.
     */
    public static String zoneColor(Zone zone) {
        return switch (zone) {
            case GrandTunis -> "#d6d6d6"; // gray
            case CapBon     -> "#ffcc99"; // orange
            case NordOuest  -> "#b6e8b6"; // green
            case Centre     -> "#c8c86e"; // olive
            case Sud        -> "#f5e0a0"; // sahara yellow
            default         -> "#f0f4ff"; // default light blue
        };
    }

    /**
     * Returns the border color hex for a given zone.
     */
    public static String zoneBorderColor(Zone zone) {
        return switch (zone) {
            case GrandTunis -> "#999999";
            case CapBon     -> "#e6952a";
            case NordOuest  -> "#4caf50";
            case Centre     -> "#8a8a2a";
            case Sud        -> "#c8a84b";
            default         -> "#aab4d4";
        };
    }
}
