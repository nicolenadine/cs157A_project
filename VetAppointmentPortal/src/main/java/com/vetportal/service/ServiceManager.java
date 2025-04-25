package com.vetportal.service;

import com.vetportal.util.DbManager;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Singleton service manager responsible for initializing and managing application-wide services.
 * <p>
 * This class creates and shares a single database connection and makes service-layer objects
 * (like {@link CustomerService}) accessible throughout the application.
 * <p>
 * The connection is automatically closed when {@link #close()} is called.
 */
public class ServiceManager {

    /** The singleton instance of ServiceManager. */
    private static ServiceManager instance;

    private final Connection connection;
    private final CustomerService customerService;

    /**
     * Initializes the ServiceManager by creating a database connection and instantiating services.
     *
     * @throws SQLException if the database connection cannot be established
     */
    public ServiceManager() throws SQLException {
        this.connection = DbManager.getConnection();
        this.customerService = new CustomerService(connection);
        instance = this;
    }

    /**
     * Returns the singleton instance of the ServiceManager.
     *
     * @return the current ServiceManager instance
     */
    public static ServiceManager getInstance() {
        return instance;
    }

    /**
     * Returns the shared database connection.
     *
     * @return an active {@link Connection} object
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Returns the shared {@link CustomerService} instance.
     *
     * @return the customer service object
     */
    public CustomerService getCustomerService() {
        return customerService;
    }

    /**
     * Closes the shared database connection, if it is open.
     * This should be called when the application exits to ensure proper cleanup.
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
