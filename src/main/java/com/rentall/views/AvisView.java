package com.rentall.views;

import com.rentall.entities.Avis;
import com.rentall.services.AvisService;
import com.rentall.views.avis.AddAvisForm;
import com.rentall.views.avis.AvisPalette;
import com.rentall.views.avis.DeleteAvisDialog;
import com.rentall.views.avis.EditAvisForm;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AvisView extends JPanel {

    private final AvisService service = new AvisService();
    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel lblCount;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] COLS = {"ID","Reservation ID","Note","Commentaire","Date Creation"};

    public AvisView() {
        setLayout(new BorderLayout(0, 0));
        setBackground(Color.WHITE);

        add(buildToolbar(), BorderLayout.NORTH);

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
        table.setSelectionBackground(new Color(232, 244, 253));
        table.setSelectionForeground(Color.BLACK);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
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
        ((DefaultTableCellRenderer) h.getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);

        table.getColumnModel().getColumn(0).setPreferredWidth(45);
        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.getColumnModel().getColumn(2).setPreferredWidth(55);
        table.getColumnModel().getColumn(3).setPreferredWidth(550);
        table.getColumnModel().getColumn(4).setPreferredWidth(140);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);

        lblCount = new JLabel("Chargement...");
        lblCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCount.setForeground(new Color(120, 120, 120));
        lblCount.setBorder(new EmptyBorder(8, 15, 8, 15));
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(248, 249, 252));
        footer.setBorder(new MatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));
        footer.add(lblCount, BorderLayout.WEST);
        add(footer, BorderLayout.SOUTH);

        load();
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(new MatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));

        JLabel sectionTitle = new JLabel("  Liste des Avis");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        sectionTitle.setForeground(new Color(44, 62, 80));
        sectionTitle.setBorder(new EmptyBorder(12, 10, 12, 0));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        btnPanel.setBackground(Color.WHITE);

        JButton bR = makeBtn("Actualiser", new Color(52,152,219),  "↻");
        JButton bA = makeBtn("Ajouter",    new Color(39,174,96),   "+");
        JButton bM = makeBtn("Modifier",   new Color(243,156,18),  "✎");
        JButton bD = makeBtn("Supprimer",  new Color(231,76,60),   "✕");

        btnPanel.add(bR); btnPanel.add(bA); btnPanel.add(bM); btnPanel.add(bD);

        toolbar.add(sectionTitle, BorderLayout.WEST);
        toolbar.add(btnPanel, BorderLayout.EAST);

        bR.addActionListener(e -> load());
        bA.addActionListener(e -> {
            AddAvisForm f = new AddAvisForm(getParentFrame(), service);
            f.setVisible(true);
            if (f.isSuccess()) load();
        });
        bM.addActionListener(e -> {
            Avis a = getSelected(); if (a == null) return;
            EditAvisForm f = new EditAvisForm(getParentFrame(), service, a);
            f.setVisible(true);
            if (f.isSuccess()) load();
        });
        bD.addActionListener(e -> {
            Avis a = getSelected(); if (a == null) return;
            DeleteAvisDialog d = new DeleteAvisDialog(getParentFrame(), service, a);
            d.setVisible(true);
            if (d.isSuccess()) load();
        });

        return toolbar;
    }

    private void load() {
        model.setRowCount(0);
        List<Avis> list = service.afficherTous();
        for (Avis a : list) {
            model.addRow(new Object[]{
                a.getId(), a.getReservationId(), AvisPalette.noteEtoiles(a.getNote()),
                a.getCommentaire(), a.getDateCreation().format(FMT)
            });
        }
        lblCount.setText("  " + list.size() + " avis trouve(s)");
    }

    private Avis getSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                "Veuillez selectionner un avis dans le tableau.",
                "Aucune selection", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return service.afficherParId((int) model.getValueAt(row, 0));
    }

    private Frame getParentFrame() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }

    private JButton makeBtn(String label, Color color, String icon) {
        JButton b = new JButton(icon + "  " + label);
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        return b;
    }
}
