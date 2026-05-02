package com.rentall.views.reservation;

import com.rentall.dto.ReservationTableRow;
import com.rentall.entities.Reservation;
import com.rentall.services.NotificationService;
import com.rentall.services.ReservationService;
import com.rentall.util.FormValidator;
import com.rentall.util.ValidationResult;
import com.rentall.views.UiRefreshHub;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Modification d'une réservation avec validations complètes.
 */
public class EditReservationFrame extends JFrame {

    private static final String[] STATUTS_AUTORISES = { "en_attente", "confirmee", "terminee" };
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color ERROR_COLOR = new Color(231, 76, 60);
    private static final Color SUCCESS_COLOR = new Color(39, 174, 96);
    private static final Color PAGE_BG = new Color(244, 247, 251);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color CARD_BORDER = new Color(214, 222, 232);
    private static final Color HEADER_ORANGE = new Color(217, 119, 6);
    private static final Color HEADER_ORANGE_HOVER = new Color(180, 83, 9);
    private static final Color HEADER_SUB = new Color(255, 237, 213);
    private static final Color FOOTER_BG = new Color(248, 250, 252);
    private static final Color TEXT_DARK = new Color(15, 23, 42);
    private static final Color TEXT_MUTED = new Color(71, 85, 105);
    private static final Color NORMAL_BORDER = new Color(203, 213, 225);
    private static final Color BTN_BACK = new Color(71, 85, 105);
    private static final Color BTN_BACK_HOVER = new Color(51, 65, 85);
    private static final Color BTN_CANCEL = new Color(226, 232, 240);
    private static final Color BTN_CANCEL_HOVER = new Color(203, 213, 225);
    private static final Color BTN_SAVE = new Color(5, 150, 105);
    private static final Color BTN_SAVE_HOVER = new Color(4, 120, 87);
    private static final int CONTROL_HEIGHT = 44;

    private static final String CARD_PICK = "pick";
    private static final String CARD_FORM = "form";

    private final ReservationService service = new ReservationService();
    private Reservation reservation;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel deck = new JPanel(cardLayout);

    private final JComboBox<ReservationTableRow> cbPick = new JComboBox<>();
    private final JLabel lblPickErr = new JLabel(" ");
    private JButton btnContinuer;

    private final JLabel lblFormTitle = new JLabel();
    private final JLabel lblFormInfo = new JLabel();
    private final JComboBox<String> cbStatut = new JComboBox<>(STATUTS_AUTORISES);
    private final JTextField tfMontant = new JTextField();
    private final JSpinner spNbPersonnes = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
    private final JLabel lblErrorStatut = new JLabel(" ");
    private final JLabel lblErrorMontant = new JLabel(" ");
    private final JLabel lblErrorNbPersonnes = new JLabel(" ");

    public EditReservationFrame(JFrame parent) {
        setTitle("Rentall — Modifier une réservation");
        setMinimumSize(new Dimension(680, 560));
        setSize(760, 640);
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
        deck.add(buildFormPanel(), CARD_FORM);
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
        header.setBackground(HEADER_ORANGE);
        header.setBorder(new EmptyBorder(24, 28, 24, 28));
        JLabel title = new JLabel("Modifier une réservation");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Choisissez le séjour à modifier dans la liste");
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
        JLabel l = new JLabel("Réservation *");
        l.setFont(new Font("Segoe UI", Font.BOLD, 14));
        l.setForeground(TEXT_DARK);
        body.add(l, gc);
        gc.gridy = 1;
        styleCombo(cbPick);
        body.add(cbPick, gc);
        lblPickErr.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblPickErr.setForeground(ERROR_COLOR);
        gc.gridy = 2;
        body.add(lblPickErr, gc);

        root.add(body, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        btnPanel.setBackground(FOOTER_BG);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, CARD_BORDER));
        JButton btnRetour = new JButton("Retour");
        styleActionButton(btnRetour, BTN_BACK, BTN_BACK_HOVER, Color.WHITE, 150);
        JButton btnCancel = new JButton("Annuler");
        styleActionButton(btnCancel, BTN_CANCEL, BTN_CANCEL_HOVER, TEXT_DARK, 150);
        btnContinuer = new JButton("Continuer");
        styleActionButton(btnContinuer, HEADER_ORANGE, HEADER_ORANGE_HOVER, Color.WHITE, 150);
        btnPanel.add(btnRetour);
        btnPanel.add(btnCancel);
        btnPanel.add(btnContinuer);
        root.add(btnPanel, BorderLayout.SOUTH);

        btnContinuer.addActionListener(e -> loadSelected());
        btnCancel.addActionListener(e -> dispose());
        btnRetour.addActionListener(e -> dispose());

        return root;
    }

    private JPanel buildFormPanel() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(CARD_BG);
        root.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_ORANGE);
        header.setBorder(new EmptyBorder(24, 28, 24, 28));
        lblFormTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblFormTitle.setForeground(Color.WHITE);
        lblFormInfo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblFormInfo.setForeground(HEADER_SUB);
        JPanel headerText = new JPanel(new GridLayout(2, 1, 4, 0));
        headerText.setOpaque(false);
        headerText.add(lblFormTitle);
        headerText.add(lblFormInfo);
        header.add(headerText, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CARD_BG);
        form.setBorder(new EmptyBorder(38, 42, 24, 42));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(8, 6, 8, 6);

        int row = 0;

        // Statut
        addLabeledRow(form, gc, row++, "Statut * :", cbStatut, lblErrorStatut);

        // Montant
        addLabeledRow(form, gc, row++, "Montant (EUR) * :", tfMontant, lblErrorMontant);

        // Nb voyageurs
        gc.gridwidth = 1;
        gc.weightx = 0.4;
        gc.gridx = 0;
        gc.gridy = row;
        JLabel lblNb = new JLabel("Nb voyageurs * :");
        lblNb.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblNb.setForeground(TEXT_DARK);
        form.add(lblNb, gc);
        gc.gridx = 1;
        gc.weightx = 0.6;
        styleSpinner(spNbPersonnes);
        JComponent spEd = spNbPersonnes.getEditor();
        if (spEd instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) spEd).getTextField().setColumns(3);
        }
        JPanel nbWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        nbWrap.setOpaque(false);
        nbWrap.add(spNbPersonnes);
        JLabel nbHint = new JLabel("  (1 à 20 voyageurs)");
        nbHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        nbHint.setForeground(TEXT_MUTED);
        nbWrap.add(nbHint);
        form.add(nbWrap, gc);

        row++;
        gc.gridx = 1;
        gc.gridy = row;
        lblErrorNbPersonnes.setForeground(ERROR_COLOR);
        lblErrorNbPersonnes.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        form.add(lblErrorNbPersonnes, gc);

        // Info
        row++;
        gc.gridx = 0;
        gc.gridy = row;
        gc.gridwidth = 2;
        JLabel lblInfo = new JLabel("* Champs obligatoires");
        lblInfo.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        lblInfo.setForeground(TEXT_MUTED);
        form.add(lblInfo, gc);

        root.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        btnPanel.setBackground(FOOTER_BG);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, CARD_BORDER));
        JButton btnRetourPick = new JButton("Changer de réservation");
        styleActionButton(btnRetourPick, BTN_BACK, BTN_BACK_HOVER, Color.WHITE, 190);
        JButton btnCancel = new JButton("Annuler");
        styleActionButton(btnCancel, BTN_CANCEL, BTN_CANCEL_HOVER, TEXT_DARK, 145);
        JButton btnValider = new JButton("Enregistrer");
        styleActionButton(btnValider, BTN_SAVE, BTN_SAVE_HOVER, Color.WHITE, 150);
        btnPanel.add(btnRetourPick);
        btnPanel.add(btnCancel);
        btnPanel.add(btnValider);
        root.add(btnPanel, BorderLayout.SOUTH);

        btnValider.addActionListener(e -> save());
        btnCancel.addActionListener(e -> dispose());
        btnRetourPick.addActionListener(e -> {
            clearErrors();
            refillPickCombo();
            cardLayout.show(deck, CARD_PICK);
        });

        setupValidationListeners();

        return root;
    }

    private void addLabeledRow(JPanel p, GridBagConstraints gc, int row, String label, JComponent field, JLabel errorLabel) {
        gc.gridwidth = 1;
        gc.weightx = 0.4;
        gc.gridx = 0;
        gc.gridy = row;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(TEXT_DARK);
        p.add(lbl, gc);
        gc.gridx = 1;
        gc.weightx = 0.6;
        if (field instanceof JTextField) {
            styleField((JTextField) field);
        } else if (field instanceof JComboBox) {
            styleCombo((JComboBox<?>) field);
        }
        p.add(field, gc);

        row++;
        gc.gridx = 1;
        gc.gridy = row;
        errorLabel.setForeground(ERROR_COLOR);
        errorLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        p.add(errorLabel, gc);
    }

    private void setupValidationListeners() {
        // Validation montant en temps réel
        tfMontant.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { validateMontantRealTime(); }
            public void removeUpdate(DocumentEvent e) { validateMontantRealTime(); }
            public void changedUpdate(DocumentEvent e) { validateMontantRealTime(); }
        });

        // Validation statut
        cbStatut.addActionListener(e -> validateStatutRealTime());

        // Validation nb personnes
        spNbPersonnes.addChangeListener(e -> validateNbPersonnesRealTime());
    }

    private void validateStatutRealTime() {
        String statut = (String) cbStatut.getSelectedItem();
        ValidationResult result = FormValidator.validateSelectionNotEmpty(statut, "statut");
        
        if (!result.isValid()) {
            setFieldError(cbStatut, lblErrorStatut, result.getFirstError());
        } else {
            setFieldValid(cbStatut, lblErrorStatut);
        }
    }

    private void validateMontantRealTime() {
        String montantStr = tfMontant.getText().trim();
        
        if (montantStr.isEmpty()) {
            setFieldError(tfMontant, lblErrorMontant, "Le montant est obligatoire.");
            return;
        }

        try {
            BigDecimal montant = new BigDecimal(montantStr);
            ValidationResult result = FormValidator.validatePositiveDecimal(montant.doubleValue(), "Le montant");
            
            if (!result.isValid()) {
                setFieldError(tfMontant, lblErrorMontant, result.getFirstError());
            } else {
                setFieldValid(tfMontant, lblErrorMontant);
            }
        } catch (NumberFormatException e) {
            setFieldError(tfMontant, lblErrorMontant, "Montant invalide. Entrez un nombre décimal (ex: 450.00).");
        }
    }

    private void validateNbPersonnesRealTime() {
        try {
            spNbPersonnes.commitEdit();
            int nb = (Integer) spNbPersonnes.getValue();
            ValidationResult result = FormValidator.validateNumberRange(nb, "Le nombre de voyageurs", 1, 20);
            
            if (!result.isValid()) {
                setFieldError(spNbPersonnes, lblErrorNbPersonnes, result.getFirstError());
            } else {
                setFieldValid(spNbPersonnes, lblErrorNbPersonnes);
            }
        } catch (Exception e) {
            setFieldError(spNbPersonnes, lblErrorNbPersonnes, "Nombre de voyageurs invalide.");
        }
    }

    private void setFieldError(JComponent field, JLabel errorLabel, String message) {
        field.setBorder(fieldBorder(ERROR_COLOR, 2));
        errorLabel.setText(message);
    }

    private void setFieldValid(JComponent field, JLabel errorLabel) {
        field.setBorder(fieldBorder(SUCCESS_COLOR, 1));
        errorLabel.setText(" ");
    }

    private void clearErrors() {
        lblPickErr.setText(" ");
        lblErrorStatut.setText(" ");
        lblErrorMontant.setText(" ");
        lblErrorNbPersonnes.setText(" ");
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
            lblPickErr.setText("Cette réservation n'existe plus. La liste a été actualisée.");
            refillPickCombo();
            return;
        }
        reservation = r;
        lblFormTitle.setText("Modifier le séjour");
        lblFormInfo.setText(pick.getFoyerLibelle() + " · " + pick.getLocataireLibelle()
                + " · " + pick.getDateDebut().format(FMT) + " → " + pick.getDateFin().format(FMT));
        preselectStatut(cbStatut, r.getStatut());
        tfMontant.setText(r.getMontantTotal().toString());
        int nbActuel = r.getNombrePersonnes();
        spNbPersonnes.setValue(Math.max(1, Math.min(20, nbActuel)));
        clearErrors();
        cardLayout.show(deck, CARD_FORM);
        
        // Validation initiale
        validateStatutRealTime();
        validateMontantRealTime();
        validateNbPersonnesRealTime();
    }

    private ValidationResult validateAll() {
        ValidationResult result = new ValidationResult();

        // Validation statut
        String statut = (String) cbStatut.getSelectedItem();
        ValidationResult statutResult = FormValidator.validateSelectionNotEmpty(statut, "statut");
        if (!statutResult.isValid()) {
            result.addError(statutResult.getFirstError());
        }

        // Validation montant
        String montantStr = tfMontant.getText().trim();
        if (montantStr.isEmpty()) {
            result.addError("Le montant est obligatoire.");
        } else {
            try {
                BigDecimal montant = new BigDecimal(montantStr);
                ValidationResult montantResult = FormValidator.validatePositiveDecimal(montant.doubleValue(), "Le montant");
                if (!montantResult.isValid()) {
                    result.addError(montantResult.getFirstError());
                }
            } catch (NumberFormatException e) {
                result.addError("Montant invalide. Entrez un nombre décimal.");
            }
        }

        // Validation nb personnes
        try {
            spNbPersonnes.commitEdit();
            int nb = (Integer) spNbPersonnes.getValue();
            ValidationResult nbResult = FormValidator.validateNumberRange(nb, "Le nombre de voyageurs", 1, 20);
            if (!nbResult.isValid()) {
                result.addError(nbResult.getFirstError());
            }
        } catch (Exception e) {
            result.addError("Nombre de voyageurs invalide.");
        }

        return result;
    }

    private void save() {
        ValidationResult validation = validateAll();
        
        if (!validation.isValid()) {
            NotificationService.showError("Erreur de validation", validation);
            return;
        }

        try {
            BigDecimal montant = new BigDecimal(tfMontant.getText().trim());
            int nbPersonnes = (Integer) spNbPersonnes.getValue();
            reservation.setStatut((String) cbStatut.getSelectedItem());
            reservation.setMontantTotal(montant);
            reservation.setNombrePersonnes(nbPersonnes);
            service.modifier(reservation);
            UiRefreshHub.notifyReservationChanged();
            NotificationService.showSuccess("Succès", "Réservation modifiée avec succès");
            dispose();
        } catch (Exception ex) {
            NotificationService.showError("Erreur", ex.getMessage());
        }
    }

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

    private void styleField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(TEXT_DARK);
        field.setBackground(Color.WHITE);
        field.setPreferredSize(new Dimension(280, CONTROL_HEIGHT));
        field.setBorder(fieldBorder(NORMAL_BORDER, 1));
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setForeground(TEXT_DARK);
        combo.setBackground(Color.WHITE);
        combo.setPreferredSize(new Dimension(280, CONTROL_HEIGHT));
        combo.setBorder(fieldBorder(NORMAL_BORDER, 1));
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        spinner.setPreferredSize(new Dimension(112, CONTROL_HEIGHT));
        spinner.setBorder(fieldBorder(NORMAL_BORDER, 1));
        spinner.setBackground(Color.WHITE);
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.setFont(new Font("Segoe UI", Font.BOLD, 14));
            textField.setForeground(TEXT_DARK);
            textField.setBackground(Color.WHITE);
            textField.setHorizontalAlignment(JTextField.CENTER);
            textField.setBorder(new EmptyBorder(8, 10, 8, 10));
        }
    }

    private Border fieldBorder(Color color, int thickness) {
        return BorderFactory.createCompoundBorder(
                new LineBorder(color, thickness),
                new EmptyBorder(8, 10, 8, 10));
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
