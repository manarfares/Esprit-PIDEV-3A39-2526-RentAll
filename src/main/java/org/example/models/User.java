package org.example.models;

import java.time.LocalDateTime;

public abstract class User {

    protected int            id;
    protected String         username;
    protected String         email;
    protected String         password;
    protected String         phone;
    protected String         profileImage;
    protected String         status;
    protected String         sessionToken;
    protected LocalDateTime  hostRequestDate;
    protected LocalDateTime  createdAt;
    protected LocalDateTime  updatedAt;

    // Constructeur INSERT (minimal)
    public User(String username, String email, String password) {
        this.username = username;
        this.email    = email;
        this.password = password;
        this.status   = "ACTIVE";
    }

    // Constructeur complet SELECT
    public User(int id, String username, String email, String password,
                String phone, String profileImage, String status,
                String sessionToken,
                LocalDateTime hostRequestDate, LocalDateTime createdAt,
                LocalDateTime updatedAt) {
        this.id              = id;
        this.username        = username;
        this.email           = email;
        this.password        = password;
        this.phone           = phone;
        this.profileImage    = profileImage;
        this.status          = status;
        this.sessionToken    = sessionToken;
        this.hostRequestDate = hostRequestDate;
        this.createdAt       = createdAt;
        this.updatedAt       = updatedAt;
    }

    // Méthodes abstraites
    public abstract String getRole();
    public abstract void   login();
    public abstract void   logout();
    public abstract String getPermissions();

    // Getters
    public int           getId()              { return id; }
    public String        getUsername()        { return username; }
    public String        getEmail()           { return email; }
    public String        getPassword()        { return password; }
    public String        getPhone()           { return phone; }
    public String        getProfileImage()    { return profileImage; }
    public String        getStatus()          { return status; }
    public String        getSessionToken()    { return sessionToken; }
    public LocalDateTime getHostRequestDate() { return hostRequestDate; }
    public LocalDateTime getCreatedAt()       { return createdAt; }
    public LocalDateTime getUpdatedAt()       { return updatedAt; }

    // Setters
    public void setId(int id)                       { this.id              = id; }
    public void setUsername(String u)               { this.username        = u; }
    public void setEmail(String e)                  { this.email           = e; }
    public void setPassword(String p)               { this.password        = p; }
    public void setPhone(String p)                  { this.phone           = p; }
    public void setProfileImage(String p)           { this.profileImage    = p; }
    public void setStatus(String s)                 { this.status          = s; }
    public void setSessionToken(String t)           { this.sessionToken    = t; }
    public void setHostRequestDate(LocalDateTime d) { this.hostRequestDate = d; }
    public void setCreatedAt(LocalDateTime d)       { this.createdAt       = d; }
    public void setUpdatedAt(LocalDateTime d)       { this.updatedAt       = d; }

    @Override
    public String toString() {
        return "User{id=" + id +
                ", username='" + username + "'" +
                ", email='"    + email    + "'" +
                ", role='"     + getRole() + "'" +
                ", status='"   + status   + "'}";
    }
}