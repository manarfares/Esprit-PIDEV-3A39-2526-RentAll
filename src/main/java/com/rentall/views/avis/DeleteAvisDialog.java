package com.rentall.views.avis;

import com.rentall.entities.Avis;
import com.rentall.services.AvisService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Dialog de confirmation de suppression d'un avis.
 */
public class DeleteAvisDialog extends JDialog {

    private final AvisService service;
    private final Avis avis;
    private boolean success = false;

    public DeleteAvisDialog(Frame parent, AvisService service, Avis avis) {
        super(parent, "Supprimer l'Avis #" + avis.getId(), true);
        this.service = service;
        this.avis    = avis;
        buildUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout(10, 15));
        content.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel titre = new JLabel("Confirmer la suppression");
        titre.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titre.setForeground(new Color(231, 76, 60));
        content.add(titre, BorderLayout.NORTH);

        JPanel details = new JPanel(new GridLayout(0, 2, 8, 6));
        details.setBorder(BorderFactory.createTitledBorder("Avis a supprimer"));
        details.add(new JLabel("ID :"));             details.add(new JLabel(String.valueOf(avis.getId())));
        details.add(new JLabel("Reservation ID :")); details.add(new JLabel(String.valueOf(avis.getReservationId())));
        details.add(new JLabel("Note :"));           details.add(new JLabel(String.valueOf(avis.getNote()) + " / 5"));
        details.add(new JLabel("Date creation :"));  details.add(new JLabel(avis.getDateCreation().toLocalDate().toString()));
        details.add(new JLabel("Commentaire :"));    details.add(new JLabel("<html><i>" + avis.getCommentaire() + "</i></html>"));
        content.add(details, BorderLayout.CENTER);

        JLabel warning = new JLabel("Cette action est irreversible.");
        warning.setForeground(new Color(231, 76, 60));
        warning.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(warning, BorderLayout.WEST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnCancel  = AvisPalette.createCancelButton("Annuler");
        JButton btnConfirm = AvisPalette.createDeleteButton("Supprimer");
        buttons.add(btnCancel);
        buttons.add(btnConfirm);
        bottom.add(buttons, BorderLayout.EAST);
        content.add(bottom, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> dispose());
        btnConfirm.addActionListener(e -> confirm());

        setContentPane(content);
    }

    private void confirm() {
        service.supprimer(avis.getId());
        success = true;
        JOptionPane.showMessageDialog(this, "Avis supprime.", "Succes", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    public boolean isSuccess() { return success; }
}
