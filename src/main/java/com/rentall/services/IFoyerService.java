package com.rentall.services;

import com.rentall.dto.FoyerListItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Lecture des informations du foyer (maison) lie a une reservation.
 * Aucune modification de la base : SELECT uniquement.
 */
public interface IFoyerService {

    /**
     * {@code prix_par_nuit} du foyer (table {@code foyer}, cle {@code id}).
     *
     * @return vide si aucune ligne pour cet id, ou si {@code prix_par_nuit} est NULL
     */
    Optional<BigDecimal> getPrixParNuitParFoyerId(int foyerId);

    /**
     * Liste des logements pour listes déroulantes (libellés construits en lecture seule).
     */
    List<FoyerListItem> listerFoyersPourSelection();
}
