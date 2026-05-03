package com.rentall.services;

import com.rentall.dto.FoyerListItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Lecture des informations du logement lie a une reservation.
 * Aucune modification de la base : SELECT uniquement.
 */
public interface IFoyerService {

    /**
     * Prix par nuit du logement, lu depuis la table detectee en base.
     *
     * @return vide si aucune ligne pour cet id, ou si le prix est NULL
     */
    Optional<BigDecimal> getPrixParNuitParFoyerId(int foyerId);

    /**
     * Liste des logements pour listes deroulantes, avec libelle et prix par nuit.
     */
    List<FoyerListItem> listerFoyersPourSelection();
}
