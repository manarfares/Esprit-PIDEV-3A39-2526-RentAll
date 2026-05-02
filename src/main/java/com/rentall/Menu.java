package com.rentall;

import com.rentall.entities.Avis;
import com.rentall.entities.Reservation;
import com.rentall.services.AvisService;
import com.rentall.services.ReservationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Menu console interactif — Rentall
 *
 * COMMENT LANCER :
 * Option A (recommandée) : Terminal Windows
 *   1. Ouvrir un terminal dans le dossier RentallJava
 *   2. mvn compile
 *   3. mvn exec:java -Dexec.mainClass="com.rentall.Menu"
 *
 * Option B : IntelliJ
 *   Run > Edit Configurations > cocher "Redirect input from" ou
 *   simplement cliquer dans la console Run et taper le choix
 */
public class Menu {

    private static final ReservationService reservationService = new ReservationService();
    private static final AvisService avisService = new AvisService();
    private static final Scanner sc = new Scanner(System.in);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // =========================================================
    // MAIN
    // =========================================================
    public static void main(String[] args) {
        afficherBienvenue();
        boolean run = true;
        while (run) {
            afficherMenuPrincipal();
            int choix = lireInt();
            switch (choix) {
                case 1:
                    menuReservation();
                    break;
                case 2:
                    menuAvis();
                    break;
                case 0:
                    run = false;
                    ligne();
                    System.out.println("  Au revoir ! A bientot sur Rentall.");
                    ligne();
                    break;
                default:
                    erreur("Choix invalide. Tapez 0, 1 ou 2.");
            }
        }
        sc.close();
    }

    // =========================================================
    // MENU RÉSERVATION
    // =========================================================
    private static void menuReservation() {
        boolean retour = false;
        while (!retour) {
            System.out.println();
            ligne();
            System.out.println("       GESTION DES RESERVATIONS");
            ligne();
            System.out.println("  1. Afficher toutes les reservations");
            System.out.println("  2. Rechercher une reservation par ID");
            System.out.println("  3. Ajouter une reservation");
            System.out.println("  4. Modifier une reservation");
            System.out.println("  5. Supprimer une reservation");
            System.out.println("  0. Retour au menu principal");
            ligne();
            System.out.print("  Votre choix : ");
            int choix = lireInt();
            switch (choix) {
                case 1: afficherReservations();  break;
                case 2: rechercherReservation(); break;
                case 3: ajouterReservation();    break;
                case 4: modifierReservation();   break;
                case 5: supprimerReservation();  break;
                case 0: retour = true;           break;
                default: erreur("Choix invalide.");
            }
        }
    }

    // =========================================================
    // MENU AVIS
    // =========================================================
    private static void menuAvis() {
        boolean retour = false;
        while (!retour) {
            System.out.println();
            ligne();
            System.out.println("           GESTION DES AVIS");
            ligne();
            System.out.println("  1. Afficher tous les avis");
            System.out.println("  2. Rechercher un avis par ID");
            System.out.println("  3. Ajouter un avis");
            System.out.println("  4. Modifier un avis");
            System.out.println("  5. Supprimer un avis");
            System.out.println("  0. Retour au menu principal");
            ligne();
            System.out.print("  Votre choix : ");
            int choix = lireInt();
            switch (choix) {
                case 1: afficherAvis();    break;
                case 2: rechercherAvis();  break;
                case 3: ajouterAvis();     break;
                case 4: modifierAvis();    break;
                case 5: supprimerAvis();   break;
                case 0: retour = true;     break;
                default: erreur("Choix invalide.");
            }
        }
    }

    // =========================================================
    // OPÉRATIONS RÉSERVATION
    // =========================================================

    /** READ — Afficher toutes les réservations en tableau */
    private static void afficherReservations() {
        List<Reservation> liste = reservationService.afficherTous();
        System.out.println();
        ligne();
        System.out.printf("  %-4s %-10s %-12s %-17s %-17s %-12s %-12s %-5s%n",
                "ID", "FoyerID", "LocataireID", "Date debut", "Date fin",
                "Montant(EUR)", "Statut", "Pers.");
        ligne();
        if (liste.isEmpty()) {
            System.out.println("  Aucune reservation trouvee.");
        } else {
            for (Reservation r : liste) {
                System.out.printf("  %-4d %-10d %-12d %-17s %-17s %-12s %-12s %-5d%n",
                        r.getId(),
                        r.getFoyerId(),
                        r.getLocataireId(),
                        r.getDateDebut().format(FMT),
                        r.getDateFin().format(FMT),
                        r.getMontantTotal().toString(),
                        r.getStatut(),
                        r.getNombrePersonnes()
                );
            }
        }
        ligne();
        System.out.println("  Total : " + liste.size() + " reservation(s)");
    }

    /** READ — Rechercher par ID */
    private static void rechercherReservation() {
        System.out.print("  Entrez l'ID de la reservation : ");
        int id = lireInt();
        Reservation r = reservationService.afficherParId(id);
        System.out.println();
        if (r == null) {
            erreur("Aucune reservation trouvee avec id=" + id);
        } else {
            ligne();
            System.out.println("  Reservation trouvee :");
            System.out.println("  ID             : " + r.getId());
            System.out.println("  Foyer ID       : " + r.getFoyerId());
            System.out.println("  Locataire ID   : " + r.getLocataireId());
            System.out.println("  Date debut     : " + r.getDateDebut().format(FMT));
            System.out.println("  Date fin       : " + r.getDateFin().format(FMT));
            System.out.println("  Montant total  : " + r.getMontantTotal() + " EUR");
            System.out.println("  Statut         : " + r.getStatut());
            System.out.println("  Nb personnes   : " + r.getNombrePersonnes());
            ligne();
        }
    }

    /** CREATE — Ajouter une réservation */
    private static void ajouterReservation() {
        System.out.println();
        ligne();
        System.out.println("  AJOUT D'UNE RESERVATION");
        System.out.println("  IDs foyer valides dans la base    : 36, 37, 38, 39, 40");
        System.out.println("  IDs locataire valides dans la base: 32, 33, 35");
        ligne();

        System.out.print("  Foyer ID          : ");
        int foyerId = lireInt();

        System.out.print("  Locataire ID      : ");
        int locataireId = lireInt();

        System.out.print("  Date debut        (format: 2026-09-01T14:00) : ");
        LocalDateTime dateDebut = lireDateTime();
        if (dateDebut == null) return;

        System.out.print("  Date fin          (format: 2026-09-10T11:00) : ");
        LocalDateTime dateFin = lireDateTime();
        if (dateFin == null) return;

        System.out.print("  Montant total     (ex: 1200.00) : ");
        BigDecimal montant = lireBigDecimal();
        if (montant == null) return;

        System.out.print("  Statut            (en_attente / confirmee / annulee) : ");
        String statut = sc.nextLine().trim();

        System.out.print("  Nombre personnes  : ");
        int nbPersonnes = lireInt();

        Reservation r = new Reservation(
                foyerId, locataireId, dateDebut, dateFin,
                montant, statut, LocalDateTime.now(), nbPersonnes
        );
        reservationService.ajouter(r);
    }

    /** UPDATE — Modifier une réservation */
    private static void modifierReservation() {
        System.out.print("  ID de la reservation a modifier : ");
        int id = lireInt();
        Reservation r = reservationService.afficherParId(id);
        if (r == null) {
            erreur("Reservation introuvable.");
            return;
        }
        System.out.println();
        System.out.println("  Valeurs actuelles :");
        System.out.println("  Statut         : " + r.getStatut());
        System.out.println("  Montant        : " + r.getMontantTotal());
        System.out.println("  Nb personnes   : " + r.getNombrePersonnes());
        System.out.println();
        System.out.println("  Entrez les nouvelles valeurs (Entree = garder l'actuel) :");

        System.out.print("  Nouveau statut [" + r.getStatut() + "] : ");
        String statut = sc.nextLine().trim();
        if (!statut.isEmpty()) r.setStatut(statut);

        System.out.print("  Nouveau montant [" + r.getMontantTotal() + "] : ");
        String montantStr = sc.nextLine().trim();
        if (!montantStr.isEmpty()) r.setMontantTotal(new BigDecimal(montantStr));

        System.out.print("  Nouveau nb personnes [" + r.getNombrePersonnes() + "] : ");
        String nbStr = sc.nextLine().trim();
        if (!nbStr.isEmpty()) r.setNombrePersonnes(Integer.parseInt(nbStr));

        reservationService.modifier(r);
        System.out.println("  Reservation apres modification : ");
        Reservation modifiee = reservationService.afficherParId(id);
        if (modifiee != null) {
            System.out.println("  Statut : " + modifiee.getStatut()
                    + " | Montant : " + modifiee.getMontantTotal()
                    + " | Personnes : " + modifiee.getNombrePersonnes());
        }
    }

    /** DELETE — Supprimer une réservation */
    private static void supprimerReservation() {
        System.out.print("  ID de la reservation a supprimer : ");
        int id = lireInt();
        System.out.print("  Confirmer la suppression ? (oui/non) : ");
        String confirm = sc.nextLine().trim();
        if (confirm.equalsIgnoreCase("oui")) {
            reservationService.supprimer(id);
        } else {
            System.out.println("  Suppression annulee.");
        }
    }

    // =========================================================
    // OPÉRATIONS AVIS
    // =========================================================

    /** READ — Afficher tous les avis */
    private static void afficherAvis() {
        List<Avis> liste = avisService.afficherTous();
        System.out.println();
        ligne();
        System.out.printf("  %-4s %-15s %-5s %-40s %-17s%n",
                "ID", "Reservation ID", "Note", "Commentaire", "Date creation");
        ligne();
        if (liste.isEmpty()) {
            System.out.println("  Aucun avis trouve.");
        } else {
            for (Avis a : liste) {
                String com = a.getCommentaire();
                if (com.length() > 40) com = com.substring(0, 37) + "...";
                System.out.printf("  %-4d %-15d %-5d %-40s %-17s%n",
                        a.getId(),
                        a.getReservationId(),
                        a.getNote(),
                        com,
                        a.getDateCreation().format(FMT)
                );
            }
        }
        ligne();
        System.out.println("  Total : " + liste.size() + " avis");
    }

    /** READ — Rechercher un avis par ID */
    private static void rechercherAvis() {
        System.out.print("  Entrez l'ID de l'avis : ");
        int id = lireInt();
        Avis a = avisService.afficherParId(id);
        System.out.println();
        if (a == null) {
            erreur("Aucun avis trouve avec id=" + id);
        } else {
            ligne();
            System.out.println("  Avis trouve :");
            System.out.println("  ID             : " + a.getId());
            System.out.println("  Reservation ID : " + a.getReservationId());
            System.out.println("  Note           : " + a.getNote() + "/5");
            System.out.println("  Date creation  : " + a.getDateCreation().format(FMT));
            System.out.println("  Commentaire    : " + a.getCommentaire());
            ligne();
        }
    }

    /** CREATE — Ajouter un avis */
    private static void ajouterAvis() {
        System.out.println();
        ligne();
        System.out.println("  AJOUT D'UN AVIS");
        System.out.println("  Rappel : une reservation ne peut avoir qu'un seul avis (UNIQUE)");
        ligne();

        // Afficher les réservations disponibles pour aider l'utilisateur
        List<Reservation> reservations = reservationService.afficherTous();
        List<Avis> avisExistants = avisService.afficherTous();
        java.util.Set<Integer> dejaCouvertes = new java.util.HashSet<>();
        for (Avis av : avisExistants) dejaCouvertes.add(av.getReservationId());

        System.out.println("  Reservations sans avis (disponibles) :");
        boolean auMoinsUne = false;
        for (Reservation r : reservations) {
            if (!dejaCouvertes.contains(r.getId())) {
                System.out.println("    -> ID=" + r.getId()
                        + " | Foyer=" + r.getFoyerId()
                        + " | " + r.getDateDebut().format(FMT)
                        + " au " + r.getDateFin().format(FMT));
                auMoinsUne = true;
            }
        }
        if (!auMoinsUne) {
            erreur("Toutes les reservations ont deja un avis.");
            return;
        }

        System.out.print("  Reservation ID : ");
        int reservationId = lireInt();

        System.out.print("  Note (1 a 5)   : ");
        int note = lireInt();

        System.out.print("  Commentaire    : ");
        String commentaire = sc.nextLine().trim();

        Avis avis = new Avis(reservationId, note, commentaire, LocalDateTime.now());
        try {
            avisService.ajouter(avis);
        } catch (java.sql.SQLException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("Duplicate")) {
                erreur("Cette reservation a deja un avis. Choisissez un autre ID.");
            } else {
                erreur("Erreur SQL : " + msg);
            }
        }
    }

    /** UPDATE — Modifier un avis */
    private static void modifierAvis() {
        System.out.print("  ID de l'avis a modifier : ");
        int id = lireInt();
        Avis a = avisService.afficherParId(id);
        if (a == null) {
            erreur("Avis introuvable.");
            return;
        }
        System.out.println("  Valeurs actuelles : note=" + a.getNote()
                + " | commentaire=" + a.getCommentaire());
        System.out.println("  Entrez les nouvelles valeurs (Entree = garder l'actuel) :");

        System.out.print("  Nouvelle note [" + a.getNote() + "] : ");
        String noteStr = sc.nextLine().trim();
        if (!noteStr.isEmpty()) a.setNote(Integer.parseInt(noteStr));

        System.out.print("  Nouveau commentaire [" + a.getCommentaire() + "] : ");
        String com = sc.nextLine().trim();
        if (!com.isEmpty()) a.setCommentaire(com);

        avisService.modifier(a);
        System.out.println("  Avis apres modification : "
                + avisService.afficherParId(id));
    }

    /** DELETE — Supprimer un avis */
    private static void supprimerAvis() {
        System.out.print("  ID de l'avis a supprimer : ");
        int id = lireInt();
        System.out.print("  Confirmer la suppression ? (oui/non) : ");
        String confirm = sc.nextLine().trim();
        if (confirm.equalsIgnoreCase("oui")) {
            avisService.supprimer(id);
        } else {
            System.out.println("  Suppression annulee.");
        }
    }

    // =========================================================
    // UTILITAIRES
    // =========================================================

    private static void afficherBienvenue() {
        System.out.println();
        ligne();
        System.out.println("      RENTALL - Application de location de maisons");
        System.out.println("      Base de donnees : smart_rental_platform");
        System.out.println("      Modules         : Reservation & Avis");
        ligne();
    }

    private static void afficherMenuPrincipal() {
        System.out.println();
        ligne();
        System.out.println("              MENU PRINCIPAL");
        ligne();
        System.out.println("  1. Gestion des Reservations");
        System.out.println("  2. Gestion des Avis");
        System.out.println("  0. Quitter");
        ligne();
        System.out.print("  Votre choix : ");
    }

    private static void ligne() {
        System.out.println("  ================================================");
    }

    private static void erreur(String msg) {
        System.out.println("  [ERREUR] " + msg);
    }

    /**
     * Lit un entier depuis la console.
     * Gère le cas où l'utilisateur tape une lettre.
     */
    private static int lireInt() {
        try {
            String ligne = sc.nextLine().trim();
            return Integer.parseInt(ligne);
        } catch (NumberFormatException e) {
            erreur("Valeur invalide, entrez un nombre entier.");
            return -1;
        }
    }

    /**
     * Lit un LocalDateTime au format ISO : 2026-09-01T14:00
     */
    private static LocalDateTime lireDateTime() {
        try {
            String saisie = sc.nextLine().trim();
            return LocalDateTime.parse(saisie);
        } catch (DateTimeParseException e) {
            erreur("Format de date invalide. Utilisez : 2026-09-01T14:00");
            return null;
        }
    }

    /**
     * Lit un BigDecimal (montant).
     */
    private static BigDecimal lireBigDecimal() {
        try {
            return new BigDecimal(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            erreur("Montant invalide. Utilisez le format : 1200.00");
            return null;
        }
    }
}
