package com.vetportal.service;

import com.vetportal.dao.impl.AppointmentDAO;
import com.vetportal.dao.impl.CustomerDAO;
import com.vetportal.dao.impl.EmployeeDAO;
import com.vetportal.dao.impl.PetDAO;
import com.vetportal.dto.ServiceResponse;
import com.vetportal.exception.AppointmentConflictException;
import com.vetportal.exception.DataAccessException;
import com.vetportal.model.Appointment;
import com.vetportal.model.Employee;
import com.vetportal.model.Pet;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service layer responsible for appointment-related business logic.
 * <p>
 * Delegates database operations to the {@link AppointmentDAO} and wraps results in
 * {@link ServiceResponse} objects to include status and error handling.
 */
public class AppointmentService {
    private final AppointmentDAO appointmentDAO;
    private final EmployeeDAO employeeDAO;
    private final PetDAO petDAO;

    /**
     * Constructs a new AppointmentService using the given database connection.
     * Initializes the necessary DAOs (CustomerDAO, EmployeeDAO, PetDAO, and AppointmentDAO)
     * for appointment operations.
     *
     * @param conn an active SQL database connection
     */
    public AppointmentService(Connection conn) {

        CustomerDAO customerDAO = new CustomerDAO(conn);
        this.employeeDAO = new EmployeeDAO(conn);
        this.petDAO = new PetDAO(conn, customerDAO);
        this.appointmentDAO = new AppointmentDAO(conn, employeeDAO, petDAO);
    }

    // -------- CREATE, UPDATE, & DELETE METHODS --------

    /**
     * Creates a new appointment in the database.
     *
     * @param appointment the appointment to create
     * @return a service response containing the created appointment or an error
     */
    public ServiceResponse<Appointment> createAppointment(Appointment appointment) {
        try {
            // Validate provider exists
            if (appointment.getProvider() == null || appointment.getProvider().getID() == null) {
                return ServiceResponse.notFound("Provider is required for appointment");
            }
            Optional<Employee> provider = employeeDAO.findByID(appointment.getProvider().getID());
            if (provider.isEmpty()) {
                return ServiceResponse.notFound("Provider with ID " + appointment.getProvider().getID() + " not found");
            }

            // Validate pet exists
            if (appointment.getPet() == null || appointment.getPet().getID() == null) {
                return ServiceResponse.notFound("Pet is required for appointment");
            }
            Optional<Pet> pet = petDAO.findByID(appointment.getPet().getID());
            if (pet.isEmpty()) {
                return ServiceResponse.notFound("Pet with ID " + appointment.getPet().getID() + " not found");
            }

            boolean created = appointmentDAO.create(appointment);
            if (!created) {
                return ServiceResponse.notFound("Appointment could not be created");
            }
            return ServiceResponse.success(appointment);

        } catch (AppointmentConflictException conflict) {
            // Properly handle AppointmentConflictException with CONFLICT status
            return ServiceResponse.conflict(conflict.getMessage());
        } catch (DataAccessException e) {
            // Check for unique constraint violation in the exception message
            if (e.getMessage() != null && e.getMessage().contains("unique_provider_time")) {
                return ServiceResponse.conflict("This provider already has an appointment at this time");
            }
            return ServiceResponse.dbError("Failed to create appointment: " + e.getMessage());
        }
    }

    /**
     * Updates an existing appointment in the database.
     *
     * @param appointment the appointment to update
     * @return a service response containing the updated appointment or an error
     */
    public ServiceResponse<Appointment> updateAppointment(Appointment appointment) {
        try {
            // Validate appointment exists
            Optional<Appointment> existingAppointment = appointmentDAO.findByID(appointment.getID());
            if (existingAppointment.isEmpty()) {
                return ServiceResponse.notFound("Appointment with ID " + appointment.getID() + " not found");
            }

            // Validate provider exists
            if (appointment.getProvider() == null || appointment.getProvider().getID() == null) {
                return ServiceResponse.notFound("Provider is required for appointment");
            }
            Optional<Employee> provider = employeeDAO.findByID(appointment.getProvider().getID());
            if (provider.isEmpty()) {
                return ServiceResponse.notFound("Provider with ID " + appointment.getProvider().getID() + " not found");
            }

            // Validate pet exists
            if (appointment.getPet() == null || appointment.getPet().getID() == null) {
                return ServiceResponse.notFound("Pet is required for appointment");
            }
            Optional<Pet> pet = petDAO.findByID(appointment.getPet().getID());
            if (pet.isEmpty()) {
                return ServiceResponse.notFound("Pet with ID " + appointment.getPet().getID() + " not found");
            }

            boolean updated = appointmentDAO.update(appointment);
            if (!updated) {
                return ServiceResponse.notFound("Appointment not found or update failed");
            }
            return ServiceResponse.success(appointment);

        } catch (AppointmentConflictException conflict) {
            return ServiceResponse.conflict(conflict.getMessage());
        } catch (DataAccessException e) {
            if (e.getMessage() != null && e.getMessage().contains("unique_provider_time")) {
                return ServiceResponse.conflict("This provider already has an appointment at this time");
            }
            return ServiceResponse.dbError("Error updating appointment: " + e.getMessage());
        }
    }

    /**
     * Deletes an appointment from the database.
     *
     * @param appointmentId the ID of the appointment to delete
     * @return a service response indicating success or failure
     */
    public ServiceResponse<Boolean> deleteAppointment(int appointmentId) {
        try {
            // Verify the appointment exists
            Optional<Appointment> appointment = appointmentDAO.findByID(appointmentId);
            if (appointment.isEmpty()) {
                return ServiceResponse.notFound("Appointment with ID " + appointmentId + " not found");
            }

            boolean deleted = appointmentDAO.delete(appointmentId);
            if (!deleted) {
                return ServiceResponse.dbError("Failed to delete appointment");
            }
            return ServiceResponse.success(true);
        } catch (DataAccessException e) {
            return ServiceResponse.dbError("Error deleting appointment: " + e.getMessage());
        }
    }

    // -------- QUERY METHODS --------

    /**
     * Finds an appointment by ID.
     *
     * @param appointmentId the ID of the appointment to find
     * @return a service response containing the appointment or an error
     */
    public ServiceResponse<Appointment> findAppointmentById(int appointmentId) {
        try {
            Optional<Appointment> result = appointmentDAO.findByID(appointmentId);
            return result.map(ServiceResponse::success)
                    .orElseGet(() -> ServiceResponse.notFound("Appointment not found with ID: " + appointmentId));
        } catch (DataAccessException e) {
            return ServiceResponse.dbError("Database error: " + e.getMessage());
        }
    }

    /**
     * Finds all appointments for a specific date.
     *
     * @param date the date to search for appointments
     * @return a service response containing the list of appointments or an error
     */
    public ServiceResponse<List<Appointment>> findAppointmentsByDate(LocalDate date) {
        try {
            List<Appointment> appointments = appointmentDAO.findAllAppointmentsByDate(date);
            if (appointments.isEmpty()) {
                return ServiceResponse.notFound("No appointments found for date: " + date);
            }
            return ServiceResponse.success(appointments);
        } catch (DataAccessException e) {
            return ServiceResponse.dbError("Error retrieving appointments by date: " + e.getMessage());
        }
    }

    /**
     * Finds all appointments for a specific date by string representation.
     *
     * @param dateString the date as a string in format YYYY-MM-DD
     * @return a service response containing the list of appointments or an error
     */
    public ServiceResponse<List<Appointment>> findAppointmentsByDate(String dateString) {
        try {
            LocalDate date = LocalDate.parse(dateString);
            return findAppointmentsByDate(date);
        } catch (DateTimeParseException e) {
            return ServiceResponse.conflict("Invalid date format. Expected YYYY-MM-DD, got: " + dateString);
        }
    }

    /**
     * Finds all appointments for a specific pet.
     *
     * @param petId the ID of the pet
     * @return a service response containing the list of appointments or an error
     */
    public ServiceResponse<List<Appointment>> findAppointmentsByPetId(int petId) {
        try {
            // Validate pet exists
            Optional<Pet> pet = petDAO.findByID(petId);
            if (pet.isEmpty()) {
                return ServiceResponse.notFound("Pet with ID " + petId + " not found");
            }

            Optional<List<Appointment>> appointments = appointmentDAO.findAppointmentsByPetId(petId);
            return appointments.map(ServiceResponse::success)
                    .orElseGet(() -> ServiceResponse.notFound("No appointments found for pet ID: " + petId));
        } catch (DataAccessException e) {
            return ServiceResponse.dbError("Error retrieving appointments by pet ID: " + e.getMessage());
        }
    }

    /**
     * Finds all appointments for a specific provider.
     *
     * @param providerId the ID of the provider (employee)
     * @return a service response containing the list of appointments or an error
     */
    public ServiceResponse<List<Appointment>> findAppointmentsByProviderId(int providerId) {
        try {
            // Validate provider exists
            Optional<Employee> provider = employeeDAO.findByID(providerId);
            if (provider.isEmpty()) {
                return ServiceResponse.notFound("Provider with ID " + providerId + " not found");
            }

            Optional<List<Appointment>> appointments = appointmentDAO.findAppointmentsByProviderId(providerId);
            return appointments.map(ServiceResponse::success)
                    .orElseGet(() -> ServiceResponse.notFound("No appointments found for provider ID: " + providerId));
        } catch (DataAccessException e) {
            return ServiceResponse.dbError("Error retrieving appointments by provider ID: " + e.getMessage());
        }
    }

    /**
     * Checks if a provider is already booked at a specific date and time.
     *
     * @param providerId the ID of the provider
     * @param date the date of the appointment
     * @param time the time of the appointment
     * @param excludeAppointmentId optional ID of an appointment to exclude from the check (for updates)
     * @return true if the slot is already taken, false otherwise
     */
    public boolean isProviderSlotTaken(int providerId, LocalDate date, LocalTime time, Integer excludeAppointmentId) {
        try {
            return appointmentDAO.isProviderSlotTaken(
                    providerId,
                    date.toString(),
                    time.toString(),
                    excludeAppointmentId != null ? excludeAppointmentId : 0
            );
        } catch (DataAccessException e) {
            System.err.println("Error checking provider availability: " + e.getMessage());
            return true; // Assume conflict if error occurs, to be safe
        }
    }

    /**
     * Looks up appointments by various attributes.
     *
     * @param attributes map of attribute names to values to search for
     * @return a service response containing the appointments or an error
     */
    public ServiceResponse<List<Appointment>> findAppointmentsByAttributes(Map<String, String> attributes) {
        try {
            List<Appointment> appointments = appointmentDAO.findAllByAttributes(attributes);
            if (appointments.isEmpty()) {
                return ServiceResponse.notFound("No appointments found with attributes: " + attributes);
            }
            return ServiceResponse.success(appointments);
        } catch (DataAccessException e) {
            return ServiceResponse.dbError("Database error: " + e.getMessage());
        }
    }

    /**
     * Retrieves all appointments from the database.
     *
     * @return a service response containing the list of appointments or an error
     */
    public ServiceResponse<List<Appointment>> getAllAppointments() {
        try {
            List<Appointment> appointments = appointmentDAO.findAll();
            if (appointments.isEmpty()) {
                return ServiceResponse.notFound("No appointments found");
            }
            return ServiceResponse.success(appointments);
        } catch (DataAccessException e) {
            return ServiceResponse.dbError("Error retrieving appointments: " + e.getMessage());
        }
    }
}