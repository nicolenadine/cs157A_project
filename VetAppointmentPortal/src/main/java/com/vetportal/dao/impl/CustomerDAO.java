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

    // -----------  FIND CUSTOMER BY PHONE, NAME, OR EMAIL -----------
    public Optional<Customer> findByPhone(String phone) {
        return findCustomer("SELECT * FROM customer WHERE phone = ?", phone);
    }

    public Optional<Customer> findByName(String firstName, String lastName) {
        String query = "SELECT * FROM customer WHERE first_name = ? AND last_name = ?";
        return findCustomer(query, firstName, lastName);
    }

    public Optional<Customer> findByEmail(String email) {
        return findCustomer("SELECT * FROM customer WHERE email = ?", email);
    }


    private Optional<Customer> findCustomer(String query, String... values) {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            for (int i = 0; i < values.length; i++) {
                statement.setString(i + 1, values[i]);  // JDBC is 1-indexed
            }
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return Optional.of(extractCustomer(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new DataAccessException("Failed to fetch customer with query: " + query, e);
        }
        return Optional.empty();
    }



    private Customer extractCustomer(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getInt("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("address")
        );
    }

}
