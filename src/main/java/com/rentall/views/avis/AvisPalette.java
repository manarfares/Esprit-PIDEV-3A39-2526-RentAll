package com.rentall.views.avis;

import com.rentall.views.components.ModernButton;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Palette de couleurs et utilitaires de style pour le module Avis.
 * Miroir de ReservationPalette — même tokens, accent vert/ambre/rouge propre au module.
 */
public final class AvisPalette {

    // ── Fonds ──────────────────────────────────────────────
    public static final Color BG_APP        = new Color(30, 41, 59);
    public static final Color BG_HEADER     = new Color(15, 23, 42);
    public static final Color CARD_BG       = Color.WHITE;
    public static final Color MENU_CARD_BG  = new Color(241, 245, 249);
    public static final Color FOOTER_BG     = new Color(241, 245, 249);
    public static final Color STRIPE        = new Color(248, 250, 252);

    // ── Textes ─────────────────────────────────────────────
    public static final Color TEXT_TITLE    = new Color(248, 250, 252);
    public static final Color TEXT_SUB      = new Color(203, 213, 225);
    public static final Color TEXT_SUB_MENU = new Color(148, 163, 184);
    public static final Color FOOTER_TEXT   = new Color(71, 85, 105);
    public static final Color ERROR_TEXT    = new Color(220, 38, 38);

    // ── Tableau ────────────────────────────────────────────
    public static final Color TABLE_HEADER_BG = new Color(51, 65, 85);
    public static final Color TABLE_HEADER_FG = new Color(248, 250, 252);
    public static final Color TABLE_GRID      = new Color(226, 232, 240);
    public static final Color SELECTION_BG    = new Color(187, 247, 208);
    public static final Color SELECTION_FG    = new Color(20, 83, 45);

    // ── Bordures ───────────────────────────────────────────
    public static final Color MENU_CARD_BORDER = new Color(203, 213, 225);
    public static final Color BORDER_NORMAL    = new Color(200, 200, 200);
    public static final Color BORDER_ERROR     = new Color(220, 38, 38);

    // ── Accents CRUD ───────────────────────────────────────
    /** Ajouter — vert */
    public static final Color ACCENT_ADD        = new Color(5, 150, 105);
    public static final Color ACCENT_ADD_PRESS  = new Color(4, 120, 87);
    /** Modifier — ambre */
    public static final Color ACCENT_EDIT       = new Color(217, 119, 6);
    public static final Color ACCENT_EDIT_PRESS = new Color(180, 83, 9);
    /** Supprimer — rouge */
    public static final Color ACCENT_DELETE       = new Color(220, 38, 38);
    public static final Color ACCENT_DELETE_PRESS = new Color(185, 28, 28);
    /** Liste — bleu */
    public static final Color ACCENT_LIST       = new Color(37, 99, 235);
    public static final Color ACCENT_LIST_PRESS = new Color(29, 78, 216);

    // ── Boutons neutres ────────────────────────────────────
    public static final Color BTN_NEUTRAL      = new Color(71, 85, 105);
    public static final Color BTN_NEUTRAL_PRESS = new Color(51, 65, 85);
    public static final Color BTN_MUTED        = new Color(100, 116, 139);
    public static final Color BTN_MUTED_PRESS  = new Color(71, 85, 105);

    // ── Infos sur fond coloré ──────────────────────────────
    public static final Color FORM_INFO_ON_ADD    = new Color(220, 252, 231);
    public static final Color FORM_INFO_ON_EDIT   = new Color(255, 237, 213);
    public static final Color FORM_INFO_ON_DELETE = new Color(254, 226, 226);

    // ── Géométrie ──────────────────────────────────────────
    public static final int MENU_CARD_RADIUS = 16;

    private AvisPalette() {}

    /** Applique le style bouton primaire standard (même logique que ReservationPalette). */
    public static void styleBtn(JButton b, Color base, Color pressed) {
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setBackground(base);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setBorder(new EmptyBorder(9, 18, 9, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addChangeListener(ev -> b.setBackground(b.getModel().isPressed() ? pressed : base));
    }

    // Méthodes pour créer des boutons modernes
    public static ModernButton createSaveButton(String text) {
        return ModernButton.createSuccessButton(text);
    }

    public static ModernButton createCancelButton(String text) {
        return ModernButton.createSecondaryButton(text);
    }

    public static ModernButton createSecondaryButton(String text) {
        return ModernButton.createSecondaryButton(text);
    }

    public static ModernButton createDeleteButton(String text) {
        return ModernButton.createDangerButton(text);
    }

    public static ModernButton createEditButton(String text) {
        return ModernButton.createWarningButton(text);
    }

    public static ModernButton createPrimaryActionButton(String text) {
        return ModernButton.createPrimaryButton(text);
    }

    /** Étoiles Unicode pour afficher une note 1-5. */
    public static String noteEtoiles(int note) {
        int n = Math.max(1, Math.min(5, note));
        return "\u2605".repeat(n) + "\u2606".repeat(5 - n) + "  (" + n + "/5)";
    }
}
