package com.rentall.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Résultat d'une validation de formulaire
 */
public class ValidationResult {
    private boolean valid;
    private List<String> errors;
    
    public ValidationResult() {
        this.valid = true;
        this.errors = new ArrayList<>();
    }
    
    public void addError(String error) {
        this.valid = false;
        this.errors.add(error);
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public String getFirstError() {
        return errors.isEmpty() ? "" : errors.get(0);
    }
    
    public String getAllErrors() {
        return String.join("\n", errors);
    }
}
