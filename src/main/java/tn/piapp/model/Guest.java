package tn.piapp.model;

import java.time.LocalDateTime;

public class Guest extends User {

    public Guest(String name, String email, String password) {
        super(name, email, password);
    }

    public Guest(int id, String name, String email, String password,
                 String phone, String profileImage, String status,
                 String sessionToken,
                 LocalDateTime hostRequestDate, LocalDateTime createdAt,
                 LocalDateTime updatedAt) {
        super(id, name, email, password, phone, profileImage,
                status, sessionToken, hostRequestDate, createdAt, updatedAt);
    }

    @Override
    public String getRole() { return "ROLE_GUEST"; }

    @Override
    public void login() {
        System.out.println("✅ Guest connecté : " + name);
    }

    @Override
    public void logout() {
        System.out.println("🚪 Guest déconnecté : " + name);
    }

    public void devenirHost() {
        System.out.println("📩 Guest " + name + " demande à devenir Host");
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