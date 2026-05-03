package com.rentall.services;

import com.rentall.util.ValidationResult;

import javax.swing.JOptionPane;

/**
 * Service utilitaire pour afficher des notifications utilisateur via Swing JOptionPane.
 * Fournit une API simple et cohérente pour les messages de succès, erreur et avertissement.
 */
public class NotificationService {
    
    // Private constructor to prevent instantiation
    private NotificationService() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Display a success notification
     * @param title The title of the dialog
     * @param message The message content
     */
    public static void showSuccess(String title, String message) {
        String safeTitle = (title == null || title.trim().isEmpty()) ? "Succès" : title;
        String safeMessage = (message == null || message.trim().isEmpty()) ? "Opération réussie" : message;
        JOptionPane.showMessageDialog(null, safeMessage, safeTitle, JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Display an error notification
     * @param title The title of the dialog
     * @param message The error message content
     */
    public static void showError(String title, String message) {
        String safeTitle = (title == null || title.trim().isEmpty()) ? "Erreur" : title;
        String safeMessage = (message == null || message.trim().isEmpty()) ? "Une erreur s'est produite" : message;
        JOptionPane.showMessageDialog(null, safeMessage, safeTitle, JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Display a warning notification
     * @param title The title of the dialog
     * @param message The warning message content
     */
    public static void showWarning(String title, String message) {
        String safeTitle = (title == null || title.trim().isEmpty()) ? "Attention" : title;
        String safeMessage = (message == null || message.trim().isEmpty()) ? "Avertissement" : message;
        JOptionPane.showMessageDialog(null, safeMessage, safeTitle, JOptionPane.WARNING_MESSAGE);
    }
    
    /**
     * Display an error notification from ValidationResult
     * @param title The title of the dialog
     * @param validationResult The validation result containing error messages
     */
    public static void showError(String title, ValidationResult validationResult) {
        String errorMessage = formatValidationErrors(validationResult);
        showError(title, errorMessage);
    }
    
    /**
     * Format validation errors into a readable message
     * @param validationResult The validation result
     * @return Formatted error message
     */
    private static String formatValidationErrors(ValidationResult validationResult) {
        if (validationResult == null || validationResult.getErrors() == null || validationResult.getErrors().isEmpty()) {
            return "Une erreur de validation s'est produite";
        }
        
        StringBuilder sb = new StringBuilder();
        for (String error : validationResult.getErrors()) {
            if (error != null && !error.trim().isEmpty()) {
                sb.append("• ").append(error).append("\n");
            }
        }
        
        String result = sb.toString().trim();
        return result.isEmpty() ? "Une erreur de validation s'est produite" : result;
    }
}
