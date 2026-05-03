package com.rentall.views.components;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Sélecteur d'heure moderne avec spinners
 */
public class ModernTimePicker extends JPanel {

    private static final int CONTROL_HEIGHT = 38;

    private JSpinner hourSpinner;
    private JSpinner minuteSpinner;
    private LocalTime selectedTime;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
    private List<TimeChangeListener> listeners = new ArrayList<>();
    
    public interface TimeChangeListener {
        void timeChanged(LocalTime newTime);
    }
    
    public ModernTimePicker() {
        this(LocalTime.of(14, 0));
    }
    
    public ModernTimePicker(LocalTime initialTime) {
        this.selectedTime = initialTime;
        setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        setOpaque(false);
        setPreferredSize(new Dimension(200, CONTROL_HEIGHT));
        setMinimumSize(new Dimension(200, CONTROL_HEIGHT));
        
        // Spinner pour les heures
        SpinnerNumberModel hourModel = new SpinnerNumberModel(
            initialTime.getHour(), 0, 23, 1
        );
        hourSpinner = new JSpinner(hourModel);
        styleSpinner(hourSpinner);
        
        // Label séparateur
        JLabel separator = new JLabel(":");
        separator.setFont(new Font("Segoe UI", Font.BOLD, 15));
        separator.setForeground(new Color(71, 85, 105));
        separator.setBorder(new EmptyBorder(0, 0, 0, 0));
        
        // Spinner pour les minutes
        SpinnerNumberModel minuteModel = new SpinnerNumberModel(
            initialTime.getMinute(), 0, 59, 5
        );
        minuteSpinner = new JSpinner(minuteModel);
        styleSpinner(minuteSpinner);
        
        // Ajouter les listeners
        hourSpinner.addChangeListener(e -> updateTime());
        minuteSpinner.addChangeListener(e -> updateTime());
        
        // Label d'icône
        JLabel clockIcon = new JLabel("🕐");
        clockIcon.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        clockIcon.setPreferredSize(new Dimension(18, CONTROL_HEIGHT));
        clockIcon.setHorizontalAlignment(SwingConstants.CENTER);
        
        add(clockIcon);
        add(hourSpinner);
        add(separator);
        add(minuteSpinner);
    }
    
    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        spinner.setPreferredSize(new Dimension(58, CONTROL_HEIGHT));
        spinner.setMinimumSize(new Dimension(58, CONTROL_HEIGHT));
        
        // Style de l'éditeur
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JSpinner.DefaultEditor spinnerEditor = (JSpinner.DefaultEditor) editor;
            spinnerEditor.getTextField().setHorizontalAlignment(JTextField.CENTER);
            spinnerEditor.getTextField().setFont(new Font("Segoe UI", Font.BOLD, 14));
            spinnerEditor.getTextField().setBorder(new CompoundBorder(
                new LineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(7, 8, 7, 8)
            ));
        }
        
        // Style des boutons
        for (Component comp : spinner.getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                button.setBackground(new Color(241, 245, 249));
                button.setBorderPainted(false);
                button.setFocusPainted(false);
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        }
    }
    
    private void updateTime() {
        int hour = (Integer) hourSpinner.getValue();
        int minute = (Integer) minuteSpinner.getValue();
        selectedTime = LocalTime.of(hour, minute);
        notifyListeners();
    }
    
    public void setTime(LocalTime time) {
        this.selectedTime = time;
        hourSpinner.setValue(time.getHour());
        minuteSpinner.setValue(time.getMinute());
    }
    
    public LocalTime getTime() {
        return selectedTime;
    }
    
    public String getTimeString() {
        return selectedTime.format(formatter);
    }
    
    public void addTimeChangeListener(TimeChangeListener listener) {
        listeners.add(listener);
    }
    
    private void notifyListeners() {
        for (TimeChangeListener listener : listeners) {
            listener.timeChanged(selectedTime);
        }
    }
}
