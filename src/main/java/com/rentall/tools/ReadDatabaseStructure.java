package com.rentall.tools;

import java.sql.*;

/**
 * Outil temporaire pour lire la structure réelle de la base smart_rental_platform.
 * Lance cette classe EN PREMIER pour voir les vrais noms de tables et colonnes.
 * Après, on supprime ce fichier.
 */
public class ReadDatabaseStructure {

    public static void main(String[] args) {
        String url      = "jdbc:mysql://localhost:3306/pidev_amine";
        String user     = "root";
        String password = "";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("✅ Connecté à pidev_amine\n");

            // Liste toutes les tables
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, null, "%", new String[]{"TABLE"});

            System.out.println("=== TABLES DISPONIBLES ===");
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                System.out.println("\n📋 Table : " + tableName);

                // Pour chaque table, affiche les colonnes
                ResultSet cols = meta.getColumns(null, null, tableName, "%");
                while (cols.next()) {
                    System.out.println("   - " + cols.getString("COLUMN_NAME")
                            + " [" + cols.getString("TYPE_NAME") + "]"
                            + (cols.getString("IS_NULLABLE").equals("NO") ? " NOT NULL" : "")
                    );
                }

                // Clés primaires
                ResultSet pk = meta.getPrimaryKeys(null, null, tableName);
                while (pk.next()) {
                    System.out.println("   🔑 PK: " + pk.getString("COLUMN_NAME"));
                }

                // Clés étrangères
                ResultSet fk = meta.getImportedKeys(null, null, tableName);
                while (fk.next()) {
                    System.out.println("   🔗 FK: " + fk.getString("FKCOLUMN_NAME")
                            + " → " + fk.getString("PKTABLE_NAME")
                            + "." + fk.getString("PKCOLUMN_NAME"));
                }
            }

            conn.close();

        } catch (Exception e) {
            System.out.println("❌ Erreur : " + e.getMessage());
        }
    }
}
