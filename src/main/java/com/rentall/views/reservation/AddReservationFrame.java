package com.rentall.views.reservation;

import com.rentall.dto.FoyerListItem;
import com.rentall.dto.LocataireListItem;
import com.rentall.entities.Reservation;
import com.rentall.services.FoyerService;
import com.rentall.services.IFoyerService;
import com.rentall.services.ILocataireService;
import com.rentall.services.LocataireService;
import com.rentall.services.NotificationService;
import com.rentall.services.PdfReservationService;
import com.rentall.services.ReservationService;
import com.rentall.views.UiRefreshHub;
import com.rentall.views.components.ModernDatePicker;
import com.rentall.views.components.ModernTimePicker;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Ajout d'une réservation : choix lisible du logement et du locataire, montant calculé (nuits × prix/nuit).
 */
public class AddReservationFrame extends JFrame {

    private static final String[] STATUTS = { "en_attente", "confirmee", "terminee" };
    private static final int CONTROL_HEIGHT = 44;
    private static final int LABEL_WIDTH = 180;
    private static final Color PAGE_BG = new Color(243, 246, 250);
    private static final Color CARD_BORDER = new Color(220, 226, 235);
    private static final Color HEADER_GREEN = new Color(4, 120, 87);
    private static final Color HEADER_SUB = new Color(209, 250, 229);
    private static final Color LABEL_TEXT = new Color(30, 41, 59);
    private static final Color FIELD_BORDER = new Color(203, 213, 225);
    private static final Color FIELD_BG = Color.WHITE;
    private static final Color FOOTER_BG = new Color(248, 250, 252);
    private static final Color BTN_BACK = new Color(71, 85, 105);
    private static final Color BTN_BACK_HOVER = new Color(51, 65, 85);
    private static final Color BTN_CANCEL = new Color(226, 232, 240);
    private static final Color BTN_CANCEL_HOVER = new Color(203, 213, 225);
    private static final Color BTN_SAVE = new Color(5, 150, 105);
    private static final Color BTN_SAVE_HOVER = new Color(4, 120, 87);

    private final ReservationService service = new ReservationService();
    private final IFoyerService foyerService = new FoyerService();
    private final ILocataireService locataireService = new LocataireService();

    private final JComboBox<FoyerListItem> cbFoyer = new JComboBox<>();
    private final JComboBox<LocataireListItem> cbLocataire = new JComboBox<>();
    private final ModernDatePicker dpDateDebut = new ModernDatePicker(LocalDate.of(2026, 9, 1));
    private final ModernTimePicker tpHeureDebut = new ModernTimePicker(LocalTime.of(14, 0));
    private final ModernDatePicker dpDateFin = new ModernDatePicker(LocalDate.of(2026, 9, 10));
    private final ModernTimePicker tpHeureFin = new ModernTimePicker(LocalTime.of(11, 0));
    private final JComboBox<String> cbStatut = new JComboBox<>(STATUTS);
    private final JSpinner spNbPersonnes;
    private final JLabel lblMontant = new JLabel("—");
    private final JLabel lblMsg = new JLabel(" ");
    private JButton btnSave;

    public AddReservationFrame(JFrame parent) {
        setTitle("Rentall — Nouvelle réservation");
        setMinimumSize(new Dimension(760, 680));
        setSize(900, 760);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        SpinnerNumberModel nbModel = new SpinnerNumberModel(1, 1, 20, 1);
        spNbPersonnes = new JSpinner(nbModel);
        spNbPersonnes.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        spNbPersonnes.setPreferredSize(new Dimension(120, CONTROL_HEIGHT));
        styleSpinnerControl(spNbPersonnes);
        JComponent ed = spNbPersonnes.getEditor();
        if (ed instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) ed).getTextField().setColumns(3);
            ((JSpinner.DefaultEditor) ed).getTextField().setFont(new Font("Segoe UI", Font.PLAIN, 14));
            ((JSpinner.DefaultEditor) ed).getTextField().setHorizontalAlignment(JTextField.CENTER);
        }

        cbStatut.setSelectedItem("en_attente");

        remplirCombos();
        applyModernStyle();
        buildUI();
        recalculateMontant();
    }

    private void remplirCombos() {
        cbFoyer.removeAllItems();
        for (FoyerListItem f : foyerService.listerFoyersPourSelection()) {
            cbFoyer.addItem(f);
        }
        cbLocataire.removeAllItems();
        for (LocataireListItem l : locataireService.listerLocatairesPourSelection()) {
            cbLocataire.addItem(l);
        }
        if (cbFoyer.getItemCount() > 0) {
            cbFoyer.setSelectedIndex(0);
        }
        if (cbLocataire.getItemCount() > 0) {
            cbLocataire.setSelectedIndex(0);
        }
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(PAGE_BG);

        JPanel card = new RoundedPanel(new BorderLayout(), 18, Color.WHITE);
        card.setBorder(new RoundedBorder(CARD_BORDER, 18, 1));

        // Header amélioré
        JPanel header = new TopRoundedPanel(new BorderLayout(), 18, HEADER_GREEN);
        header.setBorder(new EmptyBorder(22, 32, 22, 32));
        JLabel title = new JLabel("Nouvelle réservation");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Séjour, montant calculé automatiquement à partir du logement et des dates");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(HEADER_SUB);
        JPanel ht = new JPanel(new GridLayout(2, 1, 0, 4));
        ht.setOpaque(false);
        ht.add(title);
        ht.add(sub);
        header.add(ht, BorderLayout.WEST);
        card.add(header, BorderLayout.NORTH);

        // Formulaire avec meilleur espacement
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(22, 42, 18, 42));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(8, 0, 8, 0);
        gc.anchor = GridBagConstraints.NORTHWEST;

        int row = 0;
        
        // Espace vide en haut pour garantir la visibilité
        addFormRow(form, gc, row++, "Logement :", cbFoyer);
        addFormRow(form, gc, row++, "Locataire :", cbLocataire);
        
        // Séparateur visuel
        addSeparator(form, gc, row++);
        
        addFormRow(form, gc, row++, "Date de début :", dpDateDebut);
        addFormRow(form, gc, row++, "Heure de début :", tpHeureDebut);
        addFormRow(form, gc, row++, "Date de fin :", dpDateFin);
        addFormRow(form, gc, row++, "Heure de fin :", tpHeureFin);
        
        // Séparateur visuel
        addSeparator(form, gc, row++);

        // Montant total avec style amélioré
        gc.gridx = 0;
        gc.gridy = row;
        gc.gridwidth = 1;
        gc.weightx = 0.0;
        gc.insets = new Insets(12, 0, 12, 18);
        JLabel lm = new JLabel("Montant total :");
        lm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lm.setForeground(LABEL_TEXT);
        form.add(lm, gc);
        
        gc.gridx = 1;
        gc.weightx = 1.0;
        gc.insets = new Insets(12, 0, 12, 0);
        form.add(createMontantPanel(), gc);
        row++;

        // Séparateur visuel
        addSeparator(form, gc, row++);

        addFormRow(form, gc, row++, "Statut :", cbStatut);
        addFormRow(form, gc, row++, "Nombre de personnes :", spNbPersonnes);

        // Message d'erreur
        gc.gridx = 0;
        gc.gridy = row;
        gc.gridwidth = 2;
        gc.weightx = 1.0;
        gc.insets = new Insets(12, 0, 0, 0);
        lblMsg.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblMsg.setForeground(ReservationPalette.ERROR_TEXT);
        form.add(lblMsg, gc);
        row++;

        // Glue pour pousser le contenu vers le haut
        gc.gridx = 0;
        gc.gridy = row;
        gc.gridwidth = 2;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        form.add(Box.createVerticalGlue(), gc);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        formScroll.getViewport().setBackground(Color.WHITE);
        card.add(formScroll, BorderLayout.CENTER);

        // Footer avec boutons mieux stylés
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 14));
        btnPanel.setBackground(FOOTER_BG);
        btnPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, CARD_BORDER),
                new EmptyBorder(0, 24, 0, 24)
        ));
        
        JButton btnRetour = new JButton("Retour");
        styleButton(btnRetour, BTN_BACK, BTN_BACK_HOVER, Color.WHITE);
        
        JButton btnCancel = new JButton("Annuler");
        styleButton(btnCancel, BTN_CANCEL, BTN_CANCEL_HOVER, LABEL_TEXT);
        
        btnSave = new JButton("Enregistrer");
        styleButton(btnSave, BTN_SAVE, BTN_SAVE_HOVER, Color.WHITE);
        
        btnPanel.add(btnRetour);
        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);
        card.add(btnPanel, BorderLayout.SOUTH);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(18, 28, 18, 28));
        wrap.add(card, BorderLayout.CENTER);
        root.add(wrap, BorderLayout.CENTER);

        btnRetour.addActionListener(e -> dispose());
        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> save());

        dpDateDebut.addDateChangeListener(date -> SwingUtilities.invokeLater(this::recalculateMontant));
        tpHeureDebut.addTimeChangeListener(time -> SwingUtilities.invokeLater(this::recalculateMontant));
        dpDateFin.addDateChangeListener(date -> SwingUtilities.invokeLater(this::recalculateMontant));
        tpHeureFin.addTimeChangeListener(time -> SwingUtilities.invokeLater(this::recalculateMontant));
        dpDateDebut.addDateChangeListener(date -> SwingUtilities.invokeLater(this::enforceDateOrder));
        dpDateFin.addDateChangeListener(date -> SwingUtilities.invokeLater(this::enforceDateOrder));

        cbFoyer.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                SwingUtilities.invokeLater(AddReservationFrame.this::recalculateMontant);
            }
        });

        setContentPane(root);
        SwingUtilities.invokeLater(() -> formScroll.getVerticalScrollBar().setValue(0));
        updateSaveEnabled();
    }

    private void applyModernStyle() {
        styleComboBox(cbFoyer);
        styleComboBox(cbLocataire);
        styleComboBox(cbStatut);
        stylePicker(dpDateDebut);
        stylePicker(dpDateFin);
        stylePicker(tpHeureDebut);
        stylePicker(tpHeureFin);
        styleSpinnerControl(spNbPersonnes);

        lblMontant.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblMontant.setForeground(BTN_SAVE);
        lblMontant.setOpaque(false);
        lblMontant.setHorizontalAlignment(SwingConstants.LEFT);
    }

    private void addFormRow(JPanel panel, GridBagConstraints gc, int row, String labelText, JComponent component) {
        gc.gridy = row;
        gc.gridwidth = 1;
        gc.weighty = 0.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(8, 0, 8, 15);
        
        // Label
        gc.gridx = 0;
        gc.weightx = 0.0;
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(LABEL_TEXT);
        label.setPreferredSize(new Dimension(LABEL_WIDTH, CONTROL_HEIGHT));
        label.setVerticalAlignment(SwingConstants.CENTER);
        panel.add(label, gc);
        
        // Component
        gc.gridx = 1;
        gc.weightx = 1.0;
        gc.insets = new Insets(8, 0, 8, 0);
        
        if (component instanceof JComboBox) {
            styleComboBox((JComboBox<?>) component);
        } else if (component instanceof ModernDatePicker || component instanceof ModernTimePicker) {
            stylePicker(component);
        } else if (component instanceof JSpinner) {
            styleSpinnerControl((JSpinner) component);
        }
        
        panel.add(component, gc);
    }

    private void addSeparator(JPanel panel, GridBagConstraints gc, int row) {
        gc.gridx = 0;
        gc.gridy = row;
        gc.gridwidth = 2;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        gc.insets = new Insets(6, 0, 6, 0);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.WEST;
        
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(229, 234, 241));
        sep.setBackground(new Color(229, 234, 241));
        panel.add(sep, gc);
    }

    private JPanel createMontantPanel() {
        JPanel montantPanel = new RoundedPanel(new BorderLayout(), 10, new Color(236, 253, 245));
        montantPanel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(134, 239, 172), 10, 1),
                new EmptyBorder(14, 20, 14, 20)
        ));
        montantPanel.add(lblMontant, BorderLayout.CENTER);
        montantPanel.setPreferredSize(new Dimension(360, 62));
        montantPanel.setMinimumSize(new Dimension(260, 62));
        return montantPanel;
    }

    private void styleComboBox(JComboBox<?> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setPreferredSize(new Dimension(360, CONTROL_HEIGHT));
        combo.setMinimumSize(new Dimension(260, CONTROL_HEIGHT));
        combo.setBackground(FIELD_BG);
        combo.setForeground(LABEL_TEXT);
        combo.setFocusable(false);
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER, 1),
                new EmptyBorder(4, 10, 4, 10)));
    }

    private void styleButton(JButton btn, Color normalColor, Color hoverColor, Color textColor) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(textColor);
        btn.setBackground(normalColor);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(150, 44));
        btn.setBorder(new EmptyBorder(11, 24, 11, 24));
        
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (btn.isEnabled()) {
                    btn.setBackground(hoverColor);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(normalColor);
            }
        });
    }

    private void stylePicker(JComponent picker) {
        picker.setPreferredSize(new Dimension(360, CONTROL_HEIGHT));
        picker.setMinimumSize(new Dimension(260, CONTROL_HEIGHT));
        picker.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        for (Component child : picker.getComponents()) {
            styleFieldComponent(child);
        }
    }

    private void styleFieldComponent(Component component) {
        component.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        if (component instanceof JTextField) {
            JTextField field = (JTextField) component;
            field.setPreferredSize(new Dimension(field.getPreferredSize().width, CONTROL_HEIGHT));
            field.setBackground(FIELD_BG);
            field.setForeground(LABEL_TEXT);
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(FIELD_BORDER, 1),
                    new EmptyBorder(9, 12, 9, 12)));
        } else if (component instanceof JButton) {
            JButton button = (JButton) component;
            button.setPreferredSize(new Dimension(48, CONTROL_HEIGHT));
            button.setBackground(new Color(241, 245, 249));
            button.setForeground(LABEL_TEXT);
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else if (component instanceof JSpinner) {
            styleSpinnerControl((JSpinner) component);
            return;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                styleFieldComponent(child);
            }
        }
    }

    private void styleSpinnerControl(JSpinner spinner) {
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        spinner.setPreferredSize(new Dimension(122, CONTROL_HEIGHT));
        spinner.setMinimumSize(new Dimension(108, CONTROL_HEIGHT));
        spinner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER, 1),
                new EmptyBorder(0, 0, 0, 0)));
        spinner.setBackground(FIELD_BG);
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.setFont(new Font("Segoe UI", Font.BOLD, 14));
            textField.setForeground(LABEL_TEXT);
            textField.setBackground(FIELD_BG);
            textField.setBorder(new EmptyBorder(9, 10, 9, 10));
        }
    }

    private void updateSaveEnabled() {
        boolean ok = cbFoyer.getItemCount() > 0 && cbLocataire.getItemCount() > 0;
        btnSave.setEnabled(ok);
        if (!ok) {
            lblMsg.setText("Aucun logement ou aucun locataire disponible pour créer une réservation.");
        }
    }

    private LocalDateTime getDateTimeFromPickers(ModernDatePicker datePicker, ModernTimePicker timePicker) {
        return LocalDateTime.of(datePicker.getDate(), timePicker.getTime());
    }

    private long nombreNuits(LocalDateTime debut, LocalDateTime fin) {
        return ChronoUnit.DAYS.between(debut.toLocalDate(), fin.toLocalDate());
    }

    private void enforceDateOrder() {
        LocalDate debut = dpDateDebut.getDate();
        LocalDate fin = dpDateFin.getDate();
        if (fin.isBefore(debut)) {
            dpDateFin.setDate(debut.plusDays(1));
            lblMsg.setText("La date de fin a été ajustée automatiquement après la date de début.");
        }
    }

    private void recalculateMontant() {
        lblMsg.setText(" ");
        FoyerListItem foyer = (FoyerListItem) cbFoyer.getSelectedItem();
        if (foyer == null) {
            lblMontant.setText("—");
            lblMontant.setForeground(ReservationPalette.FOOTER_TEXT);
            return;
        }
        try {
            int foyerId = foyer.getId();
            LocalDateTime debut = getDateTimeFromPickers(dpDateDebut, tpHeureDebut);
            LocalDateTime fin = getDateTimeFromPickers(dpDateFin, tpHeureFin);
            LocalDateTime maintenant = LocalDateTime.now();

            // Avertissement si date dans le passé
            if (debut.isBefore(maintenant)) {
                lblMontant.setText("—");
                lblMontant.setForeground(ReservationPalette.ERROR_TEXT);
                lblMsg.setText("⚠️ La date de début ne peut pas être dans le passé.");
                return;
            }

            if (fin.isBefore(maintenant)) {
                lblMontant.setText("—");
                lblMontant.setForeground(ReservationPalette.ERROR_TEXT);
                lblMsg.setText("⚠️ La date de fin ne peut pas être dans le passé.");
                return;
            }

            if (!fin.isAfter(debut)) {
                lblMontant.setText("—");
                lblMontant.setForeground(ReservationPalette.ERROR_TEXT);
                lblMsg.setText("⚠️ La date et l'heure de fin doivent être strictement après le début.");
                return;
            }

            long nuits = nombreNuits(debut, fin);
            if (nuits <= 0) {
                lblMontant.setText("—");
                lblMontant.setForeground(ReservationPalette.ERROR_TEXT);
                lblMsg.setText("⚠️ Le séjour doit couvrir au moins une nuit complète.");
                return;
            }

            Optional<BigDecimal> prixNuit = foyerService.getPrixParNuitParFoyerId(foyerId);
            if (prixNuit.isEmpty()) {
                lblMontant.setText("—");
                lblMontant.setForeground(ReservationPalette.ERROR_TEXT);
                lblMsg.setText("⚠️ Tarif du logement introuvable (prix par nuit absent en base pour ce logement).");
                return;
            }

            BigDecimal total = prixNuit.get().multiply(BigDecimal.valueOf(nuits)).setScale(2, RoundingMode.HALF_UP);
            lblMontant.setForeground(ReservationPalette.BTN_PRIMARY);
            lblMontant.setText(total + " EUR  (" + nuits + " nuit(s) × "
                    + prixNuit.get().setScale(2, RoundingMode.HALF_UP) + " EUR/nuit)");
        } catch (Exception ex) {
            lblMontant.setText("—");
            lblMontant.setForeground(ReservationPalette.FOOTER_TEXT);
            lblMsg.setText("Erreur lors du calcul du montant.");
        }
    }

    private void save() {
        lblMsg.setText(" ");
        
        // Validation 1: Vérifier que les listes ne sont pas vides
        if (cbFoyer.getItemCount() == 0 || cbLocataire.getItemCount() == 0) {
            lblMsg.setText("Impossible d'enregistrer : aucun logement ou aucun locataire disponible.");
            NotificationService.showError("Erreur de validation", "Impossible d'enregistrer : aucun logement ou aucun locataire disponible.");
            return;
        }
        
        // Validation 2: Vérifier les sélections
        FoyerListItem foyer = (FoyerListItem) cbFoyer.getSelectedItem();
        LocataireListItem loc = (LocataireListItem) cbLocataire.getSelectedItem();
        if (foyer == null || loc == null) {
            lblMsg.setText("Veuillez sélectionner un logement et un locataire.");
            NotificationService.showError("Erreur de validation", "Veuillez sélectionner un logement et un locataire.");
            return;
        }

        // Validation 3: Nombre de personnes
        String errNb = validerNombrePersonnes();
        if (errNb != null) {
            lblMsg.setText(errNb);
            NotificationService.showError("Erreur de validation", errNb);
            return;
        }
        int nbPersonnes;
        try {
            spNbPersonnes.commitEdit();
            nbPersonnes = (Integer) spNbPersonnes.getValue();
        } catch (ParseException e) {
            lblMsg.setText("Nombre de voyageurs : valeur invalide. Entrez un entier entre 1 et 20.");
            NotificationService.showError("Erreur de validation", "Nombre de voyageurs : valeur invalide. Entrez un entier entre 1 et 20.");
            return;
        }

        // Validation 4: Statut
        String statut = (String) cbStatut.getSelectedItem();
        if (statut == null || statut.isBlank()) {
            lblMsg.setText("Veuillez choisir un statut.");
            NotificationService.showError("Erreur de validation", "Veuillez choisir un statut.");
            return;
        }

        try {
            // Récupération des dates et heures depuis les pickers
            LocalDate dateDebut = dpDateDebut.getDate();
            LocalTime heureDebut = tpHeureDebut.getTime();
            LocalDate dateFin = dpDateFin.getDate();
            LocalTime heureFin = tpHeureFin.getTime();
            
            // Construction des LocalDateTime complets
            LocalDateTime debut = LocalDateTime.of(dateDebut, heureDebut);
            LocalDateTime fin = LocalDateTime.of(dateFin, heureFin);
            LocalDateTime maintenant = LocalDateTime.now();

            // ========== VALIDATION MÉTIER CRITIQUE ==========
            
            // Validation 5: La date de début ne peut PAS être dans le passé
            if (debut.isBefore(maintenant)) {
                lblMsg.setText("❌ La date de début ne peut pas être dans le passé.");
                NotificationService.showError("Erreur de validation", "La date de début ne peut pas être dans le passé.");
                return;
            }

            // Validation 6: La date de fin ne peut PAS être dans le passé
            if (fin.isBefore(maintenant)) {
                lblMsg.setText("❌ La date de fin ne peut pas être dans le passé.");
                NotificationService.showError("Erreur de validation", "La date de fin ne peut pas être dans le passé.");
                return;
            }

            // Validation 7: La date de fin doit être APRÈS la date de début
            if (!fin.isAfter(debut)) {
                lblMsg.setText("❌ La date et l'heure de fin doivent être strictement après le début.");
                NotificationService.showError("Erreur de validation", "La date et l'heure de fin doivent être strictement après le début.");
                return;
            }

            // Validation 8: Vérifier le nombre de nuits minimum
            long nuits = nombreNuits(debut, fin);
            if (nuits <= 0) {
                lblMsg.setText("❌ Le séjour doit couvrir au moins une nuit complète.");
                NotificationService.showError("Erreur de validation", "Le séjour doit couvrir au moins une nuit complète.");
                return;
            }

            // Validation 9: Vérifier que le prix existe
            Optional<BigDecimal> prixNuit = foyerService.getPrixParNuitParFoyerId(foyer.getId());
            if (prixNuit.isEmpty()) {
                lblMsg.setText("❌ Tarif du logement introuvable (prix par nuit absent en base).");
                NotificationService.showError("Erreur de validation", "Tarif du logement introuvable (prix par nuit absent en base).");
                return;
            }

            // Calcul du montant
            BigDecimal montant = prixNuit.get().multiply(BigDecimal.valueOf(nuits)).setScale(2, RoundingMode.HALF_UP);

            // ========== TOUTES LES VALIDATIONS SONT PASSÉES ==========
            // On peut maintenant enregistrer en base
            
            int reservationId = service.ajouterEtRetournerId(new Reservation(
                    foyer.getId(),
                    loc.getId(),
                    debut,
                    fin,
                    montant,
                    statut.trim(),
                    LocalDateTime.now(),
                    nbPersonnes
            ));
            
            if (reservationId > 0) {
                // Générer le PDF de facture
                PdfReservationService pdfService = new PdfReservationService();
                pdfService.genererFactureApresAjout(reservationId);
            }
            
            UiRefreshHub.notifyReservationChanged();
            NotificationService.showSuccess("Succès", "Réservation créée avec succès");
            dispose();
            
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.contains("foreign key")) {
                lblMsg.setText("❌ Enregistrement refusé : le logement ou le locataire n'est plus valide en base.");
                NotificationService.showError("Erreur", "Enregistrement refusé : le logement ou le locataire n'est plus valide en base.");
            } else {
                String errorMsg = "Erreur : " + (msg != null ? msg : ex.getClass().getSimpleName());
                lblMsg.setText("❌ " + errorMsg);
                NotificationService.showError("Erreur", errorMsg);
            }
        }
    }

    private String validerNombrePersonnes() {
        try {
            spNbPersonnes.commitEdit();
        } catch (ParseException e) {
            return "Nombre de voyageurs : saisie invalide. Entrez un entier entre 1 et 20.";
        }
        Object v = spNbPersonnes.getValue();
        if (!(v instanceof Integer)) {
            return "Nombre de voyageurs : valeur invalide.";
        }
        int n = (Integer) v;
        if (n < 1 || n > 20) {
            return "Le nombre de voyageurs doit être compris entre 1 et 20.";
        }
        return null;
    }

    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color background;

        RoundedPanel(LayoutManager layout, int radius, Color background) {
            super(layout);
            this.radius = radius;
            this.background = background;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(background);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class TopRoundedPanel extends RoundedPanel {
        private final int radius;
        private final Color background;

        TopRoundedPanel(LayoutManager layout, int radius, Color background) {
            super(layout, radius, background);
            this.radius = radius;
            this.background = background;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(background);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() + radius, radius, radius);
            g2.fillRect(0, radius, getWidth(), getHeight() - radius);
            g2.dispose();
        }
    }

    private static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int radius;
        private final int thickness;

        RoundedBorder(Color color, int radius, int thickness) {
            this.color = color;
            this.radius = radius;
            this.thickness = thickness;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            int offset = Math.max(1, thickness);
            g2.drawRoundRect(x + offset, y + offset, width - offset * 2 - 1, height - offset * 2 - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            int inset = thickness + 1;
            return new Insets(inset, inset, inset, inset);
        }
    }
}
