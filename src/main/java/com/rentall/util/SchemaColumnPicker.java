package com.rentall.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Lecture des métadonnées JDBC pour composer des libellés sans modifier le schéma MySQL.
 */
public final class SchemaColumnPicker {

    private SchemaColumnPicker() {
    }

    public static Set<String> columns(Connection conn, String table) throws SQLException {
        Set<String> out = new HashSet<>();
        String catalog = conn.getCatalog();
        DatabaseMetaData md = conn.getMetaData();
        try (ResultSet rs = md.getColumns(catalog, null, table, "%")) {
            while (rs.next()) {
                String col = rs.getString("COLUMN_NAME");
                if (col != null) {
                    out.add(col.toLowerCase(Locale.ROOT));
                }
            }
        }
        return out;
    }

    public static boolean tableExists(Connection conn, String tableName) throws SQLException {
        String catalog = conn.getCatalog();
        DatabaseMetaData md = conn.getMetaData();
        try (ResultSet rs = md.getTables(catalog, null, tableName, new String[]{"TABLE"})) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = md.getTables(catalog, null, tableName.toUpperCase(Locale.ROOT), new String[]{"TABLE"})) {
            return rs.next();
        }
    }
}
