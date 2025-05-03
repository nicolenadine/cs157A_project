package com.vetportal.test;

import com.vetportal.dao.impl.AppointmentDAO;
import com.vetportal.dao.impl.EmployeeDAO;
import com.vetportal.dao.impl.PetDAO;
import com.vetportal.dao.impl.CustomerDAO;
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
 * Focused test class for AppointmentDAO
 * Specifically tests:
 * 1. findAllAppointmentsByDate method
 * 2. create method without bypassing
 */
public class AppointmentDAOFocusedTest {

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

            // Debug the query issue
            debugFindByDateQuery();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to initialize test database: " + e.getMessage());
        }
    }

    /**
     * Debug the findAllAppointmentsByDate query issue
     */
    private static void debugFindByDateQuery() {
        try {
            System.out.println("\n----- DEBUG FIND BY DATE QUERY -----");

            // Check Employee table schema
            System.out.println("Employee table columns:");
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("PRAGMA table_info(Employee)")) {
                while (rs.next()) {
                    System.out.println(rs.getString("name") + " - " + rs.getString("type"));
                }
            }

            // Test a modified version of the query used in AppointmentDAO.findAllAppointmentsByDate
            System.out.println("\nTesting simplified query without specialty column:");
            String testQuery = """
            SELECT a.appointment_id, date(a.date) as date, a.time, 
            a.provider, a.appointment_type, a.pet,
            e.employee_id, e.first_name as employee_first_name, e.last_name as employee_last_name, 
            e.role, p.pet_id, p.pet_name, p.species, p.breed, date(p.birth_date) as birth_date, p.owner,
            c.customer_id, c.first_name as customer_first_name, c.last_name as customer_last_name, c.address, c.phone, c.email
            FROM Appointment a
            JOIN Employee e ON a.provider = e.employee_id
            JOIN Pet p ON a.pet = p.pet_id
            JOIN Customer c ON p.owner = c.customer_id
            WHERE date(a.date) = ?
            ORDER BY a.time
            """;

            try (PreparedStatement stmt = connection.prepareStatement(testQuery)) {
                stmt.setString(1, LocalDate.now().toString());
                ResultSet rs = stmt.executeQuery();
                System.out.println("Query executed successfully!");

                // Count results
                int count = 0;
                while (rs.next()) {
                    count++;
                }
                System.out.println("Found " + count + " appointments for today");
            }

            System.out.println("----- END DEBUG -----\n");
        } catch (SQLException e) {
            System.err.println("Error in debug query: " + e.getMessage());
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

            // Debug check to make sure the role is correctly represented
            System.out.println("Veterinarian Role Class: " + testVeterinarian.getRole().getClass().getName());
            System.out.println("Veterinarian Role toString: " + testVeterinarian.getRole().toString());
            System.out.println("Veterinarian Role name(): " + testVeterinarian.getRole().name());
            System.out.println("Is role VETERINARIAN? " +
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
     * Used to help with testing findAllAppointmentsByDate
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

    /**
     * Test creating an appointment with a veterinarian using the DAO's create method
     */
    @Test
    public void testCreate_withVeterinarian() {
        // Create an appointment with a veterinarian
        LocalDate appointmentDate = LocalDate.now().plusDays(10);
        System.out.println(appointmentDate);
        LocalTime appointmentTime = LocalTime.of(14, 0);
        System.out.println(appointmentTime);

        Appointment appointment = new Appointment(
                null,
                appointmentDate,
                appointmentTime,
                testVeterinarian,
                AppointmentType.CHECKUP,
                testPet,
                testCustomer
        );

        // Debug the DAO's role check
        System.out.println("\n----- DEBUG DAO CREATE METHOD -----");
        System.out.println("Provider Role: " + testVeterinarian.getRole());
        System.out.println("Provider Role Class: " + testVeterinarian.getRole().getClass().getName());
        System.out.println("Provider Role name(): " + testVeterinarian.getRole().name());
        System.out.println("AppointmentType: " + appointment.getAppointmentType());
        System.out.println("AppointmentType name(): " + appointment.getAppointmentType().name());
        System.out.println("Role check: " +
                (!("VETERINARIAN".equals(testVeterinarian.getRole().name())) &&
                        !("VACCINATION".equals(appointment.getAppointmentType().name()))));
        System.out.println("----- END DEBUG -----\n");

        // Try to create the appointment
        try {
            boolean created = appointmentDAO.create(appointment);
            assertTrue(created);
            assertNotNull(appointment.getID());

            // Verify the appointment was created
            Optional<Appointment> fetchedAppointment = appointmentDAO.findByID(appointment.getID());
            assertTrue(fetchedAppointment.isPresent());

            // Clean up
            assertTrue(appointmentDAO.delete(appointment.getID()));
        } catch (Exception e) {
            System.err.println("Failed to create appointment: " + e.getMessage());
            e.printStackTrace();
            fail("Exception during appointment creation: " + e.getMessage());
        }
    }

    /**
     * Test creating an appointment with a vet tech for a vaccination using the DAO's create method
     */
    @Test
    public void testCreate_vetTechVaccination() {
        // Create an appointment with a vet tech for a vaccination
        LocalDate appointmentDate = LocalDate.now().plusDays(11);
        LocalTime appointmentTime = LocalTime.of(15, 0);

        Appointment appointment = new Appointment(
                null,
                appointmentDate,
                appointmentTime,
                testVetTech,
                AppointmentType.VACCINATION,
                testPet,
                testCustomer
        );

        // Debug the DAO's role check
        System.out.println("\n----- DEBUG DAO CREATE METHOD FOR VET TECH -----");
        System.out.println("Provider Role: " + testVetTech.getRole());
        System.out.println("Provider Role name(): " + testVetTech.getRole().name());
        System.out.println("AppointmentType: " + appointment.getAppointmentType());
        System.out.println("AppointmentType name(): " + appointment.getAppointmentType().name());
        System.out.println("Role check: " +
                (!("VETERINARIAN".equals(testVetTech.getRole().name())) &&
                        !("VACCINATION".equals(appointment.getAppointmentType().name()))));
        System.out.println("----- END DEBUG -----\n");

        // Try to create the appointment
        try {
            boolean created = appointmentDAO.create(appointment);
            assertTrue(created);
            assertNotNull(appointment.getID());

            // Verify the appointment was created
            Optional<Appointment> fetchedAppointment = appointmentDAO.findByID(appointment.getID());
            assertTrue(fetchedAppointment.isPresent());

            // Clean up
            assertTrue(appointmentDAO.delete(appointment.getID()));
        } catch (Exception e) {
            System.err.println("Failed to create vet tech vaccination appointment: " + e.getMessage());
            e.printStackTrace();
            fail("Exception during vet tech vaccination appointment creation: " + e.getMessage());
        }
    }

    /**
     * Test the findAllAppointmentsByDate method
     */
    @Test
    public void testFindAllAppointmentsByDate() {
        LocalDate appointmentDate = LocalDate.now().plusDays(14);
        LocalTime appointmentTime1 = LocalTime.of(9, 0);
        LocalTime appointmentTime2 = LocalTime.of(10, 0);

        int appointmentId1 = 0;
        int appointmentId2 = 0;

        try {
            // Create appointments directly using SQL
            appointmentId1 = createAppointmentDirectly(
                    appointmentDate,
                    appointmentTime1,
                    testVeterinarian,
                    AppointmentType.CHECKUP,
                    testPet
            );

            appointmentId2 = createAppointmentDirectly(
                    appointmentDate,
                    appointmentTime2,
                    testVeterinarian,
                    AppointmentType.VACCINATION,
                    testPet
            );

            // Attempt to use findAllAppointmentsByDate
            System.out.println("\nTesting findAllAppointmentsByDate with date: " + appointmentDate);
            List<Appointment> foundAppointments = appointmentDAO.findAllAppointmentsByDate(appointmentDate);

            System.out.println("Found " + foundAppointments.size() + " appointments");

            // Test if our appointments are in the list
            boolean found1 = false;
            boolean found2 = false;

            for (Appointment appointment : foundAppointments) {
                System.out.println("Found appointment ID: " + appointment.getID() +
                        ", Date: " + appointment.getDate() +
                        ", Time: " + appointment.getTime() +
                        ", Type: " + appointment.getAppointmentType());

                if (appointment.getID() == appointmentId1) {
                    found1 = true;
                }
                if (appointment.getID() == appointmentId2) {
                    found2 = true;
                }
            }

            assertTrue(found1, "First test appointment was not found");
            assertTrue(found2, "Second test appointment was not found");

        } catch (Exception e) {
            System.err.println("Error in findAllAppointmentsByDate test: " + e.getMessage());
            e.printStackTrace();
            fail("Exception during findAllAppointmentsByDate test: " + e.getMessage());
        } finally {
            // Clean up
            try {
                if (appointmentId1 > 0) {
                    assertTrue(appointmentDAO.delete(appointmentId1));
                }
                if (appointmentId2 > 0) {
                    assertTrue(appointmentDAO.delete(appointmentId2));
                }
            } catch (Exception e) {
                System.err.println("Error cleaning up test appointments: " + e.getMessage());
            }
        }
    }

    /**
     * Test the findAppointmentsByPetId method
     */
    @Test
    public void testFindAppointmentsByPetId() {
        LocalDate appointmentDate = LocalDate.now().plusDays(21);
        LocalTime appointmentTime1 = LocalTime.of(11, 0);
        LocalTime appointmentTime2 = LocalTime.of(13, 0);

        int appointmentId1 = 0;
        int appointmentId2 = 0;

        try {
            // Create appointments directly using SQL for our test pet
            appointmentId1 = createAppointmentDirectly(
                    appointmentDate,
                    appointmentTime1,
                    testVeterinarian,
                    AppointmentType.CHECKUP,
                    testPet
            );

            appointmentId2 = createAppointmentDirectly(
                    appointmentDate,
                    appointmentTime2,
                    testVeterinarian,
                    AppointmentType.VACCINATION,
                    testPet
            );

            // Attempt to find appointments by pet ID
            System.out.println("\nTesting findAppointmentsByPetId with pet ID: " + testPet.getID());
            Optional<List<Appointment>> foundAppointmentsOptional = appointmentDAO.findAppointmentsByPetId(testPet.getID());

            // Assert that appointments were found
            assertTrue(foundAppointmentsOptional.isPresent(), "Should find appointments for the test pet");
            List<Appointment> foundAppointments = foundAppointmentsOptional.get();

            // Check that we have the correct number of appointments
            assertEquals(2, foundAppointments.size(), "Should find exactly 2 appointments for the test pet");

            // Verify that our test appointments are in the returned list
            boolean found1 = false;
            boolean found2 = false;

            for (Appointment appointment : foundAppointments) {
                System.out.println("Found appointment ID: " + appointment.getID() +
                        ", Date: " + appointment.getDate() +
                        ", Time: " + appointment.getTime() +
                        ", Type: " + appointment.getAppointmentType() +
                        ", Pet ID: " + appointment.getPet().getID());

                if (appointment.getID() == appointmentId1) {
                    found1 = true;
                    assertEquals(appointmentDate, appointment.getDate(), "Dates should match for appointment 1");
                    assertEquals(appointmentTime1, appointment.getTime(), "Times should match for appointment 1");
                    assertEquals(AppointmentType.CHECKUP, appointment.getAppointmentType(), "Types should match for appointment 1");
                }
                if (appointment.getID() == appointmentId2) {
                    found2 = true;
                    assertEquals(appointmentDate, appointment.getDate(), "Dates should match for appointment 2");
                    assertEquals(appointmentTime2, appointment.getTime(), "Times should match for appointment 2");
                    assertEquals(AppointmentType.VACCINATION, appointment.getAppointmentType(), "Types should match for appointment 2");
                }
            }

            assertTrue(found1, "First test appointment was not found");
            assertTrue(found2, "Second test appointment was not found");

            // Test for a pet ID that should have no appointments
            int nonExistentPetId = 99999;
            Optional<List<Appointment>> nonExistentAppointments = appointmentDAO.findAppointmentsByPetId(nonExistentPetId);
            assertTrue(nonExistentAppointments.isEmpty(), "Should not find appointments for non-existent pet ID");

        } catch (Exception e) {
            System.err.println("Error in findAppointmentsByPetId test: " + e.getMessage());
            e.printStackTrace();
            fail("Exception during findAppointmentsByPetId test: " + e.getMessage());
        } finally {
            // Clean up
            try {
                if (appointmentId1 > 0) {
                    assertTrue(appointmentDAO.delete(appointmentId1));
                }
                if (appointmentId2 > 0) {
                    assertTrue(appointmentDAO.delete(appointmentId2));
                }
            } catch (Exception e) {
                System.err.println("Error cleaning up test appointments: " + e.getMessage());
            }
        }
    }

}