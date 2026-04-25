package tn.piapp.dao;

import tn.piapp.model.User;
import java.util.List;

public interface IUserDAO {

    // CRUD de base
    void       create(User user);
    List<User> findAll();
    User       findById(int id);
    User       findByEmail(String email);
    User       findByUsername(String name);
    void       update(User user);
    void       delete(int id);

    // Authentification
    User login(String usernameOrEmail, String password);

    // Gestion statut
    void updateStatus(int id, String status);

    // Gestion rôle
    void updateRole(int id, String role);

    // Demande Host
    void demanderHost(int id);

    // Lister les demandes Host en attente
    List<User> findHostPending();
}