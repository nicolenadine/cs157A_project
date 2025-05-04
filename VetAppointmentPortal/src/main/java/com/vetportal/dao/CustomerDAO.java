package com.vetportal.dao;

import com.vetportal.exception.DataAccessException;
import com.vetportal.mapper.CustomerMapper;
import com.vetportal.model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;



/**
 * Extends the BaseDAO functionality to support Customer table specific implementation
 * Overrides the create method of super class.
 */
public class CustomerDAO extends BaseDAO<Customer> {

    public CustomerDAO(Connection connection) {
        super(connection, new CustomerMapper());
    }

    @Override
    protected List<String> getOrderedAttributes() {
        return List.of("first_name", "last_name", "address", "phone", "email");
    }

    @Override
    protected void setCreateStatement(PreparedStatement statement, Customer customer) throws SQLException {
        setNonIdAttributes(statement, customer);
    }

    @Override
    protected void setUpdateStatement(PreparedStatement statement, Customer customer) throws SQLException {
        setNonIdAttributes(statement, customer);
        statement.setInt(6, customer.getID());
    }

    protected void setNonIdAttributes(PreparedStatement statement, Customer customer) throws SQLException {
        statement.setString(1, customer.getFirstName());
        statement.setString(2, customer.getLastName());
        statement.setString(3, customer.getAddress());
        statement.setString(4, customer.getPhone());
        statement.setString(5, customer.getEmail());
    }

    /**
     * Creates a customer and retrieves its autoincremented ID.
     * Ensures base class set ID for customer object,
     * If not, does a manual lookup by unique attributes (phone/email).
     *
     * @param customer the customer to create
     * @return Optional containing the created customer with ID
     * @throws DataAccessException if a database error occurs
     */
    public Optional<Customer> createCustomer(Customer customer) {
        try {
            boolean inserted = super.create(customer);
            if (!inserted) {
                return Optional.empty();  // insertion failed return empty optional
            }

            // IF DB says insertion was successfully but ID wasn't set for some reason.
            // Attempt to set ID another way by looking up customer first by phone number
            // Then by email since these fields are required to be unique.
            if (customer.getID() == null) {
                Map<String, String> lookupByPhone = Map.of("phone", customer.getPhone());
                Optional<Customer> byPhone = findByAttributes(lookupByPhone);
                if (byPhone.isPresent()) {
                    customer.setID(byPhone.get().getID()); // Set the ID
                    return byPhone;
                }

                // If not found by phone, try email
                Map<String, String> lookupByEmail = Map.of("email", customer.getEmail());
                Optional<Customer> byEmail = findByAttributes(lookupByEmail);
                if (byEmail.isPresent()) {
                    customer.setID(byEmail.get().getID()); // Set the ID
                    return byEmail;
                }

                return Optional.empty();
            }

            System.out.println("Created Customer with ID: " + customer.getID());
            return Optional.of(customer);

        } catch (DataAccessException e) {
            throw e;
        }
    }
}