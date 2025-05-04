package com.vetportal.test;

import com.vetportal.dto.LookupStatus;
import com.vetportal.dto.ServiceResponse;
import com.vetportal.model.*;
import com.vetportal.service.AppointmentService;
import com.vetportal.service.CustomerService;
import com.vetportal.service.EmployeeService;
import com.vetportal.util.DatabaseInitializer;
import com.vetportal.util.DbManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class AppointmentServiceTest {

    private static Connection connection;
    private static AppointmentService appointmentService;
    private static CustomerService customerService;
    private static EmployeeService employeeService;

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

            // Initialize services
            appointmentService = new AppointmentService(connection);
            customerService = new CustomerService(connection);
            employeeService = new EmployeeService(connection);

            // Create test data
            createTestData();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to initialize test database: " + e.getMessage());
        }
    }

    /**
     * Creates all test data needed for the tests
     */
    private static void createTestData() {
        // Create a test veterinarian
        String vetEmail = "vet." + System.currentTimeMillis() + "@example.com";
        String vetPhone = "555-" + (1000 + new Random().nextInt(9000));
        Employee vet = new Employee(null, "Test", "Veterinarian", "123 Vet St",
                vetPhone, vetEmail, Employee.Position.VETERINARIAN);
        ServiceResponse<Employee> vetResponse = employeeService.createEmployee(vet);
        assertTrue(vetResponse.isSuccess());
        testVeterinarian = vetResponse.getData();

        // Create a test vet tech
        String techEmail = "tech." + System.currentTimeMillis() + "@example.com";
        String techPhone = "555-" + (1000 + new Random().nextInt(9000));
        Employee tech = new Employee(null, "Test", "VetTech", "456 Tech St",
                techPhone, techEmail, Employee.Position.VET_TECH);
        ServiceResponse<Employee> techResponse = employeeService.createEmployee(tech);
        assertTrue(techResponse.isSuccess());
        testVetTech = techResponse.getData();

        // Create a test customer
        String customerEmail = "customer." + System.currentTimeMillis() + "@example.com";
        String customerPhone = "555-" + (1000 + new Random().nextInt(9000));
        Customer customer = new Customer(null, "Test", "Customer", "789 Pet Owner St",
                customerEmail, customerPhone);
        ServiceResponse<Customer> customerResponse = customerService.createCustomer(customer);
        assertTrue(customerResponse.isSuccess());
        testCustomer = customerResponse.getData();

        // Create a test pet
        Pet pet = new Pet(null, "TestPet", "Dog", "Mixed", LocalDate.now().minusYears(2), testCustomer);
        ServiceResponse<Pet> petResponse = customerService.createPet(pet);
        assertTrue(petResponse.isSuccess());
        testPet = petResponse.getData();
    }

    // -------- CREATE APPOINTMENT TESTS --------

    @Test
    public void testCreateAppointment_Success() {
        // Create an appointment with a veterinarian
        LocalDate appointmentDate = LocalDate.now().plusDays(10);
        LocalTime appointmentTime = LocalTime.of(14, 0);

        Appointment appointment = new Appointment(
                null,
                appointmentDate,
                appointmentTime,
                testVeterinarian,
                AppointmentType.CHECKUP,
                testPet,
                testCustomer
        );

        ServiceResponse<Appointment> response = appointmentService.createAppointment(appointment);

        // Verify success
        assertTrue(response.isSuccess());
        assertEquals(LookupStatus.SUCCESS, response.getStatus());
        assertNotNull(response.getData());
        assertNotNull(response.getData().getID());
        assertEquals(appointmentDate, response.getData().getDate());
        assertEquals(appointmentTime, response.getData().getTime());
        assertEquals(testVeterinarian.getID(), response.getData().getProvider().getID());
        assertEquals(AppointmentType.CHECKUP, response.getData().getAppointmentType());

        // Clean up
        ServiceResponse<Boolean> deleteResponse = appointmentService.deleteAppointment(response.getData().getID());
        assertTrue(deleteResponse.isSuccess());
    }

    @Test
    public void testCreateAppointment_VetTechVaccination() {
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

        ServiceResponse<Appointment> response = appointmentService.createAppointment(appointment);

        // Verify success
        assertTrue(response.isSuccess());
        assertEquals(LookupStatus.SUCCESS, response.getStatus());
        assertNotNull(response.getData());
        assertNotNull(response.getData().getID());
        assertEquals(testVetTech.getID(), response.getData().getProvider().getID());
        assertEquals(AppointmentType.VACCINATION, response.getData().getAppointmentType());

        // Clean up
        ServiceResponse<Boolean> deleteResponse = appointmentService.deleteAppointment(response.getData().getID());
        assertTrue(deleteResponse.isSuccess());
    }

    @Test
    public void testCreateAppointment_VetTechNonVaccination() {
        // Try to create an appointment with a vet tech for a non-vaccination
        LocalDate appointmentDate = LocalDate.now().plusDays(12);
        LocalTime appointmentTime = LocalTime.of(16, 0);

        Appointment appointment = new Appointment(
                null,
                appointmentDate,
                appointmentTime,
                testVetTech,
                AppointmentType.CHECKUP, // Non-vaccination appointment type
                testPet,
                testCustomer
        );

        ServiceResponse<Appointment> response = appointmentService.createAppointment(appointment);

        // Verify failure with conflict
        assertFalse(response.isSuccess());
        assertEquals(LookupStatus.CONFLICT, response.getStatus());
        assertNull(response.getData());
        assertTrue(response.getMessage().contains("Only veterinarians"));
    }

    @Test
    public void testCreateAppointment_ProviderNotFound() {
        // Try to create an appointment with a non-existent provider
        LocalDate appointmentDate = LocalDate.now().plusDays(13);
        LocalTime appointmentTime = LocalTime.of(10, 0);

        Employee nonExistentProvider = new Employee(9999, "Non", "Existent", "Address",
                "555-9999", "non.existent@example.com",
                Employee.Position.VETERINARIAN);

        Appointment appointment = new Appointment(
                null,
                appointmentDate,
                appointmentTime,
                nonExistentProvider,
                AppointmentType.CHECKUP,
                testPet,
                testCustomer
        );

        ServiceResponse<Appointment> response = appointmentService.createAppointment(appointment);

        // Verify failure with not found
        assertFalse(response.isSuccess());
        assertEquals(LookupStatus.NOT_FOUND, response.getStatus());
        assertNull(response.getData());
        assertTrue(response.getMessage().contains("Provider with ID"));
    }

    @Test
    public void testCreateAppointment_DuplicateTime() {
        // Create first appointment
        LocalDate appointmentDate = LocalDate.now().plusDays(14);
        LocalTime appointmentTime = LocalTime.of(11, 0);

        Appointment appointment1 = new Appointment(
                null,
                appointmentDate,
                appointmentTime,
                testVeterinarian,
                AppointmentType.CHECKUP,
                testPet,
                testCustomer
        );

        ServiceResponse<Appointment> response1 = appointmentService.createAppointment(appointment1);
        assertTrue(response1.isSuccess());

        try {
            // Try to create a second appointment at the same time with the same provider
            Appointment appointment2 = new Appointment(
                    null,
                    appointmentDate,
                    appointmentTime,
                    testVeterinarian, // Same provider
                    AppointmentType.VACCINATION,
                    testPet,
                    testCustomer
            );

            ServiceResponse<Appointment> response2 = appointmentService.createAppointment(appointment2);

            // Verify failure with conflict
            assertFalse(response2.isSuccess());
            assertEquals(LookupStatus.CONFLICT, response2.getStatus());
            assertNull(response2.getData());
            assertTrue(response2.getMessage().contains("appointment at this time"));
        } finally {
            // Clean up
            ServiceResponse<Boolean> deleteResponse = appointmentService.deleteAppointment(response1.getData().getID());
            assertTrue(deleteResponse.isSuccess());
        }
    }

    // -------- UPDATE APPOINTMENT TESTS --------

    @Test
    public void testUpdateAppointment_Success() {
        // Create an appointment to update
        LocalDate appointmentDate = LocalDate.now().plusDays(15);
        LocalTime appointmentTime = LocalTime.of(13, 0);

        Appointment appointment = new Appointment(
                null,
                appointmentDate,
                appointmentTime,
                testVeterinarian,
                AppointmentType.CHECKUP,
                testPet,
                testCustomer
        );

        ServiceResponse<Appointment> createResponse = appointmentService.createAppointment(appointment);
        assertTrue(createResponse.isSuccess());

        try {
            // Update the appointment
            Appointment appointmentToUpdate = createResponse.getData();
            LocalDate newDate = appointmentDate.plusDays(1);
            LocalTime newTime = LocalTime.of(14, 30);

            appointmentToUpdate.setDate(newDate);
            appointmentToUpdate.setTime(newTime);
            appointmentToUpdate.setAppointmentType(AppointmentType.DENTAL);

            ServiceResponse<Appointment> updateResponse = appointmentService.updateAppointment(appointmentToUpdate);

            // Verify success
            assertTrue(updateResponse.isSuccess());
            assertEquals(LookupStatus.SUCCESS, updateResponse.getStatus());
            assertNotNull(updateResponse.getData());
            assertEquals(newDate, updateResponse.getData().getDate());
            assertEquals(newTime, updateResponse.getData().getTime());
            assertEquals(AppointmentType.DENTAL, updateResponse.getData().getAppointmentType());

            // Verify by fetching the appointment
            ServiceResponse<Appointment> fetchResponse = appointmentService.findAppointmentById(appointmentToUpdate.getID());
            assertTrue(fetchResponse.isSuccess());
            assertEquals(newDate, fetchResponse.getData().getDate());
            assertEquals(newTime, fetchResponse.getData().getTime());
            assertEquals(AppointmentType.DENTAL, fetchResponse.getData().getAppointmentType());
        } finally {
            // Clean up
            ServiceResponse<Boolean> deleteResponse = appointmentService.deleteAppointment(createResponse.getData().getID());
            assertTrue(deleteResponse.isSuccess());
        }
    }

    @Test
    public void testUpdateAppointment_NonExistent() {
        // Try to update a non-existent appointment
        LocalDate appointmentDate = LocalDate.now().plusDays(16);
        LocalTime appointmentTime = LocalTime.of(15, 0);

        Appointment nonExistentAppointment = new Appointment(
                9999, // Non-existent ID
                appointmentDate,
                appointmentTime,
                testVeterinarian,
                AppointmentType.CHECKUP,
                testPet,
                testCustomer
        );

        ServiceResponse<Appointment> response = appointmentService.updateAppointment(nonExistentAppointment);

        // Verify failure with not found
        assertFalse(response.isSuccess());
        assertEquals(LookupStatus.NOT_FOUND, response.getStatus());
        assertNull(response.getData());
        assertTrue(response.getMessage().contains("not found"));
    }

    @Test
    public void testUpdateAppointment_VetTechToNonVaccination() {
        // Create a valid appointment with a vet tech for a vaccination
        LocalDate appointmentDate = LocalDate.now().plusDays(17);
        LocalTime appointmentTime = LocalTime.of(16, 0);

        Appointment appointment = new Appointment(
                null,
                appointmentDate,
                appointmentTime,
                testVetTech,
                AppointmentType.VACCINATION,
                testPet,
                testCustomer
        );

        ServiceResponse<Appointment> createResponse = appointmentService.createAppointment(appointment);
        assertTrue(createResponse.isSuccess());

        try {
            // Try to update to a non-vaccination appointment type
            Appointment appointmentToUpdate = createResponse.getData();
            appointmentToUpdate.setAppointmentType(AppointmentType.CHECKUP);

            ServiceResponse<Appointment> updateResponse = appointmentService.updateAppointment(appointmentToUpdate);

            // Verify failure with conflict
            assertFalse(updateResponse.isSuccess());
            assertEquals(LookupStatus.CONFLICT, updateResponse.getStatus());
            assertNull(updateResponse.getData());
            assertTrue(updateResponse.getMessage().contains("Only veterinarians"));
        } finally {
            // Clean up
            ServiceResponse<Boolean> deleteResponse = appointmentService.deleteAppointment(createResponse.getData().getID());
            assertTrue(deleteResponse.isSuccess());
        }
    }

    // -------- DELETE APPOINTMENT TESTS --------

    @Test
    public void testDeleteAppointment_Success() {
        // Create an appointment to delete
        LocalDate appointmentDate = LocalDate.now().plusDays(18);
        LocalTime appointmentTime = LocalTime.of(9, 0);

        Appointment appointment = new Appointment(
                null,
                appointmentDate,
                appointmentTime,
                testVeterinarian,
                AppointmentType.CHECKUP,
                testPet,
                testCustomer
        );

        ServiceResponse<Appointment> createResponse = appointmentService.createAppointment(appointment);
        assertTrue(createResponse.isSuccess());

        // Delete the appointment
        int appointmentId = createResponse.getData().getID();
        ServiceResponse<Boolean> deleteResponse = appointmentService.deleteAppointment(appointmentId);

        // Verify success
        assertTrue(deleteResponse.isSuccess());
        assertEquals(LookupStatus.SUCCESS, deleteResponse.getStatus());
        assertTrue(deleteResponse.getData());

        // Verify it's deleted by trying to fetch it
        ServiceResponse<Appointment> fetchResponse = appointmentService.findAppointmentById(appointmentId);
        assertFalse(fetchResponse.isSuccess());
        assertEquals(LookupStatus.NOT_FOUND, fetchResponse.getStatus());
    }

    @Test
    public void testDeleteAppointment_NonExistent() {
        // Try to delete a non-existent appointment
        ServiceResponse<Boolean> deleteResponse = appointmentService.deleteAppointment(9999);

        // Verify failure with not found
        assertFalse(deleteResponse.isSuccess());
        assertEquals(LookupStatus.NOT_FOUND, deleteResponse.getStatus());
        assertFalse(deleteResponse.getData() != null && deleteResponse.getData());
        assertTrue(deleteResponse.getMessage().contains("not found"));
    }

    // -------- FIND APPOINTMENT TESTS --------

    @Test
    public void testFindAppointmentById_Success() {
        // Create an appointment to find
        LocalDate appointmentDate = LocalDate.now().plusDays(19);
        LocalTime appointmentTime = LocalTime.of(10, 0);

        Appointment appointment = new Appointment(
                null,
                appointmentDate,
                appointmentTime,
                testVeterinarian,
                AppointmentType.CHECKUP,
                testPet,
                testCustomer
        );

        ServiceResponse<Appointment> createResponse = appointmentService.createAppointment(appointment);
        assertTrue(createResponse.isSuccess());

        try {
            // Find the appointment by ID
            int appointmentId = createResponse.getData().getID();
            ServiceResponse<Appointment> findResponse = appointmentService.findAppointmentById(appointmentId);

            // Verify success
            assertTrue(findResponse.isSuccess());
            assertEquals(LookupStatus.SUCCESS, findResponse.getStatus());
            assertNotNull(findResponse.getData());
            assertEquals(appointmentId, findResponse.getData().getID());
            assertEquals(appointmentDate, findResponse.getData().getDate());
            assertEquals(appointmentTime, findResponse.getData().getTime());
            assertEquals(testVeterinarian.getID(), findResponse.getData().getProvider().getID());
            assertEquals(AppointmentType.CHECKUP, findResponse.getData().getAppointmentType());
        } finally {
            // Clean up
            ServiceResponse<Boolean> deleteResponse = appointmentService.deleteAppointment(createResponse.getData().getID());
            assertTrue(deleteResponse.isSuccess());
        }
    }

    @Test
    public void testFindAppointmentById_NotFound() {
        // Try to find a non-existent appointment
        ServiceResponse<Appointment> findResponse = appointmentService.findAppointmentById(9999);

        // Verify not found
        assertFalse(findResponse.isSuccess());
        assertEquals(LookupStatus.NOT_FOUND, findResponse.getStatus());
        assertNull(findResponse.getData());
        assertTrue(findResponse.getMessage().contains("not found"));
    }

    // -------- FIND APPOINTMENTS BY DATE TESTS --------

    @Test
    public void testFindAppointmentsByDate_Success() {
        // Create appointments for a specific date
        LocalDate appointmentDate = LocalDate.now().plusDays(20);
        LocalTime appointmentTime1 = LocalTime.of(11, 0);
        LocalTime appointmentTime2 = LocalTime.of(13, 0);

        Appointment appointment1 = new Appointment(
                null,
                appointmentDate,
                appointmentTime1,
                testVeterinarian,
                AppointmentType.CHECKUP,
                testPet,
                testCustomer
        );

        Appointment appointment2 = new Appointment(
                null,
                appointmentDate,
                appointmentTime2,
                testVeterinarian,
                AppointmentType.VACCINATION,
                testPet,
                testCustomer
        );

        ServiceResponse<Appointment> createResponse1 = appointmentService.createAppointment(appointment1);
        ServiceResponse<Appointment> createResponse2 = appointmentService.createAppointment(appointment2);

        assertTrue(createResponse1.isSuccess());
        assertTrue(createResponse2.isSuccess());

        try {
            // Find appointments by date using LocalDate
            ServiceResponse<List<Appointment>> findResponse = appointmentService.findAppointmentsByDate(appointmentDate);

            // Verify success
            assertTrue(findResponse.isSuccess());
            assertEquals(LookupStatus.SUCCESS, findResponse.getStatus());
            assertNotNull(findResponse.getData());
            assertTrue(findResponse.getData().size() >= 2); // There might be other appointments on this date

            // Check that our test appointments are in the list
            List<Appointment> appointments = findResponse.getData();
            boolean found1 = false;
            boolean found2 = false;

            for (Appointment appt : appointments) {
                if (appt.getID().equals(createResponse1.getData().getID())) {
                    found1 = true;
                    assertEquals(appointmentTime1, appt.getTime());
                    assertEquals(AppointmentType.CHECKUP, appt.getAppointmentType());
                }
                if (appt.getID().equals(createResponse2.getData().getID())) {
                    found2 = true;
                    assertEquals(appointmentTime2, appt.getTime());
                    assertEquals(AppointmentType.VACCINATION, appt.getAppointmentType());
                }
            }

            assertTrue(found1, "First test appointment was not found");
            assertTrue(found2, "Second test appointment was not found");

            // Test the string version of the method
            ServiceResponse<List<Appointment>> findResponseStr = appointmentService.findAppointmentsByDate(appointmentDate.toString());
            assertTrue(findResponseStr.isSuccess());
            assertEquals(findResponse.getData().size(), findResponseStr.getData().size());
        } finally {
            // Clean up
            ServiceResponse<Boolean> deleteResponse1 = appointmentService.deleteAppointment(createResponse1.getData().getID());
            ServiceResponse<Boolean> deleteResponse2 = appointmentService.deleteAppointment(createResponse2.getData().getID());
            assertTrue(deleteResponse1.isSuccess());
            assertTrue(deleteResponse2.isSuccess());
        }
    }

    @Test
    public void testFindAppointmentsByDate_StringFormat() {
        // Test with a valid date string
        String validDateString = LocalDate.now().plusDays(21).toString();
        ServiceResponse<List<Appointment>> validResponse = appointmentService.findAppointmentsByDate(validDateString);

        // This might succeed or return not found depending on if there are appointments on this date
        if (validResponse.isSuccess()) {
            assertEquals(LookupStatus.SUCCESS, validResponse.getStatus());
            assertNotNull(validResponse.getData());
        } else {
            assertEquals(LookupStatus.NOT_FOUND, validResponse.getStatus());
            assertNull(validResponse.getData());
        }

        // Test with an invalid date string
        String invalidDateString = "not-a-date";
        ServiceResponse<List<Appointment>> invalidResponse = appointmentService.findAppointmentsByDate(invalidDateString);

        // Verify conflict for invalid format
        assertFalse(invalidResponse.isSuccess());
        assertEquals(LookupStatus.CONFLICT, invalidResponse.getStatus());
        assertNull(invalidResponse.getData());
        assertTrue(invalidResponse.getMessage().contains("Invalid date format"));
    }

    @Test
    public void testFindAppointmentsByDate_NoAppointments() {
        // Find appointments for a date far in the future
        LocalDate futureDate = LocalDate.now().plusYears(10);
        ServiceResponse<List<Appointment>> findResponse = appointmentService.findAppointmentsByDate(futureDate);

        // Verify not found
        assertFalse(findResponse.isSuccess());
        assertEquals(LookupStatus.NOT_FOUND, findResponse.getStatus());
        assertNull(findResponse.getData());
        assertTrue(findResponse.getMessage().contains("No appointments found"));
    }

    // -------- FIND APPOINTMENTS BY PET ID TESTS --------

    @Test
    public void testFindAppointmentsByPetId_Success() {
        // Create appointments for a specific pet
        LocalDate appointmentDate1 = LocalDate.now().plusDays(22);
        LocalDate appointmentDate2 = LocalDate.now().plusDays(23);
        LocalTime appointmentTime = LocalTime.of(14, 0);

        Appointment appointment1 = new Appointment(
                null,
                appointmentDate1,
                appointmentTime,
                testVeterinarian,
                AppointmentType.CHECKUP,
                testPet,
                testCustomer
        );

        Appointment appointment2 = new Appointment(
                null,
                appointmentDate2,
                appointmentTime,
                testVeterinarian,
                AppointmentType.DENTAL,
                testPet,
                testCustomer
        );

        ServiceResponse<Appointment> createResponse1 = appointmentService.createAppointment(appointment1);
        ServiceResponse<Appointment> createResponse2 = appointmentService.createAppointment(appointment2);

        assertTrue(createResponse1.isSuccess());
        assertTrue(createResponse2.isSuccess());

        try {
            // Find appointments by pet ID
            ServiceResponse<List<Appointment>> findResponse = appointmentService.findAppointmentsByPetId(testPet.getID());

            // Verify success
            assertTrue(findResponse.isSuccess());
            assertEquals(LookupStatus.SUCCESS, findResponse.getStatus());
            assertNotNull(findResponse.getData());
            assertTrue(findResponse.getData().size() >= 2); // There might be other appointments for this pet

            // Check that our test appointments are in the list
            List<Appointment> appointments = findResponse.getData();
            boolean found1 = false;
            boolean found2 = false;

            for (Appointment appt : appointments) {
                if (appt.getID().equals(createResponse1.getData().getID())) {
                    found1 = true;
                    assertEquals(appointmentDate1, appt.getDate());
                    assertEquals(AppointmentType.CHECKUP, appt.getAppointmentType());
                }
                if (appt.getID().equals(createResponse2.getData().getID())) {
                    found2 = true;
                    assertEquals(appointmentDate2, appt.getDate());
                    assertEquals(AppointmentType.DENTAL, appt.getAppointmentType());
                }
            }

            assertTrue(found1, "First test appointment was not found");
            assertTrue(found2, "Second test appointment was not found");
        } finally {
            // Clean up
            ServiceResponse<Boolean> deleteResponse1 = appointmentService.deleteAppointment(createResponse1.getData().getID());
            ServiceResponse<Boolean> deleteResponse2 = appointmentService.deleteAppointment(createResponse2.getData().getID());
            assertTrue(deleteResponse1.isSuccess());
            assertTrue(deleteResponse2.isSuccess());
        }
    }

    @Test
    public void testFindAppointmentsByPetId_NonExistentPet() {
        // Try to find appointments for a non-existent pet
        ServiceResponse<List<Appointment>> findResponse = appointmentService.findAppointmentsByPetId(9999);

        // Verify not found
        assertFalse(findResponse.isSuccess());
        assertEquals(LookupStatus.NOT_FOUND, findResponse.getStatus());
        assertNull(findResponse.getData());
        assertTrue(findResponse.getMessage().contains("Pet with ID"));
    }

    @Test
    public void testFindAppointmentsByPetId_NoAppointments() {
        // Create a pet with no appointments
        Pet petWithNoAppointments = new Pet(null, "LonelyPet", "Cat", "Tabby",
                LocalDate.now().minusYears(1), testCustomer);
        ServiceResponse<Pet> petResponse = customerService.createPet(petWithNoAppointments);
        assertTrue(petResponse.isSuccess());

        try {
            // Find appointments for this pet
            ServiceResponse<List<Appointment>> findResponse =
                    appointmentService.findAppointmentsByPetId(petWithNoAppointments.getID());

            // Verify not found
            assertFalse(findResponse.isSuccess());
            assertEquals(LookupStatus.NOT_FOUND, findResponse.getStatus());
            assertNull(findResponse.getData());
            assertTrue(findResponse.getMessage().contains("No appointments found"));
        } finally {
            // Clean up
            customerService.deletePet(petWithNoAppointments.getID());
        }
    }

    // -------- FIND APPOINTMENTS BY PROVIDER ID TESTS --------

    @Test
    public void testFindAppointmentsByProviderId_Success() {
        // Create appointments for a specific provider
        LocalDate appointmentDate1 = LocalDate.now().plusDays(24);
        LocalDate appointmentDate2 = LocalDate.now().plusDays(25);
        LocalTime appointmentTime = LocalTime.of(15, 0);

        Appointment appointment1 = new Appointment(
                null,
                appointmentDate1,
                appointmentTime,
                testVeterinarian,
                AppointmentType.CHECKUP,
                testPet,
                testCustomer
        );

        Appointment appointment2 = new Appointment(
                null,
                appointmentDate2,
                appointmentTime,
                testVeterinarian,
                AppointmentType.SURGERY,
                testPet,
                testCustomer
        );

        ServiceResponse<Appointment> createResponse1 = appointmentService.createAppointment(appointment1);
        ServiceResponse<Appointment> createResponse2 = appointmentService.createAppointment(appointment2);

        assertTrue(createResponse1.isSuccess());
        assertTrue(createResponse2.isSuccess());

        try {
            // Find appointments by provider ID
            ServiceResponse<List<Appointment>> findResponse =
                    appointmentService.findAppointmentsByProviderId(testVeterinarian.getID());

            // Verify success
            assertTrue(findResponse.isSuccess());
            assertEquals(LookupStatus.SUCCESS, findResponse.getStatus());
            assertNotNull(findResponse.getData());
            assertTrue(findResponse.getData().size() >= 2); // There might be other appointments for this provider

            // Check that our test appointments are in the list
            List<Appointment> appointments = findResponse.getData();
            boolean found1 = false;
            boolean found2 = false;

            for (Appointment appt : appointments) {
                if (appt.getID().equals(createResponse1.getData().getID())) {
                    found1 = true;
                    assertEquals(appointmentDate1, appt.getDate());
                    assertEquals(AppointmentType.CHECKUP, appt.getAppointmentType());
                }
                if (appt.getID().equals(createResponse2.getData().getID())) {
                    found2 = true;
                    assertEquals(appointmentDate2, appt.getDate());
                    assertEquals(AppointmentType.SURGERY, appt.getAppointmentType());
                }
            }

            assertTrue(found1, "First test appointment was not found");
            assertTrue(found2, "Second test appointment was not found");
        } finally {
            // Clean up
            ServiceResponse<Boolean> deleteResponse1 = appointmentService.deleteAppointment(createResponse1.getData().getID());
            ServiceResponse<Boolean> deleteResponse2 = appointmentService.deleteAppointment(createResponse2.getData().getID());
            assertTrue(deleteResponse1.isSuccess());
            assertTrue(deleteResponse2.isSuccess());
        }
    }

    @Test
    public void testFindAppointmentsByProviderId_NonExistentProvider() {
        // Try to find appointments for a non-existent provider
        ServiceResponse<List<Appointment>> findResponse = appointmentService.findAppointmentsByProviderId(9999);

        // Verify not found
        assertFalse(findResponse.isSuccess());
        assertEquals(LookupStatus.NOT_FOUND, findResponse.getStatus());
        assertNull(findResponse.getData());
        assertTrue(findResponse.getMessage().contains("Provider with ID"));
    }

    // -------- FIND APPOINTMENTS BY ATTRIBUTES TESTS --------

    @Test
    public void testFindAppointmentsByAttributes_Success() {
        // Create an appointment with specific attributes
        LocalDate appointmentDate = LocalDate.now().plusDays(26);
        LocalTime appointmentTime = LocalTime.of(16, 0);

        Appointment appointment = new Appointment(
                null,
                appointmentDate,
                appointmentTime,
                testVeterinarian,
                AppointmentType.EMERGENCY,
                testPet,
                testCustomer
        );

        ServiceResponse<Appointment> createResponse = appointmentService.createAppointment(appointment);
        assertTrue(createResponse.isSuccess());

        try {
            // Find appointments by appointment type
            Map<String, String> attributes = Map.of("appointmentType", AppointmentType.EMERGENCY.name());
            ServiceResponse<List<Appointment>> findResponse = appointmentService.findAppointmentsByAttributes(attributes);

            // Verify success
            assertTrue(findResponse.isSuccess());
            assertEquals(LookupStatus.SUCCESS, findResponse.getStatus());
            assertNotNull(findResponse.getData());

            // Check that our test appointment is in the list
            List<Appointment> appointments = findResponse.getData();
            boolean found = false;

            for (Appointment appt : appointments) {
                if (appt.getID().equals(createResponse.getData().getID())) {
                    found = true;
                    assertEquals(appointmentDate, appt.getDate());
                    assertEquals(appointmentTime, appt.getTime());
                    assertEquals(AppointmentType.EMERGENCY, appt.getAppointmentType());
                }
            }

            assertTrue(found, "Test appointment was not found");
        } finally {
            // Clean up
            ServiceResponse<Boolean> deleteResponse = appointmentService.deleteAppointment(createResponse.getData().getID());
            assertTrue(deleteResponse.isSuccess());
        }
    }

    @Test
    public void testFindAppointmentsByAttributes_NotFound() {
        // Try to find appointments by non-existent attributes
        Map<String, String> attributes = Map.of("appointmentType", "NONEXISTENT_TYPE");
        ServiceResponse<List<Appointment>> findResponse = appointmentService.findAppointmentsByAttributes(attributes);

        // Verify not found
        assertFalse(findResponse.isSuccess());
        assertEquals(LookupStatus.NOT_FOUND, findResponse.getStatus());
        assertNull(findResponse.getData());
        assertTrue(findResponse.getMessage().contains("No appointments found"));
    }

    // -------- GET ALL APPOINTMENTS TEST --------

    @Test
    public void testGetAllAppointments() {
        // Create some test appointments
        LocalDate appointmentDate1 = LocalDate.now().plusDays(27);
        LocalDate appointmentDate2 = LocalDate.now().plusDays(28);
        LocalTime appointmentTime = LocalTime.of(17, 0);

        Appointment appointment1 = new Appointment(
                null,
                appointmentDate1,
                appointmentTime,
                testVeterinarian,
                AppointmentType.CHECKUP,
                testPet,
                testCustomer
        );

        Appointment appointment2 = new Appointment(
                null,
                appointmentDate2,
                appointmentTime,
                testVeterinarian,
                AppointmentType.VACCINATION,
                testPet,
                testCustomer
        );

        ServiceResponse<Appointment> createResponse1 = appointmentService.createAppointment(appointment1);
        ServiceResponse<Appointment> createResponse2 = appointmentService.createAppointment(appointment2);

        assertTrue(createResponse1.isSuccess());
        assertTrue(createResponse2.isSuccess());

        try {
            // Get all appointments
            ServiceResponse<List<Appointment>> allResponse = appointmentService.getAllAppointments();

            // Verify success
            assertTrue(allResponse.isSuccess());
            assertEquals(LookupStatus.SUCCESS, allResponse.getStatus());
            assertNotNull(allResponse.getData());

            // Check that our test appointments are in the list
            List<Appointment> allAppointments = allResponse.getData();
            boolean found1 = allAppointments.stream()
                    .anyMatch(a -> a.getID().equals(createResponse1.getData().getID()));
            boolean found2 = allAppointments.stream()
                    .anyMatch(a -> a.getID().equals(createResponse2.getData().getID()));

            assertTrue(found1, "First test appointment was not found");
            assertTrue(found2, "Second test appointment was not found");
        } finally {
            // Clean up
            ServiceResponse<Boolean> deleteResponse1 = appointmentService.deleteAppointment(createResponse1.getData().getID());
            ServiceResponse<Boolean> deleteResponse2 = appointmentService.deleteAppointment(createResponse2.getData().getID());
            assertTrue(deleteResponse1.isSuccess());
            assertTrue(deleteResponse2.isSuccess());
        }
    }

    // -------- PROVIDER SLOT AVAILABILITY TEST --------

    @Test
    public void testIsProviderSlotTaken() {
        // Create an appointment
        LocalDate appointmentDate = LocalDate.now().plusDays(29);
        LocalTime appointmentTime = LocalTime.of(9, 30);

        Appointment appointment = new Appointment(
                null,
                appointmentDate,
                appointmentTime,
                testVeterinarian,
                AppointmentType.CHECKUP,
                testPet,
                testCustomer
        );

        ServiceResponse<Appointment> createResponse = appointmentService.createAppointment(appointment);
        assertTrue(createResponse.isSuccess());

        try {
            // Check if the slot is taken (should be)
            boolean slotTaken = appointmentService.isProviderSlotTaken(
                    testVeterinarian.getID(),
                    appointmentDate,
                    appointmentTime,
                    null // Don't exclude any appointment
            );
            assertTrue(slotTaken, "Provider slot should be taken");

            // Check with excluding the appointment itself (should not be taken)
            boolean slotTakenWithExclusion = appointmentService.isProviderSlotTaken(
                    testVeterinarian.getID(),
                    appointmentDate,
                    appointmentTime,
                    createResponse.getData().getID() // Exclude this appointment
            );
            assertFalse(slotTakenWithExclusion, "Provider slot should not be taken when excluding the appointment");

            // Check a different slot (should not be taken)
            boolean differentSlotTaken = appointmentService.isProviderSlotTaken(
                    testVeterinarian.getID(),
                    appointmentDate,
                    appointmentTime.plusHours(1), // Different time
                    null
            );
            assertFalse(differentSlotTaken, "Different slot should not be taken");
        } finally {
            // Clean up
            ServiceResponse<Boolean> deleteResponse = appointmentService.deleteAppointment(createResponse.getData().getID());
            assertTrue(deleteResponse.isSuccess());
        }
    }
}