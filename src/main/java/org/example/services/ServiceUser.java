package org.example.services;

import org.example.models.*;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ServiceUser implements IService<User> {

    private Connection connection;

    public ServiceUser() {
        connection = DatabaseConnection.getConnection2();
    }

    private Connection conn() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DatabaseConnection.getConnection2();
        }
        if (connection == null) {
            throw new SQLException("Connexion base de donnees indisponible.");
        }
        return connection;
    }

    // ===================== HELPERS JSON roles =====================
    /**
     * Convertit un rÃ´le Java vers le format JSON Symfony stockÃ© dans `roles`.
     * Symfony attend toujours ROLE_USER comme base pour un utilisateur authentifiÃ©.
     *   ROLE_GUEST        -> ["ROLE_USER"]
     *   ROLE_HOST         -> ["ROLE_USER","ROLE_HOST"]
     *   ROLE_HOST_PENDING -> ["ROLE_USER","ROLE_HOST_PENDING"]
     *   ROLE_ADMIN        -> ["ROLE_ADMIN"]
     */
    private String roleToJson(String role) {
        if (role == null || role.isEmpty()) role = "ROLE_USER";
        switch (role) {
            case "ROLE_ADMIN":        return "[\"ROLE_ADMIN\"]";
            case "ROLE_HOST":         return "[\"ROLE_USER\",\"ROLE_HOST\"]";
            case "ROLE_HOST_PENDING": return "[\"ROLE_USER\",\"ROLE_HOST_PENDING\"]";
            case "ROLE_GUEST":
            case "ROLE_USER":         return "[\"ROLE_USER\"]";
            default:                  return "[\"" + role + "\"]";
        }
    }

    /**
     * Extrait le rÃ´le "principal" depuis la colonne roles (JSON Symfony).
     * PrioritÃ© : ADMIN > HOST > HOST_PENDING > GUEST/USER.
     */
    private String extractMainRole(String rolesJson) {
        if (rolesJson == null) return "ROLE_USER";
        if (rolesJson.contains("ROLE_ADMIN"))        return "ROLE_ADMIN";
        if (rolesJson.contains("ROLE_HOST_PENDING")) return "ROLE_HOST_PENDING";
        if (rolesJson.contains("ROLE_HOST"))         return "ROLE_HOST";
        if (rolesJson.contains("ROLE_GUEST"))        return "ROLE_GUEST";
        return "ROLE_USER";
    }

    // ===================== AJOUTER =====================
    @Override
    public void ajouter(User user) {
        String sql = "INSERT INTO `user` " +
                "(username, email, `password`, roles, account_status, is_verified, " +
                " failed_login_attempts, suspicious_activity_score, phone, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, 0, 0, 0, ?, NOW(), NOW())";
        try {
            PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
            ps.setString(4, roleToJson(user.getRole()));
            ps.setString(5, mapStatusJavaToDb(user.getStatus()));
            ps.setString(6, user.getPhone());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) user.setId(keys.getInt(1));
            System.out.println("âœ… User ajoutÃ© : " + user.getUsername() + " (ID " + user.getId() + ")");
        } catch (SQLException e) {
            System.out.println("âŒ Erreur ajouter : " + e.getMessage());
        }
    }

    // ===================== MODIFIER =====================
    @Override
    public void modifier(User user) {
        String sql = "UPDATE `user` SET username=?, email=?, phone=?, " +
                "avatar=?, account_status=?, updated_at=NOW() WHERE id=?";
        try {
            PreparedStatement ps = conn().prepareStatement(sql);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPhone());
            ps.setString(4, user.getProfileImage());
            ps.setString(5, mapStatusJavaToDb(user.getStatus()));
            ps.setInt(6, user.getId());
            ps.executeUpdate();
            System.out.println("âœ… User modifiÃ© : " + user.getUsername());
        } catch (SQLException e) {
            System.out.println("âŒ Erreur modifier : " + e.getMessage());
        }
    }

    // ===================== SUPPRIMER =====================
    @Override
    public void supprimer(User user) {
        String sql = "DELETE FROM `user` WHERE id=?";
        try {
            PreparedStatement ps = conn().prepareStatement(sql);
            ps.setInt(1, user.getId());
            ps.executeUpdate();
            System.out.println("âœ… User supprimÃ© : " + user.getUsername());
        } catch (SQLException e) {
            System.out.println("âŒ Erreur supprimer : " + e.getMessage());
        }
    }

    // ===================== RECUPERER =====================
    @Override
    public List<User> recuperer() {
        List<User> liste = new ArrayList<>();
        String sql = "SELECT * FROM `user`";
        try {
            Statement stmt = conn().createStatement();
            ResultSet rs   = stmt.executeQuery(sql);
            while (rs.next()) liste.add(mapUser(rs));
        } catch (SQLException e) {
            System.out.println("âŒ Erreur recuperer : " + e.getMessage());
        }
        return liste;
    }

    // ===================== LOGIN + TOKEN =====================
    public User login(String usernameOrEmail, String password) {
        String sql = "SELECT * FROM `user` WHERE username=? OR email=?";
        try {
            PreparedStatement ps = conn().prepareStatement(sql);
            ps.setString(1, usernameOrEmail);
            ps.setString(2, usernameOrEmail);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("password");
                if (hash == null || hash.isEmpty()) {
                    System.out.println("âŒ Compte sans mot de passe");
                    return null;
                }
                // CompatibilitÃ© hash Symfony (prÃ©fixe $2y$) avec jBCrypt (attend $2a$/$2b$)
                if (hash.startsWith("$2y$")) {
                    hash = "$2a$" + hash.substring(4);
                }
                boolean ok;
                try {
                    ok = BCrypt.checkpw(password, hash);
                } catch (IllegalArgumentException ex) {
                    System.out.println("âŒ Hash invalide en base : " + ex.getMessage());
                    return null;
                }
                if (ok) {
                    User user = mapUser(rs);

                    // GÃ©nÃ©rer token unique
                    String token = UUID.randomUUID().toString();

                    // Stocker token en base
                    saveToken(user.getId(), token);

                    // Mettre Ã  jour l'objet
                    user.setSessionToken(token);

                    System.out.println("âœ… LOGIN rÃ©ussi â†’ " + usernameOrEmail);
                    System.out.println("ðŸ”‘ Token : " + token);
                    return user;
                }
                System.out.println("âŒ Mot de passe incorrect");
            } else {
                System.out.println("âŒ Utilisateur introuvable");
            }
        } catch (SQLException e) {
            System.out.println("âŒ Erreur login : " + e.getMessage());
        }
        return null;
    }

    // ===================== SAVE TOKEN (public wrapper, ex: pour login facial / OAuth) =====================
    public void saveTokenForUser(int id, String token) {
        saveToken(id, token);
    }

    // ===================== SAVE TOKEN =====================
    private void saveToken(int id, String token) {
        String sql = "UPDATE `user` SET session_token=? WHERE id=?";
        try {
            PreparedStatement ps = conn().prepareStatement(sql);
            ps.setString(1, token);
            ps.setInt(2, id);
            ps.executeUpdate();
            System.out.println("ðŸ”‘ Token sauvegardÃ© â†’ ID " + id);
        } catch (SQLException e) {
            System.out.println("âŒ Erreur saveToken : " + e.getMessage());
        }
    }

    // ===================== VERIFIER TOKEN =====================
    public boolean verifierToken(int id, String token) {
        String sql = "SELECT session_token FROM `user` WHERE id=?";
        try {
            PreparedStatement ps = conn().prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String storedToken = rs.getString("session_token");
                if (token != null && token.equals(storedToken)) {
                    System.out.println("âœ… Token valide â†’ ID " + id);
                    return true;
                }
            }
            System.out.println("âŒ Token invalide â†’ ID " + id);
        } catch (SQLException e) {
            System.out.println("âŒ Erreur verifierToken : " + e.getMessage());
        }
        return false;
    }

    // ===================== LOGOUT =====================
    public void logout(int id) {
        String sql = "UPDATE `user` SET session_token=NULL WHERE id=?";
        try {
            PreparedStatement ps = conn().prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("ðŸšª Token supprimÃ© â†’ ID " + id);
        } catch (SQLException e) {
            System.out.println("âŒ Erreur logout : " + e.getMessage());
        }
    }

    // ===================== FIND BY ID =====================
    public User findById(int id) {
        String sql = "SELECT * FROM `user` WHERE id=?";
        try {
            PreparedStatement ps = conn().prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        } catch (SQLException e) {
            System.out.println("âŒ Erreur findById : " + e.getMessage());
        }
        return null;
    }

    // ===================== FIND BY USERNAME =====================
    public User findByUsername(String username) {
        String sql = "SELECT * FROM `user` WHERE username=?";
        try {
            PreparedStatement ps = conn().prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        } catch (SQLException e) {
            System.out.println("âŒ Erreur findByUsername : " + e.getMessage());
        }
        return null;
    }

    // ===================== FIND BY EMAIL =====================
    public User findByEmail(String email) {
        String sql = "SELECT * FROM `user` WHERE email=?";
        try {
            PreparedStatement ps = conn().prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        } catch (SQLException e) {
            System.out.println("âŒ Erreur findByEmail : " + e.getMessage());
        }
        return null;
    }

    // ===================== OAUTH : FIND OR CREATE =====================
    /**
     * Cas OAuth (Google/Facebook) :
     *  - Si un compte existe avec cet email â†’ on le rÃ©cupÃ¨re et on rÃ©gÃ©nÃ¨re un session token.
     *  - Sinon on crÃ©e un compte Guest, is_verified=1, account_status=active,
     *    avec un mot de passe alÃ©atoire (l'utilisateur ne pourra se connecter
     *    qu'avec OAuth tant qu'il ne le rÃ©initialise pas).
     *
     * Retourne le User connectÃ© (avec sessionToken dÃ©fini), ou null en cas d'Ã©chec.
     */
    public User findOrCreateOAuthUser(String email, String fullName, String pictureUrl) {
        return findOrCreateOAuthUser(email, fullName, pictureUrl, "google");
    }

    public User findOrCreateOAuthUser(String email, String fullName, String pictureUrl, String provider) {
        System.out.println("ðŸ”µ findOrCreateOAuthUser START â†’ email=" + email + ", name=" + fullName + ", provider=" + provider);
        if (email == null || email.isBlank()) {
            System.out.println("âŒ findOrCreateOAuthUser : email vide");
            return null;
        }

        // 1. Compte existant ?
        User existing = findByEmail(email);
        if (existing != null) {
            System.out.println("ðŸ” OAuth : compte existant trouvÃ© â†’ ID " + existing.getId()
                    + ", status=" + existing.getStatus() + ", role=" + existing.getRole());
            // Refus uniquement si compte banni
            if ("BANNED".equals(existing.getStatus())) {
                System.out.println("ðŸš« OAuth refusÃ© (compte BANNED) â†’ " + email);
                return null;
            }
            String token = UUID.randomUUID().toString();
            saveToken(existing.getId(), token);
            existing.setSessionToken(token);
            System.out.println("âœ… OAuth login (compte existant) â†’ " + email);
            return existing;
        }

        // 2. CrÃ©ation d'un nouveau compte
        String baseUsername = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
        baseUsername = baseUsername.replaceAll("[^a-zA-Z0-9_]", "");
        if (baseUsername.length() < 3) baseUsername = "user_" + baseUsername;

        String username = baseUsername;
        int    suffix   = 1;
        while (findByUsername(username) != null) {
            username = baseUsername + suffix;
            suffix++;
            if (suffix > 1000) {
                username = baseUsername + UUID.randomUUID().toString().substring(0, 6);
                break;
            }
        }

        // Mot de passe alÃ©atoire impossible Ã  deviner (compte OAuth-only)
        String randomPwd  = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        String hash       = BCrypt.hashpw(randomPwd, BCrypt.gensalt());

        String sql = "INSERT INTO `user` " +
                "(username, email, `password`, roles, account_status, is_verified, " +
                " failed_login_attempts, suspicious_activity_score, avatar, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, 'active', 1, 0, 0, ?, NOW(), NOW())";

        int newId = -1;
        try {
            PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, hash);
            ps.setString(4, roleToJson("ROLE_GUEST"));
            ps.setString(5, pictureUrl != null && !pictureUrl.isBlank() ? pictureUrl : null);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) newId = keys.getInt(1);
            System.out.println("âœ… OAuth user crÃ©Ã© â†’ " + username + " (ID " + newId + ", " + email + ")");
        } catch (SQLException e) {
            System.out.println("âŒ Erreur crÃ©ation OAuth user (SQL) : " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        if (newId == -1) {
            System.out.println("âŒ OAuth : INSERT a rÃ©ussi mais aucun ID gÃ©nÃ©rÃ© retournÃ©");
            return null;
        }

        // 3. RÃ©cupÃ©ration + gÃ©nÃ©ration token
        User created = findById(newId);
        if (created == null) {
            System.out.println("âŒ OAuth : findById(" + newId + ") a retournÃ© null aprÃ¨s crÃ©ation");
            return null;
        }

        String token = UUID.randomUUID().toString();
        saveToken(created.getId(), token);
        created.setSessionToken(token);

        // 4. Email de bienvenue (asynchrone, on n'attend pas)
        final String finalEmail    = email;
        final String finalUsername = username;
        final String finalProvider = provider != null ? provider : "google";
        new Thread(() -> {
            try {
                EmailService emailSvc = new EmailService();
                boolean sent = emailSvc.sendOAuthWelcome(finalEmail, finalUsername, finalProvider);
                if (sent) {
                    System.out.println("ðŸ“§ Email de bienvenue OAuth envoyÃ© â†’ " + finalEmail);
                } else {
                    System.err.println("âš ï¸ Email de bienvenue OAuth non envoyÃ© â†’ " + finalEmail);
                }
            } catch (Exception ex) {
                System.err.println("âš ï¸ Erreur envoi email bienvenue OAuth : " + ex.getMessage());
            }
        }, "oauth-welcome-email").start();

        return created;
    }

    // ===================== STATISTIQUES =====================
    /**
     * Retourne un dictionnaire de statistiques :
     *   total, role_admin, role_host, role_host_pending, role_guest,
     *   status_active, status_banned, status_inactive, status_suspended,
     *   verified, face_enabled
     */
    public java.util.Map<String, Integer> getUserStats() {
        java.util.LinkedHashMap<String, Integer> stats = new java.util.LinkedHashMap<>();
        stats.put("total",              0);
        stats.put("role_admin",         0);
        stats.put("role_host",          0);
        stats.put("role_host_pending",  0);
        stats.put("role_guest",         0);
        stats.put("status_active",      0);
        stats.put("status_banned",      0);
        stats.put("status_inactive",    0);
        stats.put("status_suspended",   0);
        stats.put("verified",           0);
        stats.put("face_enabled",       0);

        String sql = "SELECT roles, account_status, is_verified, selfie_image FROM `user`";
        try {
            Statement stmt = conn().createStatement();
            ResultSet rs   = stmt.executeQuery(sql);
            while (rs.next()) {
                stats.merge("total", 1, Integer::sum);

                String role = extractMainRole(rs.getString("roles"));
                switch (role) {
                    case "ROLE_ADMIN":        stats.merge("role_admin",         1, Integer::sum); break;
                    case "ROLE_HOST":         stats.merge("role_host",          1, Integer::sum); break;
                    case "ROLE_HOST_PENDING": stats.merge("role_host_pending",  1, Integer::sum); break;
                    default:                  stats.merge("role_guest",         1, Integer::sum); break;
                }

                String status = rs.getString("account_status");
                if (status == null) status = "active";
                switch (status.toLowerCase()) {
                    case "active":     stats.merge("status_active",    1, Integer::sum); break;
                    case "banned":     stats.merge("status_banned",    1, Integer::sum); break;
                    case "inactive":   stats.merge("status_inactive",  1, Integer::sum); break;
                    case "suspended":  stats.merge("status_suspended", 1, Integer::sum); break;
                }

                if (rs.getInt("is_verified") == 1) stats.merge("verified", 1, Integer::sum);

                String selfie = rs.getString("selfie_image");
                if (selfie != null && !selfie.trim().isEmpty()) stats.merge("face_enabled", 1, Integer::sum);
            }
        } catch (SQLException e) {
            System.out.println("âŒ Erreur getUserStats : " + e.getMessage());
        }
        return stats;
    }

    // ===================== FACE LOGIN : CANDIDATS =====================
    /**
     * Retourne les utilisateurs eligibles a la connexion faciale :
     *  - is_verified = 1
     *  - selfie_image NOT NULL et NOT vide
     *  - account_status = 'active'
     *
     * Le caller construira le payload (id, fullPath) pour le script Python.
     */
    public List<User> findFaceLoginCandidates() {
        List<User> liste = new ArrayList<>();
        String sql = "SELECT * FROM `user` " +
                "WHERE is_verified = 1 " +
                "  AND selfie_image IS NOT NULL " +
                "  AND TRIM(selfie_image) <> '' " +
                "  AND account_status = 'active'";
        try {
            Statement stmt = conn().createStatement();
            ResultSet rs   = stmt.executeQuery(sql);
            while (rs.next()) liste.add(mapUser(rs));
        } catch (SQLException e) {
            System.out.println("âŒ Erreur findFaceLoginCandidates : " + e.getMessage());
        }
        return liste;
    }

    /**
     * Sauvegarde le selfie + CIN apres une verification faciale reussie.
     * Met aussi face_verified_at = NOW() et is_verified = 1 pour activer la connexion faciale.
     */
    public void updateFaceVerification(int userId, String selfieRelPath, String cinRelPath) {
        String sql = "UPDATE `user` SET selfie_image=?, identity_document_image=?, " +
                "face_verified_at=NOW(), is_verified=1, updated_at=NOW() WHERE id=?";
        try {
            PreparedStatement ps = conn().prepareStatement(sql);
            ps.setString(1, selfieRelPath);
            ps.setString(2, cinRelPath);
            ps.setInt(3, userId);
            int n = ps.executeUpdate();
            System.out.println("âœ… Face verification enregistree pour ID " + userId
                    + " (" + n + " ligne mise a jour)");
        } catch (SQLException e) {
            System.out.println("âŒ Erreur updateFaceVerification : " + e.getMessage());
        }
    }

    /** RÃ©cupÃ¨re le chemin selfie_image brut (tel que stockÃ© en DB) pour un user donnÃ©. */
    public String getSelfieImagePath(int userId) {
        String sql = "SELECT selfie_image FROM `user` WHERE id = ?";
        try {
            PreparedStatement ps = conn().prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("selfie_image");
        } catch (SQLException e) {
            System.out.println("âŒ Erreur getSelfieImagePath : " + e.getMessage());
        }
        return null;
    }

    // ===================== UPDATE ROLE =====================
    public void updateRole(int id, String role) {
        String sql = "UPDATE `user` SET roles=?, updated_at=NOW() WHERE id=?";
        try {
            PreparedStatement ps = conn().prepareStatement(sql);
            ps.setString(1, roleToJson(role));
            ps.setInt(2, id);
            ps.executeUpdate();
            System.out.println("âœ… RÃ´le mis Ã  jour â†’ ID " + id + " : " + role);
        } catch (SQLException e) {
            System.out.println("âŒ Erreur updateRole : " + e.getMessage());
        }
    }

    // ===================== UPDATE STATUS =====================
    public void updateStatus(int id, String status) {
        String sql = "UPDATE `user` SET account_status=?, updated_at=NOW() WHERE id=?";
        try {
            PreparedStatement ps = conn().prepareStatement(sql);
            ps.setString(1, mapStatusJavaToDb(status));
            ps.setInt(2, id);
            ps.executeUpdate();
            System.out.println("âœ… Status mis Ã  jour â†’ ID " + id + " : " + status);
        } catch (SQLException e) {
            System.out.println("âŒ Erreur updateStatus : " + e.getMessage());
        }
    }

    // ===================== DEMANDER HOST =====================
    public void demanderHost(int id) {
        String sql = "UPDATE `user` SET roles=?, host_request_date=NOW(), updated_at=NOW() WHERE id=?";
        try {
            PreparedStatement ps = conn().prepareStatement(sql);
            ps.setString(1, roleToJson("ROLE_HOST_PENDING"));
            ps.setInt(2, id);
            ps.executeUpdate();
            System.out.println("ðŸ“© Demande Host â†’ ID " + id);
        } catch (SQLException e) {
            System.out.println("âŒ Erreur demanderHost : " + e.getMessage());
        }
    }

    // ===================== FIND HOST PENDING =====================
    public List<User> findHostPending() {
        List<User> liste = new ArrayList<>();
        String sql = "SELECT * FROM `user` WHERE roles LIKE '%ROLE_HOST_PENDING%'";
        try {
            Statement stmt = conn().createStatement();
            ResultSet rs   = stmt.executeQuery(sql);
            while (rs.next()) liste.add(mapUser(rs));
        } catch (SQLException e) {
            System.out.println("âŒ Erreur findHostPending : " + e.getMessage());
        }
        return liste;
    }

    // ===================== CONFIRMATION EMAIL =====================
    public void saveConfirmationToken(int userId, String token, int expireHours) {
        String sql = "UPDATE `user` SET confirmation_token=?, " +
                "confirmation_token_expires=DATE_ADD(NOW(), INTERVAL ? HOUR) WHERE id=?";
        try {
            PreparedStatement ps = conn().prepareStatement(sql);
            ps.setString(1, token);
            ps.setInt(2, expireHours);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("âŒ Erreur saveConfirmationToken : " + e.getMessage());
        }
    }

    /** Active le compte si le token est valide et non expirÃ©. */
    public boolean confirmByToken(String token) {
        String sql = "SELECT id FROM `user` WHERE confirmation_token=? " +
                "AND (confirmation_token_expires IS NULL OR confirmation_token_expires > NOW())";
        try {
            PreparedStatement ps = conn().prepareStatement(sql);
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                PreparedStatement upd = conn().prepareStatement(
                        "UPDATE `user` SET is_verified=1, confirmation_token=NULL, " +
                        "confirmation_token_expires=NULL, updated_at=NOW() WHERE id=?");
                upd.setInt(1, id);
                upd.executeUpdate();
                System.out.println("âœ… Compte confirmÃ© : ID " + id);
                return true;
            }
        } catch (SQLException e) {
            System.out.println("âŒ Erreur confirmByToken : " + e.getMessage());
        }
        return false;
    }

    // ===================== RESET SMS =====================
    public void saveResetSmsCode(int userId, String code, int expireMinutes) {
        String sql = "UPDATE `user` SET reset_sms_code=?, " +
                "reset_sms_expires=DATE_ADD(NOW(), INTERVAL ? MINUTE) WHERE id=?";
        try {
            PreparedStatement ps = conn().prepareStatement(sql);
            ps.setString(1, code);
            ps.setInt(2, expireMinutes);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("âŒ Erreur saveResetSmsCode : " + e.getMessage());
        }
    }

    public User findByPhone(String phone) {
        String sql = "SELECT * FROM `user` WHERE phone=?";
        try {
            PreparedStatement ps = conn().prepareStatement(sql);
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        } catch (SQLException e) {
            System.out.println("âŒ Erreur findByPhone : " + e.getMessage());
        }
        return null;
    }

    public boolean verifyResetCodeAndUpdatePassword(int userId, String code, String newPassword) {
        String sql = "SELECT reset_sms_code, reset_sms_expires FROM `user` WHERE id=?";
        try {
            PreparedStatement ps = conn().prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String stored  = rs.getString("reset_sms_code");
                Timestamp exp  = rs.getTimestamp("reset_sms_expires");
                if (stored == null || !stored.equals(code)) {
                    System.out.println("âŒ Code incorrect");
                    return false;
                }
                if (exp != null && exp.before(new Timestamp(System.currentTimeMillis()))) {
                    System.out.println("âŒ Code expirÃ©");
                    return false;
                }
                String hash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                PreparedStatement upd = conn().prepareStatement(
                        "UPDATE `user` SET password=?, reset_sms_code=NULL, " +
                        "reset_sms_expires=NULL, updated_at=NOW() WHERE id=?");
                upd.setString(1, hash);
                upd.setInt(2, userId);
                upd.executeUpdate();
                System.out.println("âœ… Mot de passe rÃ©initialisÃ©");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("âŒ Erreur verifyResetCode : " + e.getMessage());
        }
        return false;
    }

    // ===================== MAPPINGS STATUS =====================
    /** Java logique (ACTIVE/INACTIVE/BANNED) -> BD Symfony (active/inactive/banned). */
    private String mapStatusJavaToDb(String status) {
        if (status == null) return "active";
        return status.toLowerCase();
    }

    /** BD Symfony (active/banned/suspended/...) -> Java (ACTIVE/BANNED/...). */
    private String mapStatusDbToJava(String status) {
        if (status == null) return "ACTIVE";
        return status.toUpperCase();
    }

    // ===================== MAPPER =====================
    private User mapUser(ResultSet rs) throws SQLException {
        int    id           = rs.getInt("id");
        String username     = rs.getString("username");
        String email        = rs.getString("email");
        String password     = rs.getString("password");
        String phone        = rs.getString("phone");
        String profileImage = rs.getString("avatar");
        String status       = mapStatusDbToJava(rs.getString("account_status"));
        String role         = extractMainRole(rs.getString("roles"));
        String sessionToken = rs.getString("session_token");

        // Fallback pour le "username" : si null, on prend l'email (projet historique expose username)
        if (username == null || username.isEmpty()) {
            username = email;
        }

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

