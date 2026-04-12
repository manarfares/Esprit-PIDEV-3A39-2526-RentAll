package org.example.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL      = "jdbc:mysql://localhost:3306/gestionusers";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    private static Connection connection = null;

    // Pour UserDAO
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connexion établie.");
        }
        return connection;
    }

    // Pour ServiceUser
    public static Connection getConnection2() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Connexion établie.");
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur connexion : " + e.getMessage());
        }
        return connection;
    }
}