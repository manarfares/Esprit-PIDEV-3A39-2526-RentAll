package com.rentall.tools;

import com.rentall.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Outil pour inspecter la structure de la base de données
 * Sans rien modifier
 */
public class InspectDatabase {

    public static void main(String[] args) {
        Connection conn = DatabaseConnection.getConnection();
        
        if (conn == null) {
            System.out.println("❌ Impossible de se connecter à la base de données");
            return;
        }

        try {
            DatabaseMetaData metaData = conn.getMetaData();
            
            System.out.println("\n╔══════════════════════════════════════╗");
            System.out.println("║   TABLES DANS pidev_amine            ║");
            System.out.println("╚══════════════════════════════════════╝\n");
            
            ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                System.out.println("📋 Table: " + tableName);
                
                // Afficher les colonnes de chaque table
                ResultSet columns = metaData.getColumns(null, null, tableName, null);
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    String columnType = columns.getString("TYPE_NAME");
                    System.out.println("   └─ " + columnName + " (" + columnType + ")");
                }
                
                // Compter les lignes
                try (Statement stmt = conn.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM " + tableName);
                    if (rs.next()) {
                        System.out.println("   📊 Nombre de lignes: " + rs.getInt("total"));
                    }
                } catch (Exception e) {
                    System.out.println("   ⚠️  Impossible de compter les lignes");
                }
                
                System.out.println();
            }
            
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
