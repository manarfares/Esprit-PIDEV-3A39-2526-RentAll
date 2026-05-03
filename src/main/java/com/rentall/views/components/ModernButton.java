package com.rentall.views.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Bouton moderne avec style amélioré
 */
public class ModernButton extends JButton {

    public enum ButtonStyle {
        PRIMARY,    // Bleu - Actions principales
        SUCCESS,    // Vert - Enregistrer, Confirmer
        DANGER,     // Rouge - Supprimer, Annuler
        SECONDARY,  // Gris - Actions secondaires
        WARNING     // Orange - Avertissements
    }

    private ButtonStyle style;
    private Color baseColor;
    private Color hoverColor;
    private Color pressedColor;

    public ModernButton(String text, ButtonStyle style) {
        super(text);
        this.style = style;
        initColors();
        setupStyle();
        addHoverEffect();
    }

    private void initColors() {
        switch (style) {
            case PRIMARY:
                baseColor = new Color(52, 152, 219);      // Bleu
                hoverColor = new Color(41, 128, 185);
                pressedColor = new Color(31, 97, 141);
                break;
            case SUCCESS:
                baseColor = new Color(46, 204, 113);      // Vert
                hoverColor = new Color(39, 174, 96);
                pressedColor = new Color(30, 132, 73);
                break;
            case DANGER:
                baseColor = new Color(231, 76, 60);       // Rouge
                hoverColor = new Color(192, 57, 43);
                pressedColor = new Color(169, 50, 38);
                break;
            case SECONDARY:
                baseColor = new Color(149, 165, 166);     // Gris
                hoverColor = new Color(127, 140, 141);
                pressedColor = new Color(93, 109, 126);
                break;
            case WARNING:
                baseColor = new Color(243, 156, 18);      // Orange
                hoverColor = new Color(211, 84, 0);
                pressedColor = new Color(175, 96, 26);
                break;
        }
    }

    private void setupStyle() {
        setFont(new Font("Segoe UI", Font.BOLD, 14));
        setForeground(Color.WHITE);
        setBackground(baseColor);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(true);
        setOpaque(true);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Padding
        setMargin(new Insets(10, 20, 10, 20));
        
        // Bordures arrondies
        setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
    }

    private void addHoverEffect() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    setBackground(hoverColor);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (isEnabled()) {
                    setBackground(baseColor);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    setBackground(pressedColor);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isEnabled()) {
                    setBackground(hoverColor);
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Dessiner le fond arrondi
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        
        // Dessiner le texte
        g2.setColor(getForeground());
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int textX = (getWidth() - fm.stringWidth(getText())) / 2;
        int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(getText(), textX, textY);
        
        g2.dispose();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            setBackground(new Color(189, 195, 199));
        } else {
            setBackground(baseColor);
        }
    }

    // Méthodes statiques pour créer rapidement des boutons
    public static ModernButton createPrimaryButton(String text) {
        return new ModernButton(text, ButtonStyle.PRIMARY);
    }

    public static ModernButton createSuccessButton(String text) {
        return new ModernButton(text, ButtonStyle.SUCCESS);
    }

    public static ModernButton createDangerButton(String text) {
        return new ModernButton(text, ButtonStyle.DANGER);
    }

    public static ModernButton createSecondaryButton(String text) {
        return new ModernButton(text, ButtonStyle.SECONDARY);
    }

    public static ModernButton createWarningButton(String text) {
        return new ModernButton(text, ButtonStyle.WARNING);
    }
}
