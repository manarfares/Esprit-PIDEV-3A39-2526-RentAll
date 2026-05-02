package com.rentall.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Classe DatabaseConnection
 *
 * RÔLE : Gérer la connexion à la base de données MySQL existante.
 *
 * IMPORTANT : On se connecte à "smart_rental_platform" telle qu'elle est.
 * On ne crée rien, on ne modifie rien dans la base.
 *
 * POURQUOI UNE CLASSE DÉDIÉE ?
 * - Centraliser la connexion en un seul endroit.
 * - Si on change de config (mot de passe, port...), on modifie seulement ici.
 * - Pattern Singleton : une seule connexion partagée dans tout le projet.
 *
 * COMMENT FONCTIONNE DriverManager ?
 * - C'est une classe Java qui gère les drivers de bases de données.
 * - On lui donne l'URL JDBC, le username et le password.
 * - Il retourne un objet Connection qu'on utilise pour exécuter des requêtes SQL.
 *
 * FORMAT DE L'URL JDBC :
 * jdbc:mysql://[hôte]:[port]/[nom_de_la_base]?[options]
 */
public class DatabaseConnection {

    // Nom exact de la base Symfony existante
    private static final String URL =
            "jdbc:mysql://localhost:3306/pidev_amine" +
            "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    // Utilisateur MySQL (XAMPP/WAMP par défaut = "root")
    private static final String USERNAME = "root";

    // Mot de passe MySQL (XAMPP par défaut = vide, WAMP = vide aussi)
    private static final String PASSWORD = "";

    // Instance unique de la connexion (Singleton)
    private static Connection connection = null;

    /**
     * Retourne la connexion existante, ou en crée une nouvelle si besoin.
     * Une seule connexion est créée pour tout le projet.
     */
    public static Connection getConnection() {
        if (connection == null) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                System.out.println("✅ Connecté à pidev_amine !");
            } catch (ClassNotFoundException e) {
                System.out.println("❌ Driver MySQL introuvable : " + e.getMessage());
            } catch (SQLException e) {
                System.out.println("❌ Erreur de connexion : " + e.getMessage());
            }
        }
        return connection;
    }
}
