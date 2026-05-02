package com.rentall.services;

import java.util.Arrays;
import java.util.List;

/**
 * Service d'analyse automatique des commentaires d'avis.
 * Vérifie la validité du commentaire selon les règles métier.
 */
public class AnalyseCommentaireService {

    private static final int MIN_CARACTERES = 10;
    private static final int MAX_CARACTERES = 500;
    
    private static final List<String> MOTS_INTERDITS = Arrays.asList(
        "arnaque", "nul", "horrible", "insulte", "putain", "merde", "mauvais mot",
        "connard", "con", "salope", "putain merde", "enfoiré", "bastard",
        "débile", "idiot", "stupide", "imbécile"
    );
    
    /**
     * Résultat de l'analyse du commentaire.
     */
    public static class ResultatAnalyse {
        private final boolean valide;
        private final String messageErreur;
        
        public ResultatAnalyse(boolean valide, String messageErreur) {
            this.valide = valide;
            this.messageErreur = messageErreur;
        }
        
        public boolean isValide() {
            return valide;
        }
        
        public String getMessageErreur() {
            return messageErreur;
        }
    }
    
    /**
     * Analyse un commentaire selon les règles métier.
     * @param commentaire Le commentaire à analyser
     * @return Le résultat de l'analyse
     */
    public ResultatAnalyse analyser(String commentaire) {
        // Règle 1: Le commentaire est obligatoire
        if (commentaire == null || commentaire.trim().isEmpty()) {
            return new ResultatAnalyse(false, "Le commentaire est obligatoire.");
        }
        
        String commentaireTrimmed = commentaire.trim();
        
        // Règle 2: Minimum 10 caractères
        if (commentaireTrimmed.length() < MIN_CARACTERES) {
            return new ResultatAnalyse(false, 
                "Le commentaire doit contenir au minimum " + MIN_CARACTERES + " caractères. " +
                "Actuel : " + commentaireTrimmed.length() + " caractères.");
        }
        
        // Règle 3: Maximum 500 caractères
        if (commentaireTrimmed.length() > MAX_CARACTERES) {
            return new ResultatAnalyse(false, 
                "Le commentaire ne doit pas dépasser " + MAX_CARACTERES + " caractères. " +
                "Actuel : " + commentaireTrimmed.length() + " caractères.");
        }
        
        // Règle 4: Pas de mots interdits
        String motInterdit = detecterMotInterdit(commentaireTrimmed);
        if (motInterdit != null) {
            return new ResultatAnalyse(false, 
                "Le commentaire contient un mot interdit : \"" + motInterdit + "\". " +
                "Veuillez modifier votre commentaire.");
        }
        
        return new ResultatAnalyse(true, null);
    }
    
    /**
     * Vérifie si le commentaire est valide.
     * @param commentaire Le commentaire à vérifier
     * @return true si le commentaire est valide, false sinon
     */
    public boolean estValide(String commentaire) {
        return analyser(commentaire).isValide();
    }
    
    /**
     * Détecte un mot interdit dans le commentaire.
     * @param commentaire Le commentaire à analyser
     * @return Le mot interdit trouvé, ou null si aucun
     */
    private String detecterMotInterdit(String commentaire) {
        String commentaireLower = commentaire.toLowerCase();
        
        for (String motInterdit : MOTS_INTERDITS) {
            if (commentaireLower.contains(motInterdit.toLowerCase())) {
                return motInterdit;
            }
        }
        
        return null;
    }
    
    /**
     * Retourne le nombre minimum de caractères requis.
     * @return Le minimum de caractères
     */
    public int getMinCaracteres() {
        return MIN_CARACTERES;
    }
    
    /**
     * Retourne le nombre maximum de caractères autorisés.
     * @return Le maximum de caractères
     */
    public int getMaxCaracteres() {
        return MAX_CARACTERES;
    }
}
