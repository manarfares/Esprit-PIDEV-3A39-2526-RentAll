package com.rentall.services;

import com.rentall.config.DatabaseConnection;
import com.rentall.dto.ReservationTableRow;
import com.rentall.entities.Reservation;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'integration legers contre la base {@code smart_rental_platform} (meme JDBC que l'app).
 * <p>
 * Les tests {@link Order} partagent une reservation creee puis modifiee puis supprimee : un
 * nettoyage SQL complet est fait dans {@link AfterAll} (et en secours si la suite echoue avant la suppression).
 * {@link AfterEach} verifie des invariants sans supprimer la ligne de la chaine CRUD.
 * <p>
 * Preconditions : MySQL accessible, {@code foyer_id} / {@code locataire_id} existants (voir constantes).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReservationServiceTest {

    /** IDs valides documentes dans le projet (contraintes FK). */
    private static final int FOYER_ID_VALIDE = 36;
    private static final int LOCATAIRE_ID_VALIDE = 32;

    /** Marqueur de statut pour retrouver / purger les lignes de test. */
    private static final String STATUT_TEST = "test_junit_reservation";
    private static final String STATUT_MODIFIE = "test_junit_reservation_modifiee";

    private static final BigDecimal MONTANT_TEST = new BigDecimal("12345.67");

    private static ReservationService service;

    /** Id de la reservation creee dans la chaine ordonnee ; 0 si pas encore creee ou deja supprimee. */
    private static int trackedReservationId;

    @BeforeAll
    static void beforeAll() {
        Assumptions.assumeTrue(DatabaseConnection.getConnection() != null,
                "Connexion MySQL indisponible : tests d'integration reservations ignores.");
        service = new ReservationService();
        assertNotNull(service);
    }

    @AfterEach
    void afterEach() {
        assertNotNull(service.afficherTous(), "afficherTous() ne doit jamais retourner null");
        assertFalse(STATUT_TEST.isEmpty(), "marqueur de test doit etre non vide");
    }

    @AfterAll
    static void afterAll() {
        if (DatabaseConnection.getConnection() == null) {
            return;
        }
        ReservationService rs = new ReservationService();
        purgeReservationsTest(rs);
        if (trackedReservationId > 0) {
            rs.supprimer(trackedReservationId);
        }
    }

    private static void purgeReservationsTest(ReservationService rs) {
        List<Reservation> liste = rs.afficherTous();
        for (Reservation r : liste) {
            String s = r.getStatut();
            if (s != null && (s.equals(STATUT_TEST) || s.equals(STATUT_MODIFIE))) {
                rs.supprimer(r.getId());
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("afficherTous retourne une liste non null")
    void afficherTous_retourneListeNonNull() {
        List<Reservation> liste = service.afficherTous();
        assertNotNull(liste);
        assertTrue(liste.size() >= 0);
    }

    @Test
    @Order(2)
    @DisplayName("listerPourAffichageTableau retourne autant de lignes que afficherTous")
    void listerPourAffichageTableau_memeTailleQueAfficherTous() {
        java.util.List<Reservation> reservations = service.afficherTous();
        java.util.List<ReservationTableRow> rows = service.listerPourAffichageTableau();
        assertNotNull(rows);
        assertEquals(reservations.size(), rows.size());
    }

    @Test
    @Order(3)
    @DisplayName("afficherParId avec id inexistant retourne null")
    void afficherParId_inconnu_retourneNull() {
        assertNull(service.afficherParId(-1));
        assertNull(service.afficherParId(0));
        assertNull(service.afficherParId(Integer.MAX_VALUE));
    }

    @Test
    @Order(4)
    @DisplayName("ajouter avec FK foyer invalide ne doit pas augmenter le nombre de lignes")
    void ajouter_foyerInvalide_neChangePasLeCompte() {
        int avant = service.afficherTous().size();
        Reservation bad = new Reservation(
                999_999_999,
                LOCATAIRE_ID_VALIDE,
                LocalDateTime.of(2099, 1, 1, 10, 0),
                LocalDateTime.of(2099, 1, 5, 10, 0),
                MONTANT_TEST,
                STATUT_TEST,
                LocalDateTime.now(),
                2
        );
        service.ajouter(bad);
        int apres = service.afficherTous().size();
        assertEquals(avant, apres, "Insert avec foyer_id invalide ne doit pas reussir silencieusement avec une ligne en plus");
    }

    @Test
    @Order(5)
    @DisplayName("ajouter une reservation valide puis la retrouver par id")
    void ajouter_puisAfficherParId() {
        LocalDateTime debut = LocalDateTime.of(2099, 6, 1, 14, 0);
        LocalDateTime fin = LocalDateTime.of(2099, 6, 10, 11, 0);
        Reservation nouvelle = new Reservation(
                FOYER_ID_VALIDE,
                LOCATAIRE_ID_VALIDE,
                debut,
                fin,
                MONTANT_TEST,
                STATUT_TEST,
                LocalDateTime.now(),
                3
        );
        service.ajouter(nouvelle);

        Optional<Reservation> trouvee = service.afficherTous().stream()
                .filter(r -> STATUT_TEST.equals(r.getStatut())
                        && r.getFoyerId() == FOYER_ID_VALIDE
                        && r.getMontantTotal() != null
                        && r.getMontantTotal().compareTo(MONTANT_TEST) == 0)
                .max(Comparator.comparingInt(Reservation::getId));

        assertTrue(trouvee.isPresent(), "La reservation inseree doit etre retrouvable dans afficherTous");
        trackedReservationId = trouvee.get().getId();
        assertTrue(trackedReservationId > 0);

        Reservation byId = service.afficherParId(trackedReservationId);
        assertNotNull(byId);
        assertEquals(FOYER_ID_VALIDE, byId.getFoyerId());
        assertEquals(LOCATAIRE_ID_VALIDE, byId.getLocataireId());
        assertEquals(0, MONTANT_TEST.compareTo(byId.getMontantTotal()));
        assertEquals(STATUT_TEST, byId.getStatut());
        assertEquals(3, byId.getNombrePersonnes());
    }

    @Test
    @Order(6)
    @DisplayName("modifier met a jour statut, montant et nombre de personnes")
    void modifier_metAJourChamps() {
        assertTrue(trackedReservationId > 0, "La reservation de test doit exister (ordre des tests)");

        Reservation r = service.afficherParId(trackedReservationId);
        assertNotNull(r);

        BigDecimal nouveauMontant = new BigDecimal("13000.00");
        r.setStatut(STATUT_MODIFIE);
        r.setMontantTotal(nouveauMontant);
        r.setNombrePersonnes(4);
        service.modifier(r);

        Reservation relu = service.afficherParId(trackedReservationId);
        assertNotNull(relu);
        assertEquals(STATUT_MODIFIE, relu.getStatut());
        assertEquals(0, nouveauMontant.compareTo(relu.getMontantTotal()));
        assertEquals(4, relu.getNombrePersonnes());
        assertFalse(STATUT_TEST.equals(relu.getStatut()));
    }

    @Test
    @Order(7)
    @DisplayName("supprimer puis afficherParId retourne null")
    void supprimer_puisIdInconnu() {
        assertTrue(trackedReservationId > 0);
        service.supprimer(trackedReservationId);
        assertNull(service.afficherParId(trackedReservationId));
        trackedReservationId = 0;
    }
}
