package com.rentall.views.reservation;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Libellés français pour l’interface réservation (données brutes inchangées en base).
 */
public final class ReservationUiFormat {

    private static final NumberFormat EUR = NumberFormat.getCurrencyInstance(Locale.FRANCE);

    private ReservationUiFormat() {
    }

    /**
     * Affichage du statut stocké en base (snake_case) vers une courte étiquette française.
     */
    public static String statutAffichage(String code) {
        if (code == null || code.isBlank()) {
            return "—";
        }
        String k = code.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if ("terminer".equals(k)) {
            k = "terminee";
        }
        return switch (k) {
            case "en_attente" -> "En attente";
            case "confirmee" -> "Confirmée";
            case "terminee" -> "Terminée";
            case "annulee", "annulée" -> "Annulée";
            default -> code.trim();
        };
    }

    public static String montantAffichage(BigDecimal montant) {
        if (montant == null) {
            return "—";
        }
        synchronized (EUR) {
            return EUR.format(montant);
        }
    }
}
