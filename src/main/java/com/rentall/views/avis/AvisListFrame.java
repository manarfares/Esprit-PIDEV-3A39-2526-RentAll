package com.rentall.views.avis;

import com.rentall.entities.Avis;
import com.rentall.services.AvisService;
import com.rentall.views.UiRefreshHub;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Affiche SEULEMENT la liste des avis.
 * Pas de boutons CRUD — seulement Actualiser et Retour.
 */
public class AvisListFrame extends JFrame {

    private final AvisService service = new AvisService();
    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel lblCount;
    private final JLabel lblPage = new JLabel("Page 1");
    private final JComboBox<Integer> cbPageSize = new JComboBox<>(new Integer[]{10, 20, 50, 100});
    private final JButton btnPrev = AvisPalette.createSecondaryButton("Precedent");
    private final JButton btnNext = AvisPalette.createPrimaryActionButton("Suivant");
    private final List<Avis> allAvis = new ArrayList<>();
    private int currentPage = 1;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] COLS = {"ID","Reservation ID","Note","Commentaire","Date Creation"};

    private final Runnable refreshAction = this::load;

    public AvisListFrame(JFrame parent) {
        setTitle("Rentall - Liste des Avis");
        setSize(1000, 580);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // ── HEADER ──
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(52, 152, 219));
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        JPanel headerText = new JPanel(new GridLayout(2, 1, 0, 3));
        headerText.setOpaque(false);
        JLabel title = new JLabel("Liste des Avis");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Affichage de tous les avis enregistres");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(210, 235, 255));
        headerText.add(title); headerText.add(sub);
        header.add(headerText, BorderLayout.WEST);

        JPanel headerBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerBtns.setOpaque(false);
        JButton btnRef  = AvisPalette.createPrimaryActionButton("↻ Actualiser");
        JButton btnBack = AvisPalette.createSecondaryButton("← Retour");
        btnRef.addActionListener(e -> load());
        btnBack.addActionListener(e -> dispose());
        headerBtns.add(btnRef); headerBtns.add(btnBack);
        header.add(headerBtns, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // ── TABLEAU ──
        model = new DefaultTableModel(COLS, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(32);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(235, 237, 240));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(210, 235, 255));
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                if (!sel) setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 252));
                return this;
            }
        });
        JTableHeader h = table.getTableHeader();
        h.setBackground(new Color(44, 62, 80));
        h.setForeground(Color.WHITE);
        h.setFont(new Font("Segoe UI", Font.BOLD, 13));
        h.setPreferredSize(new Dimension(0, 38));
        table.getColumnModel().getColumn(0).setPreferredWidth(45);
        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.getColumnModel().getColumn(2).setPreferredWidth(135);
        table.getColumnModel().getColumn(3).setPreferredWidth(550);
        table.getColumnModel().getColumn(4).setPreferredWidth(140);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        root.add(scroll, BorderLayout.CENTER);

        // ── FOOTER ──
        lblCount = new JLabel("  Chargement...");
        lblCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCount.setForeground(new Color(120, 120, 120));
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(248, 249, 252));
        footer.setBorder(new MatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));
        footer.add(lblCount, BorderLayout.WEST);
        footer.add(buildPaginationControls(), BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
        load();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        UiRefreshHub.registerAvisRefresh(refreshAction);
    }

    @Override
    public void removeNotify() {
        UiRefreshHub.unregisterAvisRefresh(refreshAction);
        super.removeNotify();
    }

    public void load() {
        allAvis.clear();
        allAvis.addAll(service.afficherTous());
        currentPage = 1;
        renderPage();
    }

    private void renderPage() {
        model.setRowCount(0);
        int pageSize = selectedPageSize();
        int totalPages = Math.max(1, (int) Math.ceil(allAvis.size() / (double) pageSize));
        currentPage = Math.max(1, Math.min(currentPage, totalPages));
        int from = Math.min((currentPage - 1) * pageSize, allAvis.size());
        int to = Math.min(from + pageSize, allAvis.size());
        for (int i = from; i < to; i++) {
            Avis a = allAvis.get(i);
            model.addRow(new Object[]{
                a.getId(), a.getReservationId(), AvisPalette.noteEtoiles(a.getNote()),
                a.getCommentaire(), a.getDateCreation().format(FMT)
            });
        }
        lblCount.setText("  " + allAvis.size() + " avis trouve(s)");
        lblPage.setText("Page " + currentPage + "/" + totalPages);
        btnPrev.setEnabled(currentPage > 1);
        btnNext.setEnabled(currentPage < totalPages);
    }

    private JPanel buildPaginationControls() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        panel.setOpaque(false);

        JLabel pageSizeLabel = new JLabel("Par page");
        pageSizeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pageSizeLabel.setForeground(new Color(71, 85, 105));
        cbPageSize.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbPageSize.setPreferredSize(new Dimension(70, 32));
        lblPage.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPage.setForeground(new Color(44, 62, 80));

        btnPrev.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                renderPage();
            }
        });
        btnNext.addActionListener(e -> {
            int totalPages = Math.max(1, (int) Math.ceil(allAvis.size() / (double) selectedPageSize()));
            if (currentPage < totalPages) {
                currentPage++;
                renderPage();
            }
        });
        cbPageSize.addActionListener(e -> {
            currentPage = 1;
            renderPage();
        });

        panel.add(pageSizeLabel);
        panel.add(cbPageSize);
        panel.add(btnPrev);
        panel.add(lblPage);
        panel.add(btnNext);
        return panel;
    }

    private int selectedPageSize() {
        Integer value = (Integer) cbPageSize.getSelectedItem();
        return value != null ? value : 10;
    }
}
