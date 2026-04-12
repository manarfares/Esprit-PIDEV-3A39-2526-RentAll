package org.example.test;

import org.example.models.*;
import org.example.services.ServiceUser;
import java.util.List;

public class TestCRUD {
    public static void main(String[] args) {

        ServiceUser service = new ServiceUser();

        // CREATE
        System.out.println("📌 [CREATE]");
        service.ajouter(new Guest("guest2", "guest2@app.com", "1234"));
        service.ajouter(new Host("host2", "host2@app.com", "5678"));

        // READ ALL
        System.out.println("\n📋 [READ ALL]");
        for (User u : service.recuperer()) System.out.println(u);

        // LOGIN
        System.out.println("\n🔐 [LOGIN]");
        User u = service.login("admin", "admin123");
        if (u != null) {
            System.out.println(u);
            System.out.println("Token : " + u.getSessionToken());
        }

        // LOGOUT
        System.out.println("\n🚪 [LOGOUT]");
        if (u != null) service.logout(u.getId());
    }
}