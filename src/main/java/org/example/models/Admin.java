package org.example.models;

import java.time.LocalDateTime;

public class Admin extends User {

    public Admin(String username, String email, String password) {
        super(username, email, password);
    }

    public Admin(int id, String username, String email, String password,
                 String phone, String profileImage, String status,
                 String sessionToken,
                 LocalDateTime hostRequestDate, LocalDateTime createdAt,
                 LocalDateTime updatedAt) {
        super(id, username, email, password, phone, profileImage,
                status, sessionToken, hostRequestDate, createdAt, updatedAt);
    }

    @Override
    public String getRole() { return "ROLE_ADMIN"; }

    @Override
    public void login() {
        System.out.println("✅ Admin connecté : " + username);
    }

    @Override
    public void logout() {
        System.out.println("🚪 Admin déconnecté : " + username);
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