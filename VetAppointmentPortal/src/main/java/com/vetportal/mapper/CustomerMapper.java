package com.vetportal.mapper;

import com.vetportal.model.Customer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CustomerMapper implements EntityMapper<Customer> {
    // Key = Java Entity attributes, Value = corresponding field name in database table
    @Override
    public Map<String, String> getJavaToDbAttributeMap() {
        Map<String, String> map = new HashMap<>();
        map.put("id", "customer_id");
        map.put("firstName", "first_name");
        map.put("lastName", "last_name");
        map.put("address", "address");
        map.put("phone", "phone");
        map.put("email", "email");

        return map;
    }

    @Override
    public String getTableName() {
        return "Customer";
    }

    // Extracts attribute values from database result set and
    // creates a new Java Entity from returned db values
    @Override
    public Customer mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getInt("customer_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("address"),
                rs.getString("phone"),
                rs.getString("email")
        );
    }
}