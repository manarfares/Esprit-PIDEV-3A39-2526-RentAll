package com.rentall.services;

import com.rentall.config.DatabaseConnection;
import com.rentall.dto.ReservationTableRow;
import com.rentall.entities.Reservation;
import com.rentall.util.SchemaColumnPicker;
import tn.piapp.model.User;
import tn.piapp.util.SessionManager;

import java.sql.*;
import java.time.LocalDateTime;
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
        Reservation autorisee = reservationPourCreationAutorisee(reservation);
        if (autorisee == null) {
            System.out.println("Ajout de réservation refusé : rôle non autorisé.");
            return -1;
        }

        String sql = "INSERT INTO reservation " +
                     "(logement_id, locataire_id, date_debut, date_fin, " +
                     "montant_total, statut, date_creation, nombre_personnes) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setInt(1, autorisee.getFoyerId());
            ps.setInt(2, autorisee.getLocataireId());
            ps.setTimestamp(3, Timestamp.valueOf(autorisee.getDateDebut()));
            ps.setTimestamp(4, Timestamp.valueOf(autorisee.getDateFin()));
            ps.setBigDecimal(5, autorisee.getMontantTotal());
            ps.setString(6, autorisee.getStatut());
            ps.setTimestamp(7, Timestamp.valueOf(autorisee.getDateCreation()));
            ps.setInt(8, autorisee.getNombrePersonnes());

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
        modifierEtRetournerSucces(reservation);
    }

    public boolean modifierEtRetournerSucces(Reservation reservation) {
        Reservation autorisee = reservationPourModificationAutorisee(reservation);
        if (autorisee == null) {
            System.out.println("Modification de réservation refusée : droits insuffisants.");
            return false;
        }

        String sql = "UPDATE reservation SET " +
                     "logement_id=?, locataire_id=?, date_debut=?, date_fin=?, " +
                     "montant_total=?, statut=?, date_creation=?, nombre_personnes=? " +
                     "WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);

            ps.setInt(1, autorisee.getFoyerId());
            ps.setInt(2, autorisee.getLocataireId());
            ps.setTimestamp(3, Timestamp.valueOf(autorisee.getDateDebut()));
            ps.setTimestamp(4, Timestamp.valueOf(autorisee.getDateFin()));
            ps.setBigDecimal(5, autorisee.getMontantTotal());
            ps.setString(6, autorisee.getStatut());
            ps.setTimestamp(7, Timestamp.valueOf(autorisee.getDateCreation()));
            ps.setInt(8, autorisee.getNombrePersonnes());
            // Position 9 → le WHERE id=?
            ps.setInt(9, autorisee.getId());

            int updatedRows = ps.executeUpdate();
            System.out.println("[ReservationService] update réservation id=" + autorisee.getId()
                    + ", lignes modifiées=" + updatedRows);
            System.out.println("✅ Réservation modifiée avec succès !");
            return updatedRows > 0;

        } catch (SQLException e) {
            System.out.println("❌ Erreur modification réservation : " + e.getMessage());
        }
        return false;
    }

    // =========================================================
    // DELETE — Supprimer une réservation par son id
    //
    // Requête : DELETE FROM reservation WHERE id=?
    // =========================================================
    @Override
    public void supprimer(int id) {
        if (!peutSupprimerReservation(id, currentUser())) {
            System.out.println("Suppression de réservation refusée : droits insuffisants.");
            return;
        }

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
    public boolean estLogementDisponible(int logementId, LocalDateTime dateDebut,
                                         LocalDateTime dateFin) {
        return estLogementDisponible(logementId, dateDebut, dateFin, null);
    }

    @Override
    public boolean estLogementDisponible(int logementId, LocalDateTime dateDebut,
                                         LocalDateTime dateFin,
                                         Integer reservationIdAExclure) {
        if (logementId <= 0 || dateDebut == null || dateFin == null || !dateFin.isAfter(dateDebut)) {
            return false;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM reservation "
                        + "WHERE logement_id = ? "
                        + "AND ? < date_fin "
                        + "AND ? > date_debut "
                        + "AND LOWER(COALESCE(statut, '')) NOT IN "
                        + "('annulee', 'annulée', 'cancelled', 'canceled', 'refusee', 'refusée')");

        if (reservationIdAExclure != null) {
            sql.append(" AND id <> ?");
        }

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            ps.setInt(1, logementId);
            ps.setTimestamp(2, Timestamp.valueOf(dateDebut));
            ps.setTimestamp(3, Timestamp.valueOf(dateFin));
            if (reservationIdAExclure != null) {
                ps.setInt(4, reservationIdAExclure);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            System.out.println("Erreur verification disponibilite logement : " + e.getMessage());
            return false;
        }
    }

    public boolean modifierStatut(int reservationId, String nouveauStatut) {
        Reservation existante = afficherParId(reservationId);
        if (existante == null) {
            return false;
        }
        Reservation copie = copyOf(existante);
        copie.setStatut(nouveauStatut);
        Reservation autorisee = reservationPourModificationAutorisee(copie);
        if (autorisee == null) {
            return false;
        }
        modifier(autorisee);
        return true;
    }

    private Reservation reservationPourCreationAutorisee(Reservation reservation) {
        User user = currentUser();
        if (user == null) {
            Reservation copie = copyOf(reservation);
            copie.setStatut(normalizeStatut(copie.getStatut(), "en_attente"));
            return copie;
        }

        if (isHost(user)) {
            return null;
        }

        Reservation copie = copyOf(reservation);
        if (isGuest(user)) {
            copie.setLocataireId(user.getId());
            copie.setStatut("en_attente");
            return copie;
        }

        if (isAdmin(user)) {
            copie.setStatut(normalizeStatut(copie.getStatut(), "en_attente"));
            return copie;
        }

        return null;
    }

    private Reservation reservationPourModificationAutorisee(Reservation reservation) {
        Reservation existante = reservation.getId() > 0 ? afficherParId(reservation.getId()) : null;
        if (existante == null) {
            return null;
        }

        User user = currentUser();
        if (user == null) {
            Reservation copie = copyOf(reservation);
            copie.setStatut(normalizeStatut(copie.getStatut(), existante.getStatut()));
            return copie;
        }

        if (isAdmin(user)) {
            Reservation copie = copyOf(reservation);
            copie.setStatut(normalizeStatut(copie.getStatut(), existante.getStatut()));
            return copie;
        }

        if (isGuest(user)) {
            if (!reservationAppartientAuLocataire(existante, user) || !isPending(existante.getStatut())) {
                return null;
            }
            Reservation copie = copyOf(reservation);
            copie.setLocataireId(user.getId());
            copie.setStatut(existante.getStatut());
            return copie;
        }

        if (isHost(user)) {
            String nouveauStatut = normalizeStatut(reservation.getStatut(), existante.getStatut());
            if (!reservationLieeAuHost(existante, user)
                    || !transitionStatutHostAutorisee(existante.getStatut(), nouveauStatut)) {
                return null;
            }
            Reservation copie = copyOf(existante);
            copie.setStatut(nouveauStatut);
            return copie;
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

    public List<ReservationTableRow> listerPourAffichageTableauPourUtilisateur(User user) {
        List<ReservationTableRow> rows = new ArrayList<>();
        for (ReservationTableRow row : listerPourAffichageTableau()) {
            if (peutVoirReservation(row.getId(), user)) {
                rows.add(row);
            }
        }
        return rows;
    }

    public List<Reservation> afficherTousPourUtilisateur(User user) {
        List<Reservation> rows = new ArrayList<>();
        for (Reservation reservation : afficherTous()) {
            if (peutVoirReservation(reservation, user)) {
                rows.add(reservation);
            }
        }
        return rows;
    }

    public boolean peutVoirReservation(int reservationId, User user) {
        Reservation reservation = afficherParId(reservationId);
        return reservation != null && peutVoirReservation(reservation, user);
    }

    public boolean peutVoirReservation(Reservation reservation, User user) {
        if (reservation == null) {
            return false;
        }
        if (user == null || isAdmin(user)) {
            return true;
        }
        if (isGuest(user)) {
            return reservationAppartientAuLocataire(reservation, user);
        }
        if (isHost(user)) {
            return reservationLieeAuHost(reservation, user);
        }
        return false;
    }

    public boolean peutModifierReservation(Reservation reservation, User user) {
        if (reservation == null) {
            return false;
        }
        if (user == null || isAdmin(user)) {
            return true;
        }
        return isGuest(user)
                && reservationAppartientAuLocataire(reservation, user)
                && isPending(reservation.getStatut());
    }

    public boolean peutSupprimerReservation(int reservationId, User user) {
        Reservation reservation = afficherParId(reservationId);
        if (reservation == null) {
            return false;
        }
        if (user == null || isAdmin(user)) {
            return true;
        }
        return isGuest(user)
                && reservationAppartientAuLocataire(reservation, user)
                && isPending(reservation.getStatut());
    }

    public boolean peutChangerStatutReservation(int reservationId, String nouveauStatut, User user) {
        Reservation reservation = afficherParId(reservationId);
        if (reservation == null) {
            return false;
        }
        if (user == null || isAdmin(user)) {
            return true;
        }
        return isHost(user)
                && reservationLieeAuHost(reservation, user)
                && transitionStatutHostAutorisee(reservation.getStatut(), nouveauStatut);
    }

    public boolean reservationAppartientAuLocataire(int reservationId, User user) {
        Reservation reservation = afficherParId(reservationId);
        return reservation != null && reservationAppartientAuLocataire(reservation, user);
    }

    public boolean reservationLieeAuHost(int reservationId, User user) {
        Reservation reservation = afficherParId(reservationId);
        return reservation != null && reservationLieeAuHost(reservation, user);
    }

    private boolean reservationAppartientAuLocataire(Reservation reservation, User user) {
        return user != null && reservation.getLocataireId() == user.getId();
    }

    private boolean reservationLieeAuHost(Reservation reservation, User user) {
        if (reservation == null || user == null) {
            return false;
        }

        for (String table : new String[]{"logement", "foyer", "home"}) {
            try {
                if (!SchemaColumnPicker.tableExists(connection, table)) {
                    continue;
                }
                List<String> ownerColumns = hostOwnerColumns(table);
                if (ownerColumns.isEmpty()) {
                    continue;
                }

                StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM `")
                        .append(table)
                        .append("` WHERE id=? AND (");
                for (int i = 0; i < ownerColumns.size(); i++) {
                    if (i > 0) {
                        sql.append(" OR ");
                    }
                    sql.append("`").append(ownerColumns.get(i)).append("`=?");
                }
                sql.append(")");

                try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
                    ps.setInt(1, reservation.getFoyerId());
                    for (int i = 0; i < ownerColumns.size(); i++) {
                        ps.setInt(i + 2, user.getId());
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            return true;
                        }
                    }
                }
            } catch (SQLException e) {
                System.out.println("Verification host/logement impossible : " + e.getMessage());
            }
        }
        return false;
    }

    private List<String> hostOwnerColumns(String table) throws SQLException {
        List<String> out = new ArrayList<>();
        List<String> candidates = List.of(
                "hote_id", "host_id", "proprietaire_id", "owner_id", "user_id", "utilisateur_id");
        List<String> cols = new ArrayList<>(SchemaColumnPicker.columns(connection, table));
        for (String candidate : candidates) {
            if (cols.contains(candidate)) {
                out.add(candidate);
            }
        }
        return out;
    }

    private boolean transitionStatutHostAutorisee(String ancien, String nouveau) {
        String from = normalizeStatut(ancien, "en_attente");
        String to = normalizeStatut(nouveau, from);
        return ("en_attente".equals(from) && ("confirmee".equals(to) || "refusee".equals(to)))
                || ("confirmee".equals(from) && "terminee".equals(to));
    }

    private boolean isPending(String statut) {
        return "en_attente".equals(normalizeStatut(statut, ""));
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

    private static String normalizeStatut(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback == null || fallback.isBlank() ? "en_attente" : fallback.trim().toLowerCase();
        }
        return value.trim()
                .toLowerCase()
                .replace('é', 'e')
                .replace('è', 'e')
                .replace('ê', 'e');
    }

    private static Reservation copyOf(Reservation source) {
        Reservation copy = new Reservation(
                source.getFoyerId(),
                source.getLocataireId(),
                source.getDateDebut(),
                source.getDateFin(),
                source.getMontantTotal(),
                source.getStatut(),
                source.getDateCreation(),
                source.getNombrePersonnes()
        );
        copy.setId(source.getId());
        return copy;
    }
}
