package com.vetportal.dao;

import com.vetportal.exception.AppointmentConflictException;
import com.vetportal.exception.DataAccessException;
import com.vetportal.mapper.AppointmentMapper;
import com.vetportal.mapper.AppointmentWithJoinMapper;
import com.vetportal.model.Appointment;
import com.vetportal.model.Employee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Extends the BaseDAO functionality to support Appointment table specific implementation
 *
 * Overrides the create, update, and delete methods of parent class.
 */
public class AppointmentDAO extends BaseDAO<Appointment> {
    private final EmployeeDAO employeeDAO;
    private final PetDAO petDAO;
    private final AppointmentWithJoinMapper joinMapper;

    public AppointmentDAO(Connection connection, EmployeeDAO employeeDAO, PetDAO petDAO) {
        super(connection, new AppointmentMapper(employeeDAO, petDAO));
        this.employeeDAO = employeeDAO;
        this.petDAO = petDAO;
        this.joinMapper = new AppointmentWithJoinMapper();
    }


    @Override
    protected List<String> getOrderedAttributes() {
        return List.of("appointment_date", "time", "provider", "appointment_type", "pet");
    }

    @Override
    protected void setCreateStatement(PreparedStatement statement, Appointment appointment) throws SQLException {
        setNonIdAttributes(statement, appointment);
    }

    @Override
    protected void setUpdateStatement(PreparedStatement statement, Appointment appointment) throws SQLException {
        setNonIdAttributes(statement, appointment);
        statement.setInt(6, appointment.getID());
    }

    // These lines shared between previous two methods. prevents unnecessary duplication
    protected void setNonIdAttributes(PreparedStatement statement, Appointment appointment) throws SQLException {
        statement.setString(1, appointment.getDate().toString());
        statement.setString(2, appointment.getTime().toString());
        statement.setInt(3, appointment.getProvider().getID());
        statement.setString(4, appointment.getAppointmentType().name());
        statement.setInt(5, appointment.getPet().getID());
    }

    // -------------   CREATE UPDATE AND DELETE METHODS ------------------

    /**
     * Creates an appointment based on the values in the Appointment object argument.
     *
     * @param appointment The Appointment object containing the values to be stored
     * @return true if the appointment was created, false otherwise
     * @throws DataAccessException if a database error occurs
     * @throws AppointmentConflictException if one of two constraints are violated:
     *      either the wrong type of provider is given for an appointment, or a provider
     *      is already booked at the specified time.
     */
    @Override
    public boolean create(Appointment appointment) {
        Employee provider = appointment.getProvider();

        // DB constraint specifies that only Vets can perform non-vaccine appointments
        if (!"VETERINARIAN".equals(provider.getRole().name()) &&
                !"VACCINATION".equals(appointment.getAppointmentType().name())) {
            throw new AppointmentConflictException("Only veterinarians can perform non-vaccination appointments.");
        }

        try {
            return super.create(appointment);

        } catch (DataAccessException e) {

            if (e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                throw new AppointmentConflictException("This provider already has an appointment at this time.", e);
            }
            throw new DataAccessException("Error creating appointment", e);
        }
    }


    /**
     * Updates an appointment based on the values in the Appointment object argument.
     *
     * @param appointment The Appointment object containing the values to be set
     *                    for the db row corresponding to the appointment's id attribute
     * @return true if the appointment was updated, false otherwise
     * @throws DataAccessException if a database error occurs
     * @throws AppointmentConflictException if one of two constraints are violated:
     *      either the wrong type of provider is given for an appointment, or a provider
     *      is already booked at the specified time.
     */
    @Override
    public boolean update(Appointment appointment) {
        // Check to ensure db constraint that (provider, date, time) must be unique
        if (isProviderSlotTaken(
                appointment.getProvider().getID(),
                appointment.getDate().toString(),
                appointment.getTime().toString(),
                appointment.getID())) {
            throw new AppointmentConflictException("This provider already has an appointment at this date/time.");
        }

        Employee provider = appointment.getProvider();

        // Check to ensure db constraint on which providers can offer a given service
        if (!"VETERINARIAN".equals(provider.getRole().name()) &&
                !"VACCINATION".equals(appointment.getAppointmentType().name())) {
            throw new AppointmentConflictException("Only veterinarians can perform non-vaccination appointments.");
        }

        try {
            return super.update(appointment);

        } catch (DataAccessException e) {
            throw new DataAccessException("Error updating appointment", e);
        }
    }

    /**
     * Deletes an appointment by ID.
     *
     * @param appointmentId The ID of the appointment to delete
     * @return true if the appointment was deleted, false otherwise
     * @throws DataAccessException if a database error occurs
     */
    @Override
    public boolean delete(Integer appointmentId) {
        // Fetch appointment
        Optional<Appointment> appointment = this.findByID(appointmentId);

        // Check to make sure optional type contains a value before attempting to delete
        if (appointment.isEmpty()) {
            System.out.println("No appointment found with ID: " + appointmentId);
            return false;
        }

        return super.delete(appointmentId);
    }

    // ------------------   QUERY METHODS -------------------


    /**
     * Retrieves all appointments scheduled for a specific date.
     * This method joins the Appointment table with Employee, Pet, and Customer tables
     *
     * Uses the AppointmentWithJoinMapper instead of AppointmentMapper to handle the column return format
     *      where name conflicts and other issues may otherwise arise
     *
     * @param date The date to search for appointments
     * @return A list of appointments scheduled for the specified date, ordered by time
     * @throws DataAccessException if a database error occurs
     */
    public List<Appointment> findAllAppointmentsByDate(LocalDate date) {
       // Uses the AppointmentDetailView table view to execute the join to reduce method clutter.
        String query = "SELECT * FROM AppointmentDetailView WHERE appointment_date = ? ORDER BY time";

        List<Appointment> appointments = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, date.toString()); // format 'YYYY-MM-DD'
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                Appointment appointment = joinMapper.mapResultSetToEntity(rs);
                appointments.add(appointment);
            }

            return appointments;
        } catch (SQLException e) {
            throw new DataAccessException("Error finding appointments by date", e);
        }
    }


    /**
     * Finds all appointments for a specific pet.
     *
     * @param petID The ID of the pet
     * @return An Optional containing a list of appointments for the specified pet,
     *         or an empty Optional if no appointments are found
     */
    public Optional<List<Appointment>> findAppointmentsByPetId(int petID) {
        List<Appointment> results = findAllByAttributes(Map.of("pet", String.valueOf(petID)));
        return results.isEmpty() ? Optional.empty() : Optional.of(results);
    }


    /**
     * Finds all appointments for a specific provider.
     *
     * @param providerID The ID of the provider (employee)
     * @return An Optional containing a list of appointments for the specified provider,
     *         or an empty Optional if no appointments are found
     */
    public Optional<List<Appointment>> findAppointmentsByProviderId(int providerID) {
        List<Appointment> results = findAllByAttributes(Map.of("provider", String.valueOf(providerID)));
        return results.isEmpty() ? Optional.empty() : Optional.of(results);
    }


    /**
     * Checks if a provider is already booked at a specific date and time.
     * This method is used to enforce the unique constraint on (provider, date, time) in Appointment table.
     *
     * @param providerId The ID of the provider to check
     * @param date The date to check
     * @param time The time to check
     * @param excludeAppointmentId The ID of an appointment to exclude from the check (used for updates)
     * @return true if the provider is already booked at the specified date and time, false otherwise
     * @throws DataAccessException if a database error occurs
     */
    public boolean isProviderSlotTaken(int providerId, String date, String time, Integer excludeAppointmentId) {
       // Query to check if a provider already has an appointment at the specified date and time
        String sql = """
        SELECT 1 FROM Appointment
        WHERE provider = ? AND date(appointment_date) = ? AND time = ?
        AND appointment_id != ?
        LIMIT 1
        """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, providerId);
            statement.setString(2, date);
            statement.setString(3, time);
            statement.setInt(4, excludeAppointmentId);
            ResultSet rs = statement.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new DataAccessException("Error checking provider schedule conflict", e);
        }
    }
}
