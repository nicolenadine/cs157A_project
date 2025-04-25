package com.vetportal.service;

import com.vetportal.dao.impl.AppointmentDAO;
import com.vetportal.dto.ServiceResponse;
import com.vetportal.exception.AppointmentConflictException;
import com.vetportal.exception.DataAccessException;
import com.vetportal.model.Appointment;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AppointmentService {
    private AppointmentDAO appointmentDAO;

    public AppointmentService(Connection conn) {
        this.appointmentDAO = new AppointmentDAO(conn);
    }

    public ServiceResponse<Appointment> createAppointment(Appointment appointment) {
        try {
            boolean created = appointmentDAO.create(appointment);
            if (!created) {
                return ServiceResponse.notFound("Appointment could not be created.");
            }
            return ServiceResponse.success(appointment);

        } catch (AppointmentConflictException conflict) {
            return ServiceResponse.conflict(conflict.getMessage());

        } catch (DataAccessException e) {
            return ServiceResponse.dbError("Failed to create appointment: " + e.getMessage());
        }
    }

    public ServiceResponse<Appointment> updateAppointment(Appointment appointment) {
        try {
            boolean updated = appointmentDAO.update(appointment);
            if (!updated) {
                return ServiceResponse.notFound("Appointment not found or update failed.");
            }
            return ServiceResponse.success(appointment);
        } catch (Exception e) {
            return ServiceResponse.dbError("Error updating appointment: " + e.getMessage());
        }
    }


    public boolean deleteAppointment(int appointmentId) {
        return appointmentDAO.delete(appointmentId);
    }



    public ServiceResponse<List<Appointment>> findAppointmentsByDate(String date) {
        try {
            List<Appointment> appointments = appointmentDAO.findAllByAttributes(Map.of("date", date));
            if (appointments.isEmpty()) {
                return ServiceResponse.notFound("No appointments found for date: " + date);
            }
            return ServiceResponse.success(appointments);
        } catch (DataAccessException e) {
            return ServiceResponse.dbError("Database error while retrieving appointments: " + e.getMessage());
        }
    }

}
