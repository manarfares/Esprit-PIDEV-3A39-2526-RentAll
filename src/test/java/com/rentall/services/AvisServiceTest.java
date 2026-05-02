package com.rentall.services;

import com.rentall.config.DatabaseConnection;
import com.rentall.entities.Avis;
import com.rentall.entities.Reservation;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'integration pour {@link AvisService} contre {@code smart_rental_platform}.
 * <p>
 * Une reservation dediee est creee (FK) car {@code reservation_id} est UNIQUE sur {@code avis}.
 * Nettoyage : avis puis reservation en {@link AfterAll}, avec secours par marqueurs de statut.
 * <p>
 * {@link AfterEach} : invariants sans detruire l'etat partage entre tests {@link Order}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AvisServiceTest {

    private static final int FOYER_ID_VALIDE = 36;
    private static final int LOCATAIRE_ID_VALIDE = 32;
    private static final String STATUT_RES_TEST = "test_junit_avis_reservation";

    private static ReservationService reservationService;
    private static AvisService avisService;

    private static int testReservationId;
    private static int testAvisId;

    @BeforeAll
    static void beforeAll() {
        Assumptions.assumeTrue(DatabaseConnection.getConnection() != null,
                "Connexion MySQL indisponible : tests d'integration avis ignores.");
        reservationService = new ReservationService();
        avisService = new AvisService();
        assertNotNull(reservationService);
        assertNotNull(avisService);
    }

    @AfterEach
    void afterEach() {
        assertNotNull(avisService.afficherTous());
        assertFalse(STATUT_RES_TEST.isEmpty());
    }

    @AfterAll
    static void afterAll() {
        if (DatabaseConnection.getConnection() == null) {
            return;
        }
        if (testAvisId > 0) {
            new AvisService().supprimer(testAvisId);
        }
        if (testReservationId > 0) {
            new ReservationService().supprimer(testReservationId);
        }
        purgeReservationsMarquees();
    }

    private static void purgeReservationsMarquees() {
        ReservationService rs = new ReservationService();
        for (Reservation r : rs.afficherTous()) {
            if (STATUT_RES_TEST.equals(r.getStatut())) {
                AvisService as = new AvisService();
                for (Avis a : as.afficherTous()) {
                    if (a.getReservationId() == r.getId()) {
                        as.supprimer(a.getId());
                    }
                }
                rs.supprimer(r.getId());
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("afficherTous retourne une liste non null")
    void afficherTous_retourneListeNonNull() {
        List<Avis> liste = avisService.afficherTous();
        assertNotNull(liste);
        assertTrue(liste.size() >= 0);
    }

    @Test
    @Order(2)
    @DisplayName("afficherParId avec id invalide retourne null")
    void afficherParId_inconnu_retourneNull() {
        assertNull(avisService.afficherParId(-1));
        assertNull(avisService.afficherParId(0));
    }

    @Test
    @Order(3)
    @DisplayName("cree une reservation de test sans avis (FK pour INSERT avis)")
    void creerReservationPourAvis() {
        Reservation res = new Reservation(
                FOYER_ID_VALIDE,
                LOCATAIRE_ID_VALIDE,
                LocalDateTime.of(2099, 7, 1, 15, 0),
                LocalDateTime.of(2099, 7, 8, 10, 0),
                new BigDecimal("100.00"),
                STATUT_RES_TEST,
                LocalDateTime.now(),
                2
        );
        reservationService.ajouter(res);

        Optional<Reservation> opt = reservationService.afficherTous().stream()
                .filter(r -> STATUT_RES_TEST.equals(r.getStatut()))
                .max(Comparator.comparingInt(Reservation::getId));
        assertTrue(opt.isPresent());
        testReservationId = opt.get().getId();
        assertTrue(testReservationId > 0);

        Set<Integer> reservationsAvecAvis = new HashSet<>();
        for (Avis a : avisService.afficherTous()) {
            reservationsAvecAvis.add(a.getReservationId());
        }
        assertFalse(reservationsAvecAvis.contains(testReservationId),
                "La reservation de test ne doit pas deja avoir un avis (contrainte UNIQUE)");
    }

    @Test
    @Order(4)
    @DisplayName("ajouter un avis lie a la reservation de test")
    void ajouter_avis() throws SQLException {
        assertTrue(testReservationId > 0);
        Avis avis = new Avis(
                testReservationId,
                5,
                "Commentaire test JUnit 5",
                LocalDateTime.now()
        );
        avisService.ajouter(avis);

        Optional<Avis> trouve = avisService.afficherTous().stream()
                .filter(a -> a.getReservationId() == testReservationId)
                .max(Comparator.comparingInt(Avis::getId));
        assertTrue(trouve.isPresent());
        testAvisId = trouve.get().getId();
        assertTrue(testAvisId > 0);
    }

    @Test
    @Order(5)
    @DisplayName("afficherParId retourne le bon avis")
    void afficherParId_ok() {
        assertTrue(testAvisId > 0);
        Avis a = avisService.afficherParId(testAvisId);
        assertNotNull(a);
        assertEquals(testReservationId, a.getReservationId());
        assertEquals(5, a.getNote());
        assertTrue(a.getCommentaire().contains("JUnit"));
    }

    @Test
    @Order(6)
    @DisplayName("modifier note et commentaire")
    void modifier_avis() {
        assertTrue(testAvisId > 0);
        Avis a = avisService.afficherParId(testAvisId);
        assertNotNull(a);
        a.setNote(4);
        a.setCommentaire("Commentaire modifie par test JUnit");
        avisService.modifier(a);

        Avis relu = avisService.afficherParId(testAvisId);
        assertNotNull(relu);
        assertEquals(4, relu.getNote());
        assertEquals("Commentaire modifie par test JUnit", relu.getCommentaire());
        assertFalse(relu.getCommentaire().equals("Commentaire test JUnit 5"));
    }

    @Test
    @Order(7)
    @DisplayName("supprimer l'avis puis afficherParId null")
    void supprimer_avis() {
        assertTrue(testAvisId > 0);
        avisService.supprimer(testAvisId);
        assertNull(avisService.afficherParId(testAvisId));
        testAvisId = 0;
    }

    @Test
    @Order(8)
    @DisplayName("supprimer la reservation de test")
    void supprimer_reservationDeTest() {
        assertTrue(testReservationId > 0);
        reservationService.supprimer(testReservationId);
        assertNull(reservationService.afficherParId(testReservationId));
        testReservationId = 0;
    }

    @Test
    @Order(9)
    @DisplayName("ajouter un 2e avis sur la meme reservation doit echouer (UNIQUE)")
    void ajouter_deuxiemeAvisMemeReservation_leveSQLException() throws SQLException {
        Reservation res = new Reservation(
                FOYER_ID_VALIDE,
                LOCATAIRE_ID_VALIDE,
                LocalDateTime.of(2099, 8, 1, 12, 0),
                LocalDateTime.of(2099, 8, 3, 12, 0),
                new BigDecimal("50.00"),
                STATUT_RES_TEST,
                LocalDateTime.now(),
                1
        );
        reservationService.ajouter(res);
        int resId = reservationService.afficherTous().stream()
                .filter(r -> STATUT_RES_TEST.equals(r.getStatut()))
                .max(Comparator.comparingInt(Reservation::getId))
                .map(Reservation::getId)
                .orElse(0);
        assertTrue(resId > 0);

        avisService.ajouter(new Avis(resId, 5, "Premier avis", LocalDateTime.now()));

        assertThrows(SQLException.class, () ->
                avisService.ajouter(new Avis(resId, 3, "Doublon interdit", LocalDateTime.now())));

        for (Avis a : avisService.afficherTous()) {
            if (a.getReservationId() == resId) {
                avisService.supprimer(a.getId());
            }
        }
        reservationService.supprimer(resId);
    }
}
