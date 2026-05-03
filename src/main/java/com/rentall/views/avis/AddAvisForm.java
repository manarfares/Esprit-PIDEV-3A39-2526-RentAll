package com.rentall.views.avis;

import com.rentall.entities.Avis;
import com.rentall.entities.Reservation;
import com.rentall.services.AvisService;
import com.rentall.services.ReservationService;
import com.rentall.util.FormValidator;
import com.rentall.util.ValidationResult;
import com.rentall.views.components.StarRatingPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Formulaire d'ajout d'un avis avec validations complètes.
 * Charge automatiquement les reservations disponibles (sans avis existant).
 */
public class AddAvisForm extends JDialog {

    private final AvisService service;
    private boolean success = false;

    private final JComboBox<String> cbReservation = new JComboBox<>();
    private final StarRatingPanel starNote = new StarRatingPanel(5);
    private final JTextArea taCommentaire = new JTextArea(4, 30);
    private final JLabel lblErrorReservation = new JLabel(" ");
    private final JLabel lblErrorNote = new JLabel(" ");
    private final JLabel lblErrorCommentaire = new JLabel(" ");
    private final JLabel lblCharCount = new JLabel("0 / 500 caractères");

    private static final int MIN_COMMENT_LENGTH = 10;
    private static final int MAX_COMMENT_LENGTH = 500;
    private static final Color ERROR_COLOR = new Color(231, 76, 60);
    private static final Color SUCCESS_COLOR = new Color(39, 174, 96);
    private static final Color NORMAL_BORDER = new Color(200, 200, 200);

    public AddAvisForm(Frame parent, AvisService service) {
        super(parent, "Ajouter un Avis", true);
        this.service = service;
        chargerReservationsDisponibles();
        buildUI();
        setupValidationListeners();
        pack();
        setLocationRelativeTo(parent);
    }

    private void chargerReservationsDisponibles() {
        List<Reservation> reservations = new ReservationService().afficherTous();
        List<Avis> avisExistants = service.afficherTous();

        java.util.Set<Integer> dejaPris = new java.util.HashSet<>();
        for (Avis a : avisExistants) dejaPris.add(a.getReservationId());

        for (Reservation r : reservations) {
            if (!dejaPris.contains(r.getId())) {
                cbReservation.addItem("ID " + r.getId() +
                        " | Foyer " + r.getFoyerId() +
                        " | " + r.getDateDebut().toLocalDate() +
                        " → " + r.getDateFin().toLocalDate());
            }
        }
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(20, 20, 20, 20));
        content.setBackground(Color.WHITE);

        JLabel titre = new JLabel("Nouvel Avis");
        titre.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titre.setForeground(new Color(44, 62, 80));
        content.add(titre, BorderLayout.NORTH);

        if (cbReservation.getItemCount() == 0) {
            JLabel noRes = new JLabel("Toutes les reservations ont deja un avis.");
            noRes.setForeground(ERROR_COLOR);
            noRes.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            content.add(noRes, BorderLayout.CENTER);
            JButton btnClose = AvisPalette.createCancelButton("Fermer");
            btnClose.addActionListener(e -> dispose());
            JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            bp.add(btnClose);
            content.add(bp, BorderLayout.SOUTH);
            setContentPane(content);
            return;
        }

        taCommentaire.setLineWrap(true);
        taCommentaire.setWrapStyleWord(true);
        taCommentaire.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(NORMAL_BORDER, 1),
            new EmptyBorder(5, 5, 5, 5)
        ));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        int row = 0;

        // Réservation
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        form.add(new JLabel("Réservation * :"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(cbReservation, gbc);
        
        row++;
        gbc.gridx = 1; gbc.gridy = row;
        lblErrorReservation.setForeground(ERROR_COLOR);
        lblErrorReservation.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        form.add(lblErrorReservation, gbc);

        // Note
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        form.add(new JLabel("Note (1 à 5) * :"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        form.add(starNote, gbc);
        
        row++;
        gbc.gridx = 1; gbc.gridy = row;
        lblErrorNote.setForeground(ERROR_COLOR);
        lblErrorNote.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        form.add(lblErrorNote, gbc);

        // Commentaire
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3; gbc.anchor = GridBagConstraints.NORTH;
        form.add(new JLabel("Commentaire * :"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7; gbc.anchor = GridBagConstraints.CENTER;
        form.add(new JScrollPane(taCommentaire), gbc);
        
        row++;
        gbc.gridx = 1; gbc.gridy = row;
        lblCharCount.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblCharCount.setForeground(new Color(127, 140, 141));
        form.add(lblCharCount, gbc);
        
        row++;
        gbc.gridx = 1; gbc.gridy = row;
        lblErrorCommentaire.setForeground(ERROR_COLOR);
        lblErrorCommentaire.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        form.add(lblErrorCommentaire, gbc);

        // Info validation
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JLabel lblInfo = new JLabel("* Champs obligatoires | Commentaire : " + MIN_COMMENT_LENGTH + " à " + MAX_COMMENT_LENGTH + " caractères");
        lblInfo.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        lblInfo.setForeground(new Color(127, 140, 141));
        form.add(lblInfo, gbc);

        content.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setBackground(Color.WHITE);
        JButton btnSave   = AvisPalette.createSaveButton("Enregistrer");
        JButton btnCancel = AvisPalette.createCancelButton("Annuler");
        buttons.add(btnCancel);
        buttons.add(btnSave);
        content.add(buttons, BorderLayout.SOUTH);

        btnSave.addActionListener(e -> save());
        btnCancel.addActionListener(e -> dispose());

        setContentPane(content);
    }

    private void setupValidationListeners() {
        // Validation en temps réel du commentaire
        taCommentaire.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { validateCommentaireRealTime(); }
            public void removeUpdate(DocumentEvent e) { validateCommentaireRealTime(); }
            public void changedUpdate(DocumentEvent e) { validateCommentaireRealTime(); }
        });

        // Validation de la note
        starNote.addChangeListener(e -> validateNoteRealTime());

        // Validation de la réservation
        cbReservation.addActionListener(e -> validateReservationRealTime());
    }

    private void validateReservationRealTime() {
        if (cbReservation.getSelectedItem() == null) {
            setFieldError(cbReservation, lblErrorReservation, "Veuillez sélectionner une réservation.");
        } else {
            setFieldValid(cbReservation, lblErrorReservation);
        }
    }

    private void validateNoteRealTime() {
        int note = starNote.getRating();
        ValidationResult result = FormValidator.validateRating(note, 1, 5);
        
        if (!result.isValid()) {
            setFieldError(starNote, lblErrorNote, result.getFirstError());
        } else {
            setFieldValid(starNote, lblErrorNote);
        }
    }

    private void validateCommentaireRealTime() {
        String text = taCommentaire.getText();
        int length = text.length();
        
        // Mise à jour du compteur
        lblCharCount.setText(length + " / " + MAX_COMMENT_LENGTH + " caractères");
        
        if (length > MAX_COMMENT_LENGTH) {
            lblCharCount.setForeground(ERROR_COLOR);
        } else if (length >= MIN_COMMENT_LENGTH) {
            lblCharCount.setForeground(SUCCESS_COLOR);
        } else {
            lblCharCount.setForeground(new Color(127, 140, 141));
        }

        ValidationResult result = FormValidator.validateComment(text, MIN_COMMENT_LENGTH, MAX_COMMENT_LENGTH);
        
        if (!result.isValid()) {
            setFieldError(taCommentaire, lblErrorCommentaire, result.getFirstError());
        } else {
            setFieldValid(taCommentaire, lblErrorCommentaire);
        }
    }

    private void setFieldError(JComponent field, JLabel errorLabel, String message) {
        field.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(ERROR_COLOR, 2),
            new EmptyBorder(5, 5, 5, 5)
        ));
        errorLabel.setText(message);
    }

    private void setFieldValid(JComponent field, JLabel errorLabel) {
        field.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(SUCCESS_COLOR, 1),
            new EmptyBorder(5, 5, 5, 5)
        ));
        errorLabel.setText(" ");
    }

    private ValidationResult validateAll() {
        ValidationResult result = new ValidationResult();

        // Validation réservation
        if (cbReservation.getSelectedItem() == null) {
            result.addError("Veuillez sélectionner une réservation.");
        }

        // Validation note
        int note = starNote.getRating();
        ValidationResult noteResult = FormValidator.validateRating(note, 1, 5);
        if (!noteResult.isValid()) {
            result.addError(noteResult.getFirstError());
        }

        // Validation commentaire
        String commentaire = FormValidator.sanitize(taCommentaire.getText());
        ValidationResult commentResult = FormValidator.validateComment(commentaire, MIN_COMMENT_LENGTH, MAX_COMMENT_LENGTH);
        if (!commentResult.isValid()) {
            result.addError(commentResult.getFirstError());
        }

        return result;
    }

    private void save() {
        // Validation complète
        ValidationResult validation = validateAll();
        
        if (!validation.isValid()) {
            JOptionPane.showMessageDialog(this, 
                validation.getAllErrors(), 
                "Erreurs de validation", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Extraire l'ID depuis le texte du ComboBox "ID 9 | Foyer..."
            String selected = (String) cbReservation.getSelectedItem();
            int reservationId = Integer.parseInt(selected.split("\\|")[0].replace("ID", "").trim());
            int note = starNote.getRating();
            String commentaire = FormValidator.sanitize(taCommentaire.getText());

            service.ajouter(new Avis(reservationId, note, commentaire, LocalDateTime.now()));
            success = true;
            JOptionPane.showMessageDialog(this, 
                "Avis ajouté avec succès !", 
                "Succès", 
                JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.contains("Duplicate")) {
                msg = "Cette réservation a déjà un avis.";
            }
            JOptionPane.showMessageDialog(this, 
                "Erreur : " + msg, 
                "Erreur", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSuccess() { return success; }
}
