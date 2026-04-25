package tn.piapp.model;

import java.time.LocalDateTime;

public class Admin extends User {

    public Admin(String name, String email, String password) {
        super(name, email, password);
    }

    public Admin(int id, String name, String email, String password,
                 String phone, String profileImage, String status,
                 String sessionToken,
                 LocalDateTime hostRequestDate, LocalDateTime createdAt,
                 LocalDateTime updatedAt) {
        super(id, name, email, password, phone, profileImage,
                status, sessionToken, hostRequestDate, createdAt, updatedAt);
    }

    @Override
    public String getRole() { return "ROLE_ADMIN"; }

    @Override
    public void login() {
        System.out.println("✅ Admin connecté : " + name);
    }

    @Override
    public void logout() {
        System.out.println("🚪 Admin déconnecté : " + name);
    }

    public void gererUtilisateurs() {
        System.out.println("👑 Admin gère les utilisateurs");
    }

    public void validerDemandeHost(int userId) {
        System.out.println("✅ Admin valide Host → ID : " + userId);
    }

    public void refuserDemandeHost(int userId) {
        System.out.println("❌ Admin refuse Host → ID : " + userId);
    }

    public void banirUtilisateur(int userId) {
        System.out.println("🔨 Admin banni → ID : " + userId);
    }

    @Override
    public String getPermissions() {
        return "CRUD complet + validation demandes Host";
    }
}