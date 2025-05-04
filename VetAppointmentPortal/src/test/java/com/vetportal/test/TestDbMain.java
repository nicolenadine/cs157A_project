package com.vetportal.test;

import com.vetportal.model.Customer;
import com.vetportal.model.Pet;
import com.vetportal.service.CustomerService;
import com.vetportal.service.ServiceManager;
import com.vetportal.util.DatabaseInitializer;
import com.vetportal.util.DbManager;
import com.vetportal.dto.ServiceResponse;

import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Simple test class to validate the ID retrieval functionality.
 * Uses the singleton ServiceManager pattern as expected in the application.
 */
public class TestDbMain {

    public static void main(String[] args) {
        try {
            // Initialize database schema and seed data
            DatabaseInitializer.initializeDatabase("database/schema.sql", "database/seed.sql");

            // Create service manager (which holds the DB connection)
            ServiceManager serviceManager = new ServiceManager();

            // Get the customer service from the manager
            CustomerService service = serviceManager.getCustomerService();

            // Test customer creation
            Customer testCustomer = new Customer(
                    0, // ID should be auto-generated
                    "Test",
                    "Customer",
                    "123 Test St",
                    "555-TEST",
                    "test@example.com"
            );

            System.out.println("Creating test customer: " + testCustomer.getFirstName() + " " + testCustomer.getLastName());
            ServiceResponse<Customer> createResponse = service.createCustomer(testCustomer);

            if (createResponse.isSuccess()) {
                Customer created = createResponse.getData();
                System.out.println("SUCCESS: Created customer with ID: " + created.getID());

                // Now create a pet for this customer
                Pet testPet = new Pet(
                        0, // ID should be auto-generated
                        "TestPet",
                        "Dog",
                        "Mixed",
                        LocalDate.parse("2020-01-01"),
                        created // Use the created customer as owner
                );

                System.out.println("Creating test pet: " + testPet.getName());
                ServiceResponse<Pet> petResponse = service.createPet(testPet);

                if (petResponse.isSuccess()) {
                    Pet createdPet = petResponse.getData();
                    System.out.println("SUCCESS: Created pet with ID: " + createdPet.getID());

                    // Test deletion
                    boolean deleted = service.deletePet(createdPet.getID());
                    System.out.println("Pet deletion " + (deleted ? "succeeded" : "failed"));

                    deleted = service.deleteCustomer(created.getID());
                    System.out.println("Customer deletion " + (deleted ? "succeeded" : "failed"));
                } else {
                    System.out.println("FAILURE: Could not create pet - " + petResponse.getMessage());
                }
            } else {
                System.out.println("FAILURE: Could not create customer - " + createResponse.getMessage());
            }

            // Don't close the connection here if you want to use it for other tests
            // Only close when the application is shutting down
            System.out.println("Test completed. Connection remains open.");

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}