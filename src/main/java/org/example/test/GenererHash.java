package org.example.test;

import org.mindrot.jbcrypt.BCrypt;

public class GenererHash {
    public static void main(String[] args) {
        String password = "admin123";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        System.out.println("Hash : " + hash);
    }
}