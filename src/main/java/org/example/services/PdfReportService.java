package org.example.services;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.example.models.User;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Génère un rapport PDF complet :
 *  - en-tête (logo texte + date)
 *  - bloc statistiques (total, par rôle, par statut, vérifiés, face_enabled)
 *  - tableau de tous les utilisateurs (Username, Email, Rôle, Statut, Téléphone)
 */
public class PdfReportService {

    // Polices et couleurs RentAll
    private static final Color  COLOR_PRIMARY    = new Color(108, 99, 255);   // #6C63FF
    private static final Color  COLOR_SECONDARY  = new Color(155, 89, 182);   // #9B59B6
    private static final Color  COLOR_HEADER_BG  = new Color(52, 73, 94);     // #34495e
    private static final Color  COLOR_TEXT_DARK  = new Color(44, 62, 80);     // #2c3e50
    private static final Color  COLOR_TEXT_GRAY  = new Color(127, 140, 141);  // #7f8c8d
    private static final Color  COLOR_GREEN      = new Color(39, 174, 96);    // #27ae60
    private static final Color  COLOR_ORANGE     = new Color(230, 126, 34);   // #e67e22
    private static final Color  COLOR_RED        = new Color(231, 76, 60);    // #e74c3c
    private static final Color  COLOR_ZEBRA      = new Color(245, 247, 250);  // #f5f7fa

    private static final Font FONT_TITLE     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, COLOR_PRIMARY);
    private static final Font FONT_SUBTITLE  = FontFactory.getFont(FontFactory.HELVETICA,      11, COLOR_TEXT_GRAY);
    private static final Font FONT_SECTION   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COLOR_TEXT_DARK);
    private static final Font FONT_LABEL     = FontFactory.getFont(FontFactory.HELVETICA,      10, COLOR_TEXT_GRAY);
    private static final Font FONT_VALUE     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, COLOR_TEXT_DARK);
    private static final Font FONT_TH        = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font FONT_TD        = FontFactory.getFont(FontFactory.HELVETICA,      9,  COLOR_TEXT_DARK);
    private static final Font FONT_FOOTER    = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, COLOR_TEXT_GRAY);

    /**
     * Génère un PDF avec stats + tableau des utilisateurs.
     * @return le fichier PDF créé
     */
    public File generateUserReport(File targetFile, Map<String, Integer> stats, List<User> users) throws Exception {
        Document doc = new Document(PageSize.A4, 36, 36, 50, 36);
        try (FileOutputStream out = new FileOutputStream(targetFile)) {
            PdfWriter.getInstance(doc, out);
            doc.open();

            // ── EN-TÊTE ────────────────────────────
            Paragraph title = new Paragraph("RentAll - Rapport Administrateur", FONT_TITLE);
            title.setAlignment(Element.ALIGN_LEFT);
            doc.add(title);

            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy 'a' HH:mm"));
            Paragraph subtitle = new Paragraph("Genere le " + now + " - Module Gestion des Utilisateurs", FONT_SUBTITLE);
            subtitle.setSpacingAfter(20);
            doc.add(subtitle);

            // ── SECTION STATISTIQUES ────────────────
            doc.add(sectionHeader("Vue d'ensemble"));
            doc.add(buildStatCardsTotalsByRole(stats));
            doc.add(Chunk.NEWLINE);

            doc.add(sectionHeader("Repartition par statut de compte"));
            doc.add(buildStatCardsByStatus(stats));
            doc.add(Chunk.NEWLINE);

            doc.add(sectionHeader("Securite et verification"));
            doc.add(buildSecurityStats(stats));
            doc.add(Chunk.NEWLINE);

            // ── TABLEAU UTILISATEURS ────────────────
            doc.add(sectionHeader("Liste detaillee des utilisateurs (" + users.size() + ")"));
            doc.add(buildUsersTable(users));

            // ── PIED DE PAGE ────────────────────────
            Paragraph footer = new Paragraph(
                    "\nDocument confidentiel - RentAll PIDEV - Genere automatiquement par le module Java",
                    FONT_FOOTER);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
        }
        return targetFile;
    }

    // ============================================================
    //                     Sections
    // ============================================================

    private Paragraph sectionHeader(String text) {
        Paragraph p = new Paragraph(text, FONT_SECTION);
        p.setSpacingBefore(6);
        p.setSpacingAfter(8);
        return p;
    }

    private PdfPTable buildStatCardsTotalsByRole(Map<String, Integer> s) throws Exception {
        PdfPTable t = new PdfPTable(5);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1, 1, 1, 1, 1});
        t.addCell(statCard("Total comptes",   s.getOrDefault("total", 0),             COLOR_PRIMARY));
        t.addCell(statCard("Admins",          s.getOrDefault("role_admin", 0),        COLOR_SECONDARY));
        t.addCell(statCard("Hosts verifies",  s.getOrDefault("role_host", 0),         COLOR_GREEN));
        t.addCell(statCard("Hosts en attente",s.getOrDefault("role_host_pending", 0), COLOR_ORANGE));
        t.addCell(statCard("Guests",          s.getOrDefault("role_guest", 0),        new Color(52, 152, 219)));
        return t;
    }

    private PdfPTable buildStatCardsByStatus(Map<String, Integer> s) throws Exception {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1, 1, 1, 1});
        t.addCell(statCard("Actifs",     s.getOrDefault("status_active", 0),    COLOR_GREEN));
        t.addCell(statCard("Bannis",     s.getOrDefault("status_banned", 0),    COLOR_RED));
        t.addCell(statCard("Inactifs",   s.getOrDefault("status_inactive", 0),  COLOR_TEXT_GRAY));
        t.addCell(statCard("Suspendus",  s.getOrDefault("status_suspended", 0), COLOR_ORANGE));
        return t;
    }

    private PdfPTable buildSecurityStats(Map<String, Integer> s) throws Exception {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(60);
        t.setWidths(new float[]{1, 1});
        t.setHorizontalAlignment(Element.ALIGN_LEFT);
        t.addCell(statCard("Comptes verifies (email)", s.getOrDefault("verified", 0),     COLOR_GREEN));
        t.addCell(statCard("Connexion faciale active", s.getOrDefault("face_enabled", 0), COLOR_PRIMARY));
        return t;
    }

    private PdfPCell statCard(String label, int value, Color color) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(12);
        cell.setBorderWidth(0);
        cell.setBorderColor(color);
        cell.setBorderWidthBottom(3);
        cell.setBorderWidthLeft(3);
        cell.setBackgroundColor(COLOR_ZEBRA);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph p1 = new Paragraph(String.valueOf(value),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, color));
        p1.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p1);

        Paragraph p2 = new Paragraph(label, FONT_LABEL);
        p2.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p2);

        return cell;
    }

    // ============================================================
    //                   Tableau utilisateurs
    // ============================================================

    private PdfPTable buildUsersTable(List<User> users) throws Exception {
        PdfPTable t = new PdfPTable(5);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{2.2f, 3.5f, 1.8f, 1.4f, 2.1f});
        t.setSpacingBefore(4);

        // En-têtes
        for (String h : new String[]{"Username", "Email", "Role", "Statut", "Telephone"}) {
            PdfPCell th = new PdfPCell(new Phrase(h, FONT_TH));
            th.setBackgroundColor(COLOR_HEADER_BG);
            th.setPadding(8);
            th.setBorderColor(COLOR_HEADER_BG);
            th.setHorizontalAlignment(Element.ALIGN_LEFT);
            t.addCell(th);
        }

        // Lignes (avec zébrage)
        boolean zebra = false;
        for (User u : users) {
            String role   = simplifyRole(u.getRole());
            String status = u.getStatus() != null ? u.getStatus() : "?";
            String phone  = u.getPhone() != null && !u.getPhone().isBlank() ? u.getPhone() : "-";

            t.addCell(td(safe(u.getUsername()), zebra));
            t.addCell(td(safe(u.getEmail()),    zebra));
            t.addCell(td(role,                  zebra));
            t.addCell(td(status,                zebra, statusColor(status)));
            t.addCell(td(phone,                 zebra));
            zebra = !zebra;
        }

        return t;
    }

    private PdfPCell td(String text, boolean zebra) {
        return td(text, zebra, COLOR_TEXT_DARK);
    }

    private PdfPCell td(String text, boolean zebra, Color textColor) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA, 9, textColor);
        PdfPCell cell = new PdfPCell(new Phrase(text, f));
        cell.setPadding(6);
        cell.setBorderColor(new Color(220, 220, 220));
        cell.setBorderWidth(0.5f);
        if (zebra) cell.setBackgroundColor(COLOR_ZEBRA);
        return cell;
    }

    private Color statusColor(String status) {
        if (status == null) return COLOR_TEXT_DARK;
        switch (status.toUpperCase()) {
            case "ACTIVE":    return COLOR_GREEN;
            case "BANNED":    return COLOR_RED;
            case "SUSPENDED": return COLOR_ORANGE;
            case "INACTIVE":  return COLOR_TEXT_GRAY;
            default:          return COLOR_TEXT_DARK;
        }
    }

    private String simplifyRole(String role) {
        if (role == null) return "?";
        switch (role) {
            case "ROLE_ADMIN":        return "Admin";
            case "ROLE_HOST":         return "Host";
            case "ROLE_HOST_PENDING": return "Host pending";
            case "ROLE_GUEST":        return "Guest";
            case "ROLE_USER":         return "Guest";
            default:                  return role.replace("ROLE_", "");
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
