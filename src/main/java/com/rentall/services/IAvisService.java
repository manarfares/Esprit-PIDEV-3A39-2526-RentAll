package com.rentall.services;

import com.rentall.entities.Avis;
import java.util.List;

/**
 * Interface IAvisService
 *
 * RÔLE : Définir le contrat CRUD pour la gestion des avis.
 */
public interface IAvisService {

    void ajouter(Avis avis) throws java.sql.SQLException;
    void modifier(Avis avis);
    void supprimer(int id);
    List<Avis> afficherTous();
    Avis afficherParId(int id);
}
