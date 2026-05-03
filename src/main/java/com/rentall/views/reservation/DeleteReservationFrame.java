package com.rentall.views.reservation;

import com.rentall.dto.ReservationTableRow;
import com.rentall.entities.Reservation;
import com.rentall.services.NotificationService;
import com.rentall.services.ReservationService;
import com.rentall.views.UiRefreshHub;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;

/**
 * Suppression d’une réservation : sélection lisible puis confirmation (sans saisie d’ID).
 */
public class DeleteReservationFrame extends JFrame {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color PAGE_BG = new Color(244, 247, 251);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color CARD_BORDER = new Color(214, 222, 232);
    private static final Color HEADER_RED = new Color(220, 38, 38);
    private static final Color HEADER_RED_HOVER = new Color(185, 28, 28);
    private static final Color HEADER_SUB = new Color(254, 226, 226);
    private static final Color FOOTER_BG = new Color(248, 250, 252);
    private static final Color TEXT_DARK = new Color(15, 23, 42);
    private static final Color TEXT_MUTED = new Color(71, 85, 105);
    private static final Color FIELD_BORDER = new Color(203, 213, 225);
    private static final Color WARNING_BG = new Color(254, 242, 242);
    private static final Color WARNING_BORDER = new Color(252, 165, 165);
    private static final Color BTN_BACK = new Color(71, 85, 105);
    private static final Color BTN_BACK_HOVER = new Color(51, 65, 85);
    private static final Color BTN_CANCEL = new Color(226, 232, 240);
    private static final Color BTN_CANCEL_HOVER = new Color(203, 213, 225);
    private static final int CONTROL_HEIGHT = 44;

    private static final String CARD_PICK = "pick";
    private static final String CARD_DETAIL = "detail";

    private final ReservationService service = new ReservationService();
    private Reservation reservation;
    private ReservationTableRow selectedSummary;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel deck = new JPanel(cardLayout);

    private final JComboBox<ReservationTableRow> cbPick = new JComboBox<>();
    private final JLabel lblPickErr = new JLabel(" ");
    private JButton btnContinuer;

    private final JPanel detailGrid = new JPanel(new GridLayout(0, 2, 10, 10));

    public DeleteReservationFrame(JFrame parent) {
        setTitle("Rentall — Supprimer une réservation");
        setMinimumSize(new Dimension(700, 560));
        setSize(760, 620);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(PAGE_BG);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(24, 30, 26, 30));
        deck.setOpaque(false);
        wrap.add(deck, BorderLayout.CENTER);
        root.add(wrap, BorderLayout.CENTER);

        deck.add(buildPickPanel(), CARD_PICK);
        deck.add(buildDetailPanel(), CARD_DETAIL);
        setContentPane(root);

        ReservationPickComboHelper.prepareCombo(cbPick);
        refillPickCombo();
        cardLayout.show(deck, CARD_PICK);
    }

    private void refillPickCombo() {
        cbPick.removeAllItems();
        for (ReservationTableRow row : service.listerPourAffichageTableau()) {
            cbPick.addItem(row);
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
        root.setBackground(CARD_BG);
        root.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_RED);
        header.setBorder(new EmptyBorder(24, 28, 24, 28));
        JLabel title = new JLabel("Supprimer une réservation");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Choisissez le séjour à retirer de la plateforme");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(HEADER_SUB);
        JPanel ht = new JPanel(new GridLayout(2, 1, 4, 0));
        ht.setOpaque(false);
        ht.add(title);
        ht.add(sub);
        header.add(ht, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(CARD_BG);
        body.setBorder(new EmptyBorder(42, 42, 34, 42));
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(8, 0, 8, 0);
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        JLabel lab = new JLabel("Réservation");
        lab.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lab.setForeground(TEXT_DARK);
        body.add(lab, gc);
        gc.gridy = 1;
        styleCombo(cbPick);
        body.add(cbPick, gc);
        lblPickErr.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblPickErr.setForeground(ReservationPalette.ERROR_TEXT);
        gc.gridy = 2;
        body.add(lblPickErr, gc);
        root.add(body, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        btnPanel.setBackground(FOOTER_BG);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, CARD_BORDER));
        JButton btnRetour = new JButton("Retour");
        styleActionButton(btnRetour, BTN_BACK, BTN_BACK_HOVER, Color.WHITE, 145);
        JButton btnCancel = new JButton("Annuler");
        styleActionButton(btnCancel, BTN_CANCEL, BTN_CANCEL_HOVER, TEXT_DARK, 145);
        btnContinuer = new JButton("Continuer");
        styleActionButton(btnContinuer, HEADER_RED, HEADER_RED_HOVER, Color.WHITE, 145);
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
        root.setBackground(CARD_BG);
        root.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_RED);
        header.setBorder(new EmptyBorder(24, 28, 24, 28));
        JLabel title = new JLabel("Confirmer la suppression");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 20));
        body.setBackground(CARD_BG);
        body.setBorder(new EmptyBorder(30, 42, 28, 42));
        body.add(createWarningPanel(), BorderLayout.NORTH);

        detailGrid.setBackground(CARD_BG);
        detailGrid.setBorder(new EmptyBorder(4, 0, 4, 0));
        body.add(detailGrid, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(FOOTER_BG);
        bottom.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, CARD_BORDER),
                new EmptyBorder(14, 20, 14, 20)));
        JLabel warn = new JLabel("Cette action est irréversible.");
        warn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        warn.setForeground(HEADER_RED);
        bottom.add(warn, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        JButton btnBackPick = new JButton("Changer de réservation");
        styleActionButton(btnBackPick, BTN_BACK, BTN_BACK_HOVER, Color.WHITE, 190);
        JButton btnCancel = new JButton("Annuler");
        styleActionButton(btnCancel, BTN_CANCEL, BTN_CANCEL_HOVER, TEXT_DARK, 145);
        JButton btnConfirm = new JButton("Supprimer définitivement");
        styleActionButton(btnConfirm, HEADER_RED, HEADER_RED_HOVER, Color.WHITE, 210);
        btnPanel.add(btnBackPick);
        btnPanel.add(btnCancel);
        btnPanel.add(btnConfirm);
        bottom.add(btnPanel, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);

        btnBackPick.addActionListener(e -> {
            lblPickErr.setText(" ");
            selectedSummary = null;
            reservation = null;
            refillPickCombo();
            cardLayout.show(deck, CARD_PICK);
        });
        btnCancel.addActionListener(e -> dispose());
        btnConfirm.addActionListener(e -> {
            if (reservation != null) {
                try {
                    service.supprimer(reservation.getId());
                    UiRefreshHub.notifyReservationChanged();
                    NotificationService.showSuccess("Succès", "Réservation supprimée avec succès");
                    dispose();
                } catch (Exception ex) {
                    NotificationService.showError("Erreur", ex.getMessage());
                }
            }
        });

        return root;
    }

    private JPanel createWarningPanel() {
        JPanel warning = new JPanel(new BorderLayout());
        warning.setBackground(WARNING_BG);
        warning.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WARNING_BORDER, 1),
                new EmptyBorder(14, 16, 14, 16)));

        JLabel title = new JLabel("Attention : suppression définitive");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(HEADER_RED);
        JLabel text = new JLabel("Cette action supprimera la réservation sélectionnée. Vérifiez les détails avant de confirmer.");
        text.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        text.setForeground(TEXT_DARK);

        JPanel copy = new JPanel(new GridLayout(2, 1, 0, 4));
        copy.setOpaque(false);
        copy.add(title);
        copy.add(text);
        warning.add(copy, BorderLayout.CENTER);
        return warning;
    }

    private void loadSelected() {
        lblPickErr.setText(" ");
        ReservationTableRow pick = (ReservationTableRow) cbPick.getSelectedItem();
        if (pick == null) {
            lblPickErr.setText("Sélectionnez une réservation dans la liste.");
            return;
        }
        Reservation r = service.afficherParId(pick.getId());
        if (r == null) {
            lblPickErr.setText("Cette réservation n’existe plus. La liste a été actualisée.");
            refillPickCombo();
            return;
        }
        reservation = r;
        selectedSummary = pick;
        detailGrid.removeAll();
        addDetail("Logement :", selectedSummary.getFoyerLibelle());
        addDetail("Locataire :", selectedSummary.getLocataireLibelle());
        addDetail("Arrivée :", r.getDateDebut().format(FMT));
        addDetail("Départ :", r.getDateFin().format(FMT));
        addDetail("Montant total :", ReservationUiFormat.montantAffichage(r.getMontantTotal()));
        addDetail("Statut :", ReservationUiFormat.statutAffichage(r.getStatut()));
        addDetail("Voyageurs :", String.valueOf(r.getNombrePersonnes()));
        detailGrid.revalidate();
        detailGrid.repaint();
        cardLayout.show(deck, CARD_DETAIL);
    }

    private void addDetail(String label, String value) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(TEXT_MUTED);
        JLabel val = new JLabel(value != null ? value : "—");
        val.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        val.setForeground(TEXT_DARK);
        detailGrid.add(lbl);
        detailGrid.add(val);
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setForeground(TEXT_DARK);
        combo.setBackground(Color.WHITE);
        combo.setPreferredSize(new Dimension(360, CONTROL_HEIGHT));
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER, 1),
                new EmptyBorder(8, 10, 8, 10)));
    }

    private void styleActionButton(JButton button, Color normalColor, Color hoverColor, Color textColor, int width) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(textColor);
        button.setBackground(normalColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(width, 44));
        button.setBorder(new EmptyBorder(10, 18, 10, 18));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hoverColor);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(normalColor);
            }
        });
    }
}
