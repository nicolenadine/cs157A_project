package com.vetportal.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TestDbConnection {
    public static void main(String[] args) {
        try {
            // Get connection from DbManager
            Connection connection = DbManager.getConnection();

            // Print connection status
            System.out.println("Database connection established: " + !connection.isClosed());

            // test query - will check if the Customer table exists
            String sql = "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='Customer'";

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                if (rs.next()) {
                    int count = rs.getInt(1);
                    if (count > 0) {
                        System.out.println("Customer table exists!");

                        // query the Customer check data can be processed
                        String customerQuery = "SELECT * FROM Customer LIMIT 5";
                        try (Statement custStmt = connection.createStatement();
                             ResultSet custRs = custStmt.executeQuery(customerQuery)) {

                            System.out.println("\nCustomer records:");
                            while (custRs.next()) {
                                int id = custRs.getInt("id");
                                String firstName = custRs.getString("first_name");
                                String lastName = custRs.getString("last_name");
                                String email = custRs.getString("email");

                                System.out.println(id + ": " + firstName + " " + lastName + " (" + email + ")");
                            }
                        }
                    } else {
                        System.out.println("Customer table does not exist! You need to run your schema.sql file.");
                    }
                }
            }

            // Close the connection
            DbManager.closeConnection();
            System.out.println("\nDatabase connection closed.");

        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
