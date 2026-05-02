package com.rentall.dto;

import java.util.Objects;

/**
 * Entrée de liste pour choisir un logement (lecture seule, aucun DDL).
 */
public final class FoyerListItem {

    private final int id;
    private final String libelle;

    public FoyerListItem(int id, String libelle) {
        this.id = id;
        this.libelle = libelle != null ? libelle : "";
    }

    public int getId() {
        return id;
    }

    public String getLibelle() {
        return libelle;
    }

    @Override
    public String toString() {
        return libelle.isEmpty() ? "Réf. logement " + id : libelle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FoyerListItem that = (FoyerListItem) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
