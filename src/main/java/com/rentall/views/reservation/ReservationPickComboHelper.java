package com.rentall.views.reservation;

import com.rentall.dto.ReservationTableRow;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * Liste déroulante de réservations : libellés lisibles (sans saisie d’ID).
 */
public final class ReservationPickComboHelper {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ReservationPickComboHelper() {
    }

    public static void prepareCombo(JComboBox<ReservationTableRow> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setMaximumRowCount(14);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                            boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(new EmptyBorder(8, 10, 8, 10));
                if (value instanceof ReservationTableRow row) {
                    setText(row.getFoyerLibelle() + " · " + row.getLocataireLibelle()
                            + " · " + row.getDateDebut().format(FMT) + " → " + row.getDateFin().format(FMT)
                            + " · " + ReservationUiFormat.statutAffichage(row.getStatut()));
                } else {
                    setText(" ");
                }
                return this;
            }
        });
    }
}
