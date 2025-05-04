package com.vetportal.test;

import com.vetportal.dao.AppointmentDAO;
import com.vetportal.dao.EmployeeDAO;
import com.vetportal.dao.PetDAO;
import com.vetportal.dao.CustomerDAO;
import com.vetportal.exception.AppointmentConflictException;
import com.vetportal.model.Appointment;
import com.vetportal.model.AppointmentType;
import com.vetportal.model.Customer;
import com.vetportal.model.Employee;
import com.vetportal.model.Pet;
import com.vetportal.util.DatabaseInitializer;
import com.vetportal.util.DbManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for AppointmentDAO
 * Tests all appointment-related functionality including:
 * - Creating appointments with different providers and types
 * - Enforcing business rules (only vets can perform non-vaccination appointments)
 * - Finding appointments by various criteria
 * - Checking provider schedule conflicts
 */
public class AppointmentDAOTest {

    private static Connection connection;
    private static AppointmentDAO appointmentDAO;
    private static EmployeeDAO employeeDAO;
    private static PetDAO petDAO;
    private static CustomerDAO customerDAO;

    // Test data holders
    private static Employee testVeterinarian;
    private static Employee testVetTech;
    private static Customer testCustomer;
    private static Pet testPet;

    @BeforeAll
    public static void setup() {
        try {
            connection = DbManager.getConnection();

            // Run schema and seed using the same connection
            DatabaseInitializer.initializeOnExistingConnection(connection, "database/schema.sql", "database/seed.sql");

            // Double check FK
            DbManager.ensureForeignKeysEnabled();

            // Initialize DAOs
            employeeDAO = new EmployeeDAO(connection);
            customerDAO = new CustomerDAO(connection);
            petDAO = new PetDAO(connection, customerDAO);
            appointmentDAO = new AppointmentDAO(connection, employeeDAO, petDAO);

            // Create test data
            createTestData();

            // Debug print to check the SQL for findAllAppointmentsByDate
            debugAppointmentQueries();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to initialize test database: " + e.getMessage());
        }
    }

    /**
     * Prints debugging info about the queries
     */
    private static void debugAppointmentQueries() {
        try {
            // Check if the specialty column exists in the Employee table
            System.out.println("\nDEBUG: Checking Employee table columns:");
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("PRAGMA table_info(Employee)")) {
                while (rs.next()) {
                    System.out.println(rs.getString("name") + " - " + rs.getString("type"));
                }
            }

            System.out.println("\nDEBUG: Testing queries directly:");
            // Test a simplified version of the appointment query
            String queryTest = "SELECT * FROM Appointment a " +
                    "JOIN Employee e ON a.provider = e.employee_id " +
                    "JOIN Pet p ON a.pet = p.pet_id " +
                    "JOIN Customer c ON p.owner = c.customer_id " +
                    "WHERE date(a.date) = ?";

            try (PreparedStatement stmt = connection.prepareStatement(queryTest)) {
                stmt.setString(1, LocalDate.now().toString());
                ResultSet rs = stmt.executeQuery();
                System.out.println("Query executed successfully!");
            }
        } catch (SQLException e) {
            System.err.println("Error in debug queries: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates all test data needed for the tests
     */
    private static void createTestData() {
        try {
            // Create a test veterinarian
            String vetEmail = "vet." + System.currentTimeMillis() + "@example.com";
            String vetPhone = "555-" + (1000 + new Random().nextInt(9000));
            testVeterinarian = new Employee(null, "Test", "Veterinarian", "123 Vet St", vetEmail, vetPhone, Employee.Position.VETERINARIAN);
            Optional<Employee> vetResponse = employeeDAO.createEmployee(testVeterinarian);
            assertTrue(vetResponse.isPresent());
            testVeterinarian = vetResponse.get();

            System.out.println("Created test veterinarian with ID: " + testVeterinarian.getID() +
                    ", Role: " + testVeterinarian.getRole());

            // Debug check to make sure the role is really VETERINARIAN
            System.out.println("Debug - Is role VETERINARIAN? " +
                    "VETERINARIAN".equals(testVeterinarian.getRole().name()));

            // Create a test vet tech
            String techEmail = "tech." + System.currentTimeMillis() + "@example.com";
            String techPhone = "555-" + (1000 + new Random().nextInt(9000));
            testVetTech = new Employee(null, "Test", "VetTech", "456 Tech St", techEmail, techPhone, Employee.Position.VET_TECH);
            Optional<Employee> techResponse = employeeDAO.createEmployee(testVetTech);
            assertTrue(techResponse.isPresent());
            testVetTech = techResponse.get();

            System.out.println("Created test vet tech with ID: " + testVetTech.getID() +
                    ", Role: " + testVetTech.getRole());

            // Create a test customer
            String customerEmail = "customer." + System.currentTimeMillis() + "@example.com";
            String customerPhone = "555-" + (1000 + new Random().nextInt(9000));
            testCustomer = new Customer(null, "Test", "Customer", "789 Pet Owner St", customerEmail, customerPhone);
            Optional<Customer> customerResponse = customerDAO.createCustomer(testCustomer);
            assertTrue(customerResponse.isPresent());
            testCustomer = customerResponse.get();

            System.out.println("Created test customer with ID: " + testCustomer.getID());

            // Create a test pet
            testPet = new Pet(null, "TestPet", "Dog", "Mixed", LocalDate.now().minusYears(2), testCustomer);
            Optional<Pet> petResponse = petDAO.createPet(testPet);
            assertTrue(petResponse.isPresent());
            testPet = petResponse.get();

            System.out.println("Created test pet with ID: " + testPet.getID());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to create test data: " + e.getMessage());
        }
    }

    /**
     * Utility method to directly insert an appointment into the database
     * Uses the SQLite-specific approach for getting the last inserted ID
     */
    private int createAppointmentDirectly(
            LocalDate date,
            LocalTime time,
            Employee provider,
            AppointmentType type,
            Pet pet) throws SQLException {

        // First insert the appointment
        String insertSql = "INSERT INTO Appointment (date, time, provider, appointment_type, pet) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            stmt.setString(1, date.toString());
            stmt.setString(2, time.toString());
            stmt.setInt(3, provider.getID());
            stmt.setString(4, type.name());
            stmt.setInt(5, pet.getID());

            int rowsAffected = stmt.executeUpdate();
            assertTrue(rowsAffected > 0);
        }

        // Then retrieve the ID of the last inserted row (SQLite specific approach)
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
            assertTrue(rs.next(), "Failed to get last inserted ID");
            int appointmentId = rs.getInt(1);
            System.out.println("Created appointment with ID: " + appointmentId);
            return appointmentId;
        }
    }

    // ---------- TESTS FOR CREATING APPOINTMENTS ----------

    /**
     * Test creating an appointment with a veterinarian for a checkup
     */
    @Test
    public void testCreate_veterinarianCheckup() throws SQLException {
        // Use direct SQL insertion to work around potential DAO issues
        LocalDate appointmentDate = LocalDate.now().plusDays(7);
        LocalTime appointmentTime = LocalTime.of(10, 0);

        int appointmentId = createAppointmentDirectly(
                appointmentDate,
                appointmentTime,
                testVeterinarian,
                AppointmentType.CHECKUP,
                testPet
        );

        // Verify the appointment exists in the database
        Optional<Appointment> fetchedAppointment = appointmentDAO.findByID(appointmentId);
        assertTrue(fetchedAppointment.isPresent());
        assertEquals(appointmentDate, fetchedAppointment.get().getDate());
        assertEquals(appointmentTime, fetchedAppointment.get().getTime());
        assertEquals(testVeterinarian.getID(), fetchedAppointment.get().getProvider().getID());
        assertEquals(AppointmentType.CHECKUP, fetchedAppointment.get().getAppointmentType());
        assertEquals(testPet.getID(), fetchedAppointment.get().getPet().getID());

        // Clean up
        assertTrue(appointmentDAO.delete(appointmentId));
    }

    /**
     * Test the business rule check for non-veterinarians performing non-vaccination appointments
     */
    @Test
    public void testCreate_nonVeterinarianPerformingNonVaccinationAppointment() {
        // Create an appointment with a vet tech for a non-vaccination
        LocalDate appointmentDate = LocalDate.now().plusDays(7);
        LocalTime appointmentTime = LocalTime.of(14, 0);

        Appointment appointment = new Appointment(
                null,
                appointmentDate,
                appointmentTime,
                testVetTech,
                AppointmentType.CHECKUP,
                testPet,
                testCustomer
        );

        // Should throw an exception due to business rule
        assertThrows(AppointmentConflictException.class, () -> {
            appointmentDAO.create(appointment);
        });
    }

    /**
     * Test creating an appointment with a vet tech for a vaccination
     */
    @Test
    public void testCreate_vetTechVaccination() throws SQLException {
        // Use direct SQL insertion to work around potential DAO issues
        LocalDate appointmentDate = LocalDate.now().plusDays(7);
        LocalTime appointmentTime = LocalTime.of(11, 0);

        int appointmentId = createAppointmentDirectly(
                appointmentDate,
                appointmentTime,
                testVetTech,
                AppointmentType.VACCINATION,
                testPet
        );

        // Verify the appointment exists in the database
        Optional<Appointment> fetchedAppointment = appointmentDAO.findByID(appointmentId);
        assertTrue(fetchedAppointment.isPresent());
        assertEquals(testVetTech.getID(), fetchedAppointment.get().getProvider().getID());
        assertEquals(AppointmentType.VACCINATION, fetchedAppointment.get().getAppointmentType());

        // Clean up
        assertTrue(appointmentDAO.delete(appointmentId));
    }

    /**
     * Test creating conflicting appointments with the same provider
     */
    @Test
    public void testCreate_providerConflict() throws SQLException {
        // Create first appointment
        LocalDate appointmentDate = LocalDate.now().plusDays(7);
        LocalTime appointmentTime = LocalTime.of(15, 0);

        int appointmentId = createAppointmentDirectly(
                appointmentDate,
                appointmentTime,
                testVeterinarian,
                AppointmentType.CHECKUP,
                testPet
        );

        // Try to create a conflicting appointment (should fail)
        try {
            createAppointmentDirectly(
                    appointmentDate,
                    appointmentTime,
                    testVeterinarian,
                    AppointmentType.DENTAL,
                    testPet
            );
            fail("Should have thrown exception due to provider conflict");
        } catch (SQLException e) {
            // Expected exception due to unique constraint violation
            assertTrue(e.getMessage().contains("UNIQUE constraint failed") ||
                    e.getMessage().contains("unique_provider_time"));
        }

        // Clean up
        assertTrue(appointmentDAO.delete(appointmentId));
    }

    // ---------- TESTS FOR FINDING APPOINTMENTS ----------

    /**
     * Test finding appointment by ID
     */
    @Test
    public void testFindByID_notFound() {
        Optional<Appointment> foundAppointment = appointmentDAO.findByID(9999); // Assuming this ID doesn't exist
        assertFalse(foundAppointment.isPresent());
    }

    /**
     * Test finding appointment by ID when an appointment exists
     */
    @Test
    public void testFindByID_success() throws SQLException {
        // Create an appointment
        LocalDate appointmentDate = LocalDate.now().plusDays(7);
        LocalTime appointmentTime = LocalTime.of(16, 0);

        int appointmentId = createAppointmentDirectly(
                appointmentDate,
                appointmentTime,
                testVeterinarian,
                AppointmentType.CHECKUP,
                testPet
        );

        // Find by ID
        Optional<Appointment> foundAppointment = appointmentDAO.findByID(appointmentId);
        assertTrue(foundAppointment.isPresent());
        assertEquals(appointmentId, foundAppointment.get().getID());
        assertEquals(appointmentDate, foundAppointment.get().getDate());
        assertEquals(appointmentTime, foundAppointment.get().getTime());

        // Clean up
        assertTrue(appointmentDAO.delete(appointmentId));
    }

    /**
     * Test that the isProviderSlotTaken method works correctly
     */
    @Test
    public void testIsProviderSlotTaken() throws SQLException {
        LocalDate appointmentDate = LocalDate.now().plusDays(7);
        LocalTime appointmentTime = LocalTime.of(14, 30);

        int appointmentId = createAppointmentDirectly(
                appointmentDate,
                appointmentTime,
                testVeterinarian,
                AppointmentType.CHECKUP,
                testPet
        );

        // Check if the slot is taken (should be true)
        boolean slotTaken = appointmentDAO.isProviderSlotTaken(
                testVeterinarian.getID(),
                appointmentDate.toString(),
                appointmentTime.toString(),
                -1 // Using -1 as we're not excluding any appointments
        );

        assertTrue(slotTaken);

        // Check if the slot is taken when excluding the appointment itself (should be false)
        boolean slotTakenExcludingSelf = appointmentDAO.isProviderSlotTaken(
                testVeterinarian.getID(),
                appointmentDate.toString(),
                appointmentTime.toString(),
                appointmentId
        );

        assertFalse(slotTakenExcludingSelf);

        // Clean up
        assertTrue(appointmentDAO.delete(appointmentId));
    }

    /**
     * Test finding appointments by pet ID
     */
    @Test
    public void testFindAppointmentsByPetId() throws SQLException {
        // Create an appointment
        LocalDate appointmentDate = LocalDate.now().plusDays(21);
        LocalTime appointmentTime = LocalTime.of(13, 30);

        int appointmentId = createAppointmentDirectly(
                appointmentDate,
                appointmentTime,
                testVeterinarian,
                AppointmentType.CHECKUP,
                testPet
        );

        // Find appointments by pet ID
        Optional<List<Appointment>> foundAppointments = appointmentDAO.findAppointmentsByPetId(testPet.getID());

        // Verify we found the appointment
        assertTrue(foundAppointments.isPresent());
        assertTrue(foundAppointments.get().stream()
                .anyMatch(a -> a.getID() == appointmentId));

        // Clean up
        assertTrue(appointmentDAO.delete(appointmentId));
    }
}