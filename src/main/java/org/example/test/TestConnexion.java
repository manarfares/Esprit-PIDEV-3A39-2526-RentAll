package org.example.test;

import org.example.services.DatabaseConnection;
import org.example.services.ServiceUser;
import org.example.models.*;
import java.sql.Connection;

public class TestConnexion {
    public static void main(String[] args) {

        // Test connexion
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null)
                System.out.println("✅ Connexion réussie !");
        } catch (Exception e) {
            System.out.println("❌ Erreur : " + e.getMessage());
        }

        // Test models
        Admin   admin = new Admin("adminTest", "admin@test.com", "1234");
        Host    host  = new Host("hostTest", "host@test.com", "5678");
        Guest   guest = new Guest("guestTest", "guest@test.com", "abcd");

        System.out.println(admin  + " → " + admin.getPermissions());
        System.out.println(host   + " → " + host.getPermissions());
        System.out.println(guest  + " → " + guest.getPermissions());
    }
}