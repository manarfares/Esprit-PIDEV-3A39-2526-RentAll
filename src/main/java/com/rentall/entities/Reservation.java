package com.rentall.entities;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * Classe Reservation (Entité)
 *
 * RÔLE : Représenter en Java une ligne de la table "reservation"
 *        de la base smart_rental_platform.
 *
 * CORRESPONDANCE EXACTE avec la table SQL :
 * ┌──────────────────┬──────────────┬─────────────────────────────────────────┐
 * │ Colonne SQL      │ Type SQL     │ Attribut Java                           │
 * ├──────────────────┼──────────────┼─────────────────────────────────────────┤
 * │ id               │ int(11)      │ int id                                  │
 * │ foyer_id         │ int(11) FK   │ int foyerId   (référence au foyer/maison)│
 * │ locataire_id     │ int(11) FK   │ int locataireId (référence au locataire) │
 * │ date_debut       │ datetime     │ LocalDateTime dateDebut                 │
 * │ date_fin         │ datetime     │ LocalDateTime dateFin                   │
 * │ montant_total    │ decimal(10,2)│ BigDecimal montantTotal                 │
 * │ statut           │ varchar(50)  │ String statut                           │
 * │ date_creation    │ datetime     │ LocalDateTime dateCreation              │
 * │ nombre_personnes │ int(11)      │ int nombrePersonnes                     │
 * └──────────────────┴──────────────┴─────────────────────────────────────────┘
 *
 * POURQUOI LocalDateTime et pas LocalDate ?
 * → Parce que le type SQL est "datetime" (date + heure), pas juste "date".
 *
 * POURQUOI BigDecimal pour montant_total ?
 * → Parce que decimal(10,2) en SQL = montant avec 2 décimales.
 *   BigDecimal est le type Java précis pour les montants financiers.
 *   (double peut avoir des erreurs d'arrondi, BigDecimal non)
 */
public class Reservation {

    // Clé primaire, générée automatiquement par MySQL (AUTO_INCREMENT)
    private int id;

    // Référence au foyer (maison) concerné par cette réservation
    // Correspond à la colonne "foyer_id" (clé étrangère)
    private int foyerId;

    // Référence au locataire qui a fait la réservation
    // Correspond à la colonne "locataire_id" (clé étrangère)
    private int locataireId;

    // Date et heure de début du séjour
    // Ex: 2025-07-01 14:00:00
    private LocalDateTime dateDebut;

    // Date et heure de fin du séjour
    // Ex: 2025-07-15 11:00:00
    private LocalDateTime dateFin;

    // Montant total de la réservation en euros (ou autre devise)
    // Ex: 1250.00
    private BigDecimal montantTotal;

    // État actuel de la réservation
    // Valeurs possibles (selon ton projet Symfony) : "en_attente", "confirmee", "annulee"
    private String statut;

    // Date et heure à laquelle la réservation a été créée dans le système
    private LocalDateTime dateCreation;

    // Nombre de personnes pour ce séjour
    private int nombrePersonnes;

    // =========================================================
    // CONSTRUCTEUR VIDE
    // Nécessaire pour créer un objet vide et le remplir avec des setters
    // =========================================================
    public Reservation() {}

    // =========================================================
    // CONSTRUCTEUR SANS ID (pour INSERT en base)
    // On ne passe pas l'id car MySQL le génère automatiquement
    // =========================================================
    public Reservation(int foyerId, int locataireId, LocalDateTime dateDebut,
                       LocalDateTime dateFin, BigDecimal montantTotal,
                       String statut, LocalDateTime dateCreation, int nombrePersonnes) {
        this.foyerId = foyerId;
        this.locataireId = locataireId;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.montantTotal = montantTotal;
        this.statut = statut;
        this.dateCreation = dateCreation;
        this.nombrePersonnes = nombrePersonnes;
    }

    // =========================================================
    // CONSTRUCTEUR COMPLET AVEC ID (pour lecture depuis la BDD)
    // =========================================================
    public Reservation(int id, int foyerId, int locataireId, LocalDateTime dateDebut,
                       LocalDateTime dateFin, BigDecimal montantTotal,
                       String statut, LocalDateTime dateCreation, int nombrePersonnes) {
        this.id = id;
        this.foyerId = foyerId;
        this.locataireId = locataireId;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.montantTotal = montantTotal;
        this.statut = statut;
        this.dateCreation = dateCreation;
        this.nombrePersonnes = nombrePersonnes;
    }

    // =========================================================
    // GETTERS ET SETTERS
    // Encapsulation : les attributs sont privés, on y accède via ces méthodes
    // =========================================================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getFoyerId() { return foyerId; }
    public void setFoyerId(int foyerId) { this.foyerId = foyerId; }

    public int getLocataireId() { return locataireId; }
    public void setLocataireId(int locataireId) { this.locataireId = locataireId; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public BigDecimal getMontantTotal() { return montantTotal; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public int getNombrePersonnes() { return nombrePersonnes; }
    public void setNombrePersonnes(int nombrePersonnes) { this.nombrePersonnes = nombrePersonnes; }

    // =========================================================
    // toString() : affichage lisible d'un objet Reservation
    // Appelé automatiquement par System.out.println(reservation)
    // =========================================================
    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", foyerId=" + foyerId +
                ", locataireId=" + locataireId +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", montantTotal=" + montantTotal +
                ", statut='" + statut + '\'' +
                ", dateCreation=" + dateCreation +
                ", nombrePersonnes=" + nombrePersonnes +
                '}';
    }
}
