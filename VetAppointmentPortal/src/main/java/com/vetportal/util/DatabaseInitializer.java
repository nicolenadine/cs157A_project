package com.vetportal.util;

import java.io.*;
import java.sql.Connection;
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

        } catch (SQLException | IOException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

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
                sb.append(line);

                // Execute statement when semicolon is found
                if (line.trim().endsWith(";")) {
                    System.out.println("Executing SQL: " + sb.toString());

                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(sb.toString());
                        sb.setLength(0);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        String schemaResource = "database/schema.sql";
        String seedResource = "database/seed.sql";

        initializeDatabase(schemaResource, seedResource);
    }
}
