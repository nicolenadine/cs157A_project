package com.vetportal.dao.impl;

import com.vetportal.mapper.EmployeeMapper;
import com.vetportal.model.Employee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class EmployeeDAO extends BaseDAO<Employee> {

    public EmployeeDAO(Connection connection) {super(connection, new EmployeeMapper());}

    @Override
    protected List<String> getOrderedAttributes() {
        return List.of("employee_id", "first_name", "last_name", "address", "phone", "email", "role");
    }

    @Override
    protected Set<String> getAllowedAttributes() {
        return Set.of("employee_id", "first_name", "last_name", "address", "phone", "email", "role");
    }

    @Override
    protected void setCreateStatement(PreparedStatement statement, Employee employee) throws SQLException {
        statement.setString(1, employee.getFirstName());
        statement.setString(2, employee.getLastName());
        statement.setString(3, employee.getEmail());
        statement.setString(4, employee.getPhone());
        statement.setString(5, employee.getAddress());
        statement.setString(6, employee.getRole().name());
    }

    @Override
    protected void setUpdateStatement(PreparedStatement statement, Employee employee) throws SQLException {
        statement.setString(1, employee.getFirstName());
        statement.setString(2, employee.getLastName());
        statement.setString(3, employee.getEmail());
        statement.setString(4, employee.getPhone());
        statement.setString(5, employee.getAddress());
        statement.setString(6, employee.getRole().name());
        statement.setInt(7, employee.getID());
    }

    public Optional<Employee> createEmployee(Employee employee) {
        boolean inserted = super.create(employee);
        if (!inserted) {return Optional.empty(); }

        Map<String, String> newCustomerPhone = Map.of("phone", employee.getPhone());

        return findByAttributes(newCustomerPhone);
    }
}

