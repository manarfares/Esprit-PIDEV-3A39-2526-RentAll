package com.rentall.views.avis;

import com.rentall.entities.Avis;
import com.rentall.services.AnalyseCommentaireService;
import com.rentall.services.AvisService;
import com.rentall.services.NotificationService;
import com.rentall.services.SentimentAvisService;
import com.rentall.views.UiRefreshHub;
import com.rentall.views.components.StarRatingPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Modifier un avis : sélection via JComboBox (plus de saisie d'ID brute).
 * Validation complète : note (JSpinner 1-5), commentaire (5-255 car.), bordures rouges.
 */
public class EditAvisFrame extends JFrame {

    private static final int COMMENTAIRE_MIN = 10;
    private static final int COMMENTAIRE_MAX = 500;
    private static final Color BORDER_NORMAL = new Color(200, 200, 200);
    private static final Color BORDER_ERROR  = new Color(231, 76, 60);
    private static final Color PAGE_BG = new Color(243, 246, 250);
    private static final Color CARD_BORDER = new Color(220, 226, 235);
    private static final Color HEADER_GREEN = new Color(4, 120, 87);
    private static final Color TEXT_DARK = new Color(30, 41, 59);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color FIELD_BORDER = new Color(203, 213, 225);
    private static final Color BTN_CANCEL = new Color(226, 232, 240);
    private static final Color BTN_CANCEL_HOVER = new Color(203, 213, 225);
    private static final Color BTN_BACK = new Color(71, 85, 105);
    private static final Color BTN_BACK_HOVER = new Color(51, 65, 85);
    private static final Color BTN_SAVE = new Color(5, 150, 105);
    private static final Color BTN_SAVE_HOVER = new Color(4, 120, 87);
    private static final int CONTROL_HEIGHT = 42;

    private static final String CARD_PICK = "pick";
    private static final String CARD_FORM = "form";

    private final AvisService service = new AvisService();
    private final AnalyseCommentaireService analyseService = new AnalyseCommentaireService();
    private final SentimentAvisService sentimentService = new SentimentAvisService();
    private Avis avis;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel deck = new JPanel(cardLayout);

    private final JComboBox<AvisItem> cbPick = new JComboBox<>();
    private final JLabel lblPickErr = new JLabel(" ");
    private JButton btnContinuer;

    private final JLabel lblFormTitle = new JLabel();
    private final JLabel lblFormInfo  = new JLabel();
    private final StarRatingPanel starNote = new StarRatingPanel(5);
    private final JTextArea taCommentaire = new JTextArea(5, 30);
    private final JLabel lblFormErr = new JLabel(" ");
    private final JLabel lblSentiment = new JLabel(" ");

    public EditAvisFrame(JFrame parent) {
        setTitle("Rentall - Modifier un Avis");
        setSize(700, 640);
        setMinimumSize(new Dimension(640, 580));
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        deck.add(buildPickPanel(), CARD_PICK);
        deck.add(buildFormPanel(), CARD_FORM);
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
        root.setBackground(PAGE_BG);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_GREEN);
        header.setBorder(new EmptyBorder(24, 30, 24, 30));
        JLabel title = new JLabel("Choisir l'avis à modifier");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Sélectionnez l'avis dans la liste");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(new Color(220, 252, 231));
        JPanel ht = new JPanel(new GridLayout(2, 1, 4, 0));
        ht.setOpaque(false);
        ht.add(title);
        ht.add(sub);
        header.add(ht, BorderLayout.WEST);
        card.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(28, 40, 20, 40));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(8, 6, 8, 6);
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2; gc.weightx = 1;
        JLabel lab = new JLabel("Avis à modifier");
        lab.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lab.setForeground(TEXT_DARK);
        body.add(lab, gc);
        gc.gridy = 1;
        styleComboBox(cbPick);
        body.add(cbPick, gc);
        lblPickErr.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblPickErr.setForeground(BORDER_ERROR);
        gc.gridy = 2;
        body.add(lblPickErr, gc);
        card.add(body, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        btnPanel.setBackground(new Color(248, 250, 252));
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, CARD_BORDER));
        JButton btnRetour = makeBtn("Retour", BTN_BACK, BTN_BACK_HOVER, Color.WHITE);
        JButton btnCancel = makeBtn("Annuler", BTN_CANCEL, BTN_CANCEL_HOVER, TEXT_DARK);
        btnContinuer = makeBtn("Continuer", BTN_SAVE, BTN_SAVE_HOVER, Color.WHITE);
        btnContinuer.setEnabled(false);
        btnPanel.add(btnRetour);
        btnPanel.add(btnCancel);
        btnPanel.add(btnContinuer);
        card.add(btnPanel, BorderLayout.SOUTH);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(24, 30, 28, 30));
        wrap.add(card, BorderLayout.CENTER);
        root.add(wrap, BorderLayout.CENTER);

        btnContinuer.addActionListener(e -> loadSelected());
        btnCancel.addActionListener(e -> dispose());
        btnRetour.addActionListener(e -> dispose());

        return root;
    }

    private JPanel buildFormPanel() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(PAGE_BG);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_GREEN);
        header.setBorder(new EmptyBorder(24, 30, 24, 30));
        lblFormTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblFormTitle.setForeground(Color.WHITE);
        lblFormInfo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblFormInfo.setForeground(new Color(220, 252, 231));
        JPanel ht = new JPanel(new GridLayout(2, 1, 0, 3));
        ht.setOpaque(false);
        ht.add(lblFormTitle);
        ht.add(lblFormInfo);
        header.add(ht, BorderLayout.WEST);
        card.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(30, 42, 18, 42));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(10, 6, 10, 6);

        gc.gridwidth = 1; gc.weightx = 0.4; gc.gridx = 0; gc.gridy = 0;
        form.add(label("Note (1 à 5) :"), gc);
        gc.gridx = 1; gc.weightx = 0.6;
        JPanel noteWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        noteWrap.setOpaque(false);
        noteWrap.add(starNote);
        form.add(noteWrap, gc);
        
        // Sentiment
        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0.4;
        form.add(label("Sentiment :"), gc);
        gc.gridx = 1; gc.weightx = 0.6;
        styleSentimentLabel();
        form.add(lblSentiment, gc);

        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0.4;
        form.add(label("Commentaire :"), gc);
        gc.gridx = 1; gc.weightx = 0.6;
        styleTextArea(taCommentaire);
        JScrollPane scroll = new JScrollPane(taCommentaire);
        scroll.setBorder(BorderFactory.createLineBorder(FIELD_BORDER));
        form.add(scroll, gc);

        gc.gridx = 1; gc.gridy = 3; gc.weightx = 0.6;
        JLabel hint = new JLabel(COMMENTAIRE_MIN + " à " + COMMENTAIRE_MAX + " caractères");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hint.setForeground(TEXT_MUTED);
        form.add(hint, gc);

        lblFormErr.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblFormErr.setForeground(BORDER_ERROR);
        gc.gridx = 0; gc.gridy = 4; gc.gridwidth = 2;
        form.add(lblFormErr, gc);
        card.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        btnPanel.setBackground(new Color(248, 250, 252));
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, CARD_BORDER));
        JButton btnRetourPick = makeBtn("Changer d'avis", BTN_BACK, BTN_BACK_HOVER, Color.WHITE);
        JButton btnCancel = makeBtn("Annuler", BTN_CANCEL, BTN_CANCEL_HOVER, TEXT_DARK);
        JButton btnValider = makeBtn("Enregistrer", BTN_SAVE, BTN_SAVE_HOVER, Color.WHITE);
        btnPanel.add(btnRetourPick);
        btnPanel.add(btnCancel);
        btnPanel.add(btnValider);
        card.add(btnPanel, BorderLayout.SOUTH);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(24, 30, 28, 30));
        wrap.add(card, BorderLayout.CENTER);
        root.add(wrap, BorderLayout.CENTER);

        btnValider.addActionListener(e -> save());
        btnCancel.addActionListener(e -> dispose());
        btnRetourPick.addActionListener(e -> {
            lblPickErr.setText(" ");
            lblFormErr.setText(" ");
            refillPickCombo();
            cardLayout.show(deck, CARD_PICK);
        });
        
        // Listener pour mettre à jour le sentiment en temps réel
        starNote.addChangeListener(e -> updateSentimentLabel(starNote.getRating()));

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
        lblFormTitle.setText("Modifier avis #" + a.getId());
        lblFormInfo.setText("Réservation #" + a.getReservationId()
                + "  |  Date : " + a.getDateCreation().toLocalDate());
        starNote.setRating(a.getNote());
        taCommentaire.setText(a.getCommentaire());
        setTextAreaBorder(taCommentaire, BORDER_NORMAL);
        lblFormErr.setText(" ");
        
        // Mettre à jour le sentiment
        updateSentimentLabel(a.getNote());
        
        cardLayout.show(deck, CARD_FORM);
    }

    private boolean validateForm() {
        boolean ok = true;
        lblFormErr.setText(" ");

        String commentaire = taCommentaire.getText().trim();
        AnalyseCommentaireService.ResultatAnalyse resultat = analyseService.analyser(commentaire);
        
        if (!resultat.isValide()) {
            setTextAreaBorder(taCommentaire, BORDER_ERROR);
            lblFormErr.setText(resultat.getMessageErreur());
            NotificationService.showError("Erreur de validation", resultat.getMessageErreur());
            ok = false;
        } else {
            setTextAreaBorder(taCommentaire, BORDER_NORMAL);
        }

        return ok;
    }

    private void save() {
        if (!validateForm()) return;
        try {
            int note = starNote.getRating();
            avis.setNote(note);
            avis.setCommentaire(taCommentaire.getText().trim());
            service.modifier(avis);
            
            // Afficher le sentiment dans la notification
            String sentimentStr = sentimentService.formaterSentiment(note);
            NotificationService.showSuccess("Succès", "Avis modifié avec succès. Sentiment : " + sentimentStr);
            UiRefreshHub.notifyAvisChanged();
            dispose();
        } catch (Exception ex) {
            lblFormErr.setText("Erreur : " + ex.getMessage());
            NotificationService.showError("Erreur", ex.getMessage());
        }
    }

    private void setTextAreaBorder(JTextArea ta, Color c) {
        ta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(c, c.equals(BORDER_ERROR) ? 2 : 1),
                new EmptyBorder(10, 12, 10, 12)));
    }

    private JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.BOLD, 14));
        l.setForeground(TEXT_DARK);
        return l;
    }

    private void styleComboBox(JComboBox<?> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setBackground(Color.WHITE);
        combo.setForeground(TEXT_DARK);
        combo.setPreferredSize(new Dimension(440, CONTROL_HEIGHT));
        combo.setBorder(BorderFactory.createLineBorder(FIELD_BORDER));
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        spinner.setPreferredSize(new Dimension(90, CONTROL_HEIGHT));
        spinner.setBorder(BorderFactory.createLineBorder(FIELD_BORDER));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.setFont(new Font("Segoe UI", Font.BOLD, 14));
            textField.setHorizontalAlignment(JTextField.CENTER);
            textField.setForeground(TEXT_DARK);
            textField.setBorder(new EmptyBorder(8, 10, 8, 10));
        }
    }

    private void styleTextArea(JTextArea ta) {
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        ta.setForeground(TEXT_DARK);
        ta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER),
                new EmptyBorder(10, 12, 10, 12)));
    }

    private void styleSentimentLabel() {
        lblSentiment.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblSentiment.setOpaque(true);
        lblSentiment.setBorder(new EmptyBorder(9, 14, 9, 14));
    }

    private void updateSentimentLabel(int note) {
        SentimentAvisService.Sentiment sentiment = sentimentService.getSentiment(note);
        if (sentiment == null) {
            return;
        }
        Color color = sentiment.getCouleur();
        lblSentiment.setText(sentiment.toString());
        lblSentiment.setForeground(color);
        lblSentiment.setBackground(tint(color));
    }

    private Color tint(Color color) {
        return new Color(
                255 - ((255 - color.getRed()) / 8),
                255 - ((255 - color.getGreen()) / 8),
                255 - ((255 - color.getBlue()) / 8));
    }

    private JButton makeBtn(String t, Color c, Color hover, Color textColor) {
        JButton b = new JButton(t);
        b.setBackground(c);
        b.setForeground(textColor);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(150, 42));
        b.setBorder(new EmptyBorder(10, 22, 10, 22));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (b.isEnabled()) {
                    b.setBackground(hover);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                b.setBackground(c);
            }
        });
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
                    + "  |  Note : " + AvisPalette.noteEtoiles(avis.getNote())
                    + "  |  " + avis.getDateCreation().toLocalDate();
        }
    }
}
