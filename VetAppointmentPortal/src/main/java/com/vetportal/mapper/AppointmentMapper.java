package com.vetportal.mapper;

import com.vetportal.model.*;
import com.vetportal.dao.PetDAO;
import com.vetportal.dao.EmployeeDAO;
import com.vetportal.model.AppointmentType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

// This version of Appointment mapping is intended for use when retrieving single appointments
// where doing a full join of Appointments with Employee, Pet, and Customer tables would be
// inefficient. This mapper takes the appointment Result Set and fetches the needed Employee,
// Pet, and Customer records by the id values returned in query's result set.
public class AppointmentMapper implements EntityMapper<Appointment> {
    private final PetDAO petDAO;
    private final EmployeeDAO employeeDAO;

    public AppointmentMapper(EmployeeDAO employeeDAO, PetDAO petDAO) {
        this.employeeDAO = employeeDAO;
        this.petDAO = petDAO;
    }

    // Key = Java Entity attributes, Value = corresponding field name in database table
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


    // Extracts attribute values from database result set and
    // creates a new Java Entity from returned db values
    @Override
    public Appointment mapResultSetToEntity(ResultSet rs) throws SQLException {
        int providerId = rs.getInt("provider");
        int petId = rs.getInt("pet");

        // Database only stores id references to Providers and Pets but
        // The java object expects complete Employee and Pet objects
        // So need to fetch the rest of the Employee and Pet attribute values
        Employee provider = employeeDAO.findByID(providerId)
                .orElseThrow(() -> new SQLException("Provider not found"));

        Pet pet = petDAO.findByID(petId)
                .orElseThrow(() -> new SQLException("Pet not found"));

        Customer customer = pet.getOwner();

        return new Appointment(
                rs.getInt("appointment_id"),
                LocalDate.parse(rs.getString("appointment_date")),
                LocalTime.parse(rs.getString("time")),
                provider,
                // Convert DB string to AppointmentType enum
                AppointmentType.valueOf(rs.getString("appointment_type")),
                pet,
                customer
        );
    }

}
