package com.rentall.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Requêtes SELECT en lecture seule pour remplir des listes (logement / locataire).
 * Aucune modification du schéma ; logique alignée sur {@code ReservationListeQueryBuilder}.
 */
public final class SelectableEntityLabelSql {

    private SelectableEntityLabelSql() {
    }

    public static String buildFoyerSelectSql(Connection conn) throws SQLException {
        // Essayer d'abord logement, puis foyer
        boolean hasLogement = SchemaColumnPicker.tableExists(conn, "logement");
        boolean hasFoyer = SchemaColumnPicker.tableExists(conn, "foyer");
        
        if (!hasLogement && !hasFoyer) {
            return null;
        }
        
        String tableName = hasLogement ? "logement" : "foyer";
        Set<String> cols = SchemaColumnPicker.columns(conn, tableName);
        String expr = foyerLibelleExpr(cols);
        return "SELECT f.id, " + expr + " AS libelle FROM " + tableName + " f ORDER BY f.id";
    }

    public static String buildLocataireSelectSql(Connection conn) throws SQLException {
        if (!SchemaColumnPicker.tableExists(conn, "locataire")) {
            return null;
        }
        Set<String> locCols = SchemaColumnPicker.columns(conn, "locataire");
        List<String> locParts = new ArrayList<>(locataireDirectParts(locCols, "l"));
        String joinUser = "";
        if (locCols.contains("utilisateur_id") || locCols.contains("user_id")) {
            String fkJoin = locCols.contains("utilisateur_id")
                    ? "u.id = l.utilisateur_id"
                    : "u.id = l.user_id";
            String userTable = resolveUserTableName(conn);
            if (userTable != null) {
                Set<String> userCols = SchemaColumnPicker.columns(conn, userTable);
                joinUser = " LEFT JOIN `" + userTable + "` u ON " + fkJoin + " ";
                locParts.addAll(userLabelParts(userCols, "u"));
            }
        }
        String locExpr = coalesceExpr(locParts, "CONCAT('Locataire n°', l.id)");
        return "SELECT l.id, " + locExpr + " AS libelle FROM locataire l " + joinUser + " ORDER BY l.id";
    }

    private static String resolveUserTableName(Connection conn) throws SQLException {
        for (String candidate : new String[]{"utilisateur", "user", "users"}) {
            if (SchemaColumnPicker.tableExists(conn, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String foyerLibelleExpr(Set<String> cols) {
        List<String> parts = new ArrayList<>();
        for (String cand : new String[]{"titre", "title", "nom", "name", "libelle", "intitule"}) {
            if (cols.contains(cand)) {
                parts.add("NULLIF(TRIM(f.`" + cand + "`), '')");
            }
        }
        if (parts.isEmpty()) {
            return "CONCAT('Réf. logement ', f.id)";
        }
        return "COALESCE(" + String.join(", ", parts) + ", CONCAT('Réf. logement ', f.id))";
    }

    private static List<String> locataireDirectParts(Set<String> lc, String alias) {
        List<String> parts = new ArrayList<>();
        if (lc.contains("prenom") && lc.contains("nom")) {
            parts.add("NULLIF(TRIM(CONCAT(IFNULL(" + alias + ".`prenom`,''), ' ', IFNULL(" + alias + ".`nom`,''))), '')");
        } else if (lc.contains("nom")) {
            parts.add("NULLIF(TRIM(" + alias + ".`nom`), '')");
        }
        if (lc.contains("prenom") && !lc.contains("nom")) {
            parts.add("NULLIF(TRIM(" + alias + ".`prenom`), '')");
        }
        for (String extra : new String[]{"email", "mail", "telephone", "tel", "username", "pseudo"}) {
            if (lc.contains(extra)) {
                parts.add("NULLIF(TRIM(" + alias + ".`" + extra + "`), '')");
            }
        }
        return parts;
    }

    private static List<String> userLabelParts(Set<String> uc, String alias) {
        List<String> parts = new ArrayList<>();
        if (uc.contains("prenom") && uc.contains("nom")) {
            parts.add("NULLIF(TRIM(CONCAT(IFNULL(" + alias + ".`prenom`,''), ' ', IFNULL(" + alias + ".`nom`,''))), '')");
        } else if (uc.contains("nom")) {
            parts.add("NULLIF(TRIM(" + alias + ".`nom`), '')");
        }
        for (String extra : new String[]{"email", "mail", "username"}) {
            if (uc.contains(extra)) {
                parts.add("NULLIF(TRIM(" + alias + ".`" + extra + "`), '')");
            }
        }
        return parts;
    }

    private static String coalesceExpr(List<String> exprs, String fallbackSql) {
        List<String> nonEmpty = new ArrayList<>();
        for (String s : exprs) {
            if (s != null && !s.isBlank()) {
                nonEmpty.add(s);
            }
        }
        if (nonEmpty.isEmpty()) {
            return fallbackSql;
        }
        if (nonEmpty.size() == 1) {
            return "COALESCE(" + nonEmpty.get(0) + ", " + fallbackSql + ")";
        }
        return "COALESCE(" + String.join(", ", nonEmpty) + ", " + fallbackSql + ")";
    }
}
