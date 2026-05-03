package com.rentall.entities;

import java.time.LocalDateTime;

/**
 * Classe Avis (Entité)
 *
 * RÔLE : Représenter en Java une ligne de la table "avis"
 *        de la base smart_rental_platform.
 *
 * CORRESPONDANCE EXACTE avec la table SQL :
 * ┌──────────────────┬──────────────┬──────────────────────────────────────────────┐
 * │ Colonne SQL      │ Type SQL     │ Attribut Java                                │
 * ├──────────────────┼──────────────┼──────────────────────────────────────────────┤
 * │ id               │ int(11)      │ int id                                       │
 * │ reservation_id   │ int(11) FK   │ int reservationId (lien vers la réservation) │
 * │ note             │ int(11)      │ int note                                     │
 * │ commentaire      │ longtext     │ String commentaire                           │
 * │ date_creation    │ datetime     │ LocalDateTime dateCreation                   │
 * └──────────────────┴──────────────┴──────────────────────────────────────────────┘
 *
 * REMARQUE IMPORTANTE :
 * La colonne "reservation_id" a un index UNIQUE dans ta base.
 * Cela signifie qu'une réservation ne peut avoir qu'UN SEUL avis.
 * C'est une relation One-to-One entre avis et reservation.
 *
 * POURQUOI longtext → String ?
 * → En Java, String peut contenir n'importe quelle longueur de texte.
 *   C'est le type correct pour mapper un longtext SQL.
 */
public class Avis {

    // Clé primaire, générée automatiquement par MySQL
    private int id;

    // Référence à la réservation concernée (clé étrangère UNIQUE)
    // Un avis appartient toujours à une réservation précise
    private int reservationId;

    // Note attribuée par le locataire (ex: de 1 à 5)
    private int note;

    // Texte libre du commentaire laissé par le locataire
    // Ex: "Très belle maison, propre et bien équipée"
    private String commentaire;

    // Date et heure à laquelle l'avis a été créé
    private LocalDateTime dateCreation;

    // =========================================================
    // CONSTRUCTEUR VIDE
    // =========================================================
    public Avis() {}

    // =========================================================
    // CONSTRUCTEUR SANS ID (pour INSERT en base)
    // =========================================================
    public Avis(int reservationId, int note, String commentaire, LocalDateTime dateCreation) {
        this.reservationId = reservationId;
        this.note = note;
        this.commentaire = commentaire;
        this.dateCreation = dateCreation;
    }

    // =========================================================
    // CONSTRUCTEUR COMPLET AVEC ID (pour lecture depuis la BDD)
    // =========================================================
    public Avis(int id, int reservationId, int note, String commentaire, LocalDateTime dateCreation) {
        this.id = id;
        this.reservationId = reservationId;
        this.note = note;
        this.commentaire = commentaire;
        this.dateCreation = dateCreation;
    }

    // =========================================================
    // GETTERS ET SETTERS
    // =========================================================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getReservationId() { return reservationId; }
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }

    public int getNote() { return note; }
    public void setNote(int note) { this.note = note; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    // =========================================================
    // toString()
    // =========================================================
    @Override
    public String toString() {
        return "Avis{" +
                "id=" + id +
                ", reservationId=" + reservationId +
                ", note=" + note +
                ", commentaire='" + commentaire + '\'' +
                ", dateCreation=" + dateCreation +
                '}';
    }
}
