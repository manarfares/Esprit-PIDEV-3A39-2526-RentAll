package com.rentall.services;

import com.rentall.config.DatabaseConnection;
import com.rentall.dto.ReservationTableRow;
import com.rentall.entities.Reservation;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service de génération de factures PDF pour les réservations.
 * Utilise OpenPDF pour créer des documents PDF professionnels.
 */
public class PdfReservationService {

    private static final String FACTURES_DIR = "factures";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    private final Connection connection;
    
    public PdfReservationService() {
        this.connection = DatabaseConnection.getConnection();
        ensureFacturesDirectoryExists();
    }
    
    /**
     * Génère un PDF de facture pour une réservation.
     * @param reservation La réservation à facturer
     * @return Le chemin du fichier PDF généré, ou null en cas d'erreur
     */
    public String genererFacture(Reservation reservation) {
        if (reservation == null) {
            NotificationService.showError("Erreur PDF", "Réservation invalide.");
            return null;
        }
        
        String fileName = generateFileName(reservation);
        String filePath = FACTURES_DIR + File.separator + fileName;
        
        try {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();
            
            addHeader(document);
            addReservationDetails(document, reservation);
            addFooter(document);
            
            document.close();
            
            NotificationService.showSuccess("PDF généré", "Facture créée : " + filePath);
            return filePath;
            
        } catch (Exception e) {
            NotificationService.showError("Erreur PDF", "Échec de la génération : " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Génère un PDF après un ajout réussi de réservation.
     * Récupère les informations complètes depuis la base de données.
     * @param reservationId L'ID de la réservation ajoutée
     * @return Le chemin du fichier PDF généré, ou null en cas d'erreur
     */
    public String genererReservationPdf(ReservationTableRow row) {
        if (row == null) {
            NotificationService.showError("Erreur PDF", "Reservation invalide.");
            return null;
        }

        String timestamp = LocalDateTime.now().format(FILE_DATE_FORMATTER);
        String filePath = FACTURES_DIR + File.separator + "reservation_" + row.getId() + "_" + timestamp + ".pdf";

        try {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            addHeader(document);
            addReservationSummaryDetails(document, row);
            addFooter(document);

            document.close();
            NotificationService.showSuccess("PDF genere", "Reservation exportee : " + filePath);
            return filePath;
        } catch (Exception e) {
            NotificationService.showError("Erreur PDF", "Echec de la generation : " + e.getMessage());
            return null;
        }
    }

    public void genererFicheReservationPdf(ReservationTableRow row, File outputFile)
            throws IOException, DocumentException {
        if (row == null) {
            throw new IllegalArgumentException("Réservation invalide.");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("Fichier de sortie invalide.");
        }

        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Impossible de créer le dossier : " + parent.getAbsolutePath());
        }

        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setCloseStream(false);
            try {
                document.open();
                addReservationSheet(document, row);
            } finally {
                if (document.isOpen()) {
                    document.close();
                }
            }
        }
    }

    public void genererListeReservationsPdf(List<ReservationTableRow> rows, File outputFile)
            throws IOException, DocumentException {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("Aucune réservation à exporter.");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("Fichier de sortie invalide.");
        }

        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Impossible de créer le dossier : " + parent.getAbsolutePath());
        }

        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            Document document = new Document(PageSize.A4.rotate(), 30, 30, 35, 35);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setCloseStream(false);
            try {
                document.open();

                Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD, Color.BLACK);
                Paragraph title = new Paragraph("Liste des réservations", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(10);
                document.add(title);

                Font metaFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
                Paragraph exportedAt = new Paragraph(
                        "Date d’export : " + LocalDateTime.now().format(DATE_FORMATTER),
                        metaFont);
                exportedAt.setAlignment(Element.ALIGN_CENTER);
                exportedAt.setSpacingAfter(20);
                document.add(exportedAt);

                addReservationsExportTable(document, rows);
            } finally {
                if (document.isOpen()) {
                    document.close();
                }
            }
        }
    }

    public String genererFactureApresAjout(int reservationId) {
        Reservation reservation = getReservationComplete(reservationId);
        if (reservation == null) {
            NotificationService.showError("Erreur PDF", "Impossible de récupérer la réservation.");
            return null;
        }
        return genererFacture(reservation);
    }
    
    private void ensureFacturesDirectoryExists() {
        File dir = new File(FACTURES_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    private String generateFileName(Reservation reservation) {
        String timestamp = LocalDateTime.now().format(FILE_DATE_FORMATTER);
        return "facture_reservation_" + reservation.getId() + "_" + timestamp + ".pdf";
    }
    
    private void addHeader(Document document) throws DocumentException {
        // Titre principal
        Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, new Color(34, 139, 34));
        Paragraph title = new Paragraph("RentAll", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(5);
        document.add(title);
        
        // Sous-titre
        Font subtitleFont = new Font(Font.HELVETICA, 14, Font.NORMAL, Color.GRAY);
        Paragraph subtitle = new Paragraph("Plateforme de location de logements", subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(20);
        document.add(subtitle);
        
        // Ligne de séparation
        Paragraph separator = new Paragraph("────────────────────────────");
        separator.setAlignment(Element.ALIGN_CENTER);
        separator.setSpacingAfter(15);
        document.add(separator);
        
        document.add(Chunk.NEWLINE);
        
        // Titre facture
        Font invoiceFont = new Font(Font.HELVETICA, 18, Font.BOLD, Color.BLACK);
        Paragraph invoiceTitle = new Paragraph("FACTURE DE RÉSERVATION", invoiceFont);
        invoiceTitle.setAlignment(Element.ALIGN_CENTER);
        invoiceTitle.setSpacingAfter(20);
        document.add(invoiceTitle);
    }
    
    private void addReservationDetails(Document document, Reservation reservation) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(20);
        
        // Récupérer les informations du logement et du locataire
        String[] logementInfo = getLogementInfo(reservation.getFoyerId());
        String[] locataireInfo = getLocataireInfo(reservation.getLocataireId());
        
        // Contenu de la table
        addTableRow(table, "Référence réservation :", "RES-" + reservation.getId(), true);
        addTableRow(table, "Date de génération :", LocalDateTime.now().format(DATE_FORMATTER), false);
        
        document.add(table);
        document.add(Chunk.NEWLINE);
        
        // Section Locataire
        addSectionTitle(document, "Informations locataire");
        PdfPTable locataireTable = new PdfPTable(2);
        locataireTable.setWidthPercentage(100);
        if (locataireInfo[0] != null) {
            addTableRow(locataireTable, "Nom :", locataireInfo[0], true);
        }
        if (locataireInfo[1] != null) {
            addTableRow(locataireTable, "Email :", locataireInfo[1], false);
        }
        document.add(locataireTable);
        document.add(Chunk.NEWLINE);
        
        // Section Logement
        addSectionTitle(document, "Informations logement");
        PdfPTable logementTable = new PdfPTable(2);
        logementTable.setWidthPercentage(100);
        if (logementInfo[0] != null) {
            addTableRow(logementTable, "Logement :", logementInfo[0], true);
        }
        if (logementInfo[1] != null) {
            addTableRow(logementTable, "Adresse :", logementInfo[1], false);
        }
        document.add(logementTable);
        document.add(Chunk.NEWLINE);
        
        // Section Séjour
        addSectionTitle(document, "Détails du séjour");
        PdfPTable sejourTable = new PdfPTable(2);
        sejourTable.setWidthPercentage(100);
        addTableRow(sejourTable, "Date d'arrivée :", reservation.getDateDebut().format(DATE_FORMATTER), true);
        addTableRow(sejourTable, "Date de départ :", reservation.getDateFin().format(DATE_FORMATTER), false);
        addTableRow(sejourTable, "Nombre de personnes :", String.valueOf(reservation.getNombrePersonnes()), false);
        addTableRow(sejourTable, "Statut :", formatStatut(reservation.getStatut()), false);
        document.add(sejourTable);
        document.add(Chunk.NEWLINE);
        
        // Section Tarification
        addSectionTitle(document, "Tarification");
        PdfPTable tarifTable = new PdfPTable(2);
        tarifTable.setWidthPercentage(100);
        
        BigDecimal montant = reservation.getMontantTotal();
        addTableRow(tarifTable, "Montant total :", (montant != null ? montant + " EUR" : "—"), true);
        
        document.add(tarifTable);
    }
    
    private void addReservationSummaryDetails(Document document, ReservationTableRow row) throws DocumentException {
        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.setSpacingBefore(10);
        metaTable.setSpacingAfter(20);
        addTableRow(metaTable, "ID reservation :", String.valueOf(row.getId()), true);
        addTableRow(metaTable, "Date de generation :", LocalDateTime.now().format(DATE_FORMATTER), false);
        document.add(metaTable);
        document.add(Chunk.NEWLINE);

        addSectionTitle(document, "Informations reservation");
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        addTableRow(table, "Logement :", row.getFoyerLibelle(), true);
        addTableRow(table, "Utilisateur / locataire :", row.getLocataireLibelle(), false);
        addTableRow(table, "Date debut :", row.getDateDebut().format(DATE_FORMATTER), false);
        addTableRow(table, "Date fin :", row.getDateFin().format(DATE_FORMATTER), false);
        addTableRow(table, "Nombre de personnes :", String.valueOf(row.getNombrePersonnes()), false);
        BigDecimal montant = row.getMontantTotal();
        addTableRow(table, "Montant total :", montant != null ? montant + " EUR" : "-", true);
        addTableRow(table, "Statut :", formatStatut(row.getStatut()), false);
        document.add(table);
    }

    private void addReservationSheet(Document document, ReservationTableRow row) throws DocumentException {
        Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD, Color.BLACK);
        Paragraph title = new Paragraph("Fiche de réservation", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(20);

        addTableRow(table, "Référence / ID réservation :", "RES-" + row.getId(), true);
        addTableRow(table, "Logement :", row.getFoyerLibelle(), false);
        addTableRow(table, "Locataire :", row.getLocataireLibelle(), false);
        addTableRow(table, "Date début :", row.getDateDebut().format(DATE_FORMATTER), false);
        addTableRow(table, "Date fin :", row.getDateFin().format(DATE_FORMATTER), false);
        addTableRow(table, "Nombre de jours :", String.valueOf(nombreJours(row)), false);
        addTableRow(table, "Nombre de personnes :", String.valueOf(row.getNombrePersonnes()), false);
        BigDecimal montant = row.getMontantTotal();
        addTableRow(table, "Montant total :", montant != null ? montant + " DT" : "-", true);
        addTableRow(table, "Statut :", formatStatut(row.getStatut()), false);
        addTableRow(table, "Date d'export :", LocalDateTime.now().format(DATE_FORMATTER), false);

        document.add(table);
    }

    private long nombreJours(ReservationTableRow row) {
        long jours = ChronoUnit.DAYS.between(
                row.getDateDebut().toLocalDate(),
                row.getDateFin().toLocalDate());
        return Math.max(1, jours);
    }

    private void addReservationsExportTable(Document document, List<ReservationTableRow> rows)
            throws DocumentException {
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.2f, 2.2f, 1.8f, 1.8f, 1.4f, 1.3f, 1.0f});
        table.setSpacingBefore(10);

        addHeaderCell(table, "Logement");
        addHeaderCell(table, "Locataire");
        addHeaderCell(table, "Date début");
        addHeaderCell(table, "Date fin");
        addHeaderCell(table, "Montant");
        addHeaderCell(table, "Statut");
        addHeaderCell(table, "Personnes");

        for (ReservationTableRow row : rows) {
            addBodyCell(table, row.getFoyerLibelle());
            addBodyCell(table, row.getLocataireLibelle());
            addBodyCell(table, row.getDateDebut().format(DATE_FORMATTER));
            addBodyCell(table, row.getDateFin().format(DATE_FORMATTER));
            BigDecimal montant = row.getMontantTotal();
            addBodyCell(table, montant != null ? montant + " DT" : "-");
            addBodyCell(table, formatStatut(row.getStatut()));
            addBodyCell(table, String.valueOf(row.getNombrePersonnes()));
        }

        document.add(table);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font font = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(108, 99, 255));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text) {
        Font font = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.BLACK);
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", font));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addSectionTitle(Document document, String title) throws DocumentException {
        Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(34, 139, 34));
        Paragraph section = new Paragraph(title, sectionFont);
        section.setSpacingBefore(15);
        section.setSpacingAfter(10);
        document.add(section);
    }
    
    private void addTableRow(PdfPTable table, String label, String value, boolean highlight) {
        Font labelFont = new Font(Font.HELVETICA, 11, Font.BOLD, Color.DARK_GRAY);
        Font valueFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);
        
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        labelCell.setBackgroundColor(new Color(248, 249, 250));
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "—", valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        if (highlight) {
            valueCell.setBackgroundColor(new Color(240, 253, 244));
        }
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }
    
    private void addFooter(Document document) throws DocumentException {
        document.add(Chunk.NEWLINE);
        Paragraph separator = new Paragraph("────────────────────────────");
        separator.setAlignment(Element.ALIGN_CENTER);
        separator.setSpacingAfter(15);
        document.add(separator);
        
        Font footerFont = new Font(Font.HELVETICA, 9, Font.ITALIC, Color.GRAY);
        Paragraph footer = new Paragraph(
            "Merci d'avoir choisi RentAll pour votre séjour. " +
            "Pour toute question, contactez-nous à contact@rentall.com",
            footerFont
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        document.add(footer);
    }
    
    private String formatStatut(String statut) {
        if (statut == null) return "—";
        return switch (statut) {
            case "en_attente" -> "En attente";
            case "confirmee" -> "Confirmée";
            case "refusee" -> "Refusée";
            case "terminee" -> "Terminée";
            case "annulee" -> "Annulée";
            default -> statut;
        };
    }
    
    private Reservation getReservationComplete(int id) {
        String sql = "SELECT * FROM reservation WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Reservation(
                    rs.getInt("id"),
                    rs.getInt("logement_id"),
                    rs.getInt("locataire_id"),
                    rs.getTimestamp("date_debut").toLocalDateTime(),
                    rs.getTimestamp("date_fin").toLocalDateTime(),
                    rs.getBigDecimal("montant_total"),
                    rs.getString("statut"),
                    rs.getTimestamp("date_creation").toLocalDateTime(),
                    rs.getInt("nombre_personnes")
                );
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération réservation: " + e.getMessage());
        }
        return null;
    }
    
    private String[] getLogementInfo(int logementId) {
        String[] info = new String[2]; // [0] = libellé, [1] = adresse
        String sql = "SELECT l.titre, l.adresse FROM logement l WHERE l.id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, logementId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                info[0] = rs.getString("titre");
                info[1] = rs.getString("adresse");
            }
        } catch (SQLException e) {
            // Essayer avec la table foyer si logement n'existe pas
            try (PreparedStatement ps2 = connection.prepareStatement(
                    "SELECT f.titre, f.adresse FROM foyer f WHERE f.id = ?")) {
                ps2.setInt(1, logementId);
                ResultSet rs2 = ps2.executeQuery();
                if (rs2.next()) {
                    info[0] = rs2.getString("titre");
                    info[1] = rs2.getString("adresse");
                }
            } catch (SQLException ex) {
                System.err.println("Erreur récupération logement: " + ex.getMessage());
            }
        }
        return info;
    }
    
    private String[] getLocataireInfo(int locataireId) {
        String[] info = new String[2]; // [0] = nom complet, [1] = email
        String sql = "SELECT u.nom, u.prenom, u.email FROM utilisateur u WHERE u.id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, locataireId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                info[0] = (prenom != null ? prenom + " " : "") + (nom != null ? nom : "");
                info[1] = rs.getString("email");
            }
        } catch (SQLException e) {
            // Essayer avec la table locataire si utilisateur n'existe pas
            try (PreparedStatement ps2 = connection.prepareStatement(
                    "SELECT l.nom, l.prenom, l.email FROM locataire l WHERE l.id = ?")) {
                ps2.setInt(1, locataireId);
                ResultSet rs2 = ps2.executeQuery();
                if (rs2.next()) {
                    String nom = rs2.getString("nom");
                    String prenom = rs2.getString("prenom");
                    info[0] = (prenom != null ? prenom + " " : "") + (nom != null ? nom : "");
                    info[1] = rs2.getString("email");
                }
            } catch (SQLException ex) {
                System.err.println("Erreur récupération locataire: " + ex.getMessage());
            }
        }
        return info;
    }
}
