package com.rentall.services;

import com.rentall.config.DatabaseConnection;
import com.rentall.dto.LocataireListItem;
import com.rentall.util.SchemaColumnPicker;
import com.rentall.util.SelectableEntityLabelSql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Lecture des locataires pour sélection dans l'UI (aucun DDL).
 * Cherche dans l'ordre : locataire -> utilisateur -> user -> users
 */
public class LocataireService implements ILocataireService {

    /** Tables candidates pour les locataires/utilisateurs, dans l'ordre de priorité. */
    private static final String[] TABLES_CANDIDATES = {
        "locataire", "utilisateur", "user", "users"
    };

    private final Connection connection = DatabaseConnection.getConnection();

    @Override
    public List<LocataireListItem> listerLocatairesPourSelection() {
        try {
            // 1. Essai via la logique dynamique (table "locataire" avec eventuel JOIN user)
            String sql = SelectableEntityLabelSql.buildLocataireSelectSql(connection);
            if (sql != null) {
                try (PreparedStatement ps = connection.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    List<LocataireListItem> out = new ArrayList<>();
                    while (rs.next()) {
                        out.add(new LocataireListItem(rs.getInt("id"), rs.getString("libelle")));
                    }
                    if (!out.isEmpty()) {
                        return out;
                    }
                }
            }
            // 2. Fallback : chercher dans les tables candidates (utilisateur, user, users...)
            return listerDepuisTableCandidate();
        } catch (SQLException e) {
            System.err.println("LocataireService - erreur SQL : " + e.getMessage());
            return listerDepuisTableCandidate();
        }
    }

    /**
     * Parcourt TABLES_CANDIDATES et retourne les entrees de la premiere table trouvee.
     * Construit un libelle lisible a partir des colonnes disponibles (prenom, nom, email...).
     */
    private List<LocataireListItem> listerDepuisTableCandidate() {
        for (String table : TABLES_CANDIDATES) {
            try {
                if (!SchemaColumnPicker.tableExists(connection, table)) {
                    continue;
                }
                Set<String> cols = SchemaColumnPicker.columns(connection, table);
                String labelExpr = buildLabelExpr(cols, "t");
                String sql = "SELECT t.id, " + labelExpr + " AS libelle FROM `" + table + "` t ORDER BY t.id";
                try (PreparedStatement ps = connection.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    List<LocataireListItem> out = new ArrayList<>();
                    while (rs.next()) {
                        out.add(new LocataireListItem(rs.getInt("id"), rs.getString("libelle")));
                    }
                    if (!out.isEmpty()) {
                        System.out.println("LocataireService - locataires charges depuis la table [" + table + "] (" + out.size() + " entrees)");
                        return out;
                    }
                }
            } catch (SQLException ignored) {
                // table inaccessible, on essaie la suivante
            }
        }
        System.err.println("LocataireService - aucune table trouvee parmi : "
                + String.join(", ", TABLES_CANDIDATES));
        return new ArrayList<>();
    }

    /**
     * Construit une expression SQL COALESCE pour afficher un libelle lisible.
     * Priorite : prenom+nom -> nom -> email -> username -> id.
     */
    private String buildLabelExpr(Set<String> cols, String alias) {
        List<String> parts = new ArrayList<>();
        if (cols.contains("prenom") && cols.contains("nom")) {
            parts.add("NULLIF(TRIM(CONCAT(IFNULL(" + alias + ".`prenom`,''), ' ', IFNULL(" + alias + ".`nom`,''))), '')");
        } else if (cols.contains("nom")) {
            parts.add("NULLIF(TRIM(" + alias + ".`nom`), '')");
        } else if (cols.contains("prenom")) {
            parts.add("NULLIF(TRIM(" + alias + ".`prenom`), '')");
        }
        for (String extra : new String[]{"email", "mail", "username", "pseudo", "telephone", "tel"}) {
            if (cols.contains(extra)) {
                parts.add("NULLIF(TRIM(" + alias + ".`" + extra + "`), '')");
            }
        }
        if (parts.isEmpty()) {
            return "CONCAT('Locataire n', " + alias + ".id)";
        }
        parts.add("CONCAT('Locataire n', " + alias + ".id)");
        return "COALESCE(" + String.join(", ", parts) + ")";
    }
}
