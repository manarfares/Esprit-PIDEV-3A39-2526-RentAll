package com.rentall;

import com.rentall.entities.Avis;
import com.rentall.entities.Reservation;
import com.rentall.services.AvisService;
import com.rentall.services.ReservationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Classe Main — Point d'entrée du programme
 *
 * RÔLE : Tester toutes les opérations CRUD sur les tables
 *        "reservation" et "avis" de smart_rental_platform.
 *
 * IMPORTANT : Ce code lit et écrit dans la base existante.
 *             Il ne modifie pas la structure (pas de CREATE TABLE, pas d'ALTER).
 *
 * POUR LA PRÉSENTATION :
 * Tu peux commenter/décommenter les sections selon ce que tu veux montrer.
 */
public class Main {

    public static void main(String[] args) {

        // Instanciation des services
        ReservationService reservationService = new ReservationService();
        AvisService avisService = new AvisService();

        // =====================================================
        // ===         CRUD RESERVATION                     ===
        // =====================================================
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║       CRUD - RESERVATION             ║");
        System.out.println("╚══════════════════════════════════════╝");

        // --- 1. AJOUTER une réservation (CREATE) ---
        // CORRECTION : on utilise des IDs qui existent réellement dans la base.
        // D'après les données affichées par afficherTous() :
        //   → foyer_id valides    : 36, 37, 38, 39, 40
        //   → locataire_id valides: 32, 33, 35
        // On utilise foyer_id=36 et locataire_id=32 qui sont présents dans la base.
        System.out.println("\n[1] Ajout d'une réservation...");
        Reservation nouvelleReservation = new Reservation(
                36,                                         // foyer_id  (existe dans table foyer)
                32,                                         // locataire_id (existe dans table locataire)
                LocalDateTime.of(2026, 8, 1, 14, 0),       // date_debut
                LocalDateTime.of(2026, 8, 15, 11, 0),      // date_fin
                new BigDecimal("1500.00"),                  // montant_total
                "en_attente",                               // statut
                LocalDateTime.now(),                        // date_creation
                3                                           // nombre_personnes
        );
        reservationService.ajouter(nouvelleReservation);

        // --- 2. AFFICHER toutes les réservations (READ) ---
        System.out.println("\n[2] Liste de toutes les réservations :");
        List<Reservation> reservations = reservationService.afficherTous();
        if (reservations.isEmpty()) {
            System.out.println("   Aucune réservation trouvée.");
        } else {
            reservations.forEach(System.out::println);
        }

        // --- 3. AFFICHER une réservation par ID (READ) ---
        // On prend l'id de la première réservation pour le test de recherche
        if (!reservations.isEmpty()) {
            int idTest = reservations.get(0).getId();
            System.out.println("\n[3] Recherche de la réservation id=" + idTest + " :");
            Reservation trouvee = reservationService.afficherParId(idTest);
            System.out.println("   " + trouvee);

            // --- 4. MODIFIER la réservation (UPDATE) ---
            // On modifie la première réservation existante (id=9 dans ta base)
            System.out.println("\n[4] Modification de la réservation id=" + idTest + "...");
            trouvee.setStatut("confirmee");
            trouvee.setNombrePersonnes(4);
            trouvee.setMontantTotal(new BigDecimal("1800.00"));
            reservationService.modifier(trouvee);
            System.out.println("   Après modification : " + reservationService.afficherParId(idTest));

            // --- 5. SUPPRIMER la réservation qu'on vient d'ajouter (DELETE) ---
            // On récupère la liste à jour et on prend la dernière (celle qu'on vient d'insérer)
            List<Reservation> listeActualisee = reservationService.afficherTous();
            if (!listeActualisee.isEmpty()) {
                int idASupprimer = listeActualisee.get(listeActualisee.size() - 1).getId();
                System.out.println("\n[5] Suppression de la réservation ajoutée (id=" + idASupprimer + ")...");
                reservationService.supprimer(idASupprimer);
            }
        }

        // =====================================================
        // ===              CRUD AVIS                       ===
        // =====================================================
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║           CRUD - AVIS                ║");
        System.out.println("╚══════════════════════════════════════╝");

        // Pour ajouter un avis, on a besoin d'un reservation_id existant.
        // ATTENTION : reservation_id est UNIQUE dans la table avis.
        // → On ne peut pas ajouter un avis sur une réservation qui en a déjà un.
        // → On utilise la réservation qu'on vient d'ajouter (la dernière insérée).
        // → Si elle a été supprimée, on cherche une réservation sans avis.
        List<Reservation> resDisponibles = reservationService.afficherTous();

        if (resDisponibles.isEmpty()) {
            System.out.println("\n⚠️  Aucune réservation disponible pour tester les avis.");
        } else {
            List<Avis> avisExistants = avisService.afficherTous();
            java.util.Set<Integer> reservationsAvecAvis = new java.util.HashSet<>();
            for (Avis a : avisExistants) reservationsAvecAvis.add(a.getReservationId());

            Reservation reservationSansAvis = null;
            for (Reservation r : resDisponibles) {
                if (!reservationsAvecAvis.contains(r.getId())) {
                    reservationSansAvis = r;
                    break;
                }
            }

            if (reservationSansAvis == null) {
                System.out.println("\n⚠️  Toutes les réservations ont déjà un avis.");
                avisExistants.forEach(System.out::println);
            } else {
                int reservationIdPourAvis = reservationSansAvis.getId();

                // --- 1. AJOUTER un avis (CREATE) ---
                System.out.println("\n[1] Ajout d'un avis pour la réservation id=" + reservationIdPourAvis + "...");
                try {
                    avisService.ajouter(new Avis(
                            reservationIdPourAvis, 5,
                            "Très belle maison, propre et accueillante. Je recommande vivement !",
                            LocalDateTime.now()
                    ));
                } catch (java.sql.SQLException e) {
                    String msg = e.getMessage();
                    System.out.println(msg != null && msg.contains("Duplicate")
                            ? "❌ Cette réservation a déjà un avis."
                            : "❌ Erreur ajout avis : " + msg);
                }

                // --- 2. AFFICHER tous les avis (READ) ---
                System.out.println("\n[2] Liste de tous les avis :");
                List<Avis> avisList = avisService.afficherTous();
                avisList.forEach(System.out::println);

                // --- 3. MODIFIER le dernier avis (UPDATE) ---
                if (!avisList.isEmpty()) {
                    Avis avisAModifier = avisList.get(avisList.size() - 1);
                    System.out.println("\n[3] Modification de l'avis id=" + avisAModifier.getId() + "...");
                    avisAModifier.setNote(4);
                    avisAModifier.setCommentaire("Bon séjour, quelques petits détails à améliorer.");
                    avisService.modifier(avisAModifier);
                    System.out.println("   Après modification : " + avisService.afficherParId(avisAModifier.getId()));

                    // --- 4. SUPPRIMER l'avis (DELETE) ---
                    System.out.println("\n[4] Suppression de l'avis id=" + avisAModifier.getId() + "...");
                    avisService.supprimer(avisAModifier.getId());
                }
            }
        }

        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║         FIN DES TESTS CRUD           ║");
        System.out.println("╚══════════════════════════════════════╝");
    }
}
