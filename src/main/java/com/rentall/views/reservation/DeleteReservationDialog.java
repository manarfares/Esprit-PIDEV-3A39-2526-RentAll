package com.rentall.views.reservation;

import com.rentall.entities.Reservation;
import com.rentall.services.ReservationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Dialog de confirmation de suppression d'une réservation.
 * Affiche les détails de la réservation avant de confirmer.
 */
public class DeleteReservationDialog extends JDialog {

    private final ReservationService service;
    private final Reservation reservation;
    private boolean success = false;

    public DeleteReservationDialog(Frame parent, ReservationService service, Reservation reservation) {
        super(parent, "Supprimer la Reservation #" + reservation.getId(), true);
        this.service     = service;
        this.reservation = reservation;
        buildUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout(10, 15));
        content.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Icone + titre
        JLabel titre = new JLabel("Confirmer la suppression");
        titre.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titre.setForeground(new Color(231, 76, 60));
        content.add(titre, BorderLayout.NORTH);

        // Détails de la réservation
        JPanel details = new JPanel(new GridLayout(0, 2, 8, 6));
        details.setBorder(BorderFactory.createTitledBorder("Reservation a supprimer"));
        details.add(new JLabel("ID :"));           details.add(new JLabel(String.valueOf(reservation.getId())));
        details.add(new JLabel("Foyer ID :"));     details.add(new JLabel(String.valueOf(reservation.getFoyerId())));
        details.add(new JLabel("Locataire ID :")); details.add(new JLabel(String.valueOf(reservation.getLocataireId())));
        details.add(new JLabel("Date debut :"));   details.add(new JLabel(reservation.getDateDebut().toLocalDate().toString()));
        details.add(new JLabel("Date fin :"));     details.add(new JLabel(reservation.getDateFin().toLocalDate().toString()));
        details.add(new JLabel("Montant :"));      details.add(new JLabel(reservation.getMontantTotal() + " EUR"));
        details.add(new JLabel("Statut :"));       details.add(new JLabel(reservation.getStatut()));
        content.add(details, BorderLayout.CENTER);

        // Avertissement
        JLabel warning = new JLabel("Cette action est irreversible.");
        warning.setForeground(new Color(231, 76, 60));
        warning.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        // Boutons
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(warning, BorderLayout.WEST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnCancel  = ReservationPalette.createCancelButton("Annuler");
        JButton btnConfirm = ReservationPalette.createDeleteButton("Supprimer");
        buttons.add(btnCancel);
        buttons.add(btnConfirm);
        bottom.add(buttons, BorderLayout.EAST);
        content.add(bottom, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> dispose());
        btnConfirm.addActionListener(e -> confirm());

        setContentPane(content);
    }

    private void confirm() {
        service.supprimer(reservation.getId());
        success = true;
        JOptionPane.showMessageDialog(this, "Reservation supprimee.", "Succes", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    public boolean isSuccess() { return success; }
}
