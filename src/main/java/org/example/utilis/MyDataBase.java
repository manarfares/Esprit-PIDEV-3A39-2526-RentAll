package org.example.utilis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {
    final String USERNAME="root";
    final String URL="jdbc:mysql://127.0.0.1:3307/dar" +
            "" +
            "" +
            "" +
            "" +
            "" +
            "" +
            "" +
            "" +
            "" +
            "" +
            "" +
            "" +
            "" +
            "" +
            "" +
            "" +
            "" +
            "" +
            "" +
            "" +
            "" +
            "" +
            "";
    final String PASSWORD="";

    Connection connection;

    public MyDataBase( ){
        try{
        connection = DriverManager.getConnection(URL,USERNAME,PASSWORD);
            System.out.println("connection done");
    } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
}}
