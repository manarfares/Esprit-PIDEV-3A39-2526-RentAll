package com.rentall.views.avis;

import com.rentall.entities.Avis;
import com.rentall.services.AvisService;
import com.rentall.services.NotificationService;
import com.rentall.views.UiRefreshHub;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Supprimer un avis : sélection via JComboBox (plus de saisie d'ID brute).
 * Confirmation obligatoire avant suppression.
 */
public class DeleteAvisFrame extends JFrame {

    private static final String CARD_PICK   = "pick";
    private static final String CARD_DETAIL = "detail";

    private final AvisService service = new AvisService();
    private Avis avis;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel deck = new JPanel(cardLayout);

    private final JComboBox<AvisItem> cbPick = new JComboBox<>();
    private final JLabel lblPickErr = new JLabel(" ");
    private JButton btnContinuer;

    private final JPanel detailGrid = new JPanel(new GridLayout(0, 2, 10, 10));

    public DeleteAvisFrame(JFrame parent) {
        setTitle("Rentall - Supprimer un Avis");
        setSize(540, 460);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        deck.add(buildPickPanel(), CARD_PICK);
        deck.add(buildDetailPanel(), CARD_DETAIL);
        setContentPane(deck);

        refillPickCombo();
        cardLayout.show(deck, CARD_PICK);
    }

    private void refillPickCombo() {
        cbPick.removeAllItems();
        for (Avis a : service.afficherTous()) {
            cbPick.addItem(new AvisItem(a));
        }
        if (btnContinuer != null) {
            btnContinuer.setEnabled(cbPick.getItemCount() > 0);
        }
        if (cbPick.getItemCount() > 0) {
            cbPick.setSelectedIndex(0);
        }
        lblPickErr.setText(" ");
    }

    private JPanel buildPickPanel() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(231, 76, 60));
        header.setBorder(new EmptyBorder(16, 20, 16, 20));
        JLabel title = new JLabel("Supprimer un avis");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Choisissez l'avis à supprimer dans la liste");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(254, 226, 226));
        JPanel ht = new JPanel(new GridLayout(2, 1, 4, 0));
        ht.setOpaque(false);
        ht.add(title);
        ht.add(sub);
        header.add(ht, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(28, 40, 20, 40));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(8, 6, 8, 6);
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2; gc.weightx = 1;
        JLabel lab = new JLabel("Avis à supprimer");
        lab.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lab.setForeground(new Color(51, 65, 85));
        body.add(lab, gc);
        gc.gridy = 1;
        cbPick.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbPick.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        body.add(cbPick, gc);
        lblPickErr.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblPickErr.setForeground(new Color(231, 76, 60));
        gc.gridy = 2;
        body.add(lblPickErr, gc);
        root.add(body, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        btnPanel.setBackground(new Color(248, 249, 252));
        JButton btnRetour = makeBtn("Retour", new Color(127, 140, 141));
        JButton btnCancel = makeBtn("Annuler", new Color(149, 165, 166));
        btnContinuer = makeBtn("Continuer", new Color(231, 76, 60));
        btnContinuer.setEnabled(false);
        btnPanel.add(btnRetour);
        btnPanel.add(btnCancel);
        btnPanel.add(btnContinuer);
        root.add(btnPanel, BorderLayout.SOUTH);

        btnContinuer.addActionListener(e -> loadSelected());
        btnCancel.addActionListener(e -> dispose());
        btnRetour.addActionListener(e -> dispose());

        return root;
    }

    private JPanel buildDetailPanel() {
        JPanel root = new JPanel(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(231, 76, 60));
        header.setBorder(new EmptyBorder(16, 20, 16, 20));
        JLabel title = new JLabel("Confirmer la suppression");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        detailGrid.setBackground(Color.WHITE);
        detailGrid.setBorder(new EmptyBorder(20, 40, 10, 40));
        root.add(detailGrid, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(new Color(248, 249, 252));
        bottom.setBorder(new EmptyBorder(10, 20, 10, 20));
        JLabel warn = new JLabel("Cette action est irréversible.");
        warn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        warn.setForeground(new Color(231, 76, 60));
        bottom.add(warn, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        JButton btnBackPick = makeBtn("Changer d'avis", new Color(127, 140, 141));
        JButton btnCancel   = makeBtn("Annuler", new Color(149, 165, 166));
        JButton btnConfirm  = makeBtn("Supprimer définitivement", new Color(231, 76, 60));
        btnPanel.add(btnBackPick);
        btnPanel.add(btnCancel);
        btnPanel.add(btnConfirm);
        bottom.add(btnPanel, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);

        btnBackPick.addActionListener(e -> {
            lblPickErr.setText(" ");
            avis = null;
            refillPickCombo();
            cardLayout.show(deck, CARD_PICK);
        });
        btnCancel.addActionListener(e -> dispose());
        btnConfirm.addActionListener(e -> {
            if (avis != null) {
                try {
                    service.supprimer(avis.getId());
                    NotificationService.showSuccess("Succès", "Avis supprimé avec succès");
                    UiRefreshHub.notifyAvisChanged();
                    dispose();
                } catch (Exception ex) {
                    NotificationService.showError("Erreur", ex.getMessage());
                }
            }
        });

        return root;
    }

    private void loadSelected() {
        lblPickErr.setText(" ");
        AvisItem item = (AvisItem) cbPick.getSelectedItem();
        if (item == null) {
            lblPickErr.setText("Sélectionnez un avis dans la liste.");
            return;
        }
        Avis a = service.afficherParId(item.getId());
        if (a == null) {
            lblPickErr.setText("Cet avis n'existe plus. La liste a été actualisée.");
            refillPickCombo();
            return;
        }
        avis = a;
        detailGrid.removeAll();
        addDetail("ID :", String.valueOf(a.getId()));
        addDetail("Réservation #:", String.valueOf(a.getReservationId()));
        addDetail("Note :", a.getNote() + " / 5");
        addDetail("Date création :", a.getDateCreation().toLocalDate().toString());
        JLabel cLab = new JLabel("Commentaire :");
        cLab.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cLab.setForeground(new Color(80, 80, 80));
        JLabel cVal = new JLabel("<html><body style='width:280px'>" + escapeHtml(a.getCommentaire()) + "</body></html>");
        cVal.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        detailGrid.add(cLab);
        detailGrid.add(cVal);
        detailGrid.revalidate();
        detailGrid.repaint();
        cardLayout.show(deck, CARD_DETAIL);
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void addDetail(String label, String value) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(new Color(80, 80, 80));
        JLabel val = new JLabel(value != null ? value : "—");
        val.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        detailGrid.add(lbl);
        detailGrid.add(val);
    }

    private JButton makeBtn(String t, Color c) {
        JButton b = new JButton(t);
        b.setBackground(c);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setBorder(new EmptyBorder(9, 22, 9, 22));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static class AvisItem {
        private final Avis avis;

        AvisItem(Avis a) {
            this.avis = a;
        }

        int getId() {
            return avis.getId();
        }

        @Override
        public String toString() {
            return "Avis #" + avis.getId()
                    + "  |  Réservation #" + avis.getReservationId()
                    + "  |  Note : " + avis.getNote() + "/5"
                    + "  |  " + avis.getDateCreation().toLocalDate();
        }
    }
}
