package tn.piapp.ui;

import com.rentall.dto.AvisDashboardRow;
import com.rentall.dto.ReservationTableRow;
import com.rentall.entities.Avis;
import com.rentall.entities.Reservation;
import com.rentall.services.AnalyseCommentaireService;
import com.rentall.services.AvisDashboardService;
import com.rentall.services.AvisService;
import com.rentall.services.ReservationService;
import com.rentall.services.SentimentAvisService;
import com.rentall.util.FormValidator;
import com.rentall.util.ValidationResult;
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
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tn.piapp.model.User;
import tn.piapp.util.SessionManager;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class IntegratedAvisView extends BorderPane {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AvisService avisService = new AvisService();
    private final ReservationService reservationService = new ReservationService();
    private final AvisDashboardService dashboardService = new AvisDashboardService();
    private final SentimentAvisService sentimentService = new SentimentAvisService();
    private final AnalyseCommentaireService analyseService = new AnalyseCommentaireService();
    private final ObservableList<AvisRow> allRows = FXCollections.observableArrayList();
    private final TableView<AvisRow> table = new TableView<>();
    private final TextField searchField = new TextField();
    private final ComboBox<String> noteFilter = new ComboBox<>();
    private final ComboBox<String> sentimentFilter = new ComboBox<>();
    private final Label totalLabel = new Label("0");
    private final Label moyenneLabel = new Label("-");
    private final Label positifsLabel = new Label("0");
    private final Label neutresLabel = new Label("0");
    private final Label negatifsLabel = new Label("0");
    private final Label statusLabel = new Label();
    private final User currentUser;

    public IntegratedAvisView() {
        this(SessionManager.getInstance().getCurrentUser());
    }

    public IntegratedAvisView(User currentUser) {
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
        Label title = new Label(avisTitle());
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label subtitle = new Label(avisSubtitle());
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #95a5a6;");
        titleBox.getChildren().addAll(title, subtitle);

        searchField.setPromptText("Rechercher un avis...");
        searchField.setStyle(inputStyle());
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.getChildren().addAll(titleBox, searchField);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);
        configureActions(actions);

        HBox filters = new HBox(12);
        filters.setAlignment(Pos.CENTER_LEFT);
        noteFilter.setItems(FXCollections.observableArrayList("Toutes", "1", "2", "3", "4", "5"));
        noteFilter.setValue("Toutes");
        noteFilter.setStyle(inputStyle());
        sentimentFilter.setItems(FXCollections.observableArrayList("Tous", "Positif", "Neutre", "Négatif"));
        sentimentFilter.setValue("Tous");
        sentimentFilter.setStyle(inputStyle());
        noteFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        sentimentFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        filters.getChildren().addAll(filterBlock("Note", noteFilter), filterBlock("Sentiment", sentimentFilter));

        configureTable();

        VBox tableCard = new VBox(table);
        tableCard.setStyle(cardStyle());
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        page.getChildren().add(header);
        if (!isGuest()) {
            page.getChildren().add(buildStatsPanel());
        }
        page.getChildren().add(actions);
        if (!isGuest()) {
            page.getChildren().add(filters);
        }
        page.getChildren().addAll(tableCard, statusLabel);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        setCenter(page);
    }

    private HBox buildStatsPanel() {
        HBox stats = new HBox(12);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
                statCard("Total avis", totalLabel, "#3498db"),
                statCard("Note moyenne", moyenneLabel, "#6C63FF"),
                statCard("Positifs", positifsLabel, "#27ae60"),
                statCard("Neutres", neutresLabel, "#f39c12"),
                statCard("Négatifs", negatifsLabel, "#e74c3c")
        );
        return stats;
    }

    private void configureActions(HBox actions) {
        if (isGuest()) {
            actions.getChildren().addAll(
                    actionButton("+ Ajouter", "#6C63FF", this::addAvis),
                    actionButton("Modifier mes avis", "#f39c12", this::editAvis),
                    actionButton("Supprimer mes avis", "#e74c3c", this::deleteAvis),
                    actionButton("Actualiser", "#16a085", this::refresh)
            );
        } else if (isHost()) {
            actions.getChildren().addAll(
                    actionButton("Rechercher", "#9B59B6", this::applyFilters),
                    actionButton("Actualiser", "#16a085", this::refresh)
            );
        } else {
            actions.getChildren().addAll(
                    actionButton("Supprimer", "#e74c3c", this::deleteAvis),
                    actionButton("Rechercher", "#9B59B6", this::applyFilters),
                    actionButton("Actualiser", "#16a085", this::refresh)
            );
        }
    }

    private VBox statCard(String title, Label value, String color) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        value.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        VBox card = new VBox(3, label, value);
        card.setMinWidth(120);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setStyle(cardStyle());
        return card;
    }

    private VBox filterBlock(String title, ComboBox<String> comboBox) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        VBox box = new VBox(4, label, comboBox);
        box.setMinWidth(160);
        return box;
    }

    private void configureTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Aucun avis trouvé"));
        table.setStyle("-fx-background-color: white; -fx-border-color: transparent;");

        TableColumn<AvisRow, Number> id = new TableColumn<>("ID");
        id.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().id));
        id.setMaxWidth(70);

        TableColumn<AvisRow, Number> reservation = new TableColumn<>("Réservation");
        reservation.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().reservationId));

        TableColumn<AvisRow, String> logement = new TableColumn<>("Logement");
        logement.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().logement));

        TableColumn<AvisRow, String> locataire = new TableColumn<>("Locataire");
        locataire.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().locataire));

        TableColumn<AvisRow, String> note = new TableColumn<>("Note");
        note.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().noteText));

        TableColumn<AvisRow, String> sentiment = new TableColumn<>("Sentiment");
        sentiment.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().sentiment));

        TableColumn<AvisRow, String> commentaire = new TableColumn<>("Commentaire");
        commentaire.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().commentaire));

        TableColumn<AvisRow, String> date = new TableColumn<>("Date");
        date.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().dateCreation));

        if (isGuest()) {
            table.getColumns().setAll(List.of(reservation, logement, note, commentaire, sentiment, date));
        } else {
            table.getColumns().setAll(List.of(reservation, logement, locataire, note, sentiment, commentaire, date));
        }
    }

    private void refresh() {
        List<AvisRow> rows = dashboardService.getAvisDashboardForUser(currentUser)
                .stream()
                .map(row -> AvisRow.from(row, sentimentService))
                .collect(Collectors.toList());
        allRows.setAll(rows);
        applyFilters();
        statusLabel.setText(rows.size() + " avis chargé(s)");
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String selectedNote = noteFilter.getValue() == null ? "Toutes" : noteFilter.getValue();
        String selectedSentiment = sentimentFilter.getValue() == null ? "Tous" : sentimentFilter.getValue();

        ObservableList<AvisRow> filtered = allRows.stream()
                .filter(row -> query.isEmpty() || row.matches(query))
                .filter(row -> "Toutes".equals(selectedNote) || row.note == Integer.parseInt(selectedNote))
                .filter(row -> matchesSentimentFilter(row, selectedSentiment))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        table.setItems(filtered);
        updateStats(filtered);
    }

    private static boolean matchesSentimentFilter(AvisRow row, String selectedSentiment) {
        return switch (selectedSentiment) {
            case "Positif" -> row.note >= 4;
            case "Neutre" -> row.note == 3;
            case "Négatif" -> row.note <= 2;
            default -> true;
        };
    }

    private void updateStats(List<AvisRow> rows) {
        int total = rows.size();
        long positifs = rows.stream().filter(row -> row.note >= 4).count();
        long neutres = rows.stream().filter(row -> row.note == 3).count();
        long negatifs = rows.stream().filter(row -> row.note <= 2).count();
        double moyenne = rows.stream().mapToInt(row -> row.note).average().orElse(0);

        totalLabel.setText(String.valueOf(total));
        moyenneLabel.setText(total == 0 ? "-" : String.format(Locale.FRANCE, "%.1f/5", moyenne));
        positifsLabel.setText(String.valueOf(positifs));
        neutresLabel.setText(String.valueOf(neutres));
        negatifsLabel.setText(String.valueOf(negatifs));
    }

    private void addAvis() {
        if (!isGuest()) {
            showUnauthorized();
            return;
        }
        Optional<Avis> avis = showAvisDialog(null);
        avis.ifPresent(value -> {
            try {
                avisService.ajouter(value);
                refresh();
                showInfo("Avis ajouté", "Avis ajouté avec succès. Sentiment : "
                        + sentimentService.formaterSentiment(value.getNote()));
            } catch (SQLException e) {
                showWarning("Ajout impossible", readableSqlError(e));
            }
        });
    }

    private void editAvis() {
        if (!isGuest()) {
            showUnauthorized();
            return;
        }
        AvisRow selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Sélection requise", "Sélectionnez un avis à modifier.");
            return;
        }

        Avis existing = avisService.afficherParId(selected.id);
        if (existing == null) {
            showWarning("Introuvable", "L'avis sélectionné n'existe plus.");
            refresh();
            return;
        }

        if (!avisService.peutModifierAvis(existing, currentUser)) {
            showUnauthorized();
            return;
        }

        Optional<Avis> edited = showAvisDialog(existing);
        edited.ifPresent(value -> {
            value.setId(existing.getId());
            avisService.modifier(value);
            refresh();
            showInfo("Avis modifié", "Avis modifié avec succès. Sentiment : "
                    + sentimentService.formaterSentiment(value.getNote()));
        });
    }

    private void deleteAvis() {
        if (isHost()) {
            showUnauthorized();
            return;
        }
        AvisRow selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Sélection requise", "Sélectionnez un avis à supprimer.");
            return;
        }
        if (!avisService.peutSupprimerAvis(selected.id, currentUser)) {
            showUnauthorized();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("Supprimer l'avis #" + selected.id + " ?");
        confirm.setContentText("Cette action supprimera l'avis sélectionné.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            avisService.supprimer(selected.id);
            refresh();
        }
    }

    private Optional<Avis> showAvisDialog(Avis existing) {
        if (!isGuest()) {
            showUnauthorized();
            return Optional.empty();
        }

        List<ReservationChoice> choices = loadReservationChoices(existing);
        if (choices.isEmpty()) {
            showWarning("Ajout impossible", "Aucune réservation terminée disponible pour ajouter un avis.");
            return Optional.empty();
        }

        Dialog<Avis> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Ajouter un avis" : "Modifier un avis");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<ReservationChoice> reservation = new ComboBox<>();
        reservation.setItems(FXCollections.observableArrayList(choices));
        reservation.setMaxWidth(Double.MAX_VALUE);
        preselectReservation(reservation, existing);

        Spinner<Integer> note = new Spinner<>();
        note.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                1, 5, existing == null ? 5 : Math.min(5, Math.max(1, existing.getNote()))));
        note.setEditable(true);
        note.setMaxWidth(Double.MAX_VALUE);

        Label sentiment = new Label();
        sentiment.setStyle("-fx-font-weight: bold; -fx-padding: 8 12 8 12; -fx-background-radius: 12;");
        Runnable updateSentiment = () -> updateSentimentLabel(sentiment, readNoteOrDefault(note));
        note.valueProperty().addListener((obs, oldValue, newValue) -> updateSentiment.run());
        note.getEditor().textProperty().addListener((obs, oldValue, newValue) -> updateSentiment.run());
        updateSentiment.run();

        TextArea commentaire = new TextArea(existing == null ? "" : existing.getCommentaire());
        commentaire.setPrefRowCount(4);
        commentaire.setWrapText(true);

        GridPane form = formGrid();
        addReservationRow(form, 0, "Réservation", reservation);
        addSpinnerRow(form, 1, "Note (1-5)", note);
        addLabel(form, 2, "Sentiment");
        form.add(sentiment, 1, 2);

        Label commentLabel = new Label("Commentaire");
        commentLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        commentaire.setStyle(inputStyle());
        form.add(commentLabel, 0, 3);
        form.add(commentaire, 1, 3);
        GridPane.setHgrow(commentaire, Priority.ALWAYS);

        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }

            AvisDraft draft = validateAvisForm(reservation, note, commentaire);
            return new Avis(
                    draft.reservation().id(),
                    draft.note(),
                    draft.commentaire(),
                    existing == null ? LocalDateTime.now() : existing.getDateCreation()
            );
        });

        try {
            return dialog.showAndWait();
        } catch (RuntimeException e) {
            showWarning("Données invalides", e.getMessage());
            return Optional.empty();
        }
    }

    private AvisDraft validateAvisForm(ComboBox<ReservationChoice> reservation,
                                       Spinner<Integer> note,
                                       TextArea commentaire) {
        ValidationResult result = new ValidationResult();
        addErrors(result, FormValidator.validateSelectionNotNull(reservation.getValue(), "réservation"));

        int parsedNote = readSpinnerValue(note, result);
        if (parsedNote > 0) {
            addErrors(result, FormValidator.validateRating(parsedNote, 1, 5));
            SentimentAvisService.ResultatSentiment sentiment = sentimentService.analyser(parsedNote);
            if (!sentiment.isValide()) {
                result.addError(sentiment.getMessageErreur());
            }
        }

        AnalyseCommentaireService.ResultatAnalyse analyse =
                analyseService.analyser(commentaire.getText());
        if (!analyse.isValide()) {
            result.addError(analyse.getMessageErreur());
        }

        if (!result.isValid()) {
            throw new IllegalArgumentException(result.getAllErrors());
        }

        return new AvisDraft(reservation.getValue(), parsedNote, commentaire.getText().trim());
    }

    private List<ReservationChoice> loadReservationChoices(Avis existing) {
        Set<Integer> reservationsAvecAvis = avisService.afficherTous().stream()
                .filter(avis -> existing == null || avis.getId() != existing.getId())
                .map(Avis::getReservationId)
                .collect(Collectors.toCollection(HashSet::new));

        List<ReservationTableRow> rows = reservationService.listerPourAffichageTableauPourUtilisateur(currentUser);
        List<ReservationChoice> choices = rows.stream()
                .filter(row -> isTerminee(row.getStatut()))
                .filter(row -> !reservationsAvecAvis.contains(row.getId())
                        || (existing != null && row.getId() == existing.getReservationId()))
                .map(ReservationChoice::from)
                .collect(Collectors.toList());

        if (!choices.isEmpty()) {
            return choices;
        }

        return reservationService.afficherTousPourUtilisateur(currentUser).stream()
                .filter(row -> isTerminee(row.getStatut()))
                .filter(row -> !reservationsAvecAvis.contains(row.getId())
                        || (existing != null && row.getId() == existing.getReservationId()))
                .map(ReservationChoice::fromReservation)
                .collect(Collectors.toList());
    }

    private static void preselectReservation(ComboBox<ReservationChoice> reservation, Avis existing) {
        if (existing != null) {
            reservation.getItems().stream()
                    .filter(item -> item.id() == existing.getReservationId())
                    .findFirst()
                    .ifPresent(reservation::setValue);
        }
        if (reservation.getValue() == null && !reservation.getItems().isEmpty()) {
            reservation.setValue(reservation.getItems().get(0));
        }
    }

    private void updateSentimentLabel(Label label, int note) {
        SentimentAvisService.Sentiment sentiment = sentimentService.getSentiment(note);
        if (sentiment == null) {
            label.setText("Note invalide");
            label.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            return;
        }
        String color = switch (sentiment) {
            case POSITIF -> "#27ae60";
            case NEUTRE -> "#f39c12";
            case NEGATIF -> "#e74c3c";
        };
        label.setText(sentiment.toString());
        label.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; "
                + "-fx-padding: 8 12 8 12; -fx-background-radius: 12; "
                + "-fx-background-color: rgba(0,0,0,0.04);");
    }

    private String avisTitle() {
        if (isGuest()) {
            return "Mes Avis";
        }
        if (isHost()) {
            return "Avis sur mes logements";
        }
        return "Gestion des Avis";
    }

    private String avisSubtitle() {
        if (isGuest()) {
            return "Consultez et gérez uniquement vos avis";
        }
        if (isHost()) {
            return "Consultez les avis liés à vos logements";
        }
        return "Consultez, filtrez et gérez les avis clients";
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

    private void showUnauthorized() {
        showWarning("Action non autorisée", "Action non autorisée pour votre rôle.");
    }

    private static boolean isTerminee(String statut) {
        return "terminee".equals(normalizeStatut(statut));
    }

    private static String normalizeStatut(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace("é", "e")
                .replace("è", "e")
                .replace("ê", "e");
    }

    private static int readNoteOrDefault(Spinner<Integer> spinner) {
        try {
            return readSpinnerValueOrThrow(spinner);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private static int readSpinnerValue(Spinner<Integer> spinner, ValidationResult result) {
        try {
            return readSpinnerValueOrThrow(spinner);
        } catch (RuntimeException e) {
            result.addError("La note est obligatoire et doit être un entier entre 1 et 5.");
            return 0;
        }
    }

    private static int readSpinnerValueOrThrow(Spinner<Integer> spinner) {
        String text = spinner.getEditor().getText();
        int value = text == null || text.trim().isEmpty()
                ? spinner.getValue()
                : Integer.parseInt(text.trim());
        spinner.getValueFactory().setValue(value);
        return value;
    }

    private static void addErrors(ValidationResult target, ValidationResult source) {
        if (!source.isValid()) {
            source.getErrors().forEach(target::addError);
        }
    }

    private static String readableSqlError(SQLException e) {
        String message = e.getMessage();
        if (message != null && message.toLowerCase(Locale.ROOT).contains("refus")) {
            return "Action non autorisée pour votre rôle.";
        }
        if (message != null && message.toLowerCase(Locale.ROOT).contains("duplicate")) {
            return "Un avis existe déjà pour cette réservation.";
        }
        return message == null ? "Erreur SQL lors de l'enregistrement." : message;
    }

    private static GridPane formGrid() {
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.setPadding(new Insets(15));
        return form;
    }

    private static void addReservationRow(GridPane form, int row, String labelText,
                                          ComboBox<ReservationChoice> field) {
        addLabel(form, row, labelText);
        field.setStyle(inputStyle());
        form.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private static void addSpinnerRow(GridPane form, int row, String labelText, Spinner<Integer> field) {
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

    private static void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private static String noteStars(int note) {
        StringBuilder out = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            out.append(i <= note ? "★" : "☆");
        }
        return out.toString();
    }

    private record AvisDraft(ReservationChoice reservation, int note, String commentaire) {
    }

    private record ReservationChoice(int id, String label) {
        private static ReservationChoice from(ReservationTableRow row) {
            String label = "Réservation #" + row.getId()
                    + " | " + row.getFoyerLibelle()
                    + " | " + row.getLocataireLibelle()
                    + " | " + row.getDateDebut().toLocalDate() + " -> " + row.getDateFin().toLocalDate();
            return new ReservationChoice(row.getId(), label);
        }

        private static ReservationChoice fromReservation(Reservation reservation) {
            String label = "Réservation #" + reservation.getId()
                    + " | Logement " + reservation.getFoyerId()
                    + " | " + reservation.getDateDebut().toLocalDate()
                    + " -> " + reservation.getDateFin().toLocalDate();
            return new ReservationChoice(reservation.getId(), label);
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class AvisRow {
        private final int id;
        private final int reservationId;
        private final String logement;
        private final String locataire;
        private final int note;
        private final String noteText;
        private final String sentiment;
        private final String commentaire;
        private final String dateCreation;

        private AvisRow(int id, int reservationId, String logement, String locataire,
                        int note, String noteText, String sentiment, String commentaire, String dateCreation) {
            this.id = id;
            this.reservationId = reservationId;
            this.logement = logement == null ? "" : logement;
            this.locataire = locataire == null ? "" : locataire;
            this.note = note;
            this.noteText = noteText == null ? "" : noteText;
            this.sentiment = sentiment == null ? "" : sentiment;
            this.commentaire = commentaire == null ? "" : commentaire;
            this.dateCreation = dateCreation == null ? "" : dateCreation;
        }

        private static AvisRow from(AvisDashboardRow row, SentimentAvisService sentimentService) {
            return new AvisRow(
                    row.getAvisId(),
                    row.getReservationId(),
                    row.getLogementLibelle(),
                    row.getLocataireLibelle(),
                    row.getNote(),
                    noteStars(row.getNote()) + " " + row.getNote() + "/5",
                    sentimentService.getLibelleSentiment(row.getNote()),
                    row.getCommentaire(),
                    row.getDateCreation() == null ? "" : row.getDateCreation().format(DATE_TIME)
            );
        }

        private boolean matches(String query) {
            return String.valueOf(id).contains(query)
                    || String.valueOf(reservationId).contains(query)
                    || logement.toLowerCase(Locale.ROOT).contains(query)
                    || locataire.toLowerCase(Locale.ROOT).contains(query)
                    || noteText.toLowerCase(Locale.ROOT).contains(query)
                    || sentiment.toLowerCase(Locale.ROOT).contains(query)
                    || commentaire.toLowerCase(Locale.ROOT).contains(query)
                    || dateCreation.toLowerCase(Locale.ROOT).contains(query);
        }
    }
}
