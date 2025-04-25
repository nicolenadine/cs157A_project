package com.vetportal.util;

import java.sql.Connection;
import java.sql.SQLException;

public class SimpleDbTest {
    public static void main(String[] args) {
        try {
            System.out.println("Attempting to connect to database...");

            Connection conn = DbManager.getConnection(); // Initialize connection

            // Check connection status
            boolean isConnected = !conn.isClosed();
            System.out.println("Database connection successful: " + isConnected);

            // Close connection
            DbManager.closeConnection();
            System.out.println("Database connection closed.");

        } catch (SQLException e) {
            System.err.println("Error connecting to database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}