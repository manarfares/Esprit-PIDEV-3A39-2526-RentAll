package com.rentall.services;

import com.rentall.util.SchemaColumnPicker;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Construit une requête SELECT en lecture seule pour le tableau des réservations
 * (libellés logement / locataire à partir des tables existantes, sans DDL).
 */
final class ReservationListeQueryBuilder {

    private ReservationListeQueryBuilder() {
    }

    static String buildSelectListe(Connection conn) throws SQLException {
        // Détecter si on utilise "foyer" ou "logement"
        boolean hasFoyer = SchemaColumnPicker.tableExists(conn, "foyer");
        boolean hasLogement = SchemaColumnPicker.tableExists(conn, "logement");
        boolean hasLocataire = SchemaColumnPicker.tableExists(conn, "locataire");

        // Utiliser logement si foyer n'existe pas
        String foyerTable = hasFoyer ? "foyer" : (hasLogement ? "logement" : null);
        String foyerFkColumn = hasFoyer ? "foyer_id" : (hasLogement ? "logement_id" : "foyer_id");
        
        Set<String> foyerCols = foyerTable != null ? SchemaColumnPicker.columns(conn, foyerTable) : Set.of();
        Set<String> locCols = hasLocataire ? SchemaColumnPicker.columns(conn, "locataire") : Set.of();

        String foyerExpr = foyerLibelleExpr(foyerTable != null, foyerCols);
        String joinFoyer = foyerTable != null ? " LEFT JOIN " + foyerTable + " f ON f.id = r." + foyerFkColumn + " " : " ";

        String joinUser = "";
        List<String> locataireCoalesceParts = new ArrayList<>();
        
        // Si pas de table locataire, utiliser directement user
        if (!hasLocataire) {
            String userTable = resolveUserTableName(conn);
            if (userTable != null) {
                Set<String> userCols = SchemaColumnPicker.columns(conn, userTable);
                joinUser = " LEFT JOIN `" + userTable + "` u ON u.id = r.locataire_id ";
                locataireCoalesceParts.addAll(userLabelParts(userCols, "u"));
            }
        } else {
            locataireCoalesceParts.addAll(locataireDirectParts(locCols, "l"));
            
            if (hasLocataire) {
                String fkJoin = null;
                if (locCols.contains("utilisateur_id")) {
                    fkJoin = "u.id = l.utilisateur_id";
                } else if (locCols.contains("user_id")) {
                    fkJoin = "u.id = l.user_id";
                }
                if (fkJoin != null) {
                    String userTable = resolveUserTableName(conn);
                    if (userTable != null) {
                        Set<String> userCols = SchemaColumnPicker.columns(conn, userTable);
                        joinUser = " LEFT JOIN `" + userTable + "` u ON " + fkJoin + " ";
                        locataireCoalesceParts.addAll(userLabelParts(userCols, "u"));
                    }
                }
            }
        }

        String locExpr = coalesceExpr(locataireCoalesceParts, "CONCAT('Locataire n°', r.locataire_id)");
        String joinLoc = hasLocataire ? " LEFT JOIN locataire l ON l.id = r.locataire_id " : " ";

        return "SELECT r.id, r.date_debut, r.date_fin, r.montant_total, r.statut, r.nombre_personnes, "
                + foyerExpr + " AS foyer_libelle, "
                + locExpr + " AS locataire_libelle "
                + "FROM reservation r "
                + joinFoyer
                + joinLoc
                + joinUser
                + "ORDER BY r.date_debut DESC, r.id DESC";
    }

    private static String resolveUserTableName(Connection conn) throws SQLException {
        for (String candidate : new String[]{"utilisateur", "user", "users"}) {
            if (SchemaColumnPicker.tableExists(conn, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String foyerLibelleExpr(boolean hasFoyer, Set<String> cols) {
        if (!hasFoyer) {
            return "CONCAT('Réf. logement ', r.logement_id)";
        }
        List<String> parts = new ArrayList<>();
        for (String cand : new String[]{"titre", "title", "nom", "name", "libelle", "intitule"}) {
            if (cols.contains(cand)) {
                parts.add("NULLIF(TRIM(f.`" + cand + "`), '')");
            }
        }
        if (parts.isEmpty()) {
            return "CONCAT('Réf. logement ', r.logement_id)";
        }
        return "COALESCE(" + String.join(", ", parts) + ", CONCAT('Réf. logement ', r.logement_id))";
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
