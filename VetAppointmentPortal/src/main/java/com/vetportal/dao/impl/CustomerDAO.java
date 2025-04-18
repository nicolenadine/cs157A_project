package com.vetportal.dao.impl;

import com.vetportal.model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class CustomerDAO extends BaseDAO<Customer> {

    public CustomerDAO(Connection connection) {
        super(connection);
    }

    @Override
    protected String getCreateQuery() {
        return "INSERT INTO customers (first_name, last_name, email, phone, address) VALUES (?, ?, ?, ?, ?)";
    }

    @Override
    protected String getUpdateQuery() {
        return "UPDATE customers SET first_name = ?, last_name = ?, email = ?, phone = ?, address = ? WHERE id = ?";
    }

    @Override
    protected String getDeleteQuery() {
        return "DELETE FROM customers WHERE id = ?";
    }

    @Override
    protected String getFindByIdQuery() {
        return "SELECT * FROM customers WHERE id = ?";
    }

    @Override
    protected String getFindAllQuery() {
        return "SELECT * FROM customers";
    }

    @Override
    protected void setCreateStatement(PreparedStatement statement, Customer customer) throws SQLException {
        statement.setString(1, customer.getFirstname());
        statement.setString(2, customer.getLastname());
        statement.setString(3, customer.getEmail());
        statement.setString(4, customer.getPhone());
        statement.setString(5, customer.getAddress());
    }

    @Override
    protected void setUpdateStatement(PreparedStatement statement, Customer customer) throws SQLException {
        statement.setString(1, customer.getFirstname());
        statement.setString(2, customer.getLastname());
        statement.setString(3, customer.getEmail());
        statement.setString(4, customer.getPhone());
        statement.setString(5, customer.getAddress());
        statement.setInt(6, customer.getId());
    }

    @Override
    protected Customer extractEntityFromResultSet(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getInt("id"),
                rs.getString("firstname"),
                rs.getString("lastname"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("address")
        );
    }

    public Customer findByPhone(String phone) {
        String query = "SELECT * FROM customers WHERE phone = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, phone);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return extractEntityFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            // Log exception
        }
        return null;
    }

    public Customer findByEmail(String email) {
        String query = "SELECT * FROM customers WHERE email = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return extractEntityFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            // Log exception
        }
        return null;
    }
}
