package org.example.utils;

import org.example.models.*;
import org.example.services.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // ===================== MAPPER =====================
    private User mapUser(ResultSet rs) throws SQLException {
        int    id           = rs.getInt("id");
        String username     = rs.getString("username");
        String email        = rs.getString("email");
        String password     = rs.getString("password");
        String phone        = rs.getString("phone");
        String profileImage = rs.getString("profile_image");
        String status       = rs.getString("status");
        String role         = rs.getString("role");
        String sessionToken = rs.getString("session_token");

        LocalDateTime hostRequestDate = rs.getTimestamp("host_request_date") != null
                ? rs.getTimestamp("host_request_date").toLocalDateTime() : null;
        LocalDateTime createdAt = rs.getTimestamp("created_at") != null
                ? rs.getTimestamp("created_at").toLocalDateTime() : null;
        LocalDateTime updatedAt = rs.getTimestamp("updated_at") != null
                ? rs.getTimestamp("updated_at").toLocalDateTime() : null;

        switch (role) {
            case "ROLE_ADMIN":
                return new Admin(id, username, email, password,
                        phone, profileImage, status, sessionToken,
                        hostRequestDate, createdAt, updatedAt);
            case "ROLE_HOST":
                return new Host(id, username, email, password,
                        phone, profileImage, status, sessionToken,
                        hostRequestDate, createdAt, updatedAt);
            case "ROLE_HOST_PENDING":
                return new HostPending(id, username, email, password,
                        phone, profileImage, status, sessionToken,
                        hostRequestDate, createdAt, updatedAt);
            default:
                return new Guest(id, username, email, password,
                        phone, profileImage, status, sessionToken,
                        hostRequestDate, createdAt, updatedAt);
        }
    }
}