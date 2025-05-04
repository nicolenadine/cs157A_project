package com.vetportal.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manager class for database connections.
 * Handles a single shared connection for the entire application.
 */
public class DbManager {
    private static final String URL = "jdbc:sqlite:vetappointmentportal.db";
    private static Connection connection = null;

    /**
     * Gets the shared database connection, creating it if necessary.
     * Also ensures foreign keys are enabled for this connection.
     *
     * @return the shared database connection
     * @throws SQLException if a database access error occurs
     */
    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL);

            // Makes sure foreign keys are enabled for referential integrity
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON;");


                var rs = statement.executeQuery("PRAGMA foreign_keys;");
                if (rs.next()) {
                    int enabled = rs.getInt(1);
                    System.out.println("Foreign keys status: " + (enabled == 1 ? "ENABLED" : "DISABLED"));
                }
            } catch (SQLException e) {
                System.err.println("Error enabling foreign keys: " + e.getMessage());
                // Continue anyway - don't throw here
            }
        }
        return connection;
    }

    /**
     * Closes the shared database connection.
     * This should be called when the application is shutting down.
     */
    public static synchronized void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    System.out.println("Database connection closed");
                }
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }

    /**
     * Checks if the connection is closed.
     *
     * @return true if the connection is null or closed, false otherwise
     */
    public static synchronized boolean isClosed() {
        if (connection == null) {
            return true;
        }

        try {
            return connection.isClosed();
        } catch (SQLException e) {
            System.err.println("Error checking if connection is closed: " + e.getMessage());
            return true;  // Assume closed if we can't check
        }
    }

    /**
     * Ensures foreign keys are enabled on the current connection.
     * This is useful for operations that require foreign key constraints.
     *
     * @throws SQLException if a database access error occurs
     */
    public static synchronized void ensureForeignKeysEnabled() throws SQLException {
        if (connection == null || connection.isClosed()) {
            getConnection();
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
        }
    }
}