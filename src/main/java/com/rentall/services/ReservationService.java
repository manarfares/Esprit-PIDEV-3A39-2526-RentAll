package com.rentall.services;

import com.rentall.config.DatabaseConnection;
import com.rentall.dto.ReservationTableRow;
import com.rentall.entities.Reservation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe ReservationService
 *
 * RÔLE : Implémentation concrète du CRUD pour la table "reservation"
 *        de la base smart_rental_platform.
 *
 * RAPPEL DES COLONNES RÉELLES DE LA TABLE :
 *   id, foyer_id, locataire_id, date_debut, date_fin,
 *   montant_total, statut, date_creation, nombre_personnes
 *
 * CONCEPTS CLÉS :
 *
 * PreparedStatement :
 *   → Requête SQL avec des "?" comme paramètres.
 *   → On remplace les "?" avec ps.setXxx(position, valeur).
 *   → Protège contre les injections SQL (sécurité).
 *
 * executeUpdate() :
 *   → Pour INSERT, UPDATE, DELETE.
 *   → Retourne le nombre de lignes affectées.
 *
 * executeQuery() :
 *   → Pour SELECT.
 *   → Retourne un ResultSet (les lignes trouvées).
 *
 * ResultSet :
 *   → On parcourt les lignes avec rs.next().
 *   → On lit les valeurs avec rs.getInt("colonne"), rs.getString("colonne"), etc.
 *   → rs.getTimestamp("colonne").toLocalDateTime() pour les colonnes datetime.
 */
public class ReservationService implements IReservationService {

    private Connection connection = DatabaseConnection.getConnection();

    /** Requête liste (lecture) ; recalculée si la première exécution échoue (schéma atypique). */
    private volatile String memoListeAffichageSql;

    // =========================================================
    // CREATE — Ajouter une réservation
    //
    // Requête : INSERT INTO reservation (col1, col2, ...) VALUES (?, ?, ...)
    // On ne met PAS "id" car c'est AUTO_INCREMENT, MySQL le génère seul.
    // =========================================================
    @Override
    public void ajouter(Reservation reservation) {
        ajouterEtRetournerId(reservation);
    }
    
    /**
     * Ajoute une réservation et retourne l'ID généré.
     * @param reservation La réservation à ajouter
     * @return L'ID de la réservation créée, ou -1 en cas d'erreur
     */
    public int ajouterEtRetournerId(Reservation reservation) {
        String sql = "INSERT INTO reservation " +
                     "(logement_id, locataire_id, date_debut, date_fin, " +
                     "montant_total, statut, date_creation, nombre_personnes) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setInt(1, reservation.getFoyerId());
            ps.setInt(2, reservation.getLocataireId());
            ps.setTimestamp(3, Timestamp.valueOf(reservation.getDateDebut()));
            ps.setTimestamp(4, Timestamp.valueOf(reservation.getDateFin()));
            ps.setBigDecimal(5, reservation.getMontantTotal());
            ps.setString(6, reservation.getStatut());
            ps.setTimestamp(7, Timestamp.valueOf(reservation.getDateCreation()));
            ps.setInt(8, reservation.getNombrePersonnes());

            int affectedRows = ps.executeUpdate();
            
            if (affectedRows > 0) {
                ResultSet generatedKeys = ps.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    System.out.println("✅ Réservation ajoutée avec succès ! ID: " + id);
                    return id;
                }
            }
            System.out.println("✅ Réservation ajoutée avec succès !");

        } catch (SQLException e) {
            System.out.println("Ajout de réservation refusé : données invalides ou contrainte SQL non respectée.");
        }
        return -1;
    }

    // =========================================================
    // UPDATE — Modifier une réservation existante
    //
    // Requête : UPDATE reservation SET col1=?, col2=?, ... WHERE id=?
    // Le WHERE id=? cible uniquement la réservation à modifier.
    // =========================================================
    @Override
    public void modifier(Reservation reservation) {
        String sql = "UPDATE reservation SET " +
                     "logement_id=?, locataire_id=?, date_debut=?, date_fin=?, " +
                     "montant_total=?, statut=?, date_creation=?, nombre_personnes=? " +
                     "WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);

            ps.setInt(1, reservation.getFoyerId());
            ps.setInt(2, reservation.getLocataireId());
            ps.setTimestamp(3, Timestamp.valueOf(reservation.getDateDebut()));
            ps.setTimestamp(4, Timestamp.valueOf(reservation.getDateFin()));
            ps.setBigDecimal(5, reservation.getMontantTotal());
            ps.setString(6, reservation.getStatut());
            ps.setTimestamp(7, Timestamp.valueOf(reservation.getDateCreation()));
            ps.setInt(8, reservation.getNombrePersonnes());
            // Position 9 → le WHERE id=?
            ps.setInt(9, reservation.getId());

            ps.executeUpdate();
            System.out.println("✅ Réservation modifiée avec succès !");

        } catch (SQLException e) {
            System.out.println("❌ Erreur modification réservation : " + e.getMessage());
        }
    }

    // =========================================================
    // DELETE — Supprimer une réservation par son id
    //
    // Requête : DELETE FROM reservation WHERE id=?
    // =========================================================
    @Override
    public void supprimer(int id) {
        String sql = "DELETE FROM reservation WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Réservation supprimée (id=" + id + ")");

        } catch (SQLException e) {
            System.out.println("❌ Erreur suppression réservation : " + e.getMessage());
        }
    }

    // =========================================================
    // READ — Afficher toutes les réservations
    //
    // Requête : SELECT * FROM reservation
    // On parcourt le ResultSet ligne par ligne avec rs.next()
    // Pour chaque ligne, on crée un objet Reservation et on l'ajoute à la liste.
    // =========================================================
    @Override
    public List<Reservation> afficherTous() {
        List<Reservation> liste = new ArrayList<>();
        String sql = "SELECT * FROM reservation";
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                // rs.getTimestamp("date_debut").toLocalDateTime()
                // → convertit le datetime SQL en LocalDateTime Java
                Reservation r = new Reservation(
                        rs.getInt("id"),
                        rs.getInt("logement_id"),
                        rs.getInt("locataire_id"),
                        rs.getTimestamp("date_debut").toLocalDateTime(),
                        rs.getTimestamp("date_fin").toLocalDateTime(),
                        rs.getBigDecimal("montant_total"),
                        rs.getString("statut"),
                        rs.getTimestamp("date_creation").toLocalDateTime(),
                        rs.getInt("nombre_personnes")
                );
                liste.add(r);
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur affichage réservations : " + e.getMessage());
        }
        return liste;
    }

    // =========================================================
    // READ — Afficher une réservation par son id
    //
    // Requête : SELECT * FROM reservation WHERE id=?
    // Retourne l'objet Reservation trouvé, ou null si inexistant.
    // =========================================================
    @Override
    public Reservation afficherParId(int id) {
        String sql = "SELECT * FROM reservation WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Reservation(
                        rs.getInt("id"),
                        rs.getInt("logement_id"),
                        rs.getInt("locataire_id"),
                        rs.getTimestamp("date_debut").toLocalDateTime(),
                        rs.getTimestamp("date_fin").toLocalDateTime(),
                        rs.getBigDecimal("montant_total"),
                        rs.getString("statut"),
                        rs.getTimestamp("date_creation").toLocalDateTime(),
                        rs.getInt("nombre_personnes")
                );
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur recherche réservation : " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<ReservationTableRow> listerPourAffichageTableau() {
        try {
            if (memoListeAffichageSql == null || !memoListeAffichageSql.contains("r.id")) {
                memoListeAffichageSql = ReservationListeQueryBuilder.buildSelectListe(connection);
            }
            try (PreparedStatement ps = connection.prepareStatement(memoListeAffichageSql);
                 ResultSet rs = ps.executeQuery()) {
                List<ReservationTableRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new ReservationTableRow(
                            rs.getInt("id"),
                            rs.getTimestamp("date_debut").toLocalDateTime(),
                            rs.getTimestamp("date_fin").toLocalDateTime(),
                            rs.getBigDecimal("montant_total"),
                            rs.getString("statut"),
                            rs.getInt("nombre_personnes"),
                            rs.getString("foyer_libelle"),
                            rs.getString("locataire_libelle")
                    ));
                }
                return rows;
            }
        } catch (SQLException e) {
            memoListeAffichageSql = null;
            return fallbackListeSansJointure();
        }
    }

    private List<ReservationTableRow> fallbackListeSansJointure() {
        List<ReservationTableRow> out = new ArrayList<>();
        for (Reservation r : afficherTous()) {
            out.add(new ReservationTableRow(
                    r.getId(),
                    r.getDateDebut(),
                    r.getDateFin(),
                    r.getMontantTotal(),
                    r.getStatut(),
                    r.getNombrePersonnes(),
                    "Réf. foyer " + r.getFoyerId(),
                    "Locataire n°" + r.getLocataireId()
            ));
        }
        return out;
    }
}
