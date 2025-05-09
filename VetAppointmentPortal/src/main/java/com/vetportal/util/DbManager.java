package com.vetportal.util;

import java.sql.*;

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

            //Make sure foreign keys are enabled for referential integrity
            ensureForeignKeysEnabled();
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
            statement.execute("PRAGMA foreign_keys = ON;"); // checks current status of FK enforcement

            ResultSet rs = statement.executeQuery("PRAGMA foreign_keys;");
            if (rs.next()) {
                int enabled = rs.getInt(1); // enabled will contain either 1 (enabled) or 0 (disabled)
                if (enabled != 1) {
                    throw new SQLException("Error enabling foreign keys.");
                }
            }
        }
    }
}