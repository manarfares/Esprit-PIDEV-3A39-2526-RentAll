package com.rentall.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Classe utilitaire pour valider les champs de formulaire
 * Similaire aux validations Symfony
 */
public class FormValidator {
    
    // ==================== VALIDATIONS TEXTE ====================
    
    /**
     * Valide qu'un champ texte n'est pas vide
     */
    public static ValidationResult validateNotEmpty(String value, String fieldName) {
        ValidationResult result = new ValidationResult();
        if (value == null || value.trim().isEmpty()) {
            result.addError(fieldName + " est obligatoire.");
        }
        return result;
    }
    
    /**
     * Valide la longueur d'un texte
     */
    public static ValidationResult validateLength(String value, String fieldName, int min, int max) {
        ValidationResult result = new ValidationResult();
        if (value == null) {
            result.addError(fieldName + " est obligatoire.");
            return result;
        }
        
        String trimmed = value.trim();
        if (trimmed.length() < min) {
            result.addError(fieldName + " doit contenir au moins " + min + " caractères.");
        }
        if (trimmed.length() > max) {
            result.addError(fieldName + " ne peut pas dépasser " + max + " caractères.");
        }
        return result;
    }
    
    /**
     * Valide qu'un texte contient uniquement des lettres et espaces
     */
    public static ValidationResult validateLettersOnly(String value, String fieldName) {
        ValidationResult result = new ValidationResult();
        if (value == null || value.trim().isEmpty()) {
            result.addError(fieldName + " est obligatoire.");
            return result;
        }
        
        if (!value.trim().matches("^[a-zA-ZÀ-ÿ\\s'-]+$")) {
            result.addError(fieldName + " ne peut contenir que des lettres.");
        }
        return result;
    }
    
    /**
     * Valide qu'un texte n'est pas composé uniquement d'espaces
     */
    public static ValidationResult validateNotOnlySpaces(String value, String fieldName) {
        ValidationResult result = new ValidationResult();
        if (value == null || value.trim().isEmpty()) {
            result.addError(fieldName + " ne peut pas être vide ou composé uniquement d'espaces.");
        }
        return result;
    }
    
    // ==================== VALIDATIONS NUMÉRIQUES ====================
    
    /**
     * Valide qu'un nombre est dans une plage
     */
    public static ValidationResult validateNumberRange(int value, String fieldName, int min, int max) {
        ValidationResult result = new ValidationResult();
        if (value < min) {
            result.addError(fieldName + " doit être au minimum " + min + ".");
        }
        if (value > max) {
            result.addError(fieldName + " ne peut pas dépasser " + max + ".");
        }
        return result;
    }
    
    /**
     * Valide qu'un nombre est positif
     */
    public static ValidationResult validatePositive(int value, String fieldName) {
        ValidationResult result = new ValidationResult();
        if (value <= 0) {
            result.addError(fieldName + " doit être un nombre positif.");
        }
        return result;
    }
    
    /**
     * Valide qu'un nombre décimal est positif
     */
    public static ValidationResult validatePositiveDecimal(double value, String fieldName) {
        ValidationResult result = new ValidationResult();
        if (value <= 0) {
            result.addError(fieldName + " doit être un nombre positif.");
        }
        return result;
    }
    
    // ==================== VALIDATIONS DATES ====================
    
    /**
     * Valide qu'une date n'est pas nulle
     */
    public static ValidationResult validateDateNotNull(LocalDate date, String fieldName) {
        ValidationResult result = new ValidationResult();
        if (date == null) {
            result.addError(fieldName + " est obligatoire.");
        }
        return result;
    }
    
    /**
     * Valide qu'une date/heure n'est pas nulle
     */
    public static ValidationResult validateDateTimeNotNull(LocalDateTime dateTime, String fieldName) {
        ValidationResult result = new ValidationResult();
        if (dateTime == null) {
            result.addError(fieldName + " est obligatoire.");
        }
        return result;
    }
    
    /**
     * Valide que la date de fin est après la date de début
     */
    public static ValidationResult validateDateRange(LocalDateTime debut, LocalDateTime fin) {
        ValidationResult result = new ValidationResult();
        if (debut == null || fin == null) {
            result.addError("Les dates de début et de fin sont obligatoires.");
            return result;
        }
        
        if (!fin.isAfter(debut)) {
            result.addError("La date et l'heure de fin doivent être strictement après le début.");
        }
        return result;
    }
    
    /**
     * Valide qu'un séjour couvre au moins une nuit
     */
    public static ValidationResult validateMinimumNights(LocalDateTime debut, LocalDateTime fin, int minNights) {
        ValidationResult result = new ValidationResult();
        if (debut == null || fin == null) {
            result.addError("Les dates sont obligatoires.");
            return result;
        }
        
        long days = java.time.temporal.ChronoUnit.DAYS.between(debut.toLocalDate(), fin.toLocalDate());
        if (days < minNights) {
            result.addError("Le séjour doit couvrir au moins " + minNights + " nuit(s).");
        }
        return result;
    }
    
    /**
     * Valide qu'une heure n'est pas nulle
     */
    public static ValidationResult validateTimeNotNull(LocalTime time, String fieldName) {
        ValidationResult result = new ValidationResult();
        if (time == null) {
            result.addError(fieldName + " est obligatoire.");
        }
        return result;
    }
    
    // ==================== VALIDATIONS SÉLECTIONS ====================
    
    /**
     * Valide qu'un objet sélectionné n'est pas nul
     */
    public static ValidationResult validateSelectionNotNull(Object selection, String fieldName) {
        ValidationResult result = new ValidationResult();
        if (selection == null) {
            result.addError("Veuillez sélectionner un(e) " + fieldName + ".");
        }
        return result;
    }
    
    /**
     * Valide qu'une chaîne sélectionnée n'est pas vide
     */
    public static ValidationResult validateSelectionNotEmpty(String selection, String fieldName) {
        ValidationResult result = new ValidationResult();
        if (selection == null || selection.trim().isEmpty() || selection.equals("Sélectionner...")) {
            result.addError("Veuillez sélectionner un(e) " + fieldName + ".");
        }
        return result;
    }
    
    // ==================== VALIDATIONS NOTES/RATINGS ====================
    
    /**
     * Valide qu'une note est dans une plage valide
     */
    public static ValidationResult validateRating(int rating, int min, int max) {
        ValidationResult result = new ValidationResult();
        if (rating < min || rating > max) {
            result.addError("La note doit être comprise entre " + min + " et " + max + ".");
        }
        return result;
    }
    
    // ==================== VALIDATIONS COMMENTAIRES ====================
    
    /**
     * Valide un commentaire/avis
     */
    public static ValidationResult validateComment(String comment, int minLength, int maxLength) {
        ValidationResult result = new ValidationResult();
        
        if (comment == null || comment.trim().isEmpty()) {
            result.addError("Le commentaire est obligatoire.");
            return result;
        }
        
        String trimmed = comment.trim();
        
        if (trimmed.length() < minLength) {
            result.addError("Le commentaire doit contenir au moins " + minLength + " caractères.");
        }
        
        if (trimmed.length() > maxLength) {
            result.addError("Le commentaire ne peut pas dépasser " + maxLength + " caractères.");
        }
        
        // Vérifier que ce n'est pas uniquement des espaces ou caractères répétés
        if (trimmed.matches("^(.)\\1+$")) {
            result.addError("Le commentaire ne peut pas être composé d'un seul caractère répété.");
        }
        
        return result;
    }
    
    // ==================== MÉTHODES UTILITAIRES ====================
    
    /**
     * Nettoie une chaîne (trim)
     */
    public static String sanitize(String value) {
        return value == null ? "" : value.trim();
    }
    
    /**
     * Combine plusieurs résultats de validation
     */
    public static ValidationResult combine(ValidationResult... results) {
        ValidationResult combined = new ValidationResult();
        for (ValidationResult result : results) {
            if (!result.isValid()) {
                for (String error : result.getErrors()) {
                    combined.addError(error);
                }
            }
        }
        return combined;
    }
}
