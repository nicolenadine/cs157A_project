package com.vetportal.service;

import com.vetportal.dao.CustomerDAO;
import com.vetportal.dao.PetDAO;
import com.vetportal.dto.ServiceResponse;
import com.vetportal.dao.EmployeeDAO;
import com.vetportal.exception.DataAccessException;
import com.vetportal.model.Customer;
import com.vetportal.model.Pet;

import java.util.List;
import java.util.Optional;
import java.sql.Connection;
import java.util.Map;

/**
 * Service layer responsible for customer-related business logic.
 * <p>
 * Delegates database operations to the {@link EmployeeDAO} and wraps results in
 * {@link ServiceResponse} objects to include status and error handling.
 */
public class CustomerService {
    private final CustomerDAO customerDAO;
    private final PetDAO petDAO;

    /**
     * Constructs a new CustomerService using the given database connection.
     * Initializes CustomerDAO and PetDAO for customer and pet operations.
     *
     * @param conn an active SQL database connection
     */
    public CustomerService(Connection conn) {
        this.customerDAO = new CustomerDAO(conn);
        this.petDAO = new PetDAO(conn, this.customerDAO);
    }

    // -------- CUSTOMER CLASS CREATE, UPDATE, & DELETE METHODS --------

    /**
     * Creates a new customer in the database.
     *
     * @param customer the customer to create
     * @return a service response containing the created customer or an error
     */
    public ServiceResponse<Customer> createCustomer(Customer customer) {
        try {
            Optional<Customer> result = customerDAO.createCustomer(customer);
            return result.map(ServiceResponse::success)
                    .orElseGet(() -> ServiceResponse.notFound("Customer could not be created"));
        } catch (DataAccessException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                return ServiceResponse.dbError("Error: Email or phone already exists");
            }
            return ServiceResponse.dbError("Error: " + e.getMessage());
        }
    }

    /**
     * Updates an existing customer in the database.
     *
     * @param customer the customer to update
     * @return true if the customer was updated, false otherwise
     */
    public boolean updateCustomer(Customer customer) {
        try {
            return customerDAO.update(customer);
        } catch (DataAccessException e) {
            System.err.println("Error updating customer: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a customer and all associated pets from the database.
     * Relies on foreign key constraints for cascade deletion.
     *
     * @param customerID the ID of the customer to delete
     * @return true if the customer was deleted, false otherwise
     */
    public boolean deleteCustomer(int customerID) {
        try {
            // First verify the customer exists
            Optional<Customer> customer = customerDAO.findByID(customerID);
            if (customer.isEmpty()) {
                System.out.println("Customer with ID " + customerID + " not found");
                return false;
            }

            // The foreign key constraints will handle cascade deletion of pets
            return customerDAO.delete(customerID);
        } catch (DataAccessException e) {
            System.err.println("Error deleting customer: " + e.getMessage());
            return false;
        }
    }

    // -------- PET CLASS CREATE, UPDATE, & DELETE METHODS --------

    /**
     * Creates a new pet in the database.
     *
     * @param pet the pet to create
     * @return a service response containing the created pet or an error
     */
    public ServiceResponse<Pet> createPet(Pet pet) {
        try {
            // Validate the owner exists
            Optional<Customer> owner = customerDAO.findByID(pet.getOwner().getID());
            if (owner.isEmpty()) {
                return ServiceResponse.notFound("Owner with ID " + pet.getOwner().getID() + " not found");
            }

            Optional<Pet> result = petDAO.createPet(pet);
            return result.map(ServiceResponse::success)
                    .orElseGet(() -> ServiceResponse.notFound("Pet could not be created"));
        } catch (DataAccessException e) {
            return ServiceResponse.dbError("Error: " + e.getMessage());
        }
    }

    /**
     * Updates an existing pet in the database.
     *
     * @param pet the pet to update
     * @return true if the pet was updated, false otherwise
     */
    public boolean updatePet(Pet pet) {
        System.out.println("CustomerService.updatePet - Pet ID: " + pet.getID() + ", Name: " + pet.getName());
        try {
            // Validate pet exists
            Optional<Pet> existingPet = petDAO.findByID(pet.getID());
            if (existingPet.isEmpty()) {
                System.out.println("Pet with ID " + pet.getID() + " not found");
                return false;
            }

            // Log to verify we found the pet
            System.out.println("Found existing pet before update: " + existingPet.get().getName());

            boolean result = petDAO.update(pet);
            System.out.println("Update result: " + result);

            // Verify update with a simple fetch to make sure changes were saved
            if (result) {
                Optional<Pet> updatedPet = petDAO.findByID(pet.getID());
                if (updatedPet.isPresent()) {
                    System.out.println("After update - Pet name: " + updatedPet.get().getName() +
                            ", Breed: " + updatedPet.get().getBreed());
                }
            }

            return result;
        } catch (DataAccessException e) {
            System.err.println("Error updating pet: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a pet from the database.
     *
     * @param petID the ID of the pet to delete
     * @return true if the pet was deleted, false otherwise
     */
    public boolean deletePet(int petID) {
        System.out.println("CustomerService.deletePet - Pet ID: " + petID);
        try {
            // Validate pet exists
            Optional<Pet> existingPet = petDAO.findByID(petID);
            if (existingPet.isEmpty()) {
                System.out.println("Pet with ID " + petID + " not found");
                return false;
            }

            return petDAO.delete(petID);
        } catch (DataAccessException e) {
            System.err.println("Error deleting pet: " + e.getMessage());
            return false;
        }
    }

    // ----------------- QUERY METHODS -----------------------

    /**
     * Looks up a customer by attributes (e.g., phone, email).
     *
     * @param attributes map of attribute names to values to search for
     * @return a service response containing the customer or an error
     */
    public ServiceResponse<Customer> findCustomerByAttributes(Map<String, String> attributes) {
        try {
            Optional<Customer> result = customerDAO.findByAttributes(attributes);
            return result.map(ServiceResponse::success)
                    .orElseGet(() -> ServiceResponse.notFound("Customer not found with attributes: " + attributes));
        } catch (DataAccessException e) {
            return ServiceResponse.dbError("Database error: " + e.getMessage());
        }
    }

    /**
     * Finds all pets owned by a customer.
     *
     * @param customerID the ID of the customer
     * @return a service response containing the list of pets or an error
     */
    public ServiceResponse<List<Pet>> findPetsByCustomerId(int customerID) {
        try {
            // Validate customer exists
            Optional<Customer> customer = customerDAO.findByID(customerID);
            if (customer.isEmpty()) {
                return ServiceResponse.notFound("Customer with ID " + customerID + " not found");
            }

            List<Pet> pets = petDAO.findAllPetsByCustomerId(customerID);
            // Return success even if the list is empty
            return ServiceResponse.success(pets);
        } catch (Exception e) {
            return ServiceResponse.dbError("Error retrieving pets for customer ID " + customerID + ": " + e.getMessage());
        }
    }
}