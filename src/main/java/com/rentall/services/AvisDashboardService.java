package com.rentall.services;

import com.rentall.config.DatabaseConnection;
import com.rentall.dto.AvisDashboardRow;
import com.rentall.util.SchemaColumnPicker;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service pour récupérer les avis avec les informations de logement et locataire
 * pour le dashboard des avis.
 * 
 * Relation : avis → reservation → logement
 */
public class AvisDashboardService {

    private final Connection connection = DatabaseConnection.getConnection();

    /**
     * Récupère tous les avis avec les informations de logement et locataire.
     */
    public List<AvisDashboardRow> getAvisDashboard() {
        List<AvisDashboardRow> rows = new ArrayList<>();
        
        try {
            String sql = buildDashboardQuery();
            try (PreparedStatement ps = connection.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                
                while (rs.next()) {
                    AvisDashboardRow row = new AvisDashboardRow(
                            rs.getInt("avis_id"),
                            rs.getInt("reservation_id"),
                            rs.getString("logement_libelle"),
                            rs.getString("locataire_libelle"),
                            rs.getInt("note"),
                            rs.getString("commentaire"),
                            rs.getTimestamp("date_creation") != null 
                                    ? rs.getTimestamp("date_creation").toLocalDateTime() 
                                    : null
                    );
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération dashboard avis : " + e.getMessage());
        }
        
        return rows;
    }

    /**
     * Construit la requête SQL pour le dashboard en détectant dynamiquement les tables.
     */
    private String buildDashboardQuery() throws SQLException {
        // Détecter les tables disponibles
        boolean hasLogement = SchemaColumnPicker.tableExists(connection, "logement");
        boolean hasFoyer = SchemaColumnPicker.tableExists(connection, "foyer");
        boolean hasLocataire = SchemaColumnPicker.tableExists(connection, "locataire");
        
        String logementTable = hasLogement ? "logement" : (hasFoyer ? "foyer" : null);
        String logementFk = hasLogement ? "logement_id" : (hasFoyer ? "foyer_id" : "logement_id");
        
        // Expression pour le libellé du logement
        String logementExpr = buildLogementExpr(logementTable);
        
        // Expression pour le libellé du locataire
        String locataireExpr = buildLocataireExpr(hasLocataire);
        
        // Construire la requête
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT a.id AS avis_id, ");
        sql.append("a.reservation_id, ");
        sql.append("a.note, ");
        sql.append("a.commentaire, ");
        sql.append("a.date_creation, ");
        sql.append(logementExpr).append(" AS logement_libelle, ");
        sql.append(locataireExpr).append(" AS locataire_libelle ");
        sql.append("FROM avis a ");
        sql.append("INNER JOIN reservation r ON r.id = a.reservation_id ");
        
        // Join logement
        if (logementTable != null) {
            sql.append("LEFT JOIN ").append(logementTable).append(" log ON log.id = r.").append(logementFk).append(" ");
        }
        
        // Join locataire
        if (hasLocataire) {
            sql.append("LEFT JOIN locataire loc ON loc.id = r.locataire_id ");
            // Join utilisateur si disponible
            String userTable = resolveUserTableName();
            if (userTable != null) {
                Set<String> locCols = SchemaColumnPicker.columns(connection, "locataire");
                String fkCol = locCols.contains("utilisateur_id") ? "utilisateur_id" : 
                               (locCols.contains("user_id") ? "user_id" : null);
                if (fkCol != null) {
                    sql.append("LEFT JOIN ").append(userTable).append(" u ON u.id = loc.").append(fkCol).append(" ");
                }
            }
        } else {
            // Pas de table locataire, utiliser utilisateur directement
            String userTable = resolveUserTableName();
            if (userTable != null) {
                sql.append("LEFT JOIN ").append(userTable).append(" u ON u.id = r.locataire_id ");
            }
        }
        
        sql.append("ORDER BY a.date_creation DESC, a.id DESC");
        
        return sql.toString();
    }
    
    private String buildLogementExpr(String logementTable) {
        if (logementTable == null) {
            return "CONCAT('Logement #', r.logement_id)";
        }
        
        try {
            Set<String> cols = SchemaColumnPicker.columns(connection, logementTable);
            List<String> parts = new ArrayList<>();
            
            for (String cand : new String[]{"titre", "title", "nom", "name", "libelle", "intitule"}) {
                if (cols.contains(cand)) {
                    parts.add("NULLIF(TRIM(log.`" + cand + "`), '')");
                }
            }
            
            if (parts.isEmpty()) {
                return "CONCAT('Logement #', r.logement_id)";
            }
            
            return "COALESCE(" + String.join(", ", parts) + ", CONCAT('Logement #', r.logement_id))";
        } catch (SQLException e) {
            return "CONCAT('Logement #', r.logement_id)";
        }
    }
    
    private String buildLocataireExpr(boolean hasLocataire) {
        try {
            List<String> parts = new ArrayList<>();
            
            if (hasLocataire) {
                Set<String> locCols = SchemaColumnPicker.columns(connection, "locataire");
                
                if (locCols.contains("prenom") && locCols.contains("nom")) {
                    parts.add("NULLIF(TRIM(CONCAT(IFNULL(loc.prenom,''), ' ', IFNULL(loc.nom,''))), '')");
                } else if (locCols.contains("nom")) {
                    parts.add("NULLIF(TRIM(loc.nom), '')");
                }
                if (locCols.contains("prenom") && !locCols.contains("nom")) {
                    parts.add("NULLIF(TRIM(loc.prenom), '')");
                }
                
                // Ajouter les infos utilisateur si disponibles
                String userTable = resolveUserTableName();
                if (userTable != null) {
                    Set<String> userCols = SchemaColumnPicker.columns(connection, userTable);
                    if (userCols.contains("prenom") && userCols.contains("nom")) {
                        parts.add("NULLIF(TRIM(CONCAT(IFNULL(u.prenom,''), ' ', IFNULL(u.nom,''))), '')");
                    } else if (userCols.contains("nom")) {
                        parts.add("NULLIF(TRIM(u.nom), '')");
                    }
                    for (String extra : new String[]{"email", "username"}) {
                        if (userCols.contains(extra)) {
                            parts.add("NULLIF(TRIM(u." + extra + "), '')");
                        }
                    }
                }
            } else {
                // Pas de table locataire, utiliser utilisateur
                String userTable = resolveUserTableName();
                if (userTable != null) {
                    Set<String> userCols = SchemaColumnPicker.columns(connection, userTable);
                    if (userCols.contains("prenom") && userCols.contains("nom")) {
                        parts.add("NULLIF(TRIM(CONCAT(IFNULL(u.prenom,''), ' ', IFNULL(u.nom,''))), '')");
                    } else if (userCols.contains("nom")) {
                        parts.add("NULLIF(TRIM(u.nom), '')");
                    }
                    for (String extra : new String[]{"email", "username"}) {
                        if (userCols.contains(extra)) {
                            parts.add("NULLIF(TRIM(u." + extra + "), '')");
                        }
                    }
                }
            }
            
            if (parts.isEmpty()) {
                return "CONCAT('Locataire #', r.locataire_id)";
            }
            
            return "COALESCE(" + String.join(", ", parts) + ", CONCAT('Locataire #', r.locataire_id))";
        } catch (SQLException e) {
            return "CONCAT('Locataire #', r.locataire_id)";
        }
    }
    
    private String resolveUserTableName() {
        try {
            for (String candidate : new String[]{"utilisateur", "user", "users"}) {
                if (SchemaColumnPicker.tableExists(connection, candidate)) {
                    return candidate;
                }
            }
        } catch (SQLException e) {
            // ignore
        }
        return null;
    }
}
