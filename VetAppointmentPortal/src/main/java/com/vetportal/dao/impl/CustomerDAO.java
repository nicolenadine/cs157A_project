package com.vetportal.dao.impl;

import com.vetportal.exception.DataAccessException;
import com.vetportal.model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class CustomerDAO extends BaseDAO<Customer> {

    public CustomerDAO(Connection connection) {
        super(connection);
    }

    @Override
    protected String getCreateQuery() {
        return "INSERT INTO customer (first_name, last_name, email, phone, address) VALUES (?, ?, ?, ?, ?)";
    }

    @Override
    protected String getUpdateQuery() {
        return "UPDATE customer SET first_name = ?, last_name = ?, email = ?, phone = ?, address = ? WHERE id = ?";
    }

    @Override
    protected String getDeleteQuery() {
        return "DELETE FROM customer WHERE id = ?";
    }

    @Override
    protected String getFindByIdQuery() {
        return "SELECT * FROM customer WHERE id = ?";
    }

    @Override
    protected String getFindAllQuery() {
        return "SELECT * FROM customer";
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

    @Override
    protected Customer extractEntityFromResultSet(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getInt("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("address")
        );
    }

    // -----------  FIND CUSTOMER BY SEARCH FIELD -----------
    public Optional<Customer> findByFields(Map<String, String> fieldValueMap) {
        Set<String> allowedFields = Set.of("first_name", "last_name", "address", "email", "phone");

        if (fieldValueMap.isEmpty()) {
            throw new IllegalArgumentException("At least one field must be provided for search.");
        }

        for (String field : fieldValueMap.keySet()) {
            if (!allowedFields.contains(field)) {
                throw new IllegalArgumentException("Invalid field: " + field);
            }
        }

        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM customer WHERE ");
        String[] conditions = fieldValueMap.keySet().stream()
            .map(field -> field + " = ?")
            .toArray(String[]::new);
        queryBuilder.append(String.join(" AND ", conditions));

        try (PreparedStatement statement = connection.prepareStatement(queryBuilder.toString())) {
            int i = 1;
            for (String value : fieldValueMap.values()) {
                statement.setString(i++, value);
            }

            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return Optional.of(extractCustomer(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find customer with fields: " + fieldValueMap, e);
        }

        return Optional.empty();
    }
 }

