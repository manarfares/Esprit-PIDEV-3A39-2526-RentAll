package org.example.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.example.model.Manzel;
import org.example.utilis.DatabaseConnectionManager;

/**
 * DAO for the "manzel" table.
 * Handles CRUD + listed_date + original_price + zone counter updates.
 */
public class ManzelCRUDManager {

    private final DatabaseConnectionManager connectionManager;
    private final ZoneDAO zoneDAO;

    public ManzelCRUDManager(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.zoneDAO = new ZoneDAO(connectionManager);
    }

    /**
     * Inserts a new record and increments the matching zone counter.
     */
    public boolean create(Manzel manzel) throws SQLException {
        if (manzel.getName() == null)
            throw new IllegalArgumentException("name must not be null");
        if (manzel.getName().length() > 30)
            throw new IllegalArgumentException("name must not exceed 30 characters");
        if (manzel.getAddress() == null)
            throw new IllegalArgumentException("address must not be null");
        if (manzel.getAddress().length() > 30)
            throw new IllegalArgumentException("address must not exceed 30 characters");

        String sql = "INSERT INTO manzel (Name, Address, Price, original_price, Rooms, listed_date, description, photo_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, manzel.getName());
            stmt.setString(2, manzel.getAddress());
            stmt.setInt(3, manzel.getPrice());
            stmt.setInt(4, manzel.getPrice());
            stmt.setInt(5, manzel.getRooms());
            stmt.setDate(6, java.sql.Date.valueOf(
                    manzel.getListedDate() != null ? manzel.getListedDate() : LocalDate.now()));
            stmt.setString(7, manzel.getRawDescription()); // null = auto-generate on display
            stmt.setString(8, manzel.getPhotoPath());      // null = no photo

            boolean inserted = stmt.executeUpdate() > 0;

            // Increment zone counter after successful insert
            if (inserted) zoneDAO.increment(manzel.getAddress());

            return inserted;

        } catch (SQLException e) {
            throw new SQLException("Failed to insert manzel record: " + e.getMessage(), e);
        }
    }

    /** Reads a single record by id. */
    public Manzel read(int id) throws SQLException {
        String sql = "SELECT * FROM manzel WHERE id = ?";

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
                return null;
            }

        } catch (SQLException e) {
            throw new SQLException("Failed to read manzel record with id " + id + ": " + e.getMessage(), e);
        }
    }

    /** Reads all records. */
    public List<Manzel> readAll() throws SQLException {
        String sql = "SELECT * FROM manzel";
        List<Manzel> results = new ArrayList<>();

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) results.add(mapRow(rs));
            return results;

        } catch (SQLException e) {
            throw new SQLException("Failed to read all manzel records: " + e.getMessage(), e);
        }
    }

    /**
     * Updates a record. Resets listed_date and original_price.
     * If address changed, updates zone counters accordingly.
     */
    public boolean update(Manzel manzel) throws SQLException {
        if (manzel.getName() == null)
            throw new IllegalArgumentException("name must not be null");
        if (manzel.getName().length() > 30)
            throw new IllegalArgumentException("name must not exceed 30 characters");
        if (manzel.getAddress() == null)
            throw new IllegalArgumentException("address must not be null");
        if (manzel.getAddress().length() > 30)
            throw new IllegalArgumentException("address must not exceed 30 characters");

        // Read old address to update zone counters if address changed
        Manzel old = read(manzel.getId());

        String sql = "UPDATE manzel SET Name=?, Address=?, Price=?, original_price=?, Rooms=?, listed_date=?, description=?, photo_path=? WHERE id=?";

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, manzel.getName());
            stmt.setString(2, manzel.getAddress());
            stmt.setInt(3, manzel.getPrice());
            stmt.setInt(4, manzel.getPrice());
            stmt.setInt(5, manzel.getRooms());
            stmt.setDate(6, java.sql.Date.valueOf(LocalDate.now()));
            stmt.setString(7, manzel.getRawDescription());
            stmt.setString(8, manzel.getPhotoPath());
            stmt.setInt(9, manzel.getId());

            boolean updated = stmt.executeUpdate() > 0;

            // If address changed, move zone counter
            if (updated && old != null &&
                !old.getAddress().equalsIgnoreCase(manzel.getAddress())) {
                zoneDAO.decrement(old.getAddress());
                zoneDAO.increment(manzel.getAddress());
            }

            return updated;

        } catch (SQLException e) {
            throw new SQLException("Failed to update manzel record with id " + manzel.getId() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Checks all records and applies 15% price reduction to any listed 7+ days.
     */
    public int applyPriceReductions() throws SQLException {
        List<Manzel> all = readAll();
        int count = 0;
        for (Manzel m : all) {
            if (m.needsPriceReduction()) {
                int discounted = m.effectivePrice();
                if (discounted < m.getPrice()) {
                    String sql = "UPDATE manzel SET Price = ? WHERE id = ?";
                    try (Connection conn = connectionManager.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, discounted);
                        stmt.setInt(2, m.getId());
                        stmt.executeUpdate();
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Deletes a record and decrements the matching zone counter.
     */
    public boolean delete(int id) throws SQLException {
        // Read address before deleting so we can decrement zone
        Manzel existing = read(id);

        String sql = "DELETE FROM manzel WHERE id = ?";

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, id);
            boolean deleted = stmt.executeUpdate() > 0;

            // Decrement zone counter after successful delete
            if (deleted && existing != null) {
                zoneDAO.decrement(existing.getAddress());
            }

            return deleted;

        } catch (SQLException e) {
            throw new SQLException("Failed to delete manzel record with id " + id + ": " + e.getMessage(), e);
        }
    }

    /** Maps a ResultSet row to a Manzel object. */
    private Manzel mapRow(ResultSet rs) throws SQLException {
        java.sql.Date sqlDate = rs.getDate("listed_date");
        LocalDate listedDate = sqlDate != null ? sqlDate.toLocalDate() : LocalDate.now();

        int originalPrice;
        try { originalPrice = rs.getInt("original_price"); }
        catch (SQLException e) { originalPrice = rs.getInt("Price"); }

        String description;
        try { description = rs.getString("description"); }
        catch (SQLException e) { description = null; }

        String photoPath;
        try { photoPath = rs.getString("photo_path"); }
        catch (SQLException e) { photoPath = null; }

        Manzel m = new Manzel(
                rs.getInt("id"),
                rs.getString("Name"),
                rs.getString("Address"),
                rs.getInt("Price"),
                originalPrice,
                rs.getInt("Rooms"),
                listedDate,
                description
        );
        m.setPhotoPath(photoPath);
        return m;
    }
}
