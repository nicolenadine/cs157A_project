package com.vetportal.test;

import com.vetportal.dao.EmployeeDAO;
import com.vetportal.exception.DataAccessException;
import com.vetportal.model.Employee;
import com.vetportal.util.DatabaseInitializer;
import com.vetportal.util.DbManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class EmployeeDAOTest {

    private static Connection connection;
    private static EmployeeDAO employeeDAO;

    // Ensure clean reset of DB for tests to avoid conflicts
    @BeforeAll
    public static void setup() {
        try {
            connection = DbManager.getConnection();

            // Run schema and seed using the same connection
            DatabaseInitializer.initializeOnExistingConnection(connection, "database/schema.sql", "database/seed.sql");

            // Double check FK
            DbManager.ensureForeignKeysEnabled();

            employeeDAO = new EmployeeDAO(connection);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to initialize test database: " + e.getMessage());
        }
    }

    // ---------- TESTS FOR CREATING EMPLOYEES ----------
    @Test
    public void testCreateEmployee_success() {
        // Create an employee with all required fields - use a unique email and phone
        String uniqueEmail = "john.doe." + System.currentTimeMillis() + "@example.com";
        String uniquePhone = "555-" + (1000 + new Random().nextInt(9000)); // Random 4-digit number

        Employee newEmployee = new Employee(null, "John", "Doe", "123 Test St", uniqueEmail, uniquePhone, Employee.Position.VETERINARIAN);

        Optional<Employee> optionalEmployee = employeeDAO.createEmployee(newEmployee);

        assertTrue(optionalEmployee.isPresent());
        Employee createdEmployee = optionalEmployee.get();
        assertNotNull(createdEmployee.getID());
        assertEquals("John", createdEmployee.getFirstName());
        assertEquals("Doe", createdEmployee.getLastName());
        assertEquals("123 Test St", createdEmployee.getAddress());
        assertEquals(uniqueEmail, createdEmployee.getEmail());
        assertEquals(uniquePhone, createdEmployee.getPhone());
        assertEquals(Employee.Position.VETERINARIAN, createdEmployee.getRole());

        // Clean up
        assertTrue(employeeDAO.delete(createdEmployee.getID()));
    }

    @Test
    public void testCreateEmployee_duplicateEmail() {
        // Create a unique email for this test
        String uniqueEmail = "duplicate." + System.currentTimeMillis() + "@example.com";
        String uniquePhone1 = "555-" + (1000 + new Random().nextInt(9000));
        String uniquePhone2 = "555-" + (1000 + new Random().nextInt(9000));

        // First, create an employee
        Employee firstEmployee = new Employee(null, "First", "Employee", "123 First St", uniqueEmail, uniquePhone1, Employee.Position.RECEPTIONIST);
        Optional<Employee> firstResponse = employeeDAO.createEmployee(firstEmployee);
        assertTrue(firstResponse.isPresent());

        // Now try to create another with the same email
        Employee duplicateEmployee = new Employee(null, "Duplicate", "User", "456 Test Ave", uniqueEmail, uniquePhone2, Employee.Position.VET_TECH);

        assertThrows(DataAccessException.class, () -> {
            employeeDAO.createEmployee(duplicateEmployee);
        });

        // Clean up
        assertTrue(employeeDAO.delete(firstResponse.get().getID()));
    }

    @Test
    public void testCreateEmployee_duplicatePhone() {
        // Create a unique phone for this test
        String uniquePhone = "555-" + (1000 + new Random().nextInt(9000));
        String uniqueEmail1 = "first." + System.currentTimeMillis() + "@example.com";
        String uniqueEmail2 = "second." + System.currentTimeMillis() + "@example.com";

        // First, create an employee
        Employee firstEmployee = new Employee(null, "First", "Employee", "123 First St", uniqueEmail1, uniquePhone, Employee.Position.RECEPTIONIST);
        Optional<Employee> firstResponse = employeeDAO.createEmployee(firstEmployee);
        assertTrue(firstResponse.isPresent());

        // Now try to create another with the same phone
        Employee duplicateEmployee = new Employee(null, "Duplicate", "User", "456 Test Ave", uniqueEmail2, uniquePhone, Employee.Position.VET_TECH);

        assertThrows(DataAccessException.class, () -> {
            employeeDAO.createEmployee(duplicateEmployee);
        });

        // Clean up
        assertTrue(employeeDAO.delete(firstResponse.get().getID()));
    }

    // ---------- TESTS FOR FINDING EMPLOYEES ----------
    @Test
    public void testFindByID_success() {
        // First, create an employee
        String uniqueEmail = "find." + System.currentTimeMillis() + "@example.com";
        String uniquePhone = "555-" + (1000 + new Random().nextInt(9000));

        Employee newEmployee = new Employee(null, "Find", "Me", "123 Search St", uniqueEmail, uniquePhone, Employee.Position.VET_TECH);
        Optional<Employee> createResponse = employeeDAO.createEmployee(newEmployee);
        assertTrue(createResponse.isPresent());

        // Now find the employee by ID
        Optional<Employee> foundEmployee = employeeDAO.findByID(createResponse.get().getID());

        assertTrue(foundEmployee.isPresent());
        assertEquals(createResponse.get().getID(), foundEmployee.get().getID());
        assertEquals("Find", foundEmployee.get().getFirstName());
        assertEquals("Me", foundEmployee.get().getLastName());

        // Clean up
        assertTrue(employeeDAO.delete(createResponse.get().getID()));
    }

    @Test
    public void testFindByID_notFound() {
        Optional<Employee> foundEmployee = employeeDAO.findByID(9999); // Assuming this ID doesn't exist
        assertFalse(foundEmployee.isPresent());
    }

    @Test
    public void testFindByAttributes_email() {
        // Create an employee with a unique email
        String uniqueEmail = "attribute." + System.currentTimeMillis() + "@example.com";
        String uniquePhone = "555-" + (1000 + new Random().nextInt(9000));

        Employee newEmployee = new Employee(null, "Attribute", "Search", "123 Attr St", uniqueEmail, uniquePhone, Employee.Position.VETERINARIAN);
        Optional<Employee> createResponse = employeeDAO.createEmployee(newEmployee);
        assertTrue(createResponse.isPresent());

        // Find by email
        Map<String, String> attributes = Map.of("email", uniqueEmail);
        Optional<Employee> foundEmployee = employeeDAO.findByAttributes(attributes);

        assertTrue(foundEmployee.isPresent());
        assertEquals(uniqueEmail, foundEmployee.get().getEmail());
        assertEquals("Attribute", foundEmployee.get().getFirstName());

        // Clean up
        assertTrue(employeeDAO.delete(createResponse.get().getID()));
    }

    @Test
    public void testFindByAttributes_phone() {
        // Create an employee with a unique phone
        String uniqueEmail = "phone." + System.currentTimeMillis() + "@example.com";
        String uniquePhone = "555-" + (1000 + new Random().nextInt(9000));

        Employee newEmployee = new Employee(null, "Phone", "Search", "123 Phone St", uniqueEmail, uniquePhone, Employee.Position.RECEPTIONIST);
        Optional<Employee> createResponse = employeeDAO.createEmployee(newEmployee);
        assertTrue(createResponse.isPresent());

        // Find by phone
        Map<String, String> attributes = Map.of("phone", uniquePhone);
        Optional<Employee> foundEmployee = employeeDAO.findByAttributes(attributes);

        assertTrue(foundEmployee.isPresent());
        assertEquals(uniquePhone, foundEmployee.get().getPhone());
        assertEquals("Phone", foundEmployee.get().getFirstName());

        // Clean up
        assertTrue(employeeDAO.delete(createResponse.get().getID()));
    }

    @Test
    public void testFindByAttributes_multipleAttributes() {
        // Create an employee
        String uniqueEmail = "multi." + System.currentTimeMillis() + "@example.com";
        String uniquePhone = "555-" + (1000 + new Random().nextInt(9000));

        Employee newEmployee = new Employee(null, "Multi", "Attribute", "123 Multi St", uniqueEmail, uniquePhone, Employee.Position.VETERINARIAN);
        Optional<Employee> createResponse = employeeDAO.createEmployee(newEmployee);
        assertTrue(createResponse.isPresent());

        // Find by first name and last name
        Map<String, String> attributes = Map.of(
                "firstName", "Multi",
                "lastName", "Attribute"
        );
        Optional<Employee> foundEmployee = employeeDAO.findByAttributes(attributes);

        assertTrue(foundEmployee.isPresent());
        assertEquals("Multi", foundEmployee.get().getFirstName());
        assertEquals("Attribute", foundEmployee.get().getLastName());

        // Clean up
        assertTrue(employeeDAO.delete(createResponse.get().getID()));
    }

    @Test
    public void testFindByAttributes_notFound() {
        Map<String, String> attributes = Map.of("email", "nonexistent@example.com");
        Optional<Employee> foundEmployee = employeeDAO.findByAttributes(attributes);

        assertFalse(foundEmployee.isPresent());
    }

    // ---------- TESTS FOR UPDATING EMPLOYEES ----------
    @Test
    public void testUpdate_success() {
        // First, create an employee
        String uniqueEmail = "update." + System.currentTimeMillis() + "@example.com";
        String uniquePhone = "555-" + (1000 + new Random().nextInt(9000));

        Employee newEmployee = new Employee(null, "Update", "Me", "123 Update St", uniqueEmail, uniquePhone, Employee.Position.VET_TECH);
        Optional<Employee> createResponse = employeeDAO.createEmployee(newEmployee);
        assertTrue(createResponse.isPresent());

        // Update the employee
        Employee employeeToUpdate = createResponse.get();
        employeeToUpdate.setFirstName("Updated");
        employeeToUpdate.setLastName("Name");
        employeeToUpdate.setAddress("456 New St");
        employeeToUpdate.setRole(Employee.Position.VETERINARIAN);

        boolean updateResult = employeeDAO.update(employeeToUpdate);
        assertTrue(updateResult);

        // Verify the update
        Optional<Employee> updatedEmployee = employeeDAO.findByID(employeeToUpdate.getID());
        assertTrue(updatedEmployee.isPresent());
        assertEquals("Updated", updatedEmployee.get().getFirstName());
        assertEquals("Name", updatedEmployee.get().getLastName());
        assertEquals("456 New St", updatedEmployee.get().getAddress());
        assertEquals(Employee.Position.VETERINARIAN, updatedEmployee.get().getRole());

        // Clean up
        assertTrue(employeeDAO.delete(employeeToUpdate.getID()));
    }

    @Test
    public void testUpdate_nonExistent() {
        Employee nonExistentEmployee = new Employee(9999, "Nobody", "NoWhere", "123 Nowhere St", "nobody@example.com", "555-0000", Employee.Position.RECEPTIONIST);

        boolean updateResult = employeeDAO.update(nonExistentEmployee);
        assertFalse(updateResult);
    }

    // ---------- TESTS FOR DELETING EMPLOYEES ----------
    @Test
    public void testDelete_success() {
        // First, create an employee
        String uniqueEmail = "delete." + System.currentTimeMillis() + "@example.com";
        String uniquePhone = "555-" + (1000 + new Random().nextInt(9000));

        Employee newEmployee = new Employee(null, "Delete", "Me", "123 Delete St", uniqueEmail, uniquePhone, Employee.Position.RECEPTIONIST);
        Optional<Employee> createResponse = employeeDAO.createEmployee(newEmployee);
        assertTrue(createResponse.isPresent());

        // Delete the employee
        int employeeId = createResponse.get().getID();
        boolean deleteResult = employeeDAO.delete(employeeId);
        assertTrue(deleteResult);

        // Verify the employee was deleted
        Optional<Employee> deletedEmployee = employeeDAO.findByID(employeeId);
        assertFalse(deletedEmployee.isPresent());
    }

    @Test
    public void testDelete_nonExistent() {
        boolean deleteResult = employeeDAO.delete(9999); // Assuming this ID doesn't exist
        assertFalse(deleteResult);
    }

    // ---------- TESTS FOR FINDING ALL EMPLOYEES ----------
    @Test
    public void testFindAll() {
        // Create some employees for this test
        String baseEmail = "findall." + System.currentTimeMillis();
        String basePhone = "555-" + (1000 + new Random().nextInt(9000));

        Employee employee1 = new Employee(null, "FindAll1", "Test", "123 FindAll St", baseEmail + "1@example.com", basePhone + "1", Employee.Position.RECEPTIONIST);
        Employee employee2 = new Employee(null, "FindAll2", "Test", "456 FindAll St", baseEmail + "2@example.com", basePhone + "2", Employee.Position.VETERINARIAN);

        Optional<Employee> response1 = employeeDAO.createEmployee(employee1);
        Optional<Employee> response2 = employeeDAO.createEmployee(employee2);

        assertTrue(response1.isPresent());
        assertTrue(response2.isPresent());

        // Get all employees
        List<Employee> allEmployees = employeeDAO.findAll();

        // Check that our test employees are in the list
        boolean foundEmployee1 = allEmployees.stream()
                .anyMatch(e -> e.getID().equals(response1.get().getID()));
        boolean foundEmployee2 = allEmployees.stream()
                .anyMatch(e -> e.getID().equals(response2.get().getID()));

        assertTrue(foundEmployee1);
        assertTrue(foundEmployee2);

        // Clean up
        assertTrue(employeeDAO.delete(response1.get().getID()));
        assertTrue(employeeDAO.delete(response2.get().getID()));
    }

    @Test
    public void testFindAllByAttributes_byRole() {
        // Create some employees with the same role
        String baseEmail = "role." + System.currentTimeMillis();
        String basePhone = "555-" + (1000 + new Random().nextInt(9000));

        Employee employee1 = new Employee(null, "Role1", "Test", "123 Role St", baseEmail + "1@example.com", basePhone + "1", Employee.Position.VETERINARIAN);
        Employee employee2 = new Employee(null, "Role2", "Test", "456 Role St", baseEmail + "2@example.com", basePhone + "2", Employee.Position.VETERINARIAN);

        Optional<Employee> response1 = employeeDAO.createEmployee(employee1);
        Optional<Employee> response2 = employeeDAO.createEmployee(employee2);

        assertTrue(response1.isPresent());
        assertTrue(response2.isPresent());

        // Find all employees with the VETERINARIAN role
        Map<String, String> attributes = Map.of("role", Employee.Position.VETERINARIAN.name());
        List<Employee> veterinarians = employeeDAO.findAllByAttributes(attributes);

        // Check that our test employees are in the list
        boolean foundEmployee1 = veterinarians.stream()
                .anyMatch(e -> e.getID().equals(response1.get().getID()));
        boolean foundEmployee2 = veterinarians.stream()
                .anyMatch(e -> e.getID().equals(response2.get().getID()));

        assertTrue(foundEmployee1);
        assertTrue(foundEmployee2);

        // Clean up
        assertTrue(employeeDAO.delete(response1.get().getID()));
        assertTrue(employeeDAO.delete(response2.get().getID()));
    }
}