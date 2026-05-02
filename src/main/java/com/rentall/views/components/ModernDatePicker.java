package com.rentall.views.components;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Sélecteur de date moderne avec calendrier popup
 */
public class ModernDatePicker extends JPanel {

    private static final int FIELD_HEIGHT = 38;

    private JTextField dateField;
    private JButton calendarButton;
    private LocalDate selectedDate;
    private YearMonth displayedMonth;
    private JPopupMenu calendarPopup;
    private JLabel monthLabel;
    private JPanel daysPanel;
    private JPanel calendarPanel;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final List<DateChangeListener> listeners = new ArrayList<>();

    public interface DateChangeListener {
        void dateChanged(LocalDate newDate);
    }

    public ModernDatePicker() {
        this(LocalDate.now());
    }

    public ModernDatePicker(LocalDate initialDate) {
        this.selectedDate = initialDate;
        this.displayedMonth = YearMonth.from(initialDate);
        setLayout(new BorderLayout(5, 0));
        setOpaque(false);

        // Champ de texte pour afficher la date
        dateField = new JTextField(10);
        dateField.setText(selectedDate.format(formatter));
        dateField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dateField.setEditable(false);
        dateField.setBackground(Color.WHITE);
        dateField.setPreferredSize(new Dimension(160, FIELD_HEIGHT));
        dateField.setBorder(new CompoundBorder(
            new LineBorder(new Color(203, 213, 225), 1, true),
            new EmptyBorder(7, 12, 7, 12)
        ));

        // Bouton calendrier
        calendarButton = new JButton("📅");
        calendarButton.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        calendarButton.setFocusPainted(false);
        calendarButton.setBorderPainted(false);
        calendarButton.setBackground(new Color(241, 245, 249));
        calendarButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        calendarButton.setPreferredSize(new Dimension(42, FIELD_HEIGHT));

        calendarButton.addActionListener(e -> showCalendar());

        add(dateField, BorderLayout.CENTER);
        add(calendarButton, BorderLayout.EAST);
    }

    private void showCalendar() {
        if (calendarPopup != null && calendarPopup.isVisible()) {
            calendarPopup.setVisible(false);
            return;
        }

        displayedMonth = YearMonth.from(selectedDate);
        calendarPopup = new JPopupMenu();
        calendarPopup.setBorder(BorderFactory.createEmptyBorder());
        calendarPopup.add(createCalendarPanel());

        SwingUtilities.invokeLater(() -> {
            positionPopup();
            dateField.requestFocusInWindow();
        });
    }

    private void positionPopup() {
        if (calendarPopup == null) {
            return;
        }
        if (!dateField.isShowing()) {
            return;
        }
        Dimension popupSize = calendarPopup.getPreferredSize();
        Point screenPoint;
        try {
            screenPoint = dateField.getLocationOnScreen();
        } catch (IllegalComponentStateException ex) {
            return;
        }
        Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

        int x = 0;
        int y = dateField.getHeight() + 4;
        int absoluteX = screenPoint.x;
        int absoluteY = screenPoint.y + y;

        if (absoluteY + popupSize.height > bounds.y + bounds.height) {
            y = -(popupSize.height + 4);
        }
        if (absoluteX + popupSize.width > bounds.x + bounds.width) {
            x = dateField.getWidth() - popupSize.width;
        }
        calendarPopup.show(dateField, x, y);
    }

    private JPanel createCalendarPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(new CompoundBorder(
            new LineBorder(new Color(203, 213, 225), 1, true),
            new EmptyBorder(10, 10, 10, 10)
        ));
        panel.setBackground(Color.WHITE);
        this.calendarPanel = panel;

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JButton prevMonth = new JButton("◀");
        JButton nextMonth = new JButton("▶");
        styleNavButton(prevMonth);
        styleNavButton(nextMonth);

        monthLabel = new JLabel();
        monthLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        monthLabel.setHorizontalAlignment(SwingConstants.CENTER);

        header.add(prevMonth, BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(nextMonth, BorderLayout.EAST);

        // Keep exactly 7 columns and 6 week rows (+1 header row) for stable rendering.
        daysPanel = new JPanel(new GridLayout(7, 7, 3, 3));
        daysPanel.setOpaque(false);

        String[] dayNames = {"Lu", "Ma", "Me", "Je", "Ve", "Sa", "Di"};
        for (String day : dayNames) {
            JLabel label = new JLabel(day, SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 12));
            label.setForeground(new Color(100, 116, 139));
            daysPanel.add(label);
        }

        refreshCalendarDays();

        prevMonth.addActionListener(e -> {
            displayedMonth = displayedMonth.minusMonths(1);
            refreshCalendarDays();
        });

        nextMonth.addActionListener(e -> {
            displayedMonth = displayedMonth.plusMonths(1);
            refreshCalendarDays();
        });

        panel.add(header, BorderLayout.NORTH);
        panel.add(daysPanel, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(270, 250));
        return panel;
    }

    private void refreshCalendarDays() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshCalendarDays);
            return;
        }
        String monthName = displayedMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        if (!monthName.isEmpty()) {
            monthName = monthName.substring(0, 1).toUpperCase(Locale.FRENCH) + monthName.substring(1);
        }
        monthLabel.setText(monthName + " " + displayedMonth.getYear());
        updateDaysPanel(daysPanel, displayedMonth);
        daysPanel.revalidate();
        daysPanel.repaint();
        if (calendarPanel != null) {
            calendarPanel.revalidate();
            calendarPanel.repaint();
        }
    }

    private void updateDaysPanel(JPanel daysPanel, YearMonth month) {
        Component[] components = daysPanel.getComponents();
        for (int i = 7; i < components.length; i++) {
            daysPanel.remove(components[i]);
        }

        LocalDate firstDay = month.atDay(1);
        int dayOfWeek = firstDay.getDayOfWeek().getValue();

        for (int i = 1; i < dayOfWeek; i++) {
            JLabel empty = new JLabel("");
            empty.setOpaque(true);
            empty.setBackground(Color.WHITE);
            daysPanel.add(empty);
        }

        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            System.out.println("Adding day = " + day);
            JButton dayButton = new JButton(String.valueOf(day));
            dayButton.setFont(new Font("Arial", Font.PLAIN, 12));
            dayButton.setForeground(Color.BLACK);
            dayButton.setBackground(Color.WHITE);
            dayButton.setOpaque(true);
            dayButton.setContentAreaFilled(true);
            dayButton.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            styleDayButton(dayButton, date);

            dayButton.addActionListener(e -> {
                setDate(date);
                if (calendarPopup != null) {
                    calendarPopup.setVisible(false);
                }
            });

            daysPanel.add(dayButton);
        }

        // Fill trailing cells so the month grid always occupies 6 full weeks.
        int targetCells = 49; // 7 weekday headers + 42 date cells
        while (daysPanel.getComponentCount() < targetCells) {
            JLabel empty = new JLabel("");
            empty.setOpaque(true);
            empty.setBackground(Color.WHITE);
            daysPanel.add(empty);
        }
        System.out.println("Days panel count = " + daysPanel.getComponentCount());
        daysPanel.revalidate();
        daysPanel.repaint();
        if (calendarPanel != null) {
            calendarPanel.revalidate();
            calendarPanel.repaint();
        }
    }

    private void styleNavButton(JButton btn) {
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setBackground(new Color(241, 245, 249));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(34, 28));
    }

    private void styleDayButton(JButton btn, LocalDate date) {
        btn.setFont(new Font("Arial", Font.PLAIN, 12));
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setVerticalAlignment(SwingConstants.CENTER);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(30, 28));

        if (date.equals(selectedDate)) {
            btn.setBackground(new Color(52, 152, 219));
            btn.setForeground(Color.WHITE);
        } else if (date.equals(LocalDate.now())) {
            btn.setBackground(new Color(241, 245, 249));
            btn.setForeground(new Color(52, 152, 219));
        } else {
            btn.setBackground(Color.WHITE);
            btn.setForeground(Color.BLACK);
        }

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!date.equals(selectedDate)) {
                    btn.setBackground(new Color(224, 242, 254));
                    btn.setForeground(Color.BLACK);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!date.equals(selectedDate)) {
                    btn.setBackground(date.equals(LocalDate.now()) ?
                        new Color(241, 245, 249) : Color.WHITE);
                    btn.setForeground(date.equals(LocalDate.now()) ? new Color(52, 152, 219) : Color.BLACK);
                }
            }
        });
    }

    public void setDate(LocalDate date) {
        this.selectedDate = date;
        this.displayedMonth = YearMonth.from(date);
        dateField.setText(date.format(formatter));
        notifyListeners();
    }

    public LocalDate getDate() {
        return selectedDate;
    }

    public void addDateChangeListener(DateChangeListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (DateChangeListener listener : listeners) {
            listener.dateChanged(selectedDate);
        }
    }
}