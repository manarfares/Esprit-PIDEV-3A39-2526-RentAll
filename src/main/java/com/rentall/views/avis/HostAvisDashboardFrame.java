package com.rentall.views.avis;

import com.rentall.dto.AvisDashboardRow;
import com.rentall.services.AvisDashboardService;
import com.rentall.services.SentimentAvisService;
import com.rentall.views.UiRefreshHub;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Dashboard des avis - Affiche les avis des guests sur les logements.
 * Version 1 : Affiche tous les avis (sans filtrage par host).
 * 
 * Fonctionnalités :
 * - Statistiques (total, moyenne, positifs/neutres/négatifs)
 * - Filtres par logement, note, sentiment
 * - Tableau moderne avec étoiles et sentiment
 */
public class HostAvisDashboardFrame extends JFrame {

    // Couleurs du thème
    private static final Color PAGE_BG = new Color(243, 246, 250);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color CARD_BORDER = new Color(220, 226, 235);
    private static final Color HEADER_DARK = new Color(30, 41, 59);
    private static final Color TEXT_DARK = new Color(30, 41, 59);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color STAT_POSITIVE = new Color(34, 197, 94);
    private static final Color STAT_NEUTRAL = new Color(249, 115, 22);
    private static final Color STAT_NEGATIVE = new Color(239, 68, 68);
    private static final Color STAT_TOTAL = new Color(59, 130, 246);
    private static final Color ROW_ALT = new Color(248, 250, 252);
    private static final Color SELECTION_BG = new Color(219, 234, 254);
    
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AvisDashboardService dashboardService = new AvisDashboardService();
    private final SentimentAvisService sentimentService = new SentimentAvisService();
    
    private List<AvisDashboardRow> allRows = new ArrayList<>();
    private List<AvisDashboardRow> filteredRows = new ArrayList<>();
    
    private final AvisTableModel tableModel = new AvisTableModel();
    private final JTable table = new JTable(tableModel);
    
    // Filtres
    private final JComboBox<String> cbLogement = new JComboBox<>();
    private final JComboBox<String> cbNote = new JComboBox<>();
    private final JComboBox<String> cbSentiment = new JComboBox<>();
    
    // Labels statistiques
    private final JLabel lblTotal = new JLabel("0");
    private final JLabel lblMoyenne = new JLabel("0.0");
    private final JLabel lblPositifs = new JLabel("0");
    private final JLabel lblNeutres = new JLabel("0");
    private final JLabel lblNegatifs = new JLabel("0");

    public HostAvisDashboardFrame(JFrame parent) {
        setTitle("Rentall - Dashboard des Avis");
        setSize(1200, 750);
        setMinimumSize(new Dimension(1000, 600));
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        buildUI();
        loadData();
        
        // S'abonner aux changements d'avis
        UiRefreshHub.registerAvisRefresh(refreshCallback);
    }
    
    private final Runnable refreshCallback = this::refreshData;
    
    @Override
    public void dispose() {
        UiRefreshHub.unregisterAvisRefresh(refreshCallback);
        super.dispose();
    }
    
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(PAGE_BG);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));
        
        // Header
        root.add(buildHeader(), BorderLayout.NORTH);
        
        // Centre : stats + filtres + tableau
        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);
        
        center.add(buildStatsPanel(), BorderLayout.NORTH);
        center.add(buildFiltersAndTable(), BorderLayout.CENTER);
        
        root.add(center, BorderLayout.CENTER);
        
        setContentPane(root);
    }
    
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_DARK);
        header.setBorder(new EmptyBorder(20, 28, 20, 28));
        
        JLabel title = new JLabel("Dashboard des Avis");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(Color.WHITE);
        
        JLabel subtitle = new JLabel("Vue d'ensemble des avis laisses par les guests sur les logements");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(new Color(148, 163, 184));
        
        JPanel titles = new JPanel(new GridLayout(2, 1, 4, 0));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(subtitle);
        
        // Boutons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        
        JButton btnRefresh = createButton("Actualiser", STAT_TOTAL, new Color(37, 99, 235));
        btnRefresh.addActionListener(e -> refreshData());
        
        JButton btnRetour = createButton("Retour", new Color(71, 85, 105), new Color(51, 65, 85));
        btnRetour.addActionListener(e -> dispose());
        
        btns.add(btnRefresh);
        btns.add(btnRetour);
        
        header.add(titles, BorderLayout.WEST);
        header.add(btns, BorderLayout.EAST);
        
        return header;
    }
    
    private JPanel buildStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 5, 16, 0));
        panel.setOpaque(false);
        
        panel.add(createStatCard("Total Avis", lblTotal, STAT_TOTAL, "[*]"));
        panel.add(createStatCard("Note Moyenne", lblMoyenne, STAT_TOTAL, "[*]"));
        panel.add(createStatCard("Positifs", lblPositifs, STAT_POSITIVE, "[+]"));
        panel.add(createStatCard("Neutres", lblNeutres, STAT_NEUTRAL, "[~]"));
        panel.add(createStatCard("Negatifs", lblNegatifs, STAT_NEGATIVE, "[-]"));
        
        return panel;
    }
    
    private JPanel createStatCard(String title, JLabel valueLabel, Color color, String icon) {
        JPanel card = new JPanel(new BorderLayout(8, 4));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1),
                new EmptyBorder(16, 20, 16, 20)
        ));
        
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTitle.setForeground(TEXT_MUTED);
        
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(color);
        
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(lblTitle, BorderLayout.WEST);
        
        card.add(top, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        
        // Bordure colorée à gauche
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        JPanel leftBorder = new JPanel();
        leftBorder.setBackground(color);
        leftBorder.setPreferredSize(new Dimension(4, 0));
        wrapper.add(leftBorder, BorderLayout.WEST);
        wrapper.add(card, BorderLayout.CENTER);
        
        return wrapper;
    }
    
    private JPanel buildFiltersAndTable() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        
        // Filtres
        JPanel filtersPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        filtersPanel.setBackground(CARD_BG);
        filtersPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1),
                new EmptyBorder(12, 16, 12, 16)
        ));
        
        JLabel lblFilter = new JLabel("Filtres :");
        lblFilter.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblFilter.setForeground(TEXT_DARK);
        filtersPanel.add(lblFilter);
        
        // Filtre logement
        filtersPanel.add(new JLabel("Logement :"));
        styleFilterCombo(cbLogement);
        cbLogement.addActionListener(e -> applyFilters());
        filtersPanel.add(cbLogement);
        
        // Filtre note
        filtersPanel.add(new JLabel("Note :"));
        cbNote.addItem("Toutes");
        for (int i = 1; i <= 5; i++) {
            cbNote.addItem(i + "/5");
        }
        styleFilterCombo(cbNote);
        cbNote.addActionListener(e -> applyFilters());
        filtersPanel.add(cbNote);
        
        // Filtre sentiment
        filtersPanel.add(new JLabel("Sentiment :"));
        cbSentiment.addItem("Tous");
        cbSentiment.addItem("Positif");
        cbSentiment.addItem("Neutre");
        cbSentiment.addItem("Negatif");
        styleFilterCombo(cbSentiment);
        cbSentiment.addActionListener(e -> applyFilters());
        filtersPanel.add(cbSentiment);
        
        // Bouton reset
        JButton btnReset = new JButton("Reinitialiser");
        btnReset.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnReset.addActionListener(e -> resetFilters());
        filtersPanel.add(btnReset);
        
        panel.add(filtersPanel, BorderLayout.NORTH);
        
        // Tableau
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(CARD_BG);
        tablePanel.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
        
        styleTable();
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(CARD_BG);
        tablePanel.add(scroll, BorderLayout.CENTER);
        
        panel.add(tablePanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void styleTable() {
        table.setRowHeight(48);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(TEXT_DARK);
        table.setBackground(CARD_BG);
        table.setSelectionBackground(SELECTION_BG);
        table.setSelectionForeground(TEXT_DARK);
        table.setGridColor(new Color(240, 240, 240));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        
        JTableHeader header = table.getTableHeader();
        header.setBackground(HEADER_DARK);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setPreferredSize(new Dimension(0, 44));
        header.setBorder(null);
        
        // Renderer personnalisé pour les colonnes
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);
        leftRenderer.setBorder(new EmptyBorder(0, 12, 0, 0));
        
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // ID
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // Reservation
        table.getColumnModel().getColumn(2).setCellRenderer(leftRenderer);   // Logement
        table.getColumnModel().getColumn(3).setCellRenderer(leftRenderer);   // Locataire
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Note
        table.getColumnModel().getColumn(5).setCellRenderer(leftRenderer);   // Commentaire
        table.getColumnModel().getColumn(6).setCellRenderer(centerRenderer); // Sentiment
        table.getColumnModel().getColumn(7).setCellRenderer(centerRenderer); // Date
        
        // Largeurs des colonnes
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(130);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(280);
        table.getColumnModel().getColumn(6).setPreferredWidth(120);
        table.getColumnModel().getColumn(7).setPreferredWidth(120);
    }
    
    private void styleFilterCombo(JComboBox<String> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setBackground(Color.WHITE);
        combo.setPreferredSize(new Dimension(120, 32));
    }
    
    private JButton createButton(String text, Color bg, Color hover) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setPreferredSize(new Dimension(110, 38));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(hover);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }
    
    private void loadData() {
        allRows = dashboardService.getAvisDashboard();
        filteredRows = new ArrayList<>(allRows);
        
        // Mettre à jour les filtres
        updateLogementFilter();
        
        // Mettre à jour les stats
        updateStats();
        
        // Rafraîchir le tableau
        tableModel.fireTableDataChanged();
    }
    
    private void refreshData() {
        loadData();
    }
    
    private void updateLogementFilter() {
        Set<String> logements = new TreeSet<>();
        for (AvisDashboardRow row : allRows) {
            logements.add(row.getLogementLibelle());
        }
        
        cbLogement.removeAllItems();
        cbLogement.addItem("Tous");
        for (String logement : logements) {
            cbLogement.addItem(logement);
        }
    }
    
    private void updateStats() {
        int total = filteredRows.size();
        double moyenne = 0.0;
        int positifs = 0;
        int neutres = 0;
        int negatifs = 0;
        
        if (total > 0) {
            int sum = 0;
            for (AvisDashboardRow row : filteredRows) {
                sum += row.getNote();
                SentimentAvisService.Sentiment sentiment = sentimentService.getSentiment(row.getNote());
                if (sentiment != null) {
                    switch (sentiment) {
                        case POSITIF: positifs++; break;
                        case NEUTRE: neutres++; break;
                        case NEGATIF: negatifs++; break;
                    }
                }
            }
            moyenne = (double) sum / total;
        }
        
        lblTotal.setText(String.valueOf(total));
        lblMoyenne.setText(String.format("%.1f", moyenne));
        lblPositifs.setText(String.valueOf(positifs));
        lblNeutres.setText(String.valueOf(neutres));
        lblNegatifs.setText(String.valueOf(negatifs));
    }
    
    private void applyFilters() {
        String logementFilter = (String) cbLogement.getSelectedItem();
        int noteFilter = cbNote.getSelectedIndex(); // 0 = Toutes, 1-5 = notes
        String sentimentFilter = (String) cbSentiment.getSelectedItem();
        
        filteredRows.clear();
        
        for (AvisDashboardRow row : allRows) {
            // Filtre logement
            if (logementFilter != null && !"Tous".equals(logementFilter) 
                    && !row.getLogementLibelle().equals(logementFilter)) {
                continue;
            }
            
            // Filtre note
            if (noteFilter > 0 && row.getNote() != noteFilter) {
                continue;
            }
            
            // Filtre sentiment
            if (!"Tous".equals(sentimentFilter)) {
                SentimentAvisService.Sentiment sentiment = sentimentService.getSentiment(row.getNote());
                if (sentiment == null) continue;
                
                String expected = sentiment.name();
                if (!sentimentFilter.equalsIgnoreCase(expected.substring(0, expected.length() - 1))) {
                    // Comparaison partielle (Positif vs POSITIF)
                    if (!sentimentFilter.equalsIgnoreCase(sentiment.getLibelle())) {
                        continue;
                    }
                }
            }
            
            filteredRows.add(row);
        }
        
        updateStats();
        tableModel.fireTableDataChanged();
    }
    
    private void resetFilters() {
        cbLogement.setSelectedIndex(0);
        cbNote.setSelectedIndex(0);
        cbSentiment.setSelectedIndex(0);
        filteredRows = new ArrayList<>(allRows);
        updateStats();
        tableModel.fireTableDataChanged();
    }
    
    // -------------------------------------------------------
    // Modèle de tableau
    // -------------------------------------------------------
    private class AvisTableModel extends AbstractTableModel {
        
        private final String[] COLS = {"ID", "Reservation", "Logement", "Locataire", "Note", "Commentaire", "Sentiment", "Date"};
        
        @Override
        public int getRowCount() {
            return filteredRows.size();
        }
        
        @Override
        public int getColumnCount() {
            return COLS.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return COLS[column];
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            AvisDashboardRow row = filteredRows.get(rowIndex);
            
            switch (columnIndex) {
                case 0: return row.getAvisId();
                case 1: return row.getReservationId();
                case 2: return row.getLogementLibelle();
                case 3: return row.getLocataireLibelle();
                case 4: return formatStars(row.getNote());
                case 5: return truncateComment(row.getCommentaire(), 50);
                case 6: return formatSentiment(row.getNote());
                case 7: return row.getDateCreation() != null ? row.getDateCreation().format(DATE_FMT) : "-";
                default: return "";
            }
        }
        
        private String formatStars(int note) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < 5; i++) {
                sb.append(i < note ? "*" : "-");
            }
            sb.append("] ").append(note).append("/5");
            return sb.toString();
        }
        
        private String formatSentiment(int note) {
            SentimentAvisService.Sentiment sentiment = sentimentService.getSentiment(note);
            if (sentiment == null) {
                return "-";
            }
            return "[" + sentiment.getLibelle().toUpperCase() + "]";
        }
        
        private String truncateComment(String comment, int maxLen) {
            if (comment == null) return "-";
            if (comment.length() <= maxLen) return comment;
            return comment.substring(0, maxLen) + "...";
        }
    }
}
