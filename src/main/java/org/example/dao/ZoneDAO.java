package org.example.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.example.utilis.DatabaseConnectionManager;
import org.example.utilis.TunisiaZoneClassifier;
import org.example.utilis.TunisiaZoneClassifier.Zone;

/**
 * DAO for the "zone" table.
 * The zone table has a single row with 5 INT counters:
 * GrandTunis, CapBon, NordOuest, Centre, Sud.
 *
 * Increments/decrements the correct counter based on property address.
 */
public class ZoneDAO {

    private final DatabaseConnectionManager connectionManager;

    public ZoneDAO(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Increments the zone counter matching the given address.
     * Called when a property is added.
     */
    public void increment(String address) throws SQLException {
        Zone zone = TunisiaZoneClassifier.classify(address);
        String col = TunisiaZoneClassifier.toColumnName(zone);
        if (col == null) return; // Unknown zone — skip

        String sql = "UPDATE zone SET " + col + " = " + col + " + 1";
        execute(sql);
    }

    /**
     * Decrements the zone counter matching the given address.
     * Called when a property is deleted.
     */
    public void decrement(String address) throws SQLException {
        Zone zone = TunisiaZoneClassifier.classify(address);
        String col = TunisiaZoneClassifier.toColumnName(zone);
        if (col == null) return;

        // Don't go below 0
        String sql = "UPDATE zone SET " + col + " = GREATEST(0, " + col + " - 1)";
        execute(sql);
    }

    /**
     * Returns all zone counts as a map: zone name → count.
     */
    public Map<String, Integer> getZoneCounts() throws SQLException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        String sql = "SELECT * FROM zone LIMIT 1";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                counts.put("Grand Tunis", rs.getInt("GrandTunis"));
                counts.put("Cap Bon",     rs.getInt("CapBon"));
                counts.put("Nord Ouest",  rs.getInt("NordOuest"));
                counts.put("Centre",      rs.getInt("Centre"));
                counts.put("Sud",         rs.getInt("Sud"));
            }
        }
        return counts;
    }

    private void execute(String sql) throws SQLException {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    /**
     * Rebuilds zone counters from scratch by scanning all existing manzel records.
     * Call this once on app startup to sync old properties into the zone table.
     */
    public void syncZoneCounts() throws SQLException {
        // Step 1: reset all counters to 0
        String reset = "UPDATE zone SET GrandTunis=0, CapBon=0, NordOuest=0, Centre=0, Sud=0";
        execute(reset);

        // Step 2: count each zone from manzel table
        String countSql =
            "SELECT Address FROM manzel";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(countSql);
             ResultSet rs = stmt.executeQuery()) {

            int grandTunis = 0, capBon = 0, nordOuest = 0, centre = 0, sud = 0;

            while (rs.next()) {
                String address = rs.getString("Address");
                TunisiaZoneClassifier.Zone zone = TunisiaZoneClassifier.classify(address);
                switch (zone) {
                    case GrandTunis -> grandTunis++;
                    case CapBon     -> capBon++;
                    case NordOuest  -> nordOuest++;
                    case Centre     -> centre++;
                    case Sud        -> sud++;
                    default         -> {} // Unknown — skip
                }
            }

            // Step 3: write the counts back
            String update = "UPDATE zone SET GrandTunis=?, CapBon=?, NordOuest=?, Centre=?, Sud=?";
            try (PreparedStatement upStmt = conn.prepareStatement(update)) {
                upStmt.setInt(1, grandTunis);
                upStmt.setInt(2, capBon);
                upStmt.setInt(3, nordOuest);
                upStmt.setInt(4, centre);
                upStmt.setInt(5, sud);
                upStmt.executeUpdate();
            }
        }
    }
}
