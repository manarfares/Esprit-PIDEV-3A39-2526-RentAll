package org.example.utils;

import java.util.regex.Pattern;

public class ValidationUtil {

    // ===================== USERNAME =====================
    public static String validerUsername(String username) {
        if (username == null || username.trim().isEmpty())
            return "⚠️ Le nom d'utilisateur est obligatoire.";
        if (username.length() < 3)
            return "⚠️ Minimum 3 caractères requis.";
        if (username.length() > 30)
            return "⚠️ Maximum 30 caractères autorisés.";
        if (!Pattern.matches("^[a-zA-Z0-9_.-]+$", username))
            return "⚠️ Pas d'espaces ni caractères spéciaux (sauf _ . -)";
        return null; // ✅ valide
    }

    // ===================== EMAIL =====================
    public static String validerEmail(String email) {
        if (email == null || email.trim().isEmpty())
            return "⚠️ L'email est obligatoire.";
        if (!Pattern.matches("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$", email))
            return "⚠️ Format invalide. Ex: nom@gmail.com";
        return null; // ✅ valide
    }

    // ===================== TELEPHONE =====================
    public static String validerTelephone(String phone) {
        if (phone == null || phone.trim().isEmpty())
            return null; // optionnel → OK si vide
        // Accepte : +21612345678 ou 12345678 ou +216 12 345 678
        String clean = phone.replaceAll("\\s", "");
        if (!Pattern.matches("^(\\+216)?[0-9]{8}$", clean))
            return "⚠️ Format invalide. Ex: +216 XX XXX XXX";
        return null; // ✅ valide
    }

    // ===================== MOT DE PASSE =====================
    public static String validerPassword(String password) {
        if (password == null || password.isEmpty())
            return "⚠️ Le mot de passe est obligatoire.";
        if (password.length() < 8)
            return "⚠️ Minimum 8 caractères requis.";
        if (!Pattern.matches(".*[A-Z].*", password))
            return "⚠️ Doit contenir au moins une majuscule.";
        if (!Pattern.matches(".*[a-z].*", password))
            return "⚠️ Doit contenir au moins une minuscule.";
        if (!Pattern.matches(".*[0-9].*", password))
            return "⚠️ Doit contenir au moins un chiffre.";
        if (!Pattern.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*", password))
            return "⚠️ Doit contenir au moins un symbole (!@#$...).";
        return null; // ✅ valide
    }

    // ===================== FORCE MOT DE PASSE =====================
    public static int forcePassword(String password) {
        if (password == null || password.isEmpty()) return 0;
        int score = 0;
        if (password.length() >= 8)  score++;
        if (password.length() >= 12) score++;
        if (Pattern.matches(".*[A-Z].*", password)) score++;
        if (Pattern.matches(".*[a-z].*", password)) score++;
        if (Pattern.matches(".*[0-9].*", password)) score++;
        if (Pattern.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*", password)) score++;
        return score; // 0-6
    }

    public static String labelForce(int score) {
        if (score <= 2) return "FAIBLE";
        if (score <= 4) return "MOYEN";
        return "FORT";
    }

    public static String colorForce(int score) {
        if (score <= 2) return "#e74c3c";
        if (score <= 4) return "#f39c12";
        return "#27ae60";
    }
}