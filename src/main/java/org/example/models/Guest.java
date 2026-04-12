package org.example.models;

import java.time.LocalDateTime;

public class Guest extends User {

    public Guest(String username, String email, String password) {
        super(username, email, password);
    }

    public Guest(int id, String username, String email, String password,
                 String phone, String profileImage, String status,
                 String sessionToken,
                 LocalDateTime hostRequestDate, LocalDateTime createdAt,
                 LocalDateTime updatedAt) {
        super(id, username, email, password, phone, profileImage,
                status, sessionToken, hostRequestDate, createdAt, updatedAt);
    }

    @Override
    public String getRole() { return "ROLE_GUEST"; }

    @Override
    public void login() {
        System.out.println("✅ Guest connecté : " + username);
    }

    @Override
    public void logout() {
        System.out.println("🚪 Guest déconnecté : " + username);
    }

    public void devenirHost() {
        System.out.println("📩 Guest " + username + " demande à devenir Host");
    }

    public void rechercherProduit(String produit) {
        System.out.println("🔍 Guest recherche : " + produit);
    }

    public void reserver(String produit) {
        System.out.println("📅 Guest réserve : " + produit);
    }

    @Override
    public String getPermissions() {
        return "Rechercher + Réserver + Demander Host";
    }
}