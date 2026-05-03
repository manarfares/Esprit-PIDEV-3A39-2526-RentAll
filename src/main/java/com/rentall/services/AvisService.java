package com.rentall.services;

import com.rentall.config.DatabaseConnection;
import com.rentall.entities.Avis;
import com.rentall.entities.Reservation;
import tn.piapp.model.User;
import tn.piapp.util.SessionManager;

import java.text.Normalizer;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Classe AvisService
 *
 * RÔLE : Implémentation concrète du CRUD pour la table "avis"
 *        de la base smart_rental_platform.
 *
 * RAPPEL DES COLONNES RÉELLES DE LA TABLE :
 *   id, reservation_id, note, commentaire, date_creation
 *
 * ATTENTION : "reservation_id" est UNIQUE dans ta base.
 * → Une réservation ne peut avoir qu'un seul avis.
 * → Si tu essaies d'ajouter un 2ème avis pour la même réservation,
 *   MySQL retournera une erreur (Duplicate entry).
 */
public class AvisService implements IAvisService {

    private Connection connection = DatabaseConnection.getConnection();
    private final ReservationService reservationService = new ReservationService();

    // =========================================================
    // CREATE — Ajouter un avis
    // =========================================================
    @Override
    public void ajouter(Avis avis) throws SQLException {
        if (!peutAjouterAvis(avis, currentUser())) {
            throw new SQLException("Ajout avis refusé : la réservation doit être terminée, vous appartenir, et ne pas avoir déjà un avis.");
        }

        String sql = "INSERT INTO avis (reservation_id, note, commentaire, date_creation) " +
                     "VALUES (?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, avis.getReservationId());
        ps.setInt(2, avis.getNote());
        ps.setString(3, avis.getCommentaire());
        ps.setTimestamp(4, Timestamp.valueOf(avis.getDateCreation()));
        ps.executeUpdate();
        System.out.println("✅ Avis ajouté avec succès !");
    }

    // =========================================================
    // UPDATE — Modifier un avis existant
    // =========================================================
    @Override
    public void modifier(Avis avis) {
        if (!peutModifierAvis(avis, currentUser())) {
            System.out.println("Modification avis refusée : droits insuffisants.");
            return;
        }

        String sql = "UPDATE avis SET reservation_id=?, note=?, commentaire=?, date_creation=? " +
                     "WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);

            ps.setInt(1, avis.getReservationId());
            ps.setInt(2, avis.getNote());
            ps.setString(3, avis.getCommentaire());
            ps.setTimestamp(4, Timestamp.valueOf(avis.getDateCreation()));
            ps.setInt(5, avis.getId());

            ps.executeUpdate();
            System.out.println("✅ Avis modifié avec succès !");

        } catch (SQLException e) {
            System.out.println("❌ Erreur modification avis : " + e.getMessage());
        }
    }

    // =========================================================
    // DELETE — Supprimer un avis par son id
    // =========================================================
    @Override
    public void supprimer(int id) {
        if (!peutSupprimerAvis(id, currentUser())) {
            System.out.println("Suppression avis refusée : droits insuffisants.");
            return;
        }

        String sql = "DELETE FROM avis WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Avis supprimé (id=" + id + ")");

        } catch (SQLException e) {
            System.out.println("❌ Erreur suppression avis : " + e.getMessage());
        }
    }

    // =========================================================
    // READ — Afficher tous les avis
    // =========================================================
    @Override
    public List<Avis> afficherTous() {
        List<Avis> liste = new ArrayList<>();
        String sql = "SELECT * FROM avis";
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                Avis a = new Avis(
                        rs.getInt("id"),
                        rs.getInt("reservation_id"),
                        rs.getInt("note"),
                        rs.getString("commentaire"),
                        rs.getTimestamp("date_creation").toLocalDateTime()
                );
                liste.add(a);
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur affichage avis : " + e.getMessage());
        }
        return liste;
    }

    // =========================================================
    // READ — Afficher un avis par son id
    // =========================================================
    @Override
    public Avis afficherParId(int id) {
        String sql = "SELECT * FROM avis WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Avis(
                        rs.getInt("id"),
                        rs.getInt("reservation_id"),
                        rs.getInt("note"),
                        rs.getString("commentaire"),
                        rs.getTimestamp("date_creation").toLocalDateTime()
                );
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur recherche avis : " + e.getMessage());
        }
        return null;
    }

    public boolean peutAjouterAvis(Avis avis, User user) {
        if (avis == null) {
            return false;
        }
        if (user == null) {
            return !avisExistePourReservation(avis.getReservationId());
        }
        if (isAdmin(user) || isHost(user)) {
            return false;
        }
        if (!isGuest(user)) {
            return false;
        }
        Reservation reservation = reservationService.afficherParId(avis.getReservationId());
        return reservation != null
                && reservationService.reservationAppartientAuLocataire(reservation.getId(), user)
                && "terminee".equals(normalizeStatut(reservation.getStatut()))
                && !avisExistePourReservation(reservation.getId());
    }

    public boolean peutModifierAvis(Avis avis, User user) {
        if (avis == null) {
            return false;
        }
        if (user == null) {
            return true;
        }
        if (isAdmin(user) || isHost(user)) {
            return false;
        }
        return isGuest(user) && avisAppartientAuLocataire(avis, user);
    }

    public boolean peutSupprimerAvis(int avisId, User user) {
        Avis avis = afficherParId(avisId);
        if (avis == null) {
            return false;
        }
        if (user == null || isAdmin(user)) {
            return true;
        }
        return isGuest(user) && avisAppartientAuLocataire(avis, user);
    }

    public boolean peutVoirAvis(Avis avis, User user) {
        if (avis == null) {
            return false;
        }
        return reservationService.peutVoirReservation(avis.getReservationId(), user);
    }

    public boolean avisExistePourReservation(int reservationId) {
        String sql = "SELECT COUNT(*) FROM avis WHERE reservation_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("Erreur verification avis existant : " + e.getMessage());
            return true;
        }
    }

    private boolean avisAppartientAuLocataire(Avis avis, User user) {
        return user != null
                && reservationService.reservationAppartientAuLocataire(avis.getReservationId(), user);
    }

    private User currentUser() {
        return SessionManager.getInstance().getCurrentUser();
    }

    private boolean isAdmin(User user) {
        return user != null && "ROLE_ADMIN".equals(user.getRole());
    }

    private boolean isHost(User user) {
        return user != null && "ROLE_HOST".equals(user.getRole());
    }

    private boolean isGuest(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        String role = user.getRole();
        return "ROLE_GUEST".equals(role)
                || "ROLE_USER".equals(role)
                || "ROLE_HOST_PENDING".equals(role);
    }

    private static String normalizeStatut(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(
                value.trim().toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }
}
