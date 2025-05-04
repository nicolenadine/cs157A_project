package com.vetportal.mapper;

import com.vetportal.model.Employee;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class EmployeeMapper implements EntityMapper<Employee> {
    @Override
    public Map<String, String> getJavaToDbAttributeMap() {
        Map<String, String> map = new HashMap<>();
        map.put("id", "employee_id");
        map.put("firstname", "first_name");
        map.put("lastname", "last_name");
        map.put("address", "address");
        map.put("phone", "phone");
        map.put("email", "email");
        map.put("role", "role");
        return map;
    }

    @Override
    public String getTableName() {
        return "Employee";
    }

    @Override
    public Employee mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new Employee(
                rs.getInt("employee_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("address"),
                rs.getString("phone"),
                rs.getString("email"),
                Employee.Position.valueOf(rs.getString("role"))  // Convert DB string to Position enum
        );
    }
}