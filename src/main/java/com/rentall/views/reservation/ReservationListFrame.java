package com.rentall.views.reservation;

import com.rentall.dto.ReservationTableRow;
import com.rentall.services.MapsService;
import com.rentall.services.NotificationService;
import com.rentall.services.PdfReservationService;
import com.rentall.services.ReservationService;
import com.rentall.views.UiRefreshHub;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ReservationListFrame extends JFrame {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] COLS = {
            "ID", "Logement", "Locataire", "Arrivee", "Depart", "Montant total", "Statut", "Voyageurs"
    };
    private static final int COL_ID = 0;
    private static final int COL_MONTANT = 5;
    private static final int COL_VOYAGEURS = 7;
    private static final Color PAGE_BG = new Color(244, 247, 251);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color CARD_BORDER = new Color(214, 222, 232);
    private static final Color HEADER_BG = new Color(15, 23, 42);
    private static final Color HEADER_SUB = new Color(203, 213, 225);
    private static final Color TABLE_HEADER_BG = new Color(51, 65, 85);
    private static final Color TABLE_HEADER_FG = Color.WHITE;
    private static final Color TABLE_GRID = new Color(224, 231, 240);
    private static final Color ROW_ALT = new Color(248, 250, 252);
    private static final Color TEXT_DARK = new Color(15, 23, 42);
    private static final Color TEXT_MUTED = new Color(71, 85, 105);
    private static final Color SELECTION_BG = new Color(219, 234, 254);
    private static final Color SELECTION_FG = new Color(15, 23, 42);
    private static final Color BTN_MAPS = new Color(22, 163, 74);
    private static final Color BTN_MAPS_HOVER = new Color(21, 128, 61);
    private static final Color BTN_REFRESH = new Color(14, 116, 144);
    private static final Color BTN_REFRESH_HOVER = new Color(21, 94, 117);
    private static final Color BTN_BACK = new Color(71, 85, 105);
    private static final Color BTN_BACK_HOVER = new Color(51, 65, 85);

    private final ReservationService service = new ReservationService();
    private final MapsService mapsService = new MapsService();
    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel lblCount;
    private final Runnable refreshAction = this::load;
    private final List<Integer> reservationIds = new ArrayList<>();
    private final List<ReservationTableRow> allRows = new ArrayList<>();
    private final List<ReservationTableRow> filteredRows = new ArrayList<>();
    private final List<ReservationTableRow> visibleRows = new ArrayList<>();

    private final JTextField tfId = new JTextField();
    private final JTextField tfLogement = new JTextField();
    private final JTextField tfLocataire = new JTextField();
    private final JTextField tfDateDebut = new JTextField();
    private final JTextField tfDateFin = new JTextField();
    private final JComboBox<String> cbStatut = new JComboBox<>(new String[]{"Tous"});
    private final JComboBox<String> cbSort = new JComboBox<>(new String[]{
            "Date debut recente", "Date debut ancienne", "Date fin recente", "Date fin ancienne",
            "Montant croissant", "Montant decroissant"
    });
    private final JComboBox<Integer> cbPageSize = new JComboBox<>(new Integer[]{10, 20, 50, 100});
    private final JButton btnPrev = new JButton("Precedent");
    private final JButton btnNext = new JButton("Suivant");
    private final JLabel lblPage = new JLabel("Page 1/1");
    private int currentPage = 1;
    private boolean updatingStatusChoices;

    public ReservationListFrame(JFrame parent) {
        setTitle("Rentall - Reservations");
        setSize(1240, 720);
        setMinimumSize(new Dimension(980, 540));
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(PAGE_BG);
        root.add(buildHeader(), BorderLayout.NORTH);

        model = new DefaultTableModel(COLS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == COL_ID || columnIndex == COL_VOYAGEURS ? Integer.class : String.class;
            }
        };
        table = new JTable(model);
        configureTable();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getViewport().setBackground(CARD_BG);

        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(CARD_BG);
        tableCard.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
        tableCard.add(scroll, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));
        content.add(buildSearchPanel(), BorderLayout.NORTH);
        content.add(tableCard, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);

        lblCount = new JLabel(" ");
        lblCount.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblCount.setForeground(TEXT_MUTED);
        lblCount.setBorder(new EmptyBorder(14, 28, 16, 28));
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(true);
        footer.setBackground(CARD_BG);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, CARD_BORDER));
        footer.add(lblCount, BorderLayout.WEST);
        footer.add(buildPaginationPanel(), BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
        load();
    }

    private void configureTable() {
        table.setRowHeight(42);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(TABLE_GRID);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(SELECTION_BG);
        table.setSelectionForeground(SELECTION_FG);
        table.setFillsViewportHeight(true);
        table.setBackground(CARD_BG);
        table.setForeground(TEXT_DARK);

        DefaultTableCellRenderer cell = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, value, sel, foc, row, col);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                if (sel) {
                    setBackground(SELECTION_BG);
                    setForeground(SELECTION_FG);
                } else {
                    setBackground(row % 2 == 0 ? CARD_BG : ROW_ALT);
                    setForeground(TEXT_DARK);
                }
                if (col == COL_MONTANT) {
                    setHorizontalAlignment(SwingConstants.TRAILING);
                } else if (col == COL_ID || col == COL_VOYAGEURS) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setHorizontalAlignment(SwingConstants.LEADING);
                }
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, cell);
        table.setDefaultRenderer(Integer.class, cell);

        JTableHeader header = table.getTableHeader();
        header.setBackground(TABLE_HEADER_BG);
        header.setForeground(TABLE_HEADER_FG);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setPreferredSize(new Dimension(0, 44));
        header.setReorderingAllowed(false);
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(TABLE_HEADER_BG);
        headerRenderer.setForeground(TABLE_HEADER_FG);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 13));
        headerRenderer.setBorder(new EmptyBorder(0, 12, 0, 12));
        headerRenderer.setHorizontalAlignment(SwingConstants.LEADING);
        header.setDefaultRenderer(headerRenderer);

        int[] widths = {70, 220, 195, 145, 145, 135, 135, 95};
        for (int i = 0; i < widths.length && i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_BG);
        header.setBorder(new EmptyBorder(26, 32, 28, 32));
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 6));
        text.setOpaque(false);
        JLabel title = new JLabel("Reservations");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Recherche avancee, export PDF et pagination des sejours");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(HEADER_SUB);
        text.add(title);
        text.add(sub);
        header.add(text, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        JButton btnMaps = new JButton("Voir sur Maps");
        styleHeaderButton(btnMaps, BTN_MAPS, BTN_MAPS_HOVER);
        JButton btnPdf = new JButton("Exporter PDF");
        styleHeaderButton(btnPdf, ReservationPalette.BTN_BLUE, ReservationPalette.BTN_BLUE_PRESS);
        JButton refresh = new JButton("Actualiser");
        styleHeaderButton(refresh, BTN_REFRESH, BTN_REFRESH_HOVER);
        JButton back = new JButton("Retour");
        styleHeaderButton(back, BTN_BACK, BTN_BACK_HOVER);
        btnMaps.addActionListener(e -> ouvrirMapsPourSelection());
        btnPdf.addActionListener(e -> exporterPdfSelection());
        refresh.addActionListener(e -> load());
        back.addActionListener(e -> dispose());
        btns.add(btnMaps);
        btns.add(btnPdf);
        btns.add(refresh);
        btns.add(back);
        header.add(btns, BorderLayout.EAST);
        return header;
    }

    private JComponent buildSearchPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1),
                new EmptyBorder(14, 16, 14, 16)));

        styleFilterField(tfId, "ID");
        styleFilterField(tfLogement, "Logement");
        styleFilterField(tfLocataire, "Locataire");
        styleFilterField(tfDateDebut, "yyyy-MM-dd");
        styleFilterField(tfDateFin, "yyyy-MM-dd");
        styleFilterCombo(cbStatut, 130);
        styleFilterCombo(cbSort, 180);

        JButton btnSearch = new JButton("Rechercher");
        styleFilterButton(btnSearch, ReservationPalette.BTN_PRIMARY, ReservationPalette.BTN_PRIMARY_PRESS, Color.WHITE, 126);
        JButton btnClear = new JButton("Effacer");
        styleFilterButton(btnClear, new Color(226, 232, 240), new Color(203, 213, 225), TEXT_DARK, 100);

        btnSearch.addActionListener(e -> {
            currentPage = 1;
            applyFiltersAndRender();
        });
        btnClear.addActionListener(e -> clearFilters());
        cbStatut.addActionListener(e -> {
            if (!updatingStatusChoices) {
                currentPage = 1;
                applyFiltersAndRender();
            }
        });
        cbSort.addActionListener(e -> {
            currentPage = 1;
            applyFiltersAndRender();
        });
        tfId.addActionListener(e -> btnSearch.doClick());
        tfLogement.addActionListener(e -> btnSearch.doClick());
        tfLocataire.addActionListener(e -> btnSearch.doClick());
        tfDateDebut.addActionListener(e -> btnSearch.doClick());
        tfDateFin.addActionListener(e -> btnSearch.doClick());

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        addFilter(panel, gc, 0, 0, "ID reservation", tfId, 0.13);
        addFilter(panel, gc, 1, 0, "Logement", tfLogement, 0.23);
        addFilter(panel, gc, 2, 0, "Locataire", tfLocataire, 0.23);
        addFilter(panel, gc, 3, 0, "Statut", cbStatut, 0.14);
        addFilter(panel, gc, 0, 2, "Debut >=", tfDateDebut, 0.16);
        addFilter(panel, gc, 1, 2, "Fin <=", tfDateFin, 0.16);
        addFilter(panel, gc, 2, 2, "Tri", cbSort, 0.22);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(btnSearch);
        actions.add(btnClear);
        gc.gridx = 3;
        gc.gridy = 2;
        gc.gridheight = 2;
        gc.weightx = 0.22;
        gc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(actions, gc);

        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);
        outer.setBorder(new EmptyBorder(0, 0, 14, 0));
        outer.add(panel, BorderLayout.CENTER);
        return outer;
    }

    private void addFilter(JPanel panel, GridBagConstraints gc, int x, int y, String label, JComponent field, double weight) {
        gc.gridx = x;
        gc.gridy = y;
        gc.gridheight = 1;
        gc.weightx = weight;
        gc.fill = GridBagConstraints.HORIZONTAL;
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(TEXT_MUTED);
        panel.add(l, gc);

        gc.gridy = y + 1;
        panel.add(field, gc);
    }

    private JPanel buildPaginationPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        panel.setOpaque(false);
        JLabel perPage = new JLabel("Par page");
        perPage.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        perPage.setForeground(TEXT_MUTED);
        styleFilterCombo(cbPageSize, 70);
        lblPage.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPage.setForeground(TEXT_DARK);
        styleFilterButton(btnPrev, BTN_BACK, BTN_BACK_HOVER, Color.WHITE, 112);
        styleFilterButton(btnNext, ReservationPalette.BTN_PRIMARY, ReservationPalette.BTN_PRIMARY_PRESS, Color.WHITE, 96);

        btnPrev.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                renderCurrentPage();
            }
        });
        btnNext.addActionListener(e -> {
            if (currentPage < totalPages()) {
                currentPage++;
                renderCurrentPage();
            }
        });
        cbPageSize.addActionListener(e -> {
            currentPage = 1;
            renderCurrentPage();
        });

        panel.add(perPage);
        panel.add(cbPageSize);
        panel.add(btnPrev);
        panel.add(lblPage);
        panel.add(btnNext);
        return panel;
    }

    private void styleHeaderButton(JButton button, Color background, Color hover) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(background);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(130, 42));
        button.setBorder(new EmptyBorder(10, 18, 10, 18));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hover);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(background);
            }
        });
    }

    private void styleFilterField(JTextField field, String tooltip) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(TEXT_DARK);
        field.setBackground(Color.WHITE);
        field.setToolTipText(tooltip);
        field.setPreferredSize(new Dimension(120, 38));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                new EmptyBorder(8, 10, 8, 10)));
    }

    private void styleFilterCombo(JComboBox<?> combo, int width) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setForeground(TEXT_DARK);
        combo.setBackground(Color.WHITE);
        combo.setPreferredSize(new Dimension(width, 38));
        combo.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225), 1));
    }

    private void styleFilterButton(JButton button, Color background, Color hover, Color textColor, int width) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(textColor);
        button.setBackground(background);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(width, 38));
        button.setBorder(new EmptyBorder(8, 14, 8, 14));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hover);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(background);
            }
        });
    }

    private void ouvrirMapsPourSelection() {
        ReservationTableRow selected = selectedReservationSummary("Maps");
        if (selected != null) {
            mapsService.ouvrirMapsPourReservation(selected.getId());
        }
    }

    private void exporterPdfSelection() {
        ReservationTableRow selected = selectedReservationSummary("PDF");
        if (selected != null) {
            new PdfReservationService().genererReservationPdf(selected);
        }
    }

    private ReservationTableRow selectedReservationSummary(String action) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            NotificationService.showWarning(action, "Veuillez selectionner une reservation.");
            return null;
        }
        if (selectedRow >= visibleRows.size()) {
            NotificationService.showError("Erreur", "Erreur de selection.");
            return null;
        }
        return visibleRows.get(selectedRow);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        UiRefreshHub.registerReservationRefresh(refreshAction);
    }

    @Override
    public void removeNotify() {
        UiRefreshHub.unregisterReservationRefresh(refreshAction);
        super.removeNotify();
    }

    public void load() {
        allRows.clear();
        allRows.addAll(service.listerPourAffichageTableau());
        updateStatusChoices();
        currentPage = 1;
        applyFiltersAndRender();
    }

    private void clearFilters() {
        tfId.setText("");
        tfLogement.setText("");
        tfLocataire.setText("");
        tfDateDebut.setText("");
        tfDateFin.setText("");
        cbStatut.setSelectedIndex(0);
        cbSort.setSelectedIndex(0);
        currentPage = 1;
        applyFiltersAndRender();
    }

    private void updateStatusChoices() {
        String selected = (String) cbStatut.getSelectedItem();
        Set<String> statuses = new LinkedHashSet<>();
        for (ReservationTableRow row : allRows) {
            String statut = row.getStatut();
            if (statut != null && !statut.trim().isEmpty()) {
                statuses.add(statut.trim());
            }
        }
        updatingStatusChoices = true;
        cbStatut.removeAllItems();
        cbStatut.addItem("Tous");
        for (String statut : statuses) {
            cbStatut.addItem(statut);
        }
        if (selected != null) {
            cbStatut.setSelectedItem(selected);
            if (!selected.equals(cbStatut.getSelectedItem())) {
                cbStatut.setSelectedIndex(0);
            }
        }
        updatingStatusChoices = false;
    }

    private void applyFiltersAndRender() {
        filteredRows.clear();
        String idText = tfId.getText().trim();
        Integer idFilter = null;
        if (!idText.isEmpty()) {
            try {
                idFilter = Integer.parseInt(idText);
            } catch (NumberFormatException ex) {
                renderEmpty("ID reservation invalide.");
                return;
            }
        }

        LocalDate startFilter = parseDateFilter(tfDateDebut.getText().trim(), "Date debut invalide.");
        if (startFilter == null && !tfDateDebut.getText().trim().isEmpty()) {
            return;
        }
        LocalDate endFilter = parseDateFilter(tfDateFin.getText().trim(), "Date fin invalide.");
        if (endFilter == null && !tfDateFin.getText().trim().isEmpty()) {
            return;
        }

        String logement = normalize(tfLogement.getText());
        String locataire = normalize(tfLocataire.getText());
        String statut = (String) cbStatut.getSelectedItem();

        for (ReservationTableRow row : allRows) {
            if (idFilter != null && row.getId() != idFilter) {
                continue;
            }
            if (!logement.isEmpty() && !normalize(row.getFoyerLibelle()).contains(logement)) {
                continue;
            }
            if (!locataire.isEmpty() && !normalize(row.getLocataireLibelle()).contains(locataire)) {
                continue;
            }
            if (startFilter != null && row.getDateDebut().toLocalDate().isBefore(startFilter)) {
                continue;
            }
            if (endFilter != null && row.getDateFin().toLocalDate().isAfter(endFilter)) {
                continue;
            }
            if (statut != null && !"Tous".equals(statut)
                    && (row.getStatut() == null || !row.getStatut().equalsIgnoreCase(statut))) {
                continue;
            }
            filteredRows.add(row);
        }

        sortFilteredRows();
        renderCurrentPage();
    }

    private LocalDate parseDateFilter(String value, String errorMessage) {
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            renderEmpty(errorMessage + " Format attendu : yyyy-MM-dd.");
            return null;
        }
    }

    private void sortFilteredRows() {
        String sort = (String) cbSort.getSelectedItem();
        Comparator<ReservationTableRow> byIdDesc = Comparator.comparingInt(ReservationTableRow::getId).reversed();
        Comparator<ReservationTableRow> comparator;
        if ("Date debut ancienne".equals(sort)) {
            comparator = Comparator.comparing(ReservationTableRow::getDateDebut).thenComparing(byIdDesc);
        } else if ("Date fin recente".equals(sort)) {
            comparator = Comparator.comparing(ReservationTableRow::getDateFin).reversed().thenComparing(byIdDesc);
        } else if ("Date fin ancienne".equals(sort)) {
            comparator = Comparator.comparing(ReservationTableRow::getDateFin).thenComparing(byIdDesc);
        } else if ("Montant croissant".equals(sort)) {
            comparator = this::compareMontantCroissant;
        } else if ("Montant decroissant".equals(sort)) {
            comparator = this::compareMontantDecroissant;
        } else {
            comparator = Comparator.comparing(ReservationTableRow::getDateDebut).reversed().thenComparing(byIdDesc);
        }
        filteredRows.sort(comparator);
    }

    private int compareMontantCroissant(ReservationTableRow a, ReservationTableRow b) {
        return compareMontant(a.getMontantTotal(), b.getMontantTotal());
    }

    private int compareMontantDecroissant(ReservationTableRow a, ReservationTableRow b) {
        BigDecimal left = a.getMontantTotal();
        BigDecimal right = b.getMontantTotal();
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return right.compareTo(left);
    }

    private int compareMontant(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        return a.compareTo(b);
    }

    private void renderCurrentPage() {
        model.setRowCount(0);
        reservationIds.clear();
        visibleRows.clear();

        int pageSize = selectedPageSize();
        int totalPages = totalPages();
        currentPage = Math.max(1, Math.min(currentPage, totalPages));
        int from = Math.min((currentPage - 1) * pageSize, filteredRows.size());
        int to = Math.min(from + pageSize, filteredRows.size());

        for (int i = from; i < to; i++) {
            ReservationTableRow row = filteredRows.get(i);
            visibleRows.add(row);
            reservationIds.add(row.getId());
            model.addRow(new Object[]{
                    row.getId(),
                    row.getFoyerLibelle(),
                    row.getLocataireLibelle(),
                    row.getDateDebut().format(FMT),
                    row.getDateFin().format(FMT),
                    ReservationUiFormat.montantAffichage(row.getMontantTotal()),
                    ReservationUiFormat.statutAffichage(row.getStatut()),
                    row.getNombrePersonnes()
            });
        }

        lblCount.setText(filteredRows.size() + " reservation(s) affichee(s) sur " + allRows.size());
        lblPage.setText("Page " + currentPage + "/" + totalPages);
        btnPrev.setEnabled(currentPage > 1);
        btnNext.setEnabled(currentPage < totalPages);
    }

    private void renderEmpty(String message) {
        model.setRowCount(0);
        reservationIds.clear();
        visibleRows.clear();
        filteredRows.clear();
        lblCount.setText(message);
        lblPage.setText("Page 1/1");
        btnPrev.setEnabled(false);
        btnNext.setEnabled(false);
    }

    private int totalPages() {
        return Math.max(1, (int) Math.ceil(filteredRows.size() / (double) selectedPageSize()));
    }

    private int selectedPageSize() {
        Integer value = (Integer) cbPageSize.getSelectedItem();
        return value != null ? value : 10;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
