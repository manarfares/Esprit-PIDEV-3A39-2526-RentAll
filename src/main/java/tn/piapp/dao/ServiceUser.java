package tn.piapp.dao;
import tn.piapp.db.DbConnection;

import tn.piapp.model.*;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ServiceUser implements IService<User> {

    private Connection connection;

    public ServiceUser() {
        try {
            connection = DbConnection.getInstance().getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ===================== AJOUTER =====================
    @Override
    public boolean ajouter(User user) {
        String sql = "INSERT INTO user (name, email, `password`, roles, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW())";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
            ps.setString(4, "[\"" + user.getRole() + "\"]");
            ps.setString(5, user.getStatus() != null ? user.getStatus() : "ACTIVE");
            ps.executeUpdate();
            System.out.println("✅ User ajouté : " + user.getName());
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Erreur ajouter : " + e.getMessage());
            return false;
        }
    }

    // ===================== MODIFIER =====================
    @Override
    public void modifier(User user) {
        String sql = "UPDATE user SET name=?, email=?, phone=?, " +
                "profile_image=?, status=? WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPhone());
            ps.setString(4, user.getProfileImage());
            ps.setString(5, user.getStatus());
            ps.setInt(6, user.getId());
            ps.executeUpdate();
            System.out.println("✅ User modifié : " + user.getName());
        } catch (SQLException e) {
            System.out.println("❌ Erreur modifier : " + e.getMessage());
        }
    }

    // ===================== SUPPRIMER =====================
    @Override
    public void supprimer(User user) {
        String sql = "DELETE FROM user WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, user.getId());
            ps.executeUpdate();
            System.out.println("✅ User supprimé : " + user.getName());
        } catch (SQLException e) {
            System.out.println("❌ Erreur supprimer : " + e.getMessage());
        }
    }

    // ===================== RECUPERER =====================
    @Override
    public List<User> recuperer() {
        List<User> liste = new ArrayList<>();
        String sql = "SELECT * FROM user";
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs   = stmt.executeQuery(sql);
            while (rs.next()) liste.add(mapUser(rs));
        } catch (SQLException e) {
            System.out.println("❌ Erreur recuperer : " + e.getMessage());
        }
        return liste;
    }

    // ===================== LOGIN + TOKEN =====================
    public User login(String usernameOrEmail, String password) {
        String sql = "SELECT * FROM user WHERE name=? OR email=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, usernameOrEmail);
            ps.setString(2, usernameOrEmail);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("password");
                if (BCrypt.checkpw(password, hash)) {
                    User user = mapUser(rs);

                    // Générer token unique
                    String token = UUID.randomUUID().toString();

                    // Stocker token en base
                    saveToken(user.getId(), token);

                    // Mettre à jour l'objet
                    user.setSessionToken(token);

                    System.out.println("✅ LOGIN réussi → " + usernameOrEmail);
                    System.out.println("🔑 Token : " + token);
                    return user;
                }
                System.out.println("❌ Mot de passe incorrect");
            } else {
                System.out.println("❌ Utilisateur introuvable");
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur login : " + e.getMessage());
        }
        return null;
    }

    // ===================== SAVE TOKEN =====================
    private void saveToken(int id, String token) {
        String sql = "UPDATE user SET session_token=? WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, token);
            ps.setInt(2, id);
            ps.executeUpdate();
            System.out.println("🔑 Token sauvegardé → ID " + id);
        } catch (SQLException e) {
            System.out.println("❌ Erreur saveToken : " + e.getMessage());
        }
    }

    // ===================== VERIFIER TOKEN =====================
    public boolean verifierToken(int id, String token) {
        String sql = "SELECT session_token FROM user WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String storedToken = rs.getString("session_token");
                if (token != null && token.equals(storedToken)) {
                    System.out.println("✅ Token valide → ID " + id);
                    return true;
                }
            }
            System.out.println("❌ Token invalide → ID " + id);
        } catch (SQLException e) {
            System.out.println("❌ Erreur verifierToken : " + e.getMessage());
        }
        return false;
    }

    // ===================== LOGOUT =====================
    public void logout(int id) {
        String sql = "UPDATE user SET session_token=NULL WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("🚪 Token supprimé → ID " + id);
        } catch (SQLException e) {
            System.out.println("❌ Erreur logout : " + e.getMessage());
        }
    }

    // ===================== FIND BY ID =====================
    public User findById(int id) {
        String sql = "SELECT * FROM user WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        } catch (SQLException e) {
            System.out.println("❌ Erreur findById : " + e.getMessage());
        }
        return null;
    }

    // ===================== FIND BY USERNAME =====================
    public User findByUsername(String name) {
        String sql = "SELECT * FROM user WHERE name=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        } catch (SQLException e) {
            System.out.println("❌ Erreur findByUsername : " + e.getMessage());
        }
        return null;
    }

    // ===================== FIND BY EMAIL =====================
    public User findByEmail(String email) {
        String sql = "SELECT * FROM user WHERE email=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        } catch (SQLException e) {
            System.out.println("❌ Erreur findByEmail : " + e.getMessage());
        }
        return null;
    }

    // ===================== UPDATE ROLE =====================
    public void updateRole(int id, String role) {
        String sql = "UPDATE user SET roles=? WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, "[\"" + role + "\"]");
            ps.setInt(2, id);
            ps.executeUpdate();
            System.out.println("✅ Rôle mis à jour → ID " + id + " : " + role);
        } catch (SQLException e) {
            System.out.println("❌ Erreur updateRole : " + e.getMessage());
        }
    }

    // ===================== UPDATE STATUS =====================
    public void updateStatus(int id, String status) {
        String sql = "UPDATE user SET status=? WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
            System.out.println("✅ Status mis à jour → ID " + id + " : " + status);
        } catch (SQLException e) {
            System.out.println("❌ Erreur updateStatus : " + e.getMessage());
        }
    }

    // ===================== DEMANDER HOST =====================
    public void demanderHost(int id) {
        String sql = "UPDATE user SET role='ROLE_HOST_PENDING', " +
                "host_request_date=NOW() WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("📩 Demande Host → ID " + id);
        } catch (SQLException e) {
            System.out.println("❌ Erreur demanderHost : " + e.getMessage());
        }
    }

    // ===================== FIND HOST PENDING =====================
    public List<User> findHostPending() {
        List<User> liste = new ArrayList<>();
        String sql = "SELECT * FROM user WHERE roles LIKE '%ROLE_HOST_PENDING%'";
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs   = stmt.executeQuery(sql);
            while (rs.next()) liste.add(mapUser(rs));
        } catch (SQLException e) {
            System.out.println("❌ Erreur findHostPending : " + e.getMessage());
        }
        return liste;
    }

    // ===================== MAPPER =====================
    private User mapUser(ResultSet rs) throws SQLException {
        int    id           = rs.getInt("id");
        String name     = rs.getString("name");
        String email        = rs.getString("email");
        String password     = rs.getString("password");
        String phone        = rs.getString("phone");
        String profileImage = rs.getString("profile_image");
        String status       = rs.getString("status");
        String rolesJson = rs.getString("roles");
        String role = "ROLE_GUEST";
        if (rolesJson != null) {
            if (rolesJson.contains("ROLE_ADMIN")) role = "ROLE_ADMIN";
            else if (rolesJson.contains("ROLE_HOST_PENDING")) role = "ROLE_HOST_PENDING";
            else if (rolesJson.contains("ROLE_HOST")) role = "ROLE_HOST";
            else if (rolesJson.contains("ROLE_USER")) role = "ROLE_USER";
        }
        String sessionToken = rs.getString("session_token");

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