package com.rentall.services;

/**
 * Service de classification automatique du sentiment d'un avis selon la note.
 * Règles métier :
 * - note 1 ou 2 = NEGATIF
 * - note 3 = NEUTRE
 * - note 4 ou 5 = POSITIF
 */
public class SentimentAvisService {

    /**
     * Enumération des sentiments possibles.
     */
    public enum Sentiment {
        NEGATIF("Négatif", "😔", new java.awt.Color(231, 76, 60)),
        NEUTRE("Neutre", "😐", new java.awt.Color(243, 156, 18)),
        POSITIF("Positif", "😊", new java.awt.Color(39, 174, 96));
        
        private final String libelle;
        private final String emoji;
        private final java.awt.Color couleur;
        
        Sentiment(String libelle, String emoji, java.awt.Color couleur) {
            this.libelle = libelle;
            this.emoji = emoji;
            this.couleur = couleur;
        }
        
        public String getLibelle() {
            return libelle;
        }
        
        public String getEmoji() {
            return emoji;
        }
        
        public java.awt.Color getCouleur() {
            return couleur;
        }
        
        @Override
        public String toString() {
            return emoji + " " + libelle;
        }
    }
    
    /**
     * Résultat de l'analyse du sentiment.
     */
    public static class ResultatSentiment {
        private final boolean valide;
        private final Sentiment sentiment;
        private final String messageErreur;
        
        public ResultatSentiment(boolean valide, Sentiment sentiment, String messageErreur) {
            this.valide = valide;
            this.sentiment = sentiment;
            this.messageErreur = messageErreur;
        }
        
        public boolean isValide() {
            return valide;
        }
        
        public Sentiment getSentiment() {
            return sentiment;
        }
        
        public String getMessageErreur() {
            return messageErreur;
        }
    }
    
    /**
     * Analyse le sentiment d'un avis selon sa note.
     * @param note La note de l'avis (1 à 5)
     * @return Le résultat de l'analyse
     */
    public ResultatSentiment analyser(int note) {
        // Validation de la note
        if (note < 1 || note > 5) {
            return new ResultatSentiment(false, null, 
                "La note doit être comprise entre 1 et 5. Note reçue : " + note);
        }
        
        // Classification du sentiment
        Sentiment sentiment = classifier(note);
        return new ResultatSentiment(true, sentiment, null);
    }
    
    /**
     * Classifie le sentiment selon la note.
     * @param note La note (1 à 5)
     * @return Le sentiment correspondant
     */
    private Sentiment classifier(int note) {
        if (note <= 2) {
            return Sentiment.NEGATIF;
        } else if (note == 3) {
            return Sentiment.NEUTRE;
        } else {
            return Sentiment.POSITIF;
        }
    }
    
    /**
     * Obtient le sentiment directement sans validation.
     * @param note La note (1 à 5)
     * @return Le sentiment, ou null si la note est invalide
     */
    public Sentiment getSentiment(int note) {
        if (note < 1 || note > 5) {
            return null;
        }
        return classifier(note);
    }
    
    /**
     * Obtient le libellé du sentiment pour une note donnée.
     * @param note La note (1 à 5)
     * @return Le libellé du sentiment
     */
    public String getLibelleSentiment(int note) {
        Sentiment sentiment = getSentiment(note);
        return sentiment != null ? sentiment.getLibelle() : "Invalide";
    }
    
    /**
     * Obtient l'emoji du sentiment pour une note donnée.
     * @param note La note (1 à 5)
     * @return L'emoji du sentiment
     */
    public String getEmojiSentiment(int note) {
        Sentiment sentiment = getSentiment(note);
        return sentiment != null ? sentiment.getEmoji() : "❌";
    }
    
    /**
     * Formate le sentiment pour l'affichage.
     * @param note La note (1 à 5)
     * @return Le sentiment formaté avec emoji
     */
    public String formaterSentiment(int note) {
        Sentiment sentiment = getSentiment(note);
        return sentiment != null ? sentiment.toString() : "❌ Note invalide";
    }
}
