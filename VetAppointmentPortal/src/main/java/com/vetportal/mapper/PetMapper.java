package com.vetportal.mapper;

import com.vetportal.model.Pet;
import com.vetportal.model.Customer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class PetMapper implements EntityMapper<Pet> {
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

    @Override
    public Pet mapResultSetToEntity(ResultSet rs) throws SQLException {
        Customer owner = new Customer(
                rs.getInt("customer_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("address"),
                rs.getString("phone"),
                rs.getString("email")
        );


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
