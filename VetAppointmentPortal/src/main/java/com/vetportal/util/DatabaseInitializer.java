package com.vetportal.util;

import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initializeDatabase(String schemaResource, String seedResource) {
        try (Connection conn = DbManager.getConnection()) {
            // Execute schema.sql from resources
            executeSqlFile(conn, schemaResource);
            System.out.println("Schema created successfully");

            // Execute seed.sql from resources
            executeSqlFile(conn, seedResource);
            System.out.println("Seed data inserted successfully");

            // List tables to verify
            listTables(conn);
        } catch (SQLException | IOException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void initializeOnExistingConnection(Connection conn, String schemaPath, String seedPath) throws SQLException, IOException {
        // Ensure we have a valid connection
        if (conn == null || conn.isClosed()) {
            throw new SQLException("Invalid connection: connection is null or closed");
        }

        // Make sure foreign keys are enabled
        DbManager.ensureForeignKeysEnabled();

        // Execute the SQL files
        executeSqlFile(conn, schemaPath);
        System.out.println("Schema created");

        executeSqlFile(conn, seedPath);
        System.out.println("Seed data inserted");

        // List tables to verify
        listTables(conn);
    }

    /**
     * Lists all tables in the database.
     *
     * @param conn the database connection
     * @throws SQLException if a database error occurs
     */
    private static void listTables(Connection conn) throws SQLException {
        try (Statement statement = conn.createStatement();
             ResultSet rs = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")) {
            System.out.println("Tables found after schema load:");
            while (rs.next()) {
                System.out.println(" - " + rs.getString("name"));
            }
        }
    }

    /**
     * Executes SQL statements from a file.
     *
     * @param conn the database connection
     * @param resourcePath the path to the SQL file
     * @throws SQLException if a database error occurs
     * @throws IOException if an I/O error occurs
     */
    private static void executeSqlFile(Connection conn, String resourcePath) throws SQLException, IOException {
        System.out.println("Attempting to load: " + resourcePath);

        // Use class-relative resource loading — works in Java modules
        InputStream input = DatabaseInitializer.class.getResourceAsStream("/" + resourcePath);
        if (input == null) {
            throw new FileNotFoundException(resourcePath + " not found in module resource path.");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                // Skip comments and empty lines
                if (line.trim().startsWith("--") || line.trim().isEmpty()) {
                    continue;
                }
                sb.append(line).append("\n");

                // Execute statement when it gets to a semicolon
                if (line.trim().endsWith(";")) {
                    String sql = sb.toString();
                    System.out.println("Executing SQL: " + sql);

                    try (Statement statement = conn.createStatement()) {
                        statement.execute(sql);
                        sb.setLength(0);
                    }
                }
            }
        }
    }

    /**
     * Resets the database by deleting all data and reinserting seed data.
     *
     * @param conn the database connection
     */
    public static void resetDatabase(Connection conn) {
        try {
            // Ensure the connection is valid
            if (conn == null || conn.isClosed()) {
                throw new SQLException("Invalid connection: connection is null or closed");
            }

            try (Statement statement = conn.createStatement()) {
                // Temporarily disable foreign key constraints for clean deletion
                statement.execute("PRAGMA foreign_keys = OFF");

                // Delete from tables in reverse order of dependencies
                statement.execute("DELETE FROM Appointment");
                statement.execute("DELETE FROM Pet");
                statement.execute("DELETE FROM Customer");
                statement.execute("DELETE FROM Employee");

                // Reset autoincrement counters
                statement.execute("DELETE FROM sqlite_sequence WHERE name IN ('Appointment', 'Pet', 'Customer', 'Employee')");

                // Re-enable foreign key constraints
                statement.execute("PRAGMA foreign_keys = ON");

                System.out.println("Database reset complete. Tables are empty.");
            }

            // Optionally re-seed with test data
            executeSqlFile(conn, "database/seed.sql");
            System.out.println("Test data inserted successfully");

        } catch (SQLException e) {
            System.err.println("Error resetting database: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error loading seed data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String schemaResource = "database/schema.sql";
        String seedResource = "database/seed.sql";

        initializeDatabase(schemaResource, seedResource);
    }
}