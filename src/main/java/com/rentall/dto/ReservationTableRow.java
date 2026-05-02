package com.rentall.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Ligne affichée pour une réservation : identifiant interne pour les actions CRUD,
 * libellés lisibles pour l’interface (aucun affichage obligatoire de l’ID dans les tableaux).
 */
public final class ReservationTableRow {

    private final int id;
    private final LocalDateTime dateDebut;
    private final LocalDateTime dateFin;
    private final BigDecimal montantTotal;
    private final String statut;
    private final int nombrePersonnes;
    private final String foyerLibelle;
    private final String locataireLibelle;

    public ReservationTableRow(int id,
                               LocalDateTime dateDebut,
                               LocalDateTime dateFin,
                               BigDecimal montantTotal,
                               String statut,
                               int nombrePersonnes,
                               String foyerLibelle,
                               String locataireLibelle) {
        this.id = id;
        this.dateDebut = Objects.requireNonNull(dateDebut);
        this.dateFin = Objects.requireNonNull(dateFin);
        this.montantTotal = montantTotal;
        this.statut = statut;
        this.nombrePersonnes = nombrePersonnes;
        this.foyerLibelle = foyerLibelle != null ? foyerLibelle : "";
        this.locataireLibelle = locataireLibelle != null ? locataireLibelle : "";
    }

    public int getId() {
        return id;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public BigDecimal getMontantTotal() {
        return montantTotal;
    }

    public String getStatut() {
        return statut;
    }

    public int getNombrePersonnes() {
        return nombrePersonnes;
    }

    public String getFoyerLibelle() {
        return foyerLibelle;
    }

    public String getLocataireLibelle() {
        return locataireLibelle;
    }
}
