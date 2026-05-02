package com.rentall.services;

import com.rentall.config.DatabaseConnection;
import com.rentall.entities.Avis;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

    // =========================================================
    // CREATE — Ajouter un avis
    // =========================================================
    @Override
    public void ajouter(Avis avis) throws SQLException {
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
}
