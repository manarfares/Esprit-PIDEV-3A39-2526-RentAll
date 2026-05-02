package com.rentall.views;

import com.rentall.entities.Reservation;
import com.rentall.services.ReservationService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReservationView extends JPanel {

    private final ReservationService service = new ReservationService();
    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel lblCount;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] COLS = {"ID","Foyer ID","Locataire ID","Date Debut","Date Fin","Montant","Statut","Personnes"};

    public ReservationView() {
        setLayout(new BorderLayout(0, 0));
        setBackground(Color.WHITE);

        // Header avec titre
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 23, 42));
        header.setBorder(new EmptyBorder(20, 24, 20, 24));
        
        JLabel lblTitle = new JLabel("Gestion des Réservations");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(248, 250, 252));
        header.add(lblTitle, BorderLayout.WEST);
        
        // Boutons d'action dans le header
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        JButton bR = makeBtn("↻ Actualiser", new Color(71, 85, 105));
        JButton bA = makeBtn("+ Ajouter",  new Color(34, 197, 94));
        JButton bM = makeBtn("✎ Modifier",   new Color(217, 119, 6));
        JButton bD = makeBtn("✕ Supprimer",  new Color(220, 38, 38));
        btnPanel.add(bR); btnPanel.add(bA); btnPanel.add(bM); btnPanel.add(bD);
        header.add(btnPanel, BorderLayout.EAST);
        
        add(header, BorderLayout.NORTH);

        // Conteneur pour la table
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(16, 20, 0, 20));
        
        model = new DefaultTableModel(COLS, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(34);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(226, 232, 240));
        table.setSelectionBackground(new Color(191, 219, 254));
        table.setSelectionForeground(new Color(30, 41, 59));
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        
        // Header du tableau
        JTableHeader h = table.getTableHeader();
        h.setBackground(new Color(51, 65, 85));
        h.setForeground(new Color(248, 250, 252));
        h.setFont(new Font("Segoe UI", Font.BOLD, 13));
        h.setPreferredSize(new Dimension(0, 36));
        h.setReorderingAllowed(false);
        
        // Largeurs des colonnes
        int[] w = {40,70,90,135,135,100,100,80};
        for (int i = 0; i < w.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        }
        
        // Renderer pour zebra striping
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                setBorder(new EmptyBorder(0, 12, 0, 12));
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(241, 245, 249));
        footer.setBorder(new EmptyBorder(10, 24, 10, 24));
        
        lblCount = new JLabel("...");
        lblCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCount.setForeground(new Color(100, 116, 139));
        footer.add(lblCount, BorderLayout.WEST);
        
        add(footer, BorderLayout.SOUTH);

        bR.addActionListener(e -> load());
        bA.addActionListener(e -> { doAdd();  load(); });
        bM.addActionListener(e -> { doEdit(); load(); });
        bD.addActionListener(e -> { doDel();  load(); });
        load();
    }

    private void load() {
        model.setRowCount(0);
        List<Reservation> list = service.afficherTous();
        for (Reservation r : list) {
            model.addRow(new Object[]{
                    r.getId(), r.getFoyerId(), r.getLocataireId(),
                    r.getDateDebut().format(FMT), r.getDateFin().format(FMT),
                    r.getMontantTotal()+" EUR", r.getStatut(), r.getNombrePersonnes()
            });
        }
        lblCount.setText("Total : " + list.size() + " reservation(s)");
    }

    private int selectedId() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this,"Selectionnez une ligne.","Attention",JOptionPane.WARNING_MESSAGE);
            return -1;
        }
        return (int) model.getValueAt(row, 0);
    }

    private void doAdd() {
        JTextField fId = new JTextField("36");
        JTextField lId = new JTextField("32");
        JTextField dd  = new JTextField("2026-09-01T14:00");
        JTextField df  = new JTextField("2026-09-10T11:00");
        JTextField mt  = new JTextField("1200.00");
        JTextField st  = new JTextField("en_attente");
        JTextField nb  = new JTextField("2");
        JPanel p = new JPanel(new GridLayout(0,2,6,6));
        p.add(new JLabel("Foyer ID (36,37,38,39,40):")); p.add(fId);
        p.add(new JLabel("Locataire ID (32,33,35):"));   p.add(lId);
        p.add(new JLabel("Date debut yyyy-MM-ddTHH:mm:")); p.add(dd);
        p.add(new JLabel("Date fin   yyyy-MM-ddTHH:mm:")); p.add(df);
        p.add(new JLabel("Montant:")); p.add(mt);
        p.add(new JLabel("Statut:"));  p.add(st);
        p.add(new JLabel("Nb personnes:")); p.add(nb);
        if (JOptionPane.showConfirmDialog(this, p, "Ajouter", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                service.ajouter(new Reservation(
                        Integer.parseInt(fId.getText().trim()), Integer.parseInt(lId.getText().trim()),
                        LocalDateTime.parse(dd.getText().trim()), LocalDateTime.parse(df.getText().trim()),
                        new BigDecimal(mt.getText().trim()), st.getText().trim(),
                        LocalDateTime.now(), Integer.parseInt(nb.getText().trim())
                ));
                JOptionPane.showMessageDialog(this,"Reservation ajoutee !","OK",JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,"Erreur : "+ex.getMessage(),"Erreur",JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void doEdit() {
        int id = selectedId(); if (id == -1) return;
        Reservation r = service.afficherParId(id); if (r == null) return;
        JTextField st = new JTextField(r.getStatut());
        JTextField mt = new JTextField(r.getMontantTotal().toString());
        JTextField nb = new JTextField(String.valueOf(r.getNombrePersonnes()));
        JPanel p = new JPanel(new GridLayout(0,2,6,6));
        p.add(new JLabel("Statut:"));  p.add(st);
        p.add(new JLabel("Montant:")); p.add(mt);
        p.add(new JLabel("Nb pers:")); p.add(nb);
        if (JOptionPane.showConfirmDialog(this, p, "Modifier id="+id, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                r.setStatut(st.getText().trim());
                r.setMontantTotal(new BigDecimal(mt.getText().trim()));
                r.setNombrePersonnes(Integer.parseInt(nb.getText().trim()));
                service.modifier(r);
                JOptionPane.showMessageDialog(this,"Modifie !","OK",JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,"Erreur : "+ex.getMessage(),"Erreur",JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void doDel() {
        int id = selectedId(); if (id == -1) return;
        if (JOptionPane.showConfirmDialog(this,"Supprimer id="+id+" ?","Confirmation",JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            service.supprimer(id);
            JOptionPane.showMessageDialog(this,"Supprime.","OK",JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private JButton makeBtn(String t, Color c) {
        JButton b = new JButton(t);
        b.setBackground(c);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(c, 0),
            new EmptyBorder(8, 16, 8, 16)
        ));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        
        // Effet hover
        Color hoverColor = c.darker();
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                b.setBackground(hoverColor);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                b.setBackground(c);
            }
        });
        
        return b;
    }
}
