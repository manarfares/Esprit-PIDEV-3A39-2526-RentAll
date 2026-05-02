package org.example.controller;

import java.sql.SQLException;

import org.example.dao.ManzelCRUDManager;
import org.example.model.Manzel;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for manzel-form.fxml.
 * Used for both Add and Edit operations.
 * Call setMode() before showing the window.
 */
public class ManzelFormController {

    @FXML private Label     lblTitle;
    @FXML private TextField tfName;
    @FXML private TextField tfAddress;
    @FXML private TextField tfPrice;
    @FXML private TextField tfRooms;
    @FXML private Label     lblStatus;

    private ManzelCRUDManager crudManager;
    private Manzel existingRecord; // null = Add mode, non-null = Edit mode
    private Runnable onSuccess;    // callback to refresh the main page after save

    /**
     * Call this before showing the form window.
     * @param crudManager  shared CRUD manager
     * @param record       null for Add, existing Manzel for Edit
     * @param onSuccess    callback invoked after a successful save
     */
    public void setMode(ManzelCRUDManager crudManager, Manzel record, Runnable onSuccess) {
        this.crudManager = crudManager;
        this.existingRecord = record;
        this.onSuccess = onSuccess;

        if (record != null) {
            // Edit mode — pre-fill the form
            lblTitle.setText("Edit Record");
            tfName.setText(record.getName());
            tfAddress.setText(record.getAddress());
            tfPrice.setText(String.valueOf(record.getPrice()));
            tfRooms.setText(String.valueOf(record.getRooms()));
        } else {
            lblTitle.setText("Add Record");
        }
    }

    /** Validates input and saves (insert or update) the record. */
    @FXML
    private void handleSave() {
        try {
            String name    = tfName.getText();
            String address = tfAddress.getText();
            int price      = Integer.parseInt(tfPrice.getText());
            int rooms      = Integer.parseInt(tfRooms.getText());

            if (existingRecord == null) {
                // Add mode
                crudManager.create(new Manzel(name, address, price, rooms));
            } else {
                // Edit mode — preserve originalPrice and listedDate from the existing record
                crudManager.update(new Manzel(
                        existingRecord.getId(), name, address, price,
                        existingRecord.getOriginalPrice(), rooms,
                        existingRecord.getListedDate()));
            }

            // Notify main page to refresh, then close this window
            if (onSuccess != null) onSuccess.run();
            closeWindow();

        } catch (NumberFormatException e) {
            setStatus("Price and Rooms must be valid numbers.", true);
        } catch (SQLException | IllegalArgumentException e) {
            setStatus("Error: " + e.getMessage(), true);
        }
    }

    /** Closes the form window without saving. */
    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        ((Stage) tfName.getScene().getWindow()).close();
    }

    private void setStatus(String msg, boolean error) {
        lblStatus.setText(msg);
        lblStatus.setStyle(error ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}
