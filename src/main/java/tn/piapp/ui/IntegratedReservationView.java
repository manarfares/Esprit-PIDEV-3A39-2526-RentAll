package tn.piapp.ui;

import com.rentall.dto.FoyerListItem;
import com.rentall.dto.LocataireListItem;
import com.rentall.dto.ReservationTableRow;
import com.rentall.entities.Reservation;
import com.rentall.services.FoyerService;
import com.rentall.services.IFoyerService;
import com.rentall.services.ILocataireService;
import com.rentall.services.LocataireService;
import com.rentall.services.PdfReservationService;
import com.rentall.services.ReservationService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tn.piapp.model.User;
import tn.piapp.util.SessionManager;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Desktop;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class IntegratedReservationView extends BorderPane {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final LocalTime DEFAULT_DEBUT_TIME = LocalTime.of(14, 0);
    private static final LocalTime DEFAULT_FIN_TIME = LocalTime.of(11, 0);
    private static final String[] STATUTS = {"en_attente", "confirmee", "refusee", "terminee", "annulee"};

    private final ReservationService service = new ReservationService();
    private final IFoyerService foyerService = new FoyerService();
    private final ILocataireService locataireService = new LocataireService();
    private final ObservableList<ReservationRow> allRows = FXCollections.observableArrayList();
    private final TableView<ReservationRow> table = new TableView<>();
    private final TextField searchField = new TextField();
    private final Label statusLabel = new Label();
    private final User currentUser;

    public IntegratedReservationView() {
        this(SessionManager.getInstance().getCurrentUser());
    }

    public IntegratedReservationView(User currentUser) {
        this.currentUser = currentUser;
        buildLayout();
        refresh();
    }

    private void buildLayout() {
        setStyle("-fx-background-color: #f8f9fa;");

        VBox page = new VBox(18);
        page.setPadding(new Insets(30, 25, 30, 25));

        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label(reservationTitle());
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label subtitle = new Label(reservationSubtitle());
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #95a5a6;");
        titleBox.getChildren().addAll(title, subtitle);

        searchField.setPromptText("Rechercher une réservation...");
        searchField.setStyle(inputStyle());
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applySearch());
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.getChildren().addAll(titleBox, searchField);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);
        if (isHost()) {
            actions.getChildren().addAll(
                    actionButton("Confirmer", "#27ae60", () -> changeSelectedStatus("confirmee")),
                    actionButton("Refuser", "#e67e22", () -> changeSelectedStatus("refusee")),
                    actionButton("Terminer", "#3498db", () -> changeSelectedStatus("terminee")),
                    actionButton("Rechercher", "#9B59B6", this::applySearch),
                    actionButton("Actualiser", "#16a085", this::refresh)
            );
        } else if (isAdmin()) {
            actions.getChildren().addAll(
                    actionButton("+ Ajouter", "#6C63FF", this::addReservation),
                    actionButton("Modifier", "#f39c12", this::editReservation),
                    actionButton("Supprimer", "#e74c3c", this::deleteReservation),
                    actionButton("Rechercher", "#9B59B6", this::applySearch),
                    actionButton("Actualiser", "#16a085", this::refresh),
                    actionButton("Exporter PDF", "#3498db", this::exportSelectedPdf)
            );
        } else {
            actions.getChildren().addAll(
                    actionButton("+ Ajouter", "#6C63FF", this::addReservation),
                    actionButton("Modifier", "#f39c12", this::editReservation),
                    actionButton("Supprimer", "#e74c3c", this::deleteReservation),
                    actionButton("Rechercher", "#9B59B6", this::applySearch),
                    actionButton("Actualiser", "#16a085", this::refresh)
            );
        }

        configureTable();

        VBox tableCard = new VBox(table);
        tableCard.setStyle(cardStyle());
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        page.getChildren().addAll(header, actions, tableCard, statusLabel);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        setCenter(page);
    }

    private void configureTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Aucune réservation trouvée"));
        table.setStyle("-fx-background-color: white; -fx-border-color: transparent;");

        TableColumn<ReservationRow, Number> id = new TableColumn<>("ID");
        id.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().id));
        id.setMaxWidth(70);

        TableColumn<ReservationRow, String> logement = new TableColumn<>("Logement");
        logement.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().logement));

        TableColumn<ReservationRow, String> locataire = new TableColumn<>("Locataire");
        locataire.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().locataire));

        TableColumn<ReservationRow, String> debut = new TableColumn<>("Début");
        debut.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().debut));

        TableColumn<ReservationRow, String> fin = new TableColumn<>("Fin");
        fin.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().fin));

        TableColumn<ReservationRow, String> montant = new TableColumn<>("Montant");
        montant.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().montant));

        TableColumn<ReservationRow, String> statut = new TableColumn<>("Statut");
        statut.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().statut));

        TableColumn<ReservationRow, Number> personnes = new TableColumn<>("Personnes");
        personnes.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().personnes));

        table.getColumns().setAll(List.of(id, logement, locataire, debut, fin, montant, statut, personnes));
    }

    private void refresh() {
        List<ReservationRow> rows = service.listerPourAffichageTableauPourUtilisateur(currentUser)
                .stream()
                .map(ReservationRow::from)
                .collect(Collectors.toList());
        allRows.setAll(rows);
        applySearch();
        statusLabel.setText(rows.size() + " réservation(s) chargée(s)");
    }

    private void applySearch() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            table.setItems(FXCollections.observableArrayList(allRows));
            return;
        }

        table.setItems(allRows.stream()
                .filter(row -> row.matches(query))
                .collect(Collectors.toCollection(FXCollections::observableArrayList)));
    }

    private void addReservation() {
        if (isHost()) {
            showWarning("Action non autorisée", "Un host ne crée pas de réservation à la place du client.");
            return;
        }

        Optional<Reservation> reservation = showReservationDialog(null);
        reservation.ifPresent(value -> {
            int id = service.ajouterEtRetournerId(value);
            if (id > 0) {
                new PdfReservationService().genererFactureApresAjout(id);
                refresh();
                showInfo("Réservation créée", "La réservation a été ajoutée avec succès.");
            } else {
                showWarning("Ajout impossible", "La réservation n'a pas pu être enregistrée.");
            }
        });
    }

    private void editReservation() {
        if (isHost()) {
            showWarning("Action non autorisée", "Un host peut uniquement changer le statut avec les boutons dédiés.");
            return;
        }

        ReservationRow selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorDialog("Veuillez sélectionner une réservation à modifier.");
            return;
        }

        System.out.println("[Reservation] id réservation sélectionnée = " + selected.id);
        Reservation existing = service.afficherParId(selected.id);
        if (existing == null) {
            showWarning("Introuvable", "La réservation sélectionnée n'existe plus.");
            refresh();
            return;
        }
        System.out.println("[Reservation] valeurs avant modification = " + existing);
        if (!service.peutModifierReservation(existing, currentUser)) {
            showWarning("Modification impossible", "Vous pouvez modifier uniquement une réservation en attente qui vous appartient.");
            return;
        }

        Optional<Reservation> edited = showReservationDialog(existing);
        edited.ifPresent(value -> {
            value.setId(existing.getId());
            System.out.println("[Reservation] valeurs après modification = " + value);
            boolean updated = service.modifierEtRetournerSucces(value);
            System.out.println("[Reservation] résultat update SQL/service = " + updated);
            if (updated) {
                refresh();
                showInfo("Réservation modifiée", "Réservation modifiée avec succès.");
            } else {
                showWarning("Modification impossible", "La réservation n'a pas pu être modifiée.");
            }
        });
    }

    private void deleteReservation() {
        if (isHost()) {
            showWarning("Action non autorisée", "Un host ne supprime pas les réservations.");
            return;
        }

        ReservationRow selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Sélection requise", "Sélectionnez une réservation à supprimer.");
            return;
        }
        if (!service.peutSupprimerReservation(selected.id, currentUser)) {
            showWarning("Suppression impossible", "Vous pouvez supprimer uniquement une réservation en attente qui vous appartient.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("Supprimer la réservation #" + selected.id + " ?");
        confirm.setContentText("Cette action supprimera la réservation sélectionnée.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            service.supprimer(selected.id);
            refresh();
        }
    }

    private void exportSelectedPdf() {
        ReservationRow selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(
                    null,
                    "Veuillez sélectionner une réservation à exporter en PDF.",
                    "Export PDF",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Optional<ReservationTableRow> selectedReservation = findReservationForExport(selected.id);
        if (selectedReservation.isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Impossible de retrouver la réservation sélectionnée.",
                    "Export PDF",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exporter la réservation en PDF");
        chooser.setSelectedFile(new File("fiche_reservation_" + selected.id + ".pdf"));
        chooser.setFileFilter(new FileNameExtensionFilter("Fichiers PDF (*.pdf)", "pdf"));

        int result = chooser.showSaveDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(null, "Export PDF annulé.", "Export PDF", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        File outputFile = ensurePdfExtension(chooser.getSelectedFile());
        try {
            new PdfReservationService().genererFicheReservationPdf(selectedReservation.get(), outputFile);
            String message = "PDF de la réservation exporté avec succès : " + outputFile.getAbsolutePath();
            int open = JOptionPane.showConfirmDialog(
                    null,
                    message + "\nVoulez-vous ouvrir le fichier maintenant ?",
                    "Export PDF",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE);
            if (open == JOptionPane.YES_OPTION) {
                try {
                    Desktop.getDesktop().open(outputFile);
                } catch (Exception openError) {
                    openError.printStackTrace();
                    JOptionPane.showMessageDialog(null, message, "Export PDF", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Erreur lors de l’export PDF : " + e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void changeSelectedStatus(String nouveauStatut) {
        ReservationRow selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Sélection requise", "Sélectionnez une réservation.");
            return;
        }
        if (!service.peutChangerStatutReservation(selected.id, nouveauStatut, currentUser)) {
            showWarning("Changement impossible", "Transition de statut non autorisée pour cette réservation.");
            return;
        }
        if (service.modifierStatut(selected.id, nouveauStatut)) {
            refresh();
            showInfo("Statut mis à jour", "La réservation est maintenant : " + nouveauStatut + ".");
        } else {
            showWarning("Changement impossible", "Le statut n'a pas pu être modifié.");
        }
    }

    private Optional<ReservationTableRow> findReservationForExport(int reservationId) {
        return service.listerPourAffichageTableauPourUtilisateur(currentUser).stream()
                .filter(row -> row.getId() == reservationId)
                .findFirst();
    }

    private static File ensurePdfExtension(File file) {
        if (file == null) {
            return new File("reservations_export.pdf");
        }
        String path = file.getAbsolutePath();
        if (path.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return file;
        }
        return new File(path + ".pdf");
    }

    private Optional<Reservation> showReservationDialog(Reservation existing) {
        Dialog<Reservation> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Ajouter une réservation" : "Modifier une réservation");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<FoyerListItem> logement = new ComboBox<>();
        logement.setItems(FXCollections.observableArrayList(foyerService.listerFoyersPourSelection()));
        logement.setMaxWidth(Double.MAX_VALUE);
        preselectLogement(logement, existing);

        ComboBox<LocataireListItem> locataire = new ComboBox<>();
        locataire.setItems(FXCollections.observableArrayList(locataireService.listerLocatairesPourSelection()));
        locataire.setMaxWidth(Double.MAX_VALUE);
        preselectLocataire(locataire, existing);

        DatePicker dateDebut = new DatePicker(existing == null
                ? LocalDate.now().plusDays(1)
                : existing.getDateDebut().toLocalDate());
        DatePicker dateFin = new DatePicker(existing == null
                ? LocalDate.now().plusDays(2)
                : existing.getDateFin().toLocalDate());

        TextField montant = new TextField();
        montant.setEditable(false);
        montant.setFocusTraversable(false);

        ComboBox<String> statut = new ComboBox<>(FXCollections.observableArrayList(STATUTS));
        statut.setMaxWidth(Double.MAX_VALUE);
        statut.setValue(existing == null ? "en_attente" : normalizeStatut(existing.getStatut()));
        statut.setDisable(!canManageStatus());

        Spinner<Integer> personnes = new Spinner<>();
        personnes.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                1, 20, existing == null ? 1 : Math.max(1, existing.getNombrePersonnes())));
        personnes.setEditable(true);
        personnes.setMaxWidth(Double.MAX_VALUE);

        GridPane form = formGrid();
        addComboRow(form, 0, "Logement", logement);
        addLocataireRow(form, 1, "Locataire", locataire);
        addDateRow(form, 2, "Date début", dateDebut);
        addDateRow(form, 3, "Date fin", dateFin);
        addRow(form, 4, "Montant total", montant);
        if (canManageStatus()) {
            addStringComboRow(form, 5, "Statut", statut);
            addSpinnerRow(form, 6, "Nombre de personnes", personnes);
        } else {
            addSpinnerRow(form, 5, "Nombre de personnes", personnes);
        }

        Runnable recalculate = () -> recalculateMontant(logement, dateDebut, dateFin, personnes, montant);
        logement.valueProperty().addListener((obs, oldValue, newValue) -> recalculate.run());
        dateDebut.valueProperty().addListener((obs, oldValue, newValue) -> recalculate.run());
        dateFin.valueProperty().addListener((obs, oldValue, newValue) -> recalculate.run());
        personnes.valueProperty().addListener((obs, oldValue, newValue) -> recalculate.run());
        personnes.getEditor().textProperty().addListener((obs, oldValue, newValue) -> recalculate.run());
        recalculate.run();

        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }

            ReservationDraft draft = validateReservationForm(
                    logement, locataire, dateDebut, dateFin, statut, personnes, existing);

            return new Reservation(
                    draft.logement().getId(),
                    draft.locataire().getId(),
                    draft.debut(),
                    draft.fin(),
                    draft.total(),
                    draft.statut(),
                    existing == null ? LocalDateTime.now() : existing.getDateCreation(),
                    draft.nombrePersonnes()
            );
        });

        try {
            return dialog.showAndWait();
        } catch (RuntimeException e) {
            showErrorDialog(e.getMessage());
            return Optional.empty();
        }
    }

    private ReservationDraft validateReservationForm(ComboBox<FoyerListItem> logement,
                                                     ComboBox<LocataireListItem> locataire,
                                                     DatePicker dateDebut,
                                                     DatePicker dateFin,
                                                     ComboBox<String> statut,
                                                     Spinner<Integer> personnes,
                                                     Reservation existing) {
        FoyerListItem selectedLogement = logement.getValue();
        LocataireListItem selectedLocataire = locataire.getValue();
        LocalDate debutDate = dateDebut.getValue();
        LocalDate finDate = dateFin.getValue();
        String selectedStatut = resolveStatutForCurrentRole(existing, statut);
        int nombrePersonnes;

        try {
            nombrePersonnes = readSpinnerValueOrThrow(personnes);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Veuillez remplir tous les champs obligatoires.");
        }

        if (selectedLogement == null
                || selectedLocataire == null
                || debutDate == null
                || finDate == null
                || nombrePersonnes <= 0
                || (canManageStatus() && selectedStatut.isBlank())) {
            throw new IllegalArgumentException("Veuillez remplir tous les champs obligatoires.");
        }

        if (debutDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Impossible de faire la réservation : la date de début ne peut pas être dans le passé.");
        }

        if (!finDate.isAfter(debutDate)) {
            throw new IllegalArgumentException(
                    "Impossible de faire la réservation : la date de fin doit être supérieure à la date de début.");
        }

        LocalDateTime debut = LocalDateTime.of(debutDate, DEFAULT_DEBUT_TIME);
        LocalDateTime fin = LocalDateTime.of(finDate, DEFAULT_FIN_TIME);
        Integer excludedId = existing == null ? null : existing.getId();
        if (!service.estLogementDisponible(selectedLogement.getId(), debut, fin, excludedId)) {
            throw new IllegalArgumentException(
                    "Impossible de faire la réservation : ce logement est déjà réservé pour cette période.");
        }

        BigDecimal total = calculateMontant(selectedLogement, debut, fin, nombrePersonnes);
        return new ReservationDraft(
                selectedLogement,
                selectedLocataire,
                debut,
                fin,
                total,
                selectedStatut,
                nombrePersonnes
        );
    }

    private void recalculateMontant(ComboBox<FoyerListItem> logement, DatePicker dateDebut,
                                    DatePicker dateFin, Spinner<Integer> personnes, TextField montant) {
        try {
            FoyerListItem selectedLogement = logement.getValue();
            if (selectedLogement == null || dateDebut.getValue() == null || dateFin.getValue() == null) {
                montant.setText("");
                return;
            }

            int nombrePersonnes = readSpinnerValueOrThrow(personnes);
            if (nombrePersonnes <= 0 || dateDebut.getValue().isBefore(LocalDate.now())
                    || !dateFin.getValue().isAfter(dateDebut.getValue())) {
                montant.setText("");
                return;
            }
            LocalDateTime debut = LocalDateTime.of(dateDebut.getValue(), DEFAULT_DEBUT_TIME);
            LocalDateTime fin = LocalDateTime.of(dateFin.getValue(), DEFAULT_FIN_TIME);
            if (!fin.isAfter(debut)) {
                montant.setText("");
                return;
            }

            BigDecimal total = calculateMontant(selectedLogement, debut, fin, nombrePersonnes);
            montant.setText(total.toPlainString());
        } catch (RuntimeException e) {
            montant.setText("");
        }
    }

    private BigDecimal calculateMontant(FoyerListItem logement, LocalDateTime debut,
                                        LocalDateTime fin, int nombrePersonnes) {
        BigDecimal prixNuit = logement.getPrixParNuit();
        if (prixNuit == null) {
            prixNuit = foyerService.getPrixParNuitParFoyerId(logement.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Prix par nuit introuvable pour ce logement."));
        }

        long jours = Math.max(1, ChronoUnit.DAYS.between(debut.toLocalDate(), fin.toLocalDate()));
        return prixNuit
                .multiply(BigDecimal.valueOf(jours))
                .multiply(BigDecimal.valueOf(nombrePersonnes))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static void preselectLogement(ComboBox<FoyerListItem> logement, Reservation existing) {
        if (existing != null) {
            Optional<FoyerListItem> selected = logement.getItems().stream()
                    .filter(item -> item.getId() == existing.getFoyerId())
                    .findFirst();
            if (selected.isPresent()) {
                logement.setValue(selected.get());
            } else {
                FoyerListItem fallback = new FoyerListItem(existing.getFoyerId(), "Réf. logement " + existing.getFoyerId());
                logement.getItems().add(fallback);
                logement.setValue(fallback);
            }
        }
        if (logement.getValue() == null && !logement.getItems().isEmpty()) {
            logement.setValue(logement.getItems().get(0));
        }
    }

    private static void preselectLocataire(ComboBox<LocataireListItem> locataire, Reservation existing) {
        if (existing != null) {
            Optional<LocataireListItem> selected = locataire.getItems().stream()
                    .filter(item -> item.getId() == existing.getLocataireId())
                    .findFirst();
            if (selected.isPresent()) {
                locataire.setValue(selected.get());
            } else {
                LocataireListItem fallback = new LocataireListItem(existing.getLocataireId(), "Locataire n°" + existing.getLocataireId());
                locataire.getItems().add(fallback);
                locataire.setValue(fallback);
            }
        }
        if (locataire.getValue() == null && !locataire.getItems().isEmpty()) {
            locataire.setValue(locataire.getItems().get(0));
        }
    }

    private String reservationTitle() {
        if (isAdmin()) {
            return "Gestion des Réservations";
        }
        if (isHost()) {
            return "Réservations de mes logements";
        }
        return "Mes Réservations";
    }

    private String reservationSubtitle() {
        if (isAdmin()) {
            return "Gérez toutes les réservations directement dans RentAll";
        }
        if (isHost()) {
            return "Confirmez, refusez ou terminez les réservations liées à vos logements";
        }
        return "Consultez et gérez vos réservations";
    }

    private boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    private boolean isHost() {
        return hasRole("ROLE_HOST");
    }

    private boolean isGuest() {
        return hasRole("ROLE_GUEST") || hasRole("ROLE_USER") || hasRole("ROLE_HOST_PENDING");
    }

    private boolean hasRole(String role) {
        return currentUser != null && role.equals(currentUser.getRole());
    }

    private boolean canManageStatus() {
        if (currentUser == null || currentUser.getRole() == null) {
            return true;
        }
        String role = currentUser.getRole();
        return "ROLE_ADMIN".equals(role) || "ROLE_HOST".equals(role);
    }

    private String resolveStatutForCurrentRole(Reservation existing, ComboBox<String> statut) {
        if (canManageStatus()) {
            return statut.getValue() == null ? "" : statut.getValue().trim();
        }
        if (existing == null) {
            return "en_attente";
        }
        String currentStatus = existing.getStatut();
        return currentStatus == null || currentStatus.isBlank() ? "en_attente" : currentStatus.trim();
    }

    private static String normalizeStatut(String value) {
        if (value == null || value.isBlank()) {
            return "en_attente";
        }
        for (String statut : STATUTS) {
            if (statut.equalsIgnoreCase(value.trim())) {
                return statut;
            }
        }
        return value.trim();
    }

    private static int readSpinnerValueOrThrow(Spinner<Integer> spinner) {
        String text = spinner.getEditor().getText();
        int value = text == null || text.trim().isEmpty()
                ? spinner.getValue()
                : Integer.parseInt(text.trim());
        spinner.getValueFactory().setValue(value);
        return value;
    }

    private static GridPane formGrid() {
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.setPadding(new Insets(15));
        return form;
    }

    private static void addComboRow(GridPane form, int row, String labelText, ComboBox<FoyerListItem> field) {
        addLabel(form, row, labelText);
        field.setStyle(inputStyle());
        form.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private static void addLocataireRow(GridPane form, int row, String labelText, ComboBox<LocataireListItem> field) {
        addLabel(form, row, labelText);
        field.setStyle(inputStyle());
        form.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private static void addStringComboRow(GridPane form, int row, String labelText, ComboBox<String> field) {
        addLabel(form, row, labelText);
        field.setStyle(inputStyle());
        form.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private static void addDateRow(GridPane form, int row, String labelText, DatePicker field) {
        addLabel(form, row, labelText);
        field.setStyle(inputStyle());
        field.setMaxWidth(Double.MAX_VALUE);
        form.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private static void addSpinnerRow(GridPane form, int row, String labelText, Spinner<Integer> field) {
        addLabel(form, row, labelText);
        field.setStyle(inputStyle());
        form.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private static void addRow(GridPane form, int row, String labelText, TextField field) {
        addLabel(form, row, labelText);
        field.setStyle(inputStyle());
        form.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private static void addLabel(GridPane form, int row, String labelText) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        form.add(label, 0, row);
    }

    private Button actionButton(String text, String color, Runnable action) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 13px; "
                + "-fx-font-weight: bold; -fx-padding: 10 18 10 18; -fx-background-radius: 22; -fx-cursor: hand;");
        button.setOnAction(event -> action.run());
        return button;
    }

    private static String inputStyle() {
        return "-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 12; "
                + "-fx-background-radius: 12; -fx-padding: 10 14 10 14; -fx-font-size: 13px;";
    }

    private static String cardStyle() {
        return "-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 2);";
    }

    private static void showWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private static void showErrorDialog(String content) {
        JOptionPane.showMessageDialog(null, content, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    private static void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private record ReservationDraft(FoyerListItem logement,
                                    LocataireListItem locataire,
                                    LocalDateTime debut,
                                    LocalDateTime fin,
                                    BigDecimal total,
                                    String statut,
                                    int nombrePersonnes) {
    }

    private static final class ReservationRow {
        private final int id;
        private final String logement;
        private final String locataire;
        private final String debut;
        private final String fin;
        private final String montant;
        private final String statut;
        private final int personnes;

        private ReservationRow(int id, String logement, String locataire, String debut, String fin,
                               String montant, String statut, int personnes) {
            this.id = id;
            this.logement = logement == null ? "" : logement;
            this.locataire = locataire == null ? "" : locataire;
            this.debut = debut == null ? "" : debut;
            this.fin = fin == null ? "" : fin;
            this.montant = montant == null ? "" : montant;
            this.statut = statut == null ? "" : statut;
            this.personnes = personnes;
        }

        private static ReservationRow from(ReservationTableRow row) {
            return new ReservationRow(
                    row.getId(),
                    row.getFoyerLibelle(),
                    row.getLocataireLibelle(),
                    row.getDateDebut().format(DATE_TIME),
                    row.getDateFin().format(DATE_TIME),
                    row.getMontantTotal() + " DT",
                    row.getStatut(),
                    row.getNombrePersonnes()
            );
        }

        private boolean matches(String query) {
            return String.valueOf(id).contains(query)
                    || logement.toLowerCase(Locale.ROOT).contains(query)
                    || locataire.toLowerCase(Locale.ROOT).contains(query)
                    || debut.toLowerCase(Locale.ROOT).contains(query)
                    || fin.toLowerCase(Locale.ROOT).contains(query)
                    || montant.toLowerCase(Locale.ROOT).contains(query)
                    || statut.toLowerCase(Locale.ROOT).contains(query)
                    || String.valueOf(personnes).contains(query);
        }
    }
}
