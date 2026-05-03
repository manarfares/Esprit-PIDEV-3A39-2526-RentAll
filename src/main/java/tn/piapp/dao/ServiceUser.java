package tn.piapp.dao;
import tn.piapp.db.DbConnection;

import tn.piapp.model.*;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class ServiceUser implements IService<User> {

    private static final Map<Integer, String> SESSION_TOKENS = new ConcurrentHashMap<>();

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
        String sql = "INSERT INTO user (nom, prenom, email, `password`, roles, account_status, is_verified, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            
            // Extraire nom et prénom du name
            String[] parts = user.getName().split(" ", 2);
            String prenom = parts.length > 0 ? parts[0] : "";
            String nom = parts.length > 1 ? parts[1] : "";
            
            ps.setString(1, nom);
            ps.setString(2, prenom);
            ps.setString(3, user.getEmail());
            ps.setString(4, BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
            ps.setString(5, "[\"" + user.getRole() + "\"]");
            ps.setString(6, user.getStatus() != null ? user.getStatus() : "active");
            ps.setInt(7, 1); // is_verified = 1
            ps.executeUpdate();
            System.out.println("✅ User ajouté : " + user.getName());
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Erreur ajouter : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ===================== MODIFIER =====================
    @Override
    public void modifier(User user) {
        String sql = "UPDATE user SET nom=?, prenom=?, email=?, phone=?, " +
                "profile_image=?, account_status=? WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            
            // Extraire nom et prénom du name
            String[] parts = user.getName().split(" ", 2);
            String prenom = parts.length > 0 ? parts[0] : "";
            String nom = parts.length > 1 ? parts[1] : "";
            
            ps.setString(1, nom);
            ps.setString(2, prenom);
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getProfileImage());
            ps.setString(6, user.getStatus());
            ps.setInt(7, user.getId());
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
        String sql = "SELECT * FROM user WHERE email=?";
        
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    TENTATIVE DE CONNEXION                      ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println("📧 Email saisi          : " + usernameOrEmail);
        System.out.println("🔑 Mot de passe saisi   : " + password);
        System.out.println("📝 Longueur mot de passe: " + password.length() + " caractères");
        
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, usernameOrEmail);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                System.out.println("\n✅ UTILISATEUR TROUVÉ DANS LA BASE");
                System.out.println("   ID                : " + rs.getInt("id"));
                
                // Récupérer toutes les colonnes importantes
                String hash = rs.getString("password");
                String accountStatus = null;
                int isVerified = 0;
                String nom = null;
                String prenom = null;
                String roles = null;
                
                try { accountStatus = rs.getString("account_status"); } catch (SQLException e) {}
                try { isVerified = rs.getInt("is_verified"); } catch (SQLException e) {}
                try { nom = rs.getString("nom"); } catch (SQLException e) {}
                try { prenom = rs.getString("prenom"); } catch (SQLException e) {}
                try { roles = rs.getString("roles"); } catch (SQLException e) {}
                
                System.out.println("   Nom               : " + nom);
                System.out.println("   Prénom            : " + prenom);
                System.out.println("   Roles             : " + roles);
                System.out.println("   Account Status    : " + accountStatus);
                System.out.println("   Is Verified       : " + isVerified);
                System.out.println("   Hash (30 premiers): " + hash.substring(0, Math.min(30, hash.length())) + "...");
                System.out.println("   Hash complet      : " + hash);
                
                System.out.println("\n🔐 VÉRIFICATION BCRYPT...");
                System.out.println("   Password fourni   : '" + password + "'");
                System.out.println("   Hash en base      : '" + hash + "'");
                
                boolean bcryptResult = BCrypt.checkpw(password, hash);
                System.out.println("   Résultat BCrypt   : " + (bcryptResult ? "✅ SUCCÈS" : "❌ ÉCHEC"));
                
                if (bcryptResult) {
                    System.out.println("\n✅ MOT DE PASSE CORRECT - Création de la session...");
                    User user = mapUser(rs);

                    // Générer token unique
                    String token = UUID.randomUUID().toString();

                    // Stocker le token pour la session courante
                    saveToken(user.getId(), token);

                    // Mettre à jour l'objet
                    user.setSessionToken(token);

                    System.out.println("✅ LOGIN RÉUSSI → " + usernameOrEmail);
                    System.out.println("🔑 Token généré : " + token);
                    System.out.println("👤 Rôle         : " + user.getRole());
                    System.out.println("📊 Status       : " + user.getStatus());
                    System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
                    return user;
                } else {
                    System.out.println("\n❌ MOT DE PASSE INCORRECT");
                    System.out.println("   BCrypt.checkpw() a retourné false");
                    System.out.println("   Vérifiez que le mot de passe en base est bien hashé avec BCrypt");
                    System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
                }
            } else {
                System.out.println("\n❌ UTILISATEUR INTROUVABLE");
                System.out.println("   Aucun utilisateur avec l'email : " + usernameOrEmail);
                System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
            }
        } catch (SQLException e) {
            System.out.println("\n❌ ERREUR SQL");
            System.out.println("   Message : " + e.getMessage());
            System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("\n❌ ERREUR GÉNÉRALE");
            System.out.println("   Message : " + e.getMessage());
            System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
            e.printStackTrace();
        }
        return null;
    }

    // ===================== SAVE TOKEN =====================
    private void saveToken(int id, String token) {
        SESSION_TOKENS.put(id, token);

        String sql = "UPDATE user SET session_token=? WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, token);
            ps.setInt(2, id);
            ps.executeUpdate();
            System.out.println("🔑 Token sauvegardé → ID " + id);
        } catch (SQLException e) {
            System.out.println("❌ Erreur saveToken : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===================== VERIFIER TOKEN =====================
    public boolean verifierToken(int id, String token) {
        String memoryToken = SESSION_TOKENS.get(id);
        if (token != null && token.equals(memoryToken)) {
            System.out.println("✅ Token valide en mémoire → ID " + id);
            return true;
        }

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
            e.printStackTrace();

        }
        return false;
    }

    // ===================== LOGOUT =====================
    public void logout(int id) {
        SESSION_TOKENS.remove(id);

        String sql = "UPDATE user SET session_token=NULL WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("🚪 Token supprimé → ID " + id);
        } catch (SQLException e) {
            System.out.println("❌ Erreur logout : " + e.getMessage());
            e.printStackTrace();
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
        // Recherche par email car la colonne name n'existe pas
        String sql = "SELECT * FROM user WHERE email=? OR CONCAT(prenom, ' ', nom) LIKE ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, "%" + name + "%");
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
        String sql = "UPDATE user SET account_status=? WHERE id=?";
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
        String sql = "UPDATE user SET roles='[\"ROLE_HOST_PENDING\"]', " +
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
        
        // Utiliser nom et prenom au lieu de name
        String nom = rs.getString("nom");
        String prenom = rs.getString("prenom");
        String name = (prenom != null ? prenom + " " : "") + (nom != null ? nom : "");
        if (name.trim().isEmpty()) {
            name = rs.getString("email").split("@")[0]; // Fallback sur email
        }
        
        String email        = rs.getString("email");
        String password     = rs.getString("password");
        
        // Colonnes optionnelles
        String phone = null;
        try { phone = rs.getString("phone"); } catch (SQLException e) {}
        
        String profileImage = null;
        try { profileImage = rs.getString("profile_image"); } catch (SQLException e) {}
        
        // Utiliser account_status au lieu de status
        String status = "ACTIVE";
        try {
            String accountStatus = rs.getString("account_status");
            if (accountStatus != null) {
                status = accountStatus.toUpperCase();
            }
        } catch (SQLException e) {
            // Si account_status n'existe pas, vérifier is_verified
            try {
                int isVerified = rs.getInt("is_verified");
                status = (isVerified == 1) ? "ACTIVE" : "INACTIVE";
            } catch (SQLException ex) {
                status = "ACTIVE"; // Par défaut
            }
        }
        
        String rolesJson = rs.getString("roles");
        String role = "ROLE_GUEST";
        if (rolesJson != null) {
            if (rolesJson.contains("ROLE_ADMIN")) role = "ROLE_ADMIN";
            else if (rolesJson.contains("ROLE_HOST_PENDING")) role = "ROLE_HOST_PENDING";
            else if (rolesJson.contains("ROLE_HOST")) role = "ROLE_HOST";
            else if (rolesJson.contains("ROLE_USER")) role = "ROLE_USER";
        }
        
        String sessionToken = null;
        try { sessionToken = rs.getString("session_token"); } catch (SQLException e) {}

        LocalDateTime hostRequestDate = null;
        try {
            if (rs.getTimestamp("host_request_date") != null) {
                hostRequestDate = rs.getTimestamp("host_request_date").toLocalDateTime();
            }
        } catch (SQLException e) {}
        
        LocalDateTime createdAt = null;
        try {
            if (rs.getTimestamp("created_at") != null) {
                createdAt = rs.getTimestamp("created_at").toLocalDateTime();
            }
        } catch (SQLException e) {}
        
        LocalDateTime updatedAt = null;
        try {
            if (rs.getTimestamp("updated_at") != null) {
                updatedAt = rs.getTimestamp("updated_at").toLocalDateTime();
            }
        } catch (SQLException e) {}

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
