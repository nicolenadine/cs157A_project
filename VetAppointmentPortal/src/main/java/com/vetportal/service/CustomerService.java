package com.vetportal.service;

import com.vetportal.dao.impl.PetDAO;
import com.vetportal.dto.ServiceResponse;
import com.vetportal.dao.impl.CustomerDAO;
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
 * Delegates database operations to the {@link CustomerDAO} and wraps results in
 * {@link ServiceResponse} objects to include status and error handling.
 */
public class CustomerService {
    private CustomerDAO customerDAO;
    private PetDAO petDAO;

    /**
     * Constructs a new CustomerService using the given database connection.
     *
     * @param conn an active SQL database connection
     */
    public CustomerService(Connection conn) {
        this.customerDAO = new CustomerDAO(conn);
        this.petDAO = new PetDAO(conn, this.customerDAO);
    }

    /**
     * Looks up a customer by their phone number.
     * <p>
     * Returns a {@link ServiceResponse} with:
     * <ul>
     *     <li>{@code SUCCESS} if the customer is found</li>
     *     <li>{@code NOT_FOUND} if no customer exists with the given phone</li>
     *     <li>{@code DB_ERROR} if a database access error occurs</li>
     * </ul>
     *
     * @return a service response containing the result of the lookup
     */
    public ServiceResponse<Customer> findCustomerByFields(Map<String, String> fields) {
        try {
            Optional<Customer> result = customerDAO.findByFields(fields);
            return result.map(ServiceResponse::success)
                    .orElseGet(() -> ServiceResponse.notFound("Customer not found with phone: " + fields));
        } catch (DataAccessException e) {
            return ServiceResponse.dbError("Database error: " + e.getMessage());
        }
    }

    public ServiceResponse<List<Pet>> findPetsByCustomerId(int customerID) {
        try {
            List<Pet> pets = petDAO.findAllPetsByCustomerId(customerID);
            if (pets.isEmpty()) {
                return ServiceResponse.notFound("No pets found for customer ID " + customerID);
            }
            return ServiceResponse.success(pets);
        } catch (Exception e) {
            return ServiceResponse.dbError("Error retrieving pets for customer ID " + customerID + ": " + e.getMessage());
        }
    }

}
