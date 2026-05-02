package com.rentall.services;

import com.rentall.dto.ReservationTableRow;
import com.rentall.entities.Reservation;

import java.util.List;

/**
 * Interface IReservationService
 *
 * RÔLE : Définir le contrat (les méthodes obligatoires) pour tout service de réservation.
 *
 * POURQUOI UNE INTERFACE ?
 * - Une interface dit "quoi faire" sans dire "comment".
 * - La classe ReservationService dira "comment" le faire concrètement.
 * - C'est une bonne pratique professionnelle en Java (programmation par abstraction).
 *
 * LES 4 OPÉRATIONS CRUD :
 * C → Create  → ajouter()
 * R → Read    → afficherTous() + afficherParId()
 * U → Update  → modifier()
 * D → Delete  → supprimer()
 */
public interface IReservationService {

    void ajouter(Reservation reservation);
    void modifier(Reservation reservation);
    void supprimer(int id);
    List<Reservation> afficherTous();

    /**
     * Lignes pour le tableau Swing : libellés logement / locataire (requête en lecture seule, aucun DDL).
     */
    List<ReservationTableRow> listerPourAffichageTableau();

    Reservation afficherParId(int id);
}
