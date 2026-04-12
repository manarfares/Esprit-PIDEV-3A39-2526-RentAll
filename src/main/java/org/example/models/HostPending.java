package org.example.models;

import java.time.LocalDateTime;

public class HostPending extends User {

    public HostPending(String username, String email, String password) {
        super(username, email, password);
    }

    public HostPending(int id, String username, String email, String password,
                       String phone, String profileImage, String status,
                       String sessionToken,
                       LocalDateTime hostRequestDate, LocalDateTime createdAt,
                       LocalDateTime updatedAt) {
        super(id, username, email, password, phone, profileImage,
                status, sessionToken, hostRequestDate, createdAt, updatedAt);
    }

    @Override
    public String getRole() { return "ROLE_HOST_PENDING"; }

    @Override
    public void login() {
        System.out.println("✅ HostPending connecté : " + username);
    }

    @Override
    public void logout() {
        System.out.println("🚪 HostPending déconnecté : " + username);
    }

    public void voirStatutDemande() {
        System.out.println("⏳ Demande en attente depuis : " + hostRequestDate);
    }

    @Override
    public String getPermissions() {
        return "En attente de validation Admin";
    }
}