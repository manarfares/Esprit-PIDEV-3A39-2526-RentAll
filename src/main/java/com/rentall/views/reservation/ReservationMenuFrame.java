package com.rentall.views.reservation;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Menu du module Réservation : navigation vers des écrans dédiés.
 */
public class ReservationMenuFrame extends JFrame {

    public ReservationMenuFrame(JFrame parent) {
        setTitle("Rentall — Réservations");
        setSize(520, 560);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(440, 480));
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ReservationPalette.BG_APP);

        root.add(buildHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(8, 36, 28, 36));

        center.add(Box.createVerticalStrut(8));
        center.add(menuCard("Consulter", "Liste des séjours avec logement et locataire",
                ReservationPalette.BTN_PRIMARY, ReservationPalette.BTN_PRIMARY_PRESS,
                () -> new ReservationListFrame(this).setVisible(true)));
        center.add(Box.createVerticalStrut(12));
        center.add(menuCard("Créer", "Nouvelle réservation",
                ReservationPalette.BTN_BLUE, ReservationPalette.BTN_BLUE_PRESS,
                () -> new AddReservationFrame(this).setVisible(true)));
        center.add(Box.createVerticalStrut(12));
        center.add(menuCard("Modifier", "Mettre à jour statut, montant ou nombre de personnes",
                ReservationPalette.ACCENT_EDIT, ReservationPalette.ACCENT_EDIT_PRESS,
                () -> new EditReservationFrame(this).setVisible(true)));
        center.add(Box.createVerticalStrut(12));
        center.add(menuCard("Supprimer", "Retirer une réservation de la base",
                ReservationPalette.ACCENT_DELETE, ReservationPalette.ACCENT_DELETE_PRESS,
                () -> new DeleteReservationFrame(this).setVisible(true)));
        center.add(Box.createVerticalGlue());

        root.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(0, 36, 24, 36));
        JButton back = outlineBtn("← Menu principal");
        back.addActionListener(e -> dispose());
        bottom.add(back);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JComponent buildHeader() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(ReservationPalette.BG_HEADER);
        wrap.setBorder(new EmptyBorder(26, 32, 22, 32));

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Réservations", SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(ReservationPalette.TEXT_TITLE);

        JLabel sub = new JLabel("Choisissez une action", SwingConstants.CENTER);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(ReservationPalette.TEXT_SUB_MENU);
        sub.setBorder(new EmptyBorder(8, 0, 0, 0));

        inner.add(title);
        inner.add(sub);
        wrap.add(inner, BorderLayout.CENTER);
        return wrap;
    }

    private JComponent menuCard(String badge, String description, Color btnColor, Color btnPressed, Runnable action) {
        RoundedPanel card = new RoundedPanel(ReservationPalette.MENU_CARD_RADIUS, ReservationPalette.MENU_CARD_BG);
        card.setLayout(new BorderLayout(12, 10));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        card.setMaximumSize(new Dimension(Short.MAX_VALUE, 92));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel b = new JLabel(badge.toUpperCase());
        b.setFont(new Font("Segoe UI", Font.BOLD, 11));
        b.setForeground(new Color(100, 116, 139));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel desc = new JLabel("<html><div style='width:300px'>" + description + "</div></html>");
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        desc.setForeground(new Color(51, 65, 85));
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        desc.setBorder(new EmptyBorder(6, 0, 0, 0));

        left.add(b);
        left.add(desc);

        JButton go = new JButton("Ouvrir");
        ReservationPalette.stylePrimaryButton(go, btnColor, btnPressed);
        go.addActionListener(e -> action.run());

        card.add(left, BorderLayout.CENTER);
        card.add(go, BorderLayout.EAST);

        return card;
    }

    private JButton outlineBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(ReservationPalette.TEXT_SUB_MENU);
        b.setBackground(ReservationPalette.BTN_NEUTRAL);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setBorder(new EmptyBorder(10, 22, 10, 22));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addChangeListener(ev -> {
            if (b.getModel().isPressed()) {
                b.setBackground(ReservationPalette.BTN_NEUTRAL_PRESS);
            } else {
                b.setBackground(ReservationPalette.BTN_NEUTRAL);
            }
        });
        return b;
    }

    private static final class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fill;

        RoundedPanel(int radius, Color fill) {
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, radius, radius));
            g2.setColor(ReservationPalette.MENU_CARD_BORDER);
            g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, radius, radius));
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
