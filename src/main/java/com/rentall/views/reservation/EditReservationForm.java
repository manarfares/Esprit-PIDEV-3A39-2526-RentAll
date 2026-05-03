package com.rentall.views.reservation;

import com.rentall.entities.Reservation;
import com.rentall.services.ReservationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.Locale;

/**
 * Formulaire de modification d'une réservation existante.
 * Reçoit l'objet Reservation à modifier et pré-remplit les champs.
 */
public class EditReservationForm extends JDialog {

    private static final String[] STATUTS_AUTORISES = { "en_attente", "confirmee", "terminee" };

    private final ReservationService service;
    private final Reservation reservation;
    private boolean success = false;

    private final JComboBox<String> cbStatut = new JComboBox<>(STATUTS_AUTORISES);
    private final JTextField tfMontant     = new JTextField();
    private final JTextField tfNbPersonnes = new JTextField();

    public EditReservationForm(Frame parent, ReservationService service, Reservation reservation) {
        super(parent, "Modifier la Reservation #" + reservation.getId(), true);
        this.service     = service;
        this.reservation = reservation;
        prefill();
        buildUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void prefill() {
        preselectStatut(cbStatut, reservation.getStatut());
        tfMontant.setText(reservation.getMontantTotal().toString());
        tfNbPersonnes.setText(String.valueOf(reservation.getNombrePersonnes()));
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Titre avec info de la réservation
        JLabel titre = new JLabel("Modifier Reservation #" + reservation.getId());
        titre.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titre.setForeground(new Color(44, 62, 80));

        JLabel info = new JLabel("Foyer " + reservation.getFoyerId() +
                " | Locataire " + reservation.getLocataireId() +
                " | " + reservation.getDateDebut().toLocalDate() +
                " → " + reservation.getDateFin().toLocalDate());
        info.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        info.setForeground(new Color(127, 140, 141));

        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        headerPanel.add(titre);
        headerPanel.add(info);
        content.add(headerPanel, BorderLayout.NORTH);

        // Formulaire (seulement les champs modifiables)
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.add(new JLabel("Statut :"));
        styleCombo(cbStatut);
        form.add(cbStatut);
        form.add(new JLabel("Montant (EUR) :"));    form.add(tfMontant);
        form.add(new JLabel("Nb personnes :"));     form.add(tfNbPersonnes);
        content.add(form, BorderLayout.CENTER);

        // Boutons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnSave   = ReservationPalette.createEditButton("Enregistrer");
        JButton btnCancel = ReservationPalette.createCancelButton("Annuler");
        buttons.add(btnCancel);
        buttons.add(btnSave);
        content.add(buttons, BorderLayout.SOUTH);

        btnSave.addActionListener(e -> save());
        btnCancel.addActionListener(e -> dispose());

        setContentPane(content);
    }

    private void save() {
        try {
            reservation.setStatut((String) cbStatut.getSelectedItem());
            reservation.setMontantTotal(new BigDecimal(tfMontant.getText().trim()));
            reservation.setNombrePersonnes(Integer.parseInt(tfNbPersonnes.getText().trim()));
            service.modifier(reservation);
            success = true;
            JOptionPane.showMessageDialog(this, "Reservation modifiee avec succes !", "Succes", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSuccess() { return success; }

    private static void preselectStatut(JComboBox<String> cb, String fromDb) {
        if (fromDb == null || fromDb.isBlank()) {
            cb.setSelectedIndex(0);
            return;
        }
        String n = fromDb.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        for (String s : STATUTS_AUTORISES) {
            if (s.equals(n)) {
                cb.setSelectedItem(s);
                return;
            }
        }
        cb.setSelectedIndex(0);
    }

    private void styleCombo(JComboBox<String> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                new EmptyBorder(5, 8, 5, 8)));
    }
}
