package com.vetportal.mapper;

import com.vetportal.model.*;
import com.vetportal.dao.impl.PetDAO;
import com.vetportal.dao.impl.EmployeeDAO;
import com.vetportal.model.AppointmentType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class AppointmentMapper implements EntityMapper<Appointment> {
    private PetDAO petDAO;
    private EmployeeDAO employeeDAO;

    @Override
    public Map<String, String> getJavaToDbAttributeMap() {
        Map<String, String> map = new HashMap<>();
        map.put("id", "appointment_id");
        map.put("date", "date");
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

    @Override
    public Appointment mapResultSetToEntity(ResultSet rs) throws SQLException {
        int providerId = rs.getInt("provider");
        int petId = rs.getInt("pet");

        Employee provider = employeeDAO.findByID(providerId)
                .orElseThrow(() -> new SQLException("Provider not found"));

        Pet pet = petDAO.findByID(petId)
                .orElseThrow(() -> new SQLException("Pet not found"));

        Customer customer = pet.getOwner();

        return new Appointment(
                rs.getInt("appointment_id"),
                rs.getDate("date").toLocalDate().toString(),
                rs.getTime("time").toLocalTime().toString(),
                provider,
                AppointmentType.valueOf(rs.getString("appointment_type")),
                pet,
                customer
        );
    }

}
