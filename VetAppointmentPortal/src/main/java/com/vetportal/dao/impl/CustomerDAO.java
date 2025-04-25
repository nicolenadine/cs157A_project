package com.vetportal.dao.impl;

import com.vetportal.mapper.CustomerMapper;
import com.vetportal.model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class CustomerDAO extends BaseDAO<Customer> {

    public CustomerDAO(Connection connection) {super(connection, new CustomerMapper());}

    @Override
    protected List<String> getOrderedAttributes() {
        return List.of("customer_id", "first_name", "last_name", "email", "phone", "address");
    }

    @Override
    protected Set<String> getAllowedAttributes() {
        return Set.of("customer_id", "first_name", "last_name", "email", "phone", "address");
    }

    @Override
    protected void setCreateStatement(PreparedStatement statement, Customer customer) throws SQLException {
        statement.setString(1, customer.getFirstName());
        statement.setString(2, customer.getLastName());
        statement.setString(3, customer.getEmail());
        statement.setString(4, customer.getPhone());
        statement.setString(5, customer.getAddress());
    }

    @Override
    protected void setUpdateStatement(PreparedStatement statement, Customer customer) throws SQLException {
        statement.setString(1, customer.getFirstName());
        statement.setString(2, customer.getLastName());
        statement.setString(3, customer.getEmail());
        statement.setString(4, customer.getPhone());
        statement.setString(5, customer.getAddress());
        statement.setInt(6, customer.getId());
    }

    public Optional<Customer> createCustomer(Customer customer) {
            boolean inserted = super.create(customer);
            if (!inserted) {return Optional.empty(); }

            Map<String, String> newCustomerPhone = Map.of("phone", customer.getPhone());

            return findByAttributes(newCustomerPhone);
    }
}

