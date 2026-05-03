module tn.piapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires java.net.http;
    requires jbcrypt;
    requires java.desktop;  // Pour Swing/AWT (com.rentall.views)
    requires com.github.librepdf.openpdf;  // Pour OpenPDF

    opens tn.piapp.ui to javafx.fxml;
    opens tn.piapp.model to javafx.base;

    exports tn.piapp.ui;
    exports tn.piapp.model;
    exports tn.piapp.dao;
    exports tn.piapp.db;
    exports tn.piapp.util;
    exports tn.piapp.service;
    
    // Exports pour com.rentall
    exports com.rentall.config;
    exports com.rentall.dto;
    exports com.rentall.entities;
    exports com.rentall.services;
    exports com.rentall.views;
    exports com.rentall.util;
}
