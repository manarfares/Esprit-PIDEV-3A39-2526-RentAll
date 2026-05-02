package com.rentall.views;

import com.rentall.views.avis.AvisMenuFrame;
import com.rentall.views.reservation.ReservationMenuFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Fenêtre d'accueil : accès aux modules Reservations et Avis.
 * Design centré sur des cartes (cards) avec navigation inchangée.
 */
public class MainFrame extends JFrame {

    /* --- Palette (slate / bleu ardoise) --- */
    private static final Color BG_APP = new Color(30, 41, 59);
    private static final Color BG_HEADER = new Color(15, 23, 42);
    private static final Color TEXT_TITLE = new Color(248, 250, 252);
    private static final Color TEXT_SUB = new Color(203, 213, 225);
    private static final Color TEXT_MUTED = new Color(148, 163, 184);
    private static final Color SEPARATOR = new Color(51, 65, 85);
    private static final Color CARD_BG = new Color(241, 245, 249);
    private static final Color CARD_TITLE = new Color(30, 41, 59);
    private static final Color CARD_DESC = new Color(71, 85, 105);
    private static final Color BTN_RES = new Color(5, 150, 105);
    private static final Color BTN_RES_HOVER = new Color(4, 120, 87);
    private static final Color BTN_AVIS = new Color(37, 99, 235);
    private static final Color BTN_AVIS_HOVER = new Color(29, 78, 216);
    private static final int CARD_RADIUS = 18;

    public MainFrame() {
        setTitle("Rentall - Location de Maisons");
        setSize(780, 560);
        setMinimumSize(new Dimension(640, 480));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_APP);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildCenter(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JComponent buildHeader() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(BG_HEADER);
        wrap.setBorder(new EmptyBorder(28, 40, 22, 40));

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("RENTALL", SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 40));
        title.setForeground(TEXT_TITLE);

        JLabel sub = new JLabel("Application de Location de Maisons", SwingConstants.CENTER);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        sub.setForeground(TEXT_SUB);
        sub.setBorder(new EmptyBorder(10, 0, 0, 0));

        JSeparator line = new JSeparator(SwingConstants.HORIZONTAL);
        line.setAlignmentX(Component.CENTER_ALIGNMENT);
        line.setForeground(SEPARATOR);
        line.setMaximumSize(new Dimension(280, 8));
        line.setBorder(new EmptyBorder(18, 0, 0, 0));

        inner.add(title);
        inner.add(sub);
        inner.add(line);

        wrap.add(inner, BorderLayout.CENTER);
        return wrap;
    }

    private JComponent buildCenter() {
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(BG_APP);
        center.setBorder(new EmptyBorder(8, 48, 28, 48));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.weightx = 1;
        gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.insets = new Insets(12, 10, 12, 10);

        gc.gridx = 0;
        JPanel cardRes = createModuleCard(
                "\uD83C\uDFE0",
                "Reservations",
                "<html><body style='width:220px'>"
                        + "Gérer les réservations : consultation, création, "
                        + "modification et suppression des séjours."
                        + "</body></html>",
                BTN_RES,
                BTN_RES_HOVER,
                () -> new ReservationMenuFrame(MainFrame.this).setVisible(true)
        );
        center.add(cardRes, gc);

        gc.gridx = 1;
        JPanel cardAvis = createModuleCard(
                "\u2B50",
                "Avis",
                "<html><body style='width:220px'>"
                        + "Consulter et administrer les avis des locataires "
                        + "liés aux réservations."
                        + "</body></html>",
                BTN_AVIS,
                BTN_AVIS_HOVER,
                () -> new AvisMenuFrame(MainFrame.this).setVisible(true)
        );
        center.add(cardAvis, gc);

        return center;
    }

    /**
     * Carte module : icône, titre, description, bouton « Ouvrir ».
     */
    private JPanel createModuleCard(String iconText, String heading, String descriptionHtml,
                                    Color btnColor, Color btnPressColor, Runnable onOpen) {
        RoundedPanel card = new RoundedPanel(CARD_RADIUS, CARD_BG);
        card.setLayout(new BorderLayout(0, 0));
        card.setBorder(new EmptyBorder(24, 26, 22, 26));

        JPanel top = new JPanel(new BorderLayout(16, 0));
        top.setOpaque(false);

        JLabel icon = new JLabel(iconText);
        icon.setFont(resolveEmojiFont(42f));
        icon.setForeground(CARD_TITLE);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        JLabel h = new JLabel(heading);
        h.setFont(new Font("Segoe UI", Font.BOLD, 20));
        h.setForeground(CARD_TITLE);

        JLabel desc = new JLabel(descriptionHtml);
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        desc.setForeground(CARD_DESC);
        desc.setBorder(new EmptyBorder(14, 0, 0, 0));

        titles.add(h);
        top.add(icon, BorderLayout.WEST);
        top.add(titles, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        south.setOpaque(false);
        south.setBorder(new EmptyBorder(20, 0, 0, 0));

        JButton open = new JButton("Ouvrir");
        stylePrimaryButton(open, btnColor, btnPressColor);
        open.addActionListener(e -> onOpen.run());
        south.add(open);

        card.add(top, BorderLayout.NORTH);
        card.add(desc, BorderLayout.CENTER);
        card.add(south, BorderLayout.SOUTH);

        return card;
    }

    private static Font resolveEmojiFont(float size) {
        String[] families = { "Segoe UI Emoji", "Segoe UI Symbol", Font.SANS_SERIF };
        for (String fam : families) {
            Font f = new Font(fam, Font.PLAIN, (int) size);
            if (!f.getFamily().equals(Font.DIALOG) || fam.equals(Font.SANS_SERIF)) {
                return f.deriveFont(size);
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, (int) size);
    }

    private void stylePrimaryButton(JButton b, Color base, Color pressed) {
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setBackground(base);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(10, 22, 10, 22));
        b.addChangeListener(ev -> {
            if (b.getModel().isPressed()) {
                b.setBackground(pressed);
            } else {
                b.setBackground(base);
            }
        });
    }

    private JComponent buildFooter() {
        JLabel footer = new JLabel(
                "Rentall Java Desktop  ·  CRUD Réservation & Avis  ·  Base : smart_rental_platform",
                SwingConstants.CENTER);
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.setForeground(TEXT_MUTED);
        footer.setBorder(new EmptyBorder(6, 16, 18, 16));
        footer.setOpaque(true);
        footer.setBackground(BG_APP);
        return footer;
    }

    /**
     * Panneau avec fond arrondi (anti-alias).
     */
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
            g2.setColor(new Color(0, 0, 0, 18));
            g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, radius, radius));
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
