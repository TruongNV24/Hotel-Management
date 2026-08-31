package com.cnj42.hotel;
import com.cnj42.hotel.utils.DBConnection;
import java.sql.Connection;
import java.sql.SQLException;

public class App {

    public static void main(String[] args) {

        try (Connection connection = DBConnection.getConnection()) {

            System.out.println("KET NOI DATABASE THANH CONG!");
            System.out.println("Database: " + connection.getCatalog());

        } catch (SQLException e) {

            System.out.println("KET NOI DATABASE THAT BAI!");
            e.printStackTrace();
        }
    }
}