package org.example.utilis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages JDBC connections to the "dar" MySQL database.
 * Provides methods to open and safely close connections.
 */
public class DatabaseConnectionManager {

    private static final String USERNAME = "root";
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/dar";
    private static final String PASSWORD = "";

    /**
     * Opens and returns a new connection to the "dar" database.
     * @throws SQLException if the connection cannot be established
     */
    public Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(URL, USERNAME, PASSWORD);
        } catch (SQLException e) {
            throw new SQLException("Failed to connect to database 'dar' at 127.0.0.1:3306: " + e.getMessage(), e);
        }
    }

    /**
     * Safely closes the given connection.
     * Does nothing if the connection is already null or closed.
     */
    public void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Warning: Failed to close database connection: " + e.getMessage());
            }
        }
    }
}
