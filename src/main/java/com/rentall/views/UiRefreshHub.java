package com.rentall.views;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Notifie les fenetres liste ouvertes apres un CRUD, pour eviter de dupliquer les donnees
 * sans modifier la base. Les listes s'enregistrent a l'ouverture et se desenregistrent a la fermeture.
 */
public final class UiRefreshHub {

    private static final List<Runnable> reservationListeners = new CopyOnWriteArrayList<>();
    private static final List<Runnable> avisListeners = new CopyOnWriteArrayList<>();

    private UiRefreshHub() {}

    public static void registerReservationRefresh(Runnable r) {
        if (r != null) reservationListeners.add(r);
    }

    public static void unregisterReservationRefresh(Runnable r) {
        reservationListeners.remove(r);
    }

    public static void notifyReservationChanged() {
        for (Runnable r : reservationListeners) {
            try {
                r.run();
            } catch (Exception ignored) {
                // evite qu'une liste fermee casse les autres
            }
        }
    }

    public static void registerAvisRefresh(Runnable r) {
        if (r != null) avisListeners.add(r);
    }

    public static void unregisterAvisRefresh(Runnable r) {
        avisListeners.remove(r);
    }

    public static void notifyAvisChanged() {
        for (Runnable r : avisListeners) {
            try {
                r.run();
            } catch (Exception ignored) {
            }
        }
    }
}
