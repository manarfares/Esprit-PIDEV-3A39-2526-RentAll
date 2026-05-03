package tn.piapp.util;

import tn.piapp.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // ===================== MAPPER =====================
    private User mapUser(ResultSet rs) throws SQLException {
        int    id           = rs.getInt("id");
        String name     = rs.getString("name");
        String email        = rs.getString("email");
        String password     = rs.getString("password");
        String phone        = rs.getString("phone");
        String profileImage = rs.getString("avatar");
        String status       = rs.getString("account_status");
        String rolesJson    = rs.getString("roles");
        String sessionToken = rs.getString("session_token");

        // Fallback username = email si null (intégration Symfony)
        if (username == null || username.isEmpty()) username = email;

        // Extraire rôle principal depuis le JSON Symfony
        String role;
        if (rolesJson == null)                                role = "ROLE_USER";
        else if (rolesJson.contains("ROLE_ADMIN"))            role = "ROLE_ADMIN";
        else if (rolesJson.contains("ROLE_HOST_PENDING"))     role = "ROLE_HOST_PENDING";
        else if (rolesJson.contains("ROLE_HOST"))             role = "ROLE_HOST";
        else if (rolesJson.contains("ROLE_GUEST"))            role = "ROLE_GUEST";
        else                                                  role = "ROLE_USER";

        // Normaliser status en majuscules pour le code Java existant
        if (status != null) status = status.toUpperCase();

        LocalDateTime hostRequestDate = rs.getTimestamp("host_request_date") != null
                ? rs.getTimestamp("host_request_date").toLocalDateTime() : null;
        LocalDateTime createdAt = rs.getTimestamp("created_at") != null
                ? rs.getTimestamp("created_at").toLocalDateTime() : null;
        LocalDateTime updatedAt = rs.getTimestamp("updated_at") != null
                ? rs.getTimestamp("updated_at").toLocalDateTime() : null;

        switch (role) {
            case "ROLE_ADMIN":
                return new Admin(id, name, email, password,
                        phone, profileImage, status, sessionToken,
                        hostRequestDate, createdAt, updatedAt);
            case "ROLE_HOST":
                return new Host(id, name, email, password,
                        phone, profileImage, status, sessionToken,
                        hostRequestDate, createdAt, updatedAt);
            case "ROLE_HOST_PENDING":
                return new HostPending(id, name, email, password,
                        phone, profileImage, status, sessionToken,
                        hostRequestDate, createdAt, updatedAt);
            default:
                return new Guest(id, name, email, password,
                        phone, profileImage, status, sessionToken,
                        hostRequestDate, createdAt, updatedAt);
        }
    }
}
