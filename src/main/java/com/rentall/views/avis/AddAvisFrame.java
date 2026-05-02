package com.rentall.views.avis;

import com.rentall.entities.Avis;
import com.rentall.entities.Reservation;
import com.rentall.services.AnalyseCommentaireService;
import com.rentall.services.AvisService;
import com.rentall.services.NotificationService;
import com.rentall.services.ReservationService;
import com.rentall.services.SentimentAvisService;
import com.rentall.views.UiRefreshHub;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Fenetre dédiée : Ajouter un Avis
 * Validation complète : réservation, note (JSpinner 1-5), commentaire (5-255 car.).
 * Bordure rouge sur champ invalide. Blocage de l'enregistrement si données invalides.
 */
public class AddAvisFrame extends JFrame {

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

    private final AvisService service = new AvisService();
    private final AnalyseCommentaireService analyseService = new AnalyseCommentaireService();
    private final SentimentAvisService sentimentService = new SentimentAvisService();
    private final JComboBox<ReservationItem> cbReservation = new JComboBox<>();
    private final JSpinner spinnerNote = new JSpinner(new SpinnerNumberModel(5, 1, 5, 1));
    private final JLabel lblStars = new JLabel();
    private final JTextArea taCommentaire = new JTextArea(5, 30);
    private final JLabel lblMsg = new JLabel(" ");
    private final JLabel lblSentiment = new JLabel(" ");
    private JButton btnSave;

    public AddAvisFrame(JFrame parent) {
        setTitle("Rentall - Ajouter un Avis");
        setSize(680, 620);
        setMinimumSize(new Dimension(620, 560));
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        chargerReservations();
        buildUI();
    }

    // -------------------------------------------------------
    // Chargement des réservations sans avis existant
    // -------------------------------------------------------
    private void chargerReservations() {
        List<Reservation> reservations = new ReservationService().afficherTous();
        List<Avis> avisExistants = service.afficherTous();
        java.util.Set<Integer> dejaPris = new java.util.HashSet<>();
        for (Avis a : avisExistants) {
            dejaPris.add(a.getReservationId());
        }
        for (Reservation r : reservations) {
            if (!dejaPris.contains(r.getId())) {
                cbReservation.addItem(new ReservationItem(r));
            }
        }
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(PAGE_BG);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_GREEN);
        header.setBorder(new EmptyBorder(24, 30, 24, 30));
        JLabel title = new JLabel("Nouvel avis");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Note, commentaire et sentiment calculé en temps réel");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(new Color(220, 252, 231));
        JPanel ht = new JPanel(new GridLayout(2, 1, 4, 0));
        ht.setOpaque(false);
        ht.add(title);
        ht.add(sub);
        header.add(ht, BorderLayout.WEST);
        card.add(header, BorderLayout.NORTH);

        // Formulaire
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(30, 42, 18, 42));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(10, 6, 10, 6);

        if (cbReservation.getItemCount() == 0) {
            gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2;
            JLabel noRes = new JLabel("Toutes les réservations ont déjà un avis.");
            noRes.setForeground(BORDER_ERROR);
            noRes.setFont(new Font("Segoe UI", Font.BOLD, 13));
            form.add(noRes, gc);
        } else {
            // Réservation
            gc.gridwidth = 1; gc.weightx = 0.4; gc.gridx = 0; gc.gridy = 0;
            form.add(label("Réservation :"), gc);
            gc.gridx = 1; gc.weightx = 0.6;
            styleComboBox(cbReservation);
            form.add(cbReservation, gc);

            // Note avec JSpinner et label d'étoiles
            gc.gridx = 0; gc.gridy = 1; gc.weightx = 0.4;
            form.add(label("Note (1 à 5) :"), gc);
            gc.gridx = 1; gc.weightx = 0.6;
            JPanel noteWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            noteWrap.setOpaque(false);
            styleSpinner(spinnerNote);
            noteWrap.add(spinnerNote);
            lblStars.setFont(new Font("Monospaced", Font.BOLD, 16));
            lblStars.setForeground(new Color(217, 119, 6));
            updateStarsLabel(5);
            noteWrap.add(lblStars);
            form.add(noteWrap, gc);
            
            // Sentiment
            gc.gridx = 0; gc.gridy = 2; gc.weightx = 0.4;
            form.add(label("Sentiment :"), gc);
            gc.gridx = 1; gc.weightx = 0.6;
            styleSentimentLabel();
            updateSentimentLabel(5);
            form.add(lblSentiment, gc);

            // Commentaire
            gc.gridx = 0; gc.gridy = 3; gc.weightx = 0.4;
            form.add(label("Commentaire :"), gc);
            gc.gridx = 1; gc.weightx = 0.6;
            styleTextArea(taCommentaire);
            JScrollPane scroll = new JScrollPane(taCommentaire);
            scroll.setBorder(BorderFactory.createLineBorder(FIELD_BORDER));
            form.add(scroll, gc);

            // Hint longueur
            gc.gridx = 1; gc.gridy = 4; gc.weightx = 0.6;
            JLabel hint = new JLabel(COMMENTAIRE_MIN + " à " + COMMENTAIRE_MAX + " caractères obligatoires");
            hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            hint.setForeground(TEXT_MUTED);
            form.add(hint, gc);

            // Message erreur
            gc.gridx = 0; gc.gridy = 5; gc.gridwidth = 2;
            lblMsg.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            lblMsg.setForeground(BORDER_ERROR);
            form.add(lblMsg, gc);
        }

        card.add(form, BorderLayout.CENTER);

        // Boutons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        btnPanel.setBackground(new Color(248, 250, 252));
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, CARD_BORDER));
        JButton btnRetour = makeBtn("Retour", BTN_BACK, BTN_BACK_HOVER, Color.WHITE);
        JButton btnCancel = makeBtn("Annuler", BTN_CANCEL, BTN_CANCEL_HOVER, TEXT_DARK);
        btnSave = makeBtn("Enregistrer", BTN_SAVE, BTN_SAVE_HOVER, Color.WHITE);
        btnSave.setEnabled(cbReservation.getItemCount() > 0);
        btnPanel.add(btnRetour);
        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);
        card.add(btnPanel, BorderLayout.SOUTH);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(24, 30, 28, 30));
        wrap.add(card, BorderLayout.CENTER);
        root.add(wrap, BorderLayout.CENTER);

        btnRetour.addActionListener(e -> dispose());
        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> save());
        
        // Listener pour mettre à jour l'affichage en temps réel
        spinnerNote.addChangeListener(e -> {
            int note = (Integer) spinnerNote.getValue();
            updateStarsLabel(note);
            updateSentimentLabel(note);
        });

        setContentPane(root);
    }

    // -------------------------------------------------------
    // Validation complète
    // -------------------------------------------------------
    private boolean validateForm() {
        boolean ok = true;
        lblMsg.setText(" ");

        // Réservation
        if (cbReservation.getSelectedItem() == null) {
            lblMsg.setText("Veuillez sélectionner une réservation valide.");
            cbReservation.setBorder(BorderFactory.createLineBorder(BORDER_ERROR, 2));
            ok = false;
        } else {
            cbReservation.setBorder(BorderFactory.createLineBorder(FIELD_BORDER));
        }

        // Commentaire avec analyse automatique
        String commentaire = taCommentaire.getText().trim();
        AnalyseCommentaireService.ResultatAnalyse resultat = analyseService.analyser(commentaire);
        
        if (!resultat.isValide()) {
            setTextAreaBorder(taCommentaire, BORDER_ERROR);
            if (ok) lblMsg.setText(resultat.getMessageErreur());
            ok = false;
        } else {
            setTextAreaBorder(taCommentaire, BORDER_NORMAL);
        }

        return ok;
    }

    private void setTextAreaBorder(JTextArea ta, Color c) {
        ta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(c, c.equals(BORDER_ERROR) ? 2 : 1),
                new EmptyBorder(5, 8, 5, 8)));
    }

    private void save() {
        if (!validateForm()) {
            NotificationService.showError("Erreur de validation", lblMsg.getText());
            return;
        }
        try {
            ReservationItem item = (ReservationItem) cbReservation.getSelectedItem();
            int reservationId = item.getId();
            int note = (Integer) spinnerNote.getValue();
            String commentaire = taCommentaire.getText().trim();
            service.ajouter(new Avis(reservationId, note, commentaire, LocalDateTime.now()));
            
            // Afficher le sentiment dans la notification
            String sentimentStr = sentimentService.formaterSentiment(note);
            NotificationService.showSuccess("Succès", "Avis ajouté avec succès. Sentiment : " + sentimentStr);
            UiRefreshHub.notifyAvisChanged();
            dispose();
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.contains("Duplicate")) {
                NotificationService.showError("Erreur", "Un avis existe déjà pour cette réservation.");
                lblMsg.setText("Un avis existe déjà pour cette réservation.");
            } else {
                NotificationService.showError("Erreur", msg != null ? msg : ex.getClass().getSimpleName());
                lblMsg.setText("Erreur : " + (msg != null ? msg : ex.getClass().getSimpleName()));
            }
        }
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------
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
        combo.setPreferredSize(new Dimension(360, CONTROL_HEIGHT));
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
        lblSentiment.setFont(new Font("Monospaced", Font.BOLD, 14));
        lblSentiment.setOpaque(true);
        lblSentiment.setBorder(new EmptyBorder(9, 14, 9, 14));
    }

    /**
     * Met à jour le label des étoiles avec des caractères ASCII garantis.
     */
    private void updateStarsLabel(int note) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < 5; i++) {
            sb.append(i < note ? "*" : "-");
        }
        sb.append("] ").append(note).append("/5");
        lblStars.setText(sb.toString());
    }

    private void updateSentimentLabel(int note) {
        SentimentAvisService.Sentiment sentiment = sentimentService.getSentiment(note);
        if (sentiment == null) {
            return;
        }
        Color color = sentiment.getCouleur();
        // Affichage ASCII garanti : étoiles + note + sentiment sans emoji
        String stars = formatStarsAscii(note);
        String sentimentTag = formatSentimentTag(sentiment);
        lblSentiment.setText(stars + "  " + sentimentTag);
        lblSentiment.setForeground(color);
        lblSentiment.setBackground(tint(color));
    }
    
    /**
     * Formate les étoiles en ASCII garanti.
     */
    private String formatStarsAscii(int note) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < 5; i++) {
            sb.append(i < note ? "*" : "-");
        }
        sb.append("] ").append(note).append("/5");
        return sb.toString();
    }
    
    /**
     * Formate le sentiment en tag textuel sans emoji.
     */
    private String formatSentimentTag(SentimentAvisService.Sentiment sentiment) {
        switch (sentiment) {
            case POSITIF:
                return "[POSITIF]";
            case NEUTRE:
                return "[NEUTRE]";
            case NEGATIF:
                return "[NEGATIF]";
            default:
                return "[INCONNU]";
        }
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
        b.setPreferredSize(new Dimension(140, 42));
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

    // -------------------------------------------------------
    // Item de combo pour les réservations
    // -------------------------------------------------------
    private static class ReservationItem {
        private final Reservation reservation;

        ReservationItem(Reservation r) {
            this.reservation = r;
        }

        int getId() {
            return reservation.getId();
        }

        @Override
        public String toString() {
            return "Réservation #" + reservation.getId()
                    + "  |  Foyer " + reservation.getFoyerId()
                    + "  |  " + reservation.getDateDebut().toLocalDate()
                    + " -> " + reservation.getDateFin().toLocalDate();
        }
    }
}
