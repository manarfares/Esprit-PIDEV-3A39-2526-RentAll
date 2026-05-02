package com.rentall.services;

import com.rentall.config.DatabaseConnection;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Service pour ouvrir Google Maps avec l'adresse d'un logement.
 * Utilise l'API Desktop pour ouvrir le navigateur par défaut.
 */
public class MapsService {

    private static final String GOOGLE_MAPS_BASE_URL = "https://www.google.com/maps/search/?api=1&query=";
    
    private final Connection connection;
    
    public MapsService() {
        this.connection = DatabaseConnection.getConnection();
    }
    
    /**
     * Ouvre Google Maps avec l'adresse du logement d'une réservation.
     * @param reservationId L'ID de la réservation
     * @return true si l'ouverture a réussi, false sinon
     */
    public boolean ouvrirMapsPourReservation(int reservationId) {
        String adresse = getAdresseLogementPourReservation(reservationId);
        
        if (adresse == null || adresse.trim().isEmpty()) {
            NotificationService.showError("Erreur Maps", "Aucune adresse trouvée pour ce logement.");
            return false;
        }
        
        return ouvrirGoogleMaps(adresse);
    }
    
    /**
     * Ouvre Google Maps avec une adresse donnée.
     * @param adresse L'adresse à rechercher
     * @return true si l'ouverture a réussi, false sinon
     */
    public boolean ouvrirGoogleMaps(String adresse) {
        if (adresse == null || adresse.trim().isEmpty()) {
            NotificationService.showError("Erreur Maps", "L'adresse est vide.");
            return false;
        }
        
        // Vérifier que Desktop est supporté
        if (!Desktop.isDesktopSupported()) {
            NotificationService.showError("Erreur Maps", "L'ouverture du navigateur n'est pas supportée sur ce système.");
            return false;
        }
        
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            NotificationService.showError("Erreur Maps", "L'ouverture du navigateur n'est pas supportée.");
            return false;
        }
        
        try {
            // Encoder l'adresse pour l'URL
            String encodedAdresse = URLEncoder.encode(adresse.trim(), StandardCharsets.UTF_8);
            String url = GOOGLE_MAPS_BASE_URL + encodedAdresse;
            
            // Ouvrir dans le navigateur
            desktop.browse(URI.create(url));
            NotificationService.showSuccess("Maps", "Carte ouverte dans le navigateur");
            return true;
            
        } catch (IOException e) {
            NotificationService.showError("Erreur Maps", "Impossible d'ouvrir le navigateur : " + e.getMessage());
            return false;
        } catch (Exception e) {
            NotificationService.showError("Erreur Maps", "Erreur inattendue : " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Récupère l'adresse du logement associé à une réservation.
     * @param reservationId L'ID de la réservation
     * @return L'adresse du logement, ou null si non trouvée
     */
    private String getAdresseLogementPourReservation(int reservationId) {
        // D'abord essayer avec la table logement
        String sql = "SELECT l.adresse FROM logement l " +
                     "INNER JOIN reservation r ON r.logement_id = l.id " +
                     "WHERE r.id = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("adresse");
            }
        } catch (SQLException e) {
            // Continuer avec la table foyer
        }
        
        // Essayer avec la table foyer
        String sqlFoyer = "SELECT f.adresse FROM foyer f " +
                          "INNER JOIN reservation r ON r.logement_id = f.id " +
                          "WHERE r.id = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(sqlFoyer)) {
            ps.setInt(1, reservationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("adresse");
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération adresse: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Récupère l'adresse d'un logement par son ID.
     * @param logementId L'ID du logement
     * @return L'adresse du logement, ou null si non trouvée
     */
    public String getAdresseLogement(int logementId) {
        // Essayer avec la table logement
        String sql = "SELECT adresse FROM logement WHERE id = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, logementId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("adresse");
            }
        } catch (SQLException e) {
            // Continuer avec la table foyer
        }
        
        // Essayer avec la table foyer
        String sqlFoyer = "SELECT adresse FROM foyer WHERE id = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(sqlFoyer)) {
            ps.setInt(1, logementId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("adresse");
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération adresse: " + e.getMessage());
        }
        
        return null;
    }
}
