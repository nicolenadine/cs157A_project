package com.vetportal.util;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initializeDatabase(String schemaFile, String seedFile) {
        try (Connection conn = DbManager.getConnection()) {

            // Execute schema.sql to create tables
            executeSqlFile(conn, schemaFile);
            System.out.println("Schema created successfully");

            // Execute seed.sql to insert initial data
            executeSqlFile(conn, seedFile);
            System.out.println("Seed data inserted successfully");

        } catch (SQLException | IOException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void executeSqlFile(Connection conn, String filePath) throws SQLException, IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
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
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(sb.toString());
                        sb.setLength(0);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        // Path to schema and seed files
        String schemaPath = "src/resources/database/schema.sql";
        String seedPath = "src/resources/database/seed.sql";

        initializeDatabase(schemaPath, seedPath);
    }
}
