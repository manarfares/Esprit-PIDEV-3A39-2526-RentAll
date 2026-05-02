package com.rentall.services;

import com.rentall.dto.LocataireListItem;

import java.util.List;

/**
 * Lecture des locataires pour l’interface (SELECT uniquement).
 */
public interface ILocataireService {

    /**
     * Liste des locataires pour listes déroulantes (libellés construits en lecture seule).
     */
    List<LocataireListItem> listerLocatairesPourSelection();
}
