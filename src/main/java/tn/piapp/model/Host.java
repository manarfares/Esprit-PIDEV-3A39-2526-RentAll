package tn.piapp.model;

import java.time.LocalDateTime;

public class Host extends User {

    public Host(String name, String email, String password) {
        super(name, email, password);
    }

    public Host(int id, String name, String email, String password,
                String phone, String profileImage, String status,
                String sessionToken,
                LocalDateTime hostRequestDate, LocalDateTime createdAt,
                LocalDateTime updatedAt) {
        super(id, name, email, password, phone, profileImage,
                status, sessionToken, hostRequestDate, createdAt, updatedAt);
    }

    @Override
    public String getRole() { return "ROLE_HOST"; }

    @Override
    public void login() {
        System.out.println("✅ Host connecté : " + name);
    }

    @Override
    public void logout() {
        System.out.println("🚪 Host déconnecté : " + name);
    }

    public void ajouterProduit(String produit) {
        System.out.println("🏠 Host ajoute : " + produit);
    }

    public void gererAnnonces() {
        System.out.println("📋 Host gère ses annonces");
    }

    public void voirReservations() {
        System.out.println("📅 Host consulte ses réservations");
    }

    @Override
    public String getPermissions() {
        return "Ajouter produits + gérer annonces + voir réservations";
    }
}