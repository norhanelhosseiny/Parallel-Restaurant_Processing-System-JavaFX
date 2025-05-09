/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package restaurant_parallelproject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

/**
 *
 * @author DELL
 */
public class TestDatabase {
     private static final String DB_URL = "jdbc:sqlite:RestaurantSystem.db";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Enable foreign keys
            conn.createStatement().execute("PRAGMA foreign_keys = ON;");

            // Insert sample data into Users
            String insertCustomer = "INSERT INTO Users (username, password, role) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(insertCustomer);
            pstmt.setString(1, "norhan");
            pstmt.setString(2, "pass123");
            pstmt.setString(3, "customer");
            pstmt.executeUpdate();
            
            String insertChef = "INSERT INTO Users (username, password, role) VALUES (?, ?, ?)";
            PreparedStatement pstmt1 = conn.prepareStatement(insertChef);
            pstmt1.setString(1, "mirna");
            pstmt1.setString(2, "pass123");
            pstmt1.setString(3, "chef");
            pstmt1.executeUpdate();
            
            String insertCustomer2 = "INSERT INTO Users (username, password, role) VALUES (?, ?, ?)";
            PreparedStatement pstmt2 = conn.prepareStatement(insertCustomer2);
            pstmt2.setString(1, "khalid");
            pstmt2.setString(2, "pass123");
            pstmt2.setString(3, "customer");
            pstmt2.executeUpdate();


            System.out.println("Sample data inserted successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


