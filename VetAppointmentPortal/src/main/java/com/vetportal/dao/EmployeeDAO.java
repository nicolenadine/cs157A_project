package com.vetportal.dao;

import com.vetportal.exception.DataAccessException;
import com.vetportal.mapper.EmployeeMapper;
import com.vetportal.model.Employee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Extends the BaseDAO functionality to support Employee table specific implementation
 */
public class EmployeeDAO extends BaseDAO<Employee> {

    public EmployeeDAO(Connection connection) {
        super(connection, new EmployeeMapper());
    }

    @Override
    protected List<String> getOrderedAttributes() {
        // Remove employee_id as it's auto-incremented
        return List.of("first_name", "last_name", "address", "phone", "email", "role");
    }

    @Override
    protected void setCreateStatement(PreparedStatement statement, Employee employee) throws SQLException {
        setNonIdAttributes(statement, employee);
    }

    @Override
    protected void setUpdateStatement(PreparedStatement statement, Employee employee) throws SQLException {
        setNonIdAttributes(statement, employee);
        statement.setInt(7, employee.getID());
    }

    protected void setNonIdAttributes(PreparedStatement statement, Employee employee) throws SQLException {
        statement.setString(1, employee.getFirstName());
        statement.setString(2, employee.getLastName());
        statement.setString(3, employee.getAddress());
        statement.setString(4, employee.getPhone());
        statement.setString(5, employee.getEmail());
        statement.setString(6, employee.getRole().name());
    }

    /**
     * Creates an employee and retrieves its autoincremented ID.
     * Uses the base class create method which attempts to set the ID.
     * If that fails, does a manual lookup.
     *
     * @param employee the employee to create
     * @return Optional containing the created employee with ID
     * @throws DataAccessException if a database error occurs
     */
    public Optional<Employee> createEmployee(Employee employee) {
        try {
            boolean inserted = super.create(employee);
            if (!inserted) {
                return Optional.empty();  // insertion failed return empty optional
            }

            // IF DB says insertion was successfully but ID wasn't set for some reason.
            // Attempt to set ID another way by looking up employee first by phone number
            // Then by email since these fields are required to be unique in DB schema.
            if (employee.getID() == null) {
                Map<String, String> lookupByPhone = Map.of("phone", employee.getPhone());
                Optional<Employee> byPhone = findByAttributes(lookupByPhone);
                if (byPhone.isPresent()) {
                    employee.setID(byPhone.get().getID()); // Set the ID
                    return byPhone;
                }

                // If not found by phone, try email
                Map<String, String> lookupByEmail = Map.of("email", employee.getEmail());
                Optional<Employee> byEmail = findByAttributes(lookupByEmail);
                if (byEmail.isPresent()) {
                    employee.setID(byEmail.get().getID()); // Set the ID
                    return byEmail;
                }

                return Optional.empty();
            }

            System.out.println("Created Employee with ID: " + employee.getID());
            return Optional.of(employee);

        } catch (DataAccessException e) {
            throw e;
        }
    }
}