package com.vetportal.mapper;

import com.vetportal.model.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class AppointmentWithJoinMapper implements EntityMapper<Appointment> {

    @Override
    public Map<String, String> getJavaToDbAttributeMap() {
        Map<String, String> map = new HashMap<>();
        map.put("id", "appointment_id");
        map.put("date", "appointment_date");
        map.put("time", "time");
        map.put("provider", "provider");
        map.put("appointmentType", "appointment_type");
        map.put("pet", "pet");
        return map;
    }

    @Override
    public String getTableName() {
        return "Appointment";
    }

    // Creates entities in the order required to ensure objects are created before
    // other entities that depend on them (e.g., Pet has a Customer dependency)
    @Override
    public Appointment mapResultSetToEntity(ResultSet rs) throws SQLException {

        Employee provider = new Employee(
                rs.getInt("employee_id"),
                rs.getString("employee_first_name"),
                rs.getString("employee_last_name"),
                rs.getString("employee_address"),
                rs.getString("employee_phone"),
                rs.getString("employee_email"),
                Employee.Position.valueOf(rs.getString("role"))
        );

        Customer owner = new Customer(
                rs.getInt("customer_id"),
                rs.getString("customer_first_name"),
                rs.getString("customer_last_name"),
                rs.getString("customer_address"),
                rs.getString("customer_phone"),
                rs.getString("customer_email")
        );

        Pet pet = new Pet(
                rs.getInt("pet_id"),
                rs.getString("pet_name"),
                rs.getString("species"),
                rs.getString("breed"),
                LocalDate.parse(rs.getString("birth_date")),
                owner
        );

        return new Appointment(
                rs.getInt("appointment_id"),
                LocalDate.parse(rs.getString("appointment_date")),
                LocalTime.parse(rs.getString("time")),
                provider,
                AppointmentType.valueOf(rs.getString("appointment_type")),
                pet,
                owner
        );
    }
}