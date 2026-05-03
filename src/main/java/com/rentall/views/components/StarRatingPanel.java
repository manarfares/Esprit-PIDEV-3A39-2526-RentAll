package com.rentall.views.components;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Selecteur de note 1-5 conserve sous forme d'entier, affiche sous forme d'etoiles.
 */
public class StarRatingPanel extends JPanel {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final String FILLED_STAR = "\u2605";
    private static final String EMPTY_STAR = "\u2606";
    private static final Color ACTIVE = new Color(217, 119, 6);
    private static final Color INACTIVE = new Color(148, 163, 184);

    private final JButton[] buttons = new JButton[MAX_RATING];
    private final List<ChangeListener> listeners = new ArrayList<>();
    private int rating;

    public StarRatingPanel(int initialRating) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setPreferredSize(new Dimension(180, 42));
        setMinimumSize(new Dimension(150, 42));

        for (int i = 0; i < buttons.length; i++) {
            final int value = i + 1;
            JButton button = new JButton();
            button.setHorizontalAlignment(SwingConstants.CENTER);
            button.setPreferredSize(new Dimension(34, 42));
            button.setFont(new Font("Segoe UI Symbol", Font.BOLD, 25));
            button.setFocusPainted(false);
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setOpaque(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setToolTipText(value + "/5");
            button.addActionListener(e -> setRating(value));
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    render(value);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    render(rating);
                }
            });
            buttons[i] = button;
            add(button);
        }

        setRating(initialRating);
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        int normalized = Math.max(MIN_RATING, Math.min(MAX_RATING, rating));
        if (this.rating == normalized) {
            render(normalized);
            return;
        }
        this.rating = normalized;
        render(normalized);
        fireChangeEvent();
    }

    public void addChangeListener(ChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    private void render(int previewRating) {
        for (int i = 0; i < buttons.length; i++) {
            boolean filled = i < previewRating;
            buttons[i].setText(filled ? FILLED_STAR : EMPTY_STAR);
            buttons[i].setForeground(filled ? ACTIVE : INACTIVE);
        }
        setToolTipText(rating + "/5");
    }

    private void fireChangeEvent() {
        ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener listener : listeners) {
            listener.stateChanged(event);
        }
    }
}
