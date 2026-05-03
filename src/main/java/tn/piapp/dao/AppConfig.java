package org.example.services;

import java.io.InputStream;
import java.util.Properties;

/**
 * Chargeur de configuration (config.properties dans resources).
 * Fournit un accès centralisé aux credentials des services externes.
 */
public class AppConfig {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = AppConfig.class.getResourceAsStream("/config.properties")) {
            if (in != null) {
                PROPS.load(in);
                System.out.println("✅ config.properties chargé (" + PROPS.size() + " clés)");
            } else {
                System.err.println("⚠️  config.properties introuvable dans resources/");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement config.properties : " + e.getMessage());
        }
    }

    public static String get(String key) {
        String v = PROPS.getProperty(key, "");
        return v == null ? "" : v.trim();
    }

    public static String get(String key, String defaultValue) {
        String v = PROPS.getProperty(key, defaultValue);
        return v == null ? defaultValue : v.trim();
    }

    public static int getInt(String key, int defaultValue) {
        try { return Integer.parseInt(PROPS.getProperty(key, "").trim()); }
        catch (Exception e) { return defaultValue; }
    }
}
