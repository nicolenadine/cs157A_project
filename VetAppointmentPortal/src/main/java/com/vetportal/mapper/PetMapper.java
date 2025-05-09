package com.vetportal.mapper;

import com.vetportal.model.Pet;
import com.vetportal.model.Customer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class PetMapper implements EntityMapper<Pet> {
    // Key = Java Entity attributes, Value = corresponding field name in database table
    @Override
    public Map<String, String> getJavaToDbAttributeMap() {
        Map<String, String> map = new HashMap<>();
        map.put("id", "pet_id");
        map.put("name", "pet_name");
        map.put("species", "species");
        map.put("breed", "breed");
        map.put("birthDate", "birth_date");

        return map;
    }

    @Override
    public String getTableName() {
        return "Pet";
    }

    // Extracts attribute values from database result set and
    // creates a new Java Entity from returned db values
    @Override
    public Pet mapResultSetToEntity(ResultSet rs) throws SQLException {

        // Create customer first since pet depends on customer id
        Customer owner = new Customer(
                rs.getInt("customer_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("address"),
                rs.getString("phone"),
                rs.getString("email")
        );

        // Create and return pet using newly created customer object for owner attribute
        return new Pet(
                rs.getInt("pet_id"),
                rs.getString("pet_name"),
                rs.getString("species"),
                rs.getString("breed"),
                LocalDate.parse(rs.getString("birth_date")),
                owner
        );
    }
}
