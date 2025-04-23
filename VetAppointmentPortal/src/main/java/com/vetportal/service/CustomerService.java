package com.vetportal.service;

import com.vetportal.dto.ServiceResponse;
import com.vetportal.dao.impl.CustomerDAO;
import com.vetportal.exception.DataAccessException;
import com.vetportal.model.Customer;

import java.util.Optional;
import java.sql.Connection;

/**
 * Service layer responsible for customer-related business logic.
 * <p>
 * Delegates database operations to the {@link CustomerDAO} and wraps results in
 * {@link ServiceResponse} objects to include status and error handling.
 */
public class CustomerService {
    private CustomerDAO customerDAO;

    /**
     * Constructs a new CustomerService using the given database connection.
     *
     * @param conn an active SQL database connection
     */
    public CustomerService(Connection conn) {
        this.customerDAO = new CustomerDAO(conn);
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
     * @param phone the phone number to search for
     * @return a service response containing the result of the lookup
     */
    public ServiceResponse<Customer> findCustomerByFields(Map<String, String> fields) {
        try {
            Optional<Customer> result = customerDAO.findByFields(fields);
            return result.map(ServiceResponse::success)
                    .orElseGet(() -> ServiceResponse.notFound("Customer not found with phone: " + phone));
        } catch (DataAccessException e) {
            return ServiceResponse.dbError("Database error: " + e.getMessage());
        }
    }
}
