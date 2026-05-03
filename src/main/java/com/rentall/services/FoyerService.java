package com.rentall.services;

import com.rentall.config.DatabaseConnection;
import com.rentall.dto.FoyerListItem;
import com.rentall.util.SchemaColumnPicker;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class FoyerService implements IFoyerService {

    private final Connection connection = DatabaseConnection.getConnection();

    @Override
    public Optional<BigDecimal> getPrixParNuitParFoyerId(int foyerId) {
        try {
            String table = resolveLogementTable();
            if (table == null) {
                return Optional.empty();
            }

            Set<String> cols = SchemaColumnPicker.columns(connection, table);
            String priceColumn = pickFirst(cols,
                    "prix_par_nuit", "prix_nuit", "prixparnuit", "price_per_night", "price", "tarif_nuit");
            if (priceColumn == null) {
                return Optional.empty();
            }

            String sql = "SELECT `" + priceColumn + "` AS prix_par_nuit FROM `" + table + "` WHERE id = ? LIMIT 1";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, foyerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    BigDecimal value = rs.getBigDecimal("prix_par_nuit");
                    if (value == null || rs.wasNull()) {
                        return Optional.empty();
                    }
                    return Optional.of(value);
                }
            }
        } catch (SQLException e) {
            System.err.println("FoyerService : " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<FoyerListItem> listerFoyersPourSelection() {
        try {
            String table = resolveLogementTable();
            if (table == null) {
                return List.of();
            }

            Set<String> cols = SchemaColumnPicker.columns(connection, table);
            String priceColumn = pickFirst(cols,
                    "prix_par_nuit", "prix_nuit", "prixparnuit", "price_per_night", "price", "tarif_nuit");
            if (priceColumn == null) {
                return List.of();
            }

            String sql = buildLogementSelectSql(table, cols, priceColumn);
            try (PreparedStatement ps = connection.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                List<FoyerListItem> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new FoyerListItem(
                            rs.getInt("id"),
                            rs.getString("libelle"),
                            rs.getBigDecimal("prix_par_nuit")
                    ));
                }
                return out;
            }
        } catch (SQLException e) {
            return listerFoyersIdsSeuls();
        }
    }

    private List<FoyerListItem> listerFoyersIdsSeuls() {
        List<FoyerListItem> out = new ArrayList<>();
        try {
            String table = resolveLogementTable();
            if (table == null) {
                return out;
            }
            String sql = "SELECT id FROM `" + table + "` ORDER BY id";
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    out.add(new FoyerListItem(id, "Ref. logement " + id));
                }
            }
        } catch (SQLException ignored) {
            // table absente ou autre : liste vide
        }
        return out;
    }

    private String resolveLogementTable() throws SQLException {
        for (String candidate : new String[]{"logement", "foyer", "home"}) {
            if (SchemaColumnPicker.tableExists(connection, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private String buildLogementSelectSql(String table, Set<String> cols, String priceColumn) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT f.id, ")
                .append(buildLabelExpr(cols))
                .append(" AS libelle, f.`")
                .append(priceColumn)
                .append("` AS prix_par_nuit FROM `")
                .append(table)
                .append("` f WHERE f.`")
                .append(priceColumn)
                .append("` IS NOT NULL");

        if (cols.contains("disponible")) {
            sql.append(" AND (f.`disponible` = 1 OR f.`disponible` IS NULL)");
        }
        if (cols.contains("is_active")) {
            sql.append(" AND (f.`is_active` = 1 OR f.`is_active` IS NULL)");
        }

        sql.append(" ORDER BY libelle, f.id");
        return sql.toString();
    }

    private String buildLabelExpr(Set<String> cols) {
        List<String> parts = new ArrayList<>();
        for (String candidate : new String[]{"titre", "title", "nom", "name", "libelle", "intitule"}) {
            if (cols.contains(candidate)) {
                parts.add("NULLIF(TRIM(f.`" + candidate + "`), '')");
            }
        }
        for (String candidate : new String[]{"adresse", "address", "localisation", "city"}) {
            if (cols.contains(candidate)) {
                parts.add("NULLIF(TRIM(f.`" + candidate + "`), '')");
            }
        }
        if (parts.isEmpty()) {
            return "CONCAT('Ref. logement ', f.id)";
        }
        return "COALESCE(" + String.join(", ", parts) + ", CONCAT('Ref. logement ', f.id))";
    }

    private String pickFirst(Set<String> cols, String... candidates) {
        for (String candidate : candidates) {
            if (cols.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
