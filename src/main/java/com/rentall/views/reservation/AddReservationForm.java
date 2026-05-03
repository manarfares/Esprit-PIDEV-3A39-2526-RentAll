package com.rentall.views.reservation;

import com.rentall.entities.Reservation;
import com.rentall.services.ReservationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Formulaire d'ajout d'une réservation.
 * S'ouvre comme une fenêtre modale (JDialog) par-dessus la vue principale.
 */
public class AddReservationForm extends JDialog {

    private final ReservationService service;
    private boolean success = false;

    private final JTextField tfFoyerId     = new JTextField("36");
    private final JTextField tfLocataireId = new JTextField("32");
    private final JTextField tfDateDebut   = new JTextField("2026-09-01T14:00");
    private final JTextField tfDateFin     = new JTextField("2026-09-10T11:00");
    private final JTextField tfMontant     = new JTextField("1200.00");
    private final JTextField tfStatut      = new JTextField("en_attente");
    private final JTextField tfNbPersonnes = new JTextField("2");

    public AddReservationForm(Frame parent, ReservationService service) {
        super(parent, "Ajouter une Reservation", true);
        this.service = service;
        buildUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Titre
        JLabel titre = new JLabel("Nouvelle Reservation");
        titre.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titre.setForeground(new Color(44, 62, 80));
        content.add(titre, BorderLayout.NORTH);

        // Formulaire
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.add(new JLabel("Foyer ID (36, 37, 38, 39, 40) :")); form.add(tfFoyerId);
        form.add(new JLabel("Locataire ID (32, 33, 35) :"));      form.add(tfLocataireId);
        form.add(new JLabel("Date debut (yyyy-MM-ddTHH:mm) :"));  form.add(tfDateDebut);
        form.add(new JLabel("Date fin   (yyyy-MM-ddTHH:mm) :"));  form.add(tfDateFin);
        form.add(new JLabel("Montant total (EUR) :"));            form.add(tfMontant);
        form.add(new JLabel("Statut :"));                         form.add(tfStatut);
        form.add(new JLabel("Nombre de personnes :"));            form.add(tfNbPersonnes);
        content.add(form, BorderLayout.CENTER);

        // Boutons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnSave   = ReservationPalette.createSaveButton("Enregistrer");
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
            Reservation r = new Reservation(
                Integer.parseInt(tfFoyerId.getText().trim()),
                Integer.parseInt(tfLocataireId.getText().trim()),
                LocalDateTime.parse(tfDateDebut.getText().trim()),
                LocalDateTime.parse(tfDateFin.getText().trim()),
                new BigDecimal(tfMontant.getText().trim()),
                tfStatut.getText().trim(),
                LocalDateTime.now(),
                Integer.parseInt(tfNbPersonnes.getText().trim())
            );
            service.ajouter(r);
            success = true;
            JOptionPane.showMessageDialog(this, "Reservation ajoutee avec succes !", "Succes", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.contains("foreign key")) {
                msg = "Foyer ID ou Locataire ID invalide.\nUtilisez : Foyer 36-40, Locataire 32/33/35";
            }
            JOptionPane.showMessageDialog(this, "Erreur : " + msg, "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Retourne true si l'ajout a réussi (pour rafraîchir la table) */
    public boolean isSuccess() { return success; }
}
