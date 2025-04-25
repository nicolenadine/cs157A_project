package com.vetportal.dao.impl;

import com.vetportal.exception.AppointmentConflictException;
import com.vetportal.exception.DataAccessException;
import com.vetportal.mapper.AppointmentMapper;
import com.vetportal.model.Appointment;
import com.vetportal.model.Employee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AppointmentDAO extends BaseDAO<Appointment> {

    public AppointmentDAO(Connection connection) {super(connection, new AppointmentMapper());}

    @Override
    protected List<String> getOrderedAttributes() {
        return List.of("appointment_id", "date", "time", "provider", "appointment_type", "pet");
    }

    @Override
    protected Set<String> getAllowedAttributes() {
        return Set.of("appointment_id", "date", "time", "provider", "appointment_type", "pet");
    }

    @Override
    protected void setCreateStatement(PreparedStatement statement, Appointment appointment) throws SQLException {
        statement.setString(1, appointment.getDate());
        statement.setString(2, appointment.getTime());
        statement.setInt(3, appointment.getProvider().getID());
        statement.setString(4, appointment.getAppointmentType().name());
        statement.setInt(5, appointment.getPet().getID());
    }

    @Override
    protected void setUpdateStatement(PreparedStatement statement, Appointment appointment) throws SQLException {
        statement.setString(1, appointment.getDate());
        statement.setString(2, appointment.getTime());
        statement.setInt(3, appointment.getProvider().getID());
        statement.setString(4, appointment.getAppointmentType().name());
        statement.setInt(5, appointment.getPet().getID());
        statement.setInt(6, appointment.getID());
    }

    @Override
    public boolean create(Appointment appointment) {
        Employee provider = appointment.getProvider();

        if (!"VETERINARIAN".equals(provider.getRole()) &&
                !"VACCINATION".equals(appointment.getAppointmentType().name())) {
            throw new AppointmentConflictException("Only veterinarians can perform non-vaccination appointments.");
        }

        try (PreparedStatement statement = connection.prepareStatement(getCreateQuery())) {
            setCreateStatement(statement, appointment);
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            if (e.getMessage().contains("unique_provider_time")) {
                throw new AppointmentConflictException("This provider already has an appointment at this time.", e);
            }
            throw new DataAccessException("Error creating appointment", e);
        }
    }

    @Override
    public boolean update(Appointment appointment) {
        if (isProviderSlotTaken(
                appointment.getProvider().getID(),
                appointment.getDate(),
                appointment.getTime(),
                appointment.getID())) {
            throw new AppointmentConflictException("This provider already has an appointment at this time.");
        }

        Employee provider = appointment.getProvider();

        if (!"VETERINARIAN".equals(provider.getRole()) &&
                !"VACCINATION".equals(appointment.getAppointmentType().name())) {
            throw new AppointmentConflictException("Only veterinarians can perform non-vaccination appointments.");
        }

        return super.update(appointment);
    }


    public Optional<List<Appointment>> findAppointmentsByPetId(int petID) {
        List<Appointment> results = findAllByAttributes(Map.of("pet", String.valueOf(petID)));
        return results.isEmpty() ? Optional.empty() : Optional.of(results);
    }

    // Ensure that provider is not double booked is enforced to avoid db constraint errors
    public boolean isProviderSlotTaken(int providerId, String date, String time, Integer excludeAppointmentId) {
        String sql = """
        SELECT 1 FROM Appointment
        WHERE provider = ? AND date = ? AND time = ?
        AND appointment_id != ?
        LIMIT 1
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, providerId);
            stmt.setString(2, date);
            stmt.setString(3, time);
            stmt.setInt(4, excludeAppointmentId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new DataAccessException("Error checking provider schedule conflict", e);
        }
    }
}
