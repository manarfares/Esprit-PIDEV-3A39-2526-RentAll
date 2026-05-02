package com.rentall.dto;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Ligne affichée dans le dashboard des avis.
 * Contient les informations d'un avis avec le logement et le locataire associés.
 */
public final class AvisDashboardRow {

    private final int avisId;
    private final int reservationId;
    private final String logementLibelle;
    private final String locataireLibelle;
    private final int note;
    private final String commentaire;
    private final LocalDateTime dateCreation;

    public AvisDashboardRow(int avisId,
                            int reservationId,
                            String logementLibelle,
                            String locataireLibelle,
                            int note,
                            String commentaire,
                            LocalDateTime dateCreation) {
        this.avisId = avisId;
        this.reservationId = reservationId;
        this.logementLibelle = logementLibelle != null ? logementLibelle : "Logement #" + reservationId;
        this.locataireLibelle = locataireLibelle != null ? locataireLibelle : "Locataire inconnu";
        this.note = note;
        this.commentaire = Objects.requireNonNull(commentaire);
        this.dateCreation = dateCreation;
    }

    public int getAvisId() {
        return avisId;
    }

    public int getReservationId() {
        return reservationId;
    }

    public String getLogementLibelle() {
        return logementLibelle;
    }

    public String getLocataireLibelle() {
        return locataireLibelle;
    }

    public int getNote() {
        return note;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }
}
