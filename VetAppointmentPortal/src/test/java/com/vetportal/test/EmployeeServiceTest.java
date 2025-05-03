package com.vetportal.test;

import com.vetportal.dto.LookupStatus;
import com.vetportal.dto.ServiceResponse;
import com.vetportal.model.Employee;
import com.vetportal.service.EmployeeService;
import com.vetportal.util.DatabaseInitializer;
import com.vetportal.util.DbManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.util.*;

public class EmployeeServiceTest {

    private static Connection connection;
    private static EmployeeService employeeService;

    @BeforeAll
    public static void setupDatabase() {
        try {
            connection = DbManager.getConnection();

            // Initialize the database schema and seed data
            DatabaseInitializer.initializeOnExistingConnection(connection, "database/schema.sql", "database/seed.sql");

            // Ensure foreign keys are enabled
            DbManager.ensureForeignKeysEnabled();

        } catch (Exception e) {
            fail("Failed to initialize test database: " + e.getMessage());
        }
    }

    @BeforeEach
    public void setup() {
        // Create a fresh EmployeeService instance for each test
        employeeService = new EmployeeService(connection);

        // Verify the connection is still valid
        try {
            if (connection == null || connection.isClosed()) {
                connection = DbManager.getConnection();
                employeeService = new EmployeeService(connection);
            }
        } catch (Exception e) {
            fail("Failed to establish database connection: " + e.getMessage());
        }
    }

    // ---------- CREATE EMPLOYEE TESTS ----------

    @Test
    public void testCreateEmployee_Success() {
        // Create a unique employee
        String uniqueEmail = "create." + System.currentTimeMillis() + "@example.com";
        String uniquePhone = "555-" + (1000 + new Random().nextInt(9000));

        Employee employee = new Employee(null, "John", "Doe", "123 Test St",
                uniquePhone, uniqueEmail, Employee.Position.VETERINARIAN);

        // Create the employee
        ServiceResponse<Employee> response = employeeService.createEmployee(employee);

        // Verify success
        assertTrue(response.isSuccess());
        assertEquals(LookupStatus.SUCCESS, response.getStatus());
        assertNotNull(response.getData());
        assertNotNull(response.getData().getID());
        assertEquals("John", response.getData().getFirstName());
        assertEquals(uniqueEmail, response.getData().getEmail());

        // Clean up
        employeeService.deleteEmployee(response.getData().getID());
    }

    @Test
    public void testCreateEmployee_DuplicateEmail() {
        // Create a unique email for this test
        String uniqueEmail = "duplicate." + System.currentTimeMillis() + "@example.com";
        String uniquePhone1 = "555-" + (1000 + new Random().nextInt(9000));
        String uniquePhone2 = "555-" + (1000 + new Random().nextInt(9000));

        // First, create an employee
        Employee firstEmployee = new Employee(null, "First", "Employee",
                "123 First St", uniquePhone1, uniqueEmail,
                Employee.Position.RECEPTIONIST);

        ServiceResponse<Employee> firstResponse = employeeService.createEmployee(firstEmployee);
        assertTrue(firstResponse.isSuccess());

        try {
            // Now try to create another with the same email
            Employee duplicateEmployee = new Employee(null, "Duplicate", "User",
                    "456 Test Ave", uniquePhone2, uniqueEmail,
                    Employee.Position.VET_TECH);

            ServiceResponse<Employee> duplicateResponse = employeeService.createEmployee(duplicateEmployee);

            // Verify failure
            assertFalse(duplicateResponse.isSuccess());
            assertEquals(LookupStatus.DB_ERROR, duplicateResponse.getStatus());
            assertNull(duplicateResponse.getData());
            assertTrue(duplicateResponse.getMessage().contains("Error: "));
            assertTrue(duplicateResponse.getMessage().contains("already exists"));
        } finally {
            // Clean up
            employeeService.deleteEmployee(firstResponse.getData().getID());
        }
    }

    @Test
    public void testCreateEmployee_DuplicatePhone() {
        // Create a unique phone for this test
        String uniquePhone = "555-" + (1000 + new Random().nextInt(9000));
        String uniqueEmail1 = "first." + System.currentTimeMillis() + "@example.com";
        String uniqueEmail2 = "second." + System.currentTimeMillis() + "@example.com";

        // First, create an employee
        Employee firstEmployee = new Employee(null, "First", "Employee",
                "123 First St", uniquePhone, uniqueEmail1,
                Employee.Position.RECEPTIONIST);

        ServiceResponse<Employee> firstResponse = employeeService.createEmployee(firstEmployee);
        assertTrue(firstResponse.isSuccess());

        try {
            // Now try to create another with the same phone
            Employee duplicateEmployee = new Employee(null, "Duplicate", "User",
                    "456 Test Ave", uniquePhone, uniqueEmail2,
                    Employee.Position.VET_TECH);

            ServiceResponse<Employee> duplicateResponse = employeeService.createEmployee(duplicateEmployee);

            // Verify failure
            assertFalse(duplicateResponse.isSuccess());
            assertEquals(LookupStatus.DB_ERROR, duplicateResponse.getStatus());
            assertNull(duplicateResponse.getData());
            assertTrue(duplicateResponse.getMessage().contains("Error: "));
            assertTrue(duplicateResponse.getMessage().contains("already exists"));
        } finally {
            // Clean up
            employeeService.deleteEmployee(firstResponse.getData().getID());
        }
    }

    // ---------- UPDATE EMPLOYEE TESTS ----------

    @Test
    public void testUpdateEmployee_Success() {
        // First, create an employee
        String uniqueEmail = "update." + System.currentTimeMillis() + "@example.com";
        String uniquePhone = "555-" + (1000 + new Random().nextInt(9000));

        Employee newEmployee = new Employee(null, "Update", "Me",
                "123 Update St", uniquePhone, uniqueEmail,
                Employee.Position.VET_TECH);

        ServiceResponse<Employee> createResponse = employeeService.createEmployee(newEmployee);
        assertTrue(createResponse.isSuccess());

        try {
            // Update the employee
            Employee employeeToUpdate = createResponse.getData();
            employeeToUpdate.setFirstName("Updated");
            employeeToUpdate.setLastName("Name");
            employeeToUpdate.setAddress("456 New St");
            employeeToUpdate.setRole(Employee.Position.VETERINARIAN);

            boolean updateResult = employeeService.updateEmployee(employeeToUpdate);
            assertTrue(updateResult);

            // Verify the update
            ServiceResponse<Employee> findResponse = employeeService.findEmployeeByID(employeeToUpdate.getID());
            assertTrue(findResponse.isSuccess());
            assertEquals("Updated", findResponse.getData().getFirstName());
            assertEquals("Name", findResponse.getData().getLastName());
            assertEquals("456 New St", findResponse.getData().getAddress());
            assertEquals(Employee.Position.VETERINARIAN, findResponse.getData().getRole());
        } finally {
            // Clean up
            employeeService.deleteEmployee(createResponse.getData().getID());
        }
    }

    @Test
    public void testUpdateEmployee_NonExistent() {
        // Try to update a non-existent employee
        Employee nonExistentEmployee = new Employee(9999, "Nobody", "NoWhere",
                "123 Nowhere St", "555-0000", "nobody@example.com",
                Employee.Position.RECEPTIONIST);

        boolean updateResult = employeeService.updateEmployee(nonExistentEmployee);
        assertFalse(updateResult);
    }

    // ---------- DELETE EMPLOYEE TESTS ----------

    @Test
    public void testDeleteEmployee_Success() {
        // First, create an employee
        String uniqueEmail = "delete." + System.currentTimeMillis() + "@example.com";
        String uniquePhone = "555-" + (1000 + new Random().nextInt(9000));

        Employee newEmployee = new Employee(null, "Delete", "Me",
                "123 Delete St", uniquePhone, uniqueEmail,
                Employee.Position.RECEPTIONIST);

        ServiceResponse<Employee> createResponse = employeeService.createEmployee(newEmployee);
        assertTrue(createResponse.isSuccess());

        // Delete the employee
        int employeeId = createResponse.getData().getID();
        boolean deleteResult = employeeService.deleteEmployee(employeeId);
        assertTrue(deleteResult);

        // Verify the employee was deleted
        ServiceResponse<Employee> findResponse = employeeService.findEmployeeByID(employeeId);
        assertFalse(findResponse.isSuccess());
        assertEquals(LookupStatus.NOT_FOUND, findResponse.getStatus());
    }

    @Test
    public void testDeleteEmployee_NonExistent() {
        // Try to delete a non-existent employee
        boolean deleteResult = employeeService.deleteEmployee(9999);
        assertFalse(deleteResult);
    }

    // ---------- FIND EMPLOYEE TESTS ----------

    @Test
    public void testFindEmployeeByID_Success() {
        // First, create an employee
        String uniqueEmail = "find." + System.currentTimeMillis() + "@example.com";
        String uniquePhone = "555-" + (1000 + new Random().nextInt(9000));

        Employee newEmployee = new Employee(null, "Find", "Me",
                "123 Find St", uniquePhone, uniqueEmail,
                Employee.Position.VETERINARIAN);

        ServiceResponse<Employee> createResponse = employeeService.createEmployee(newEmployee);
        assertTrue(createResponse.isSuccess());

        try {
            // Find the employee by ID
            int employeeId = createResponse.getData().getID();
            ServiceResponse<Employee> findResponse = employeeService.findEmployeeByID(employeeId);

            // Verify success
            assertTrue(findResponse.isSuccess());
            assertEquals(LookupStatus.SUCCESS, findResponse.getStatus());
            assertNotNull(findResponse.getData());
            assertEquals(employeeId, findResponse.getData().getID());
            assertEquals("Find", findResponse.getData().getFirstName());
            assertEquals("Me", findResponse.getData().getLastName());
        } finally {
            // Clean up
            employeeService.deleteEmployee(createResponse.getData().getID());
        }
    }

    @Test
    public void testFindEmployeeByID_NotFound() {
        // Try to find a non-existent employee
        ServiceResponse<Employee> findResponse = employeeService.findEmployeeByID(9999);

        // Verify not found
        assertFalse(findResponse.isSuccess());
        assertEquals(LookupStatus.NOT_FOUND, findResponse.getStatus());
        assertNull(findResponse.getData());
        assertEquals("Employee not found with ID: 9999", findResponse.getMessage());
    }

    @Test
    public void testFindEmployeeByAttributes_Email() {
        // Create an employee with a unique email
        String uniqueEmail = "attribute." + System.currentTimeMillis() + "@example.com";
        String uniquePhone = "555-" + (1000 + new Random().nextInt(9000));

        Employee newEmployee = new Employee(null, "Attribute", "Search",
                "123 Attr St", uniquePhone, uniqueEmail,
                Employee.Position.VETERINARIAN);

        ServiceResponse<Employee> createResponse = employeeService.createEmployee(newEmployee);
        assertTrue(createResponse.isSuccess());

        try {
            // Find by email
            Map<String, String> attributes = Map.of("email", uniqueEmail);
            ServiceResponse<Employee> findResponse = employeeService.findEmployeeByAttributes(attributes);

            // Verify success
            assertTrue(findResponse.isSuccess());
            assertEquals(LookupStatus.SUCCESS, findResponse.getStatus());
            assertNotNull(findResponse.getData());
            assertEquals(uniqueEmail, findResponse.getData().getEmail());
            assertEquals("Attribute", findResponse.getData().getFirstName());
        } finally {
            // Clean up
            employeeService.deleteEmployee(createResponse.getData().getID());
        }
    }

    @Test
    public void testFindEmployeeByAttributes_Phone() {
        // Create an employee with a unique phone
        String uniqueEmail = "phone." + System.currentTimeMillis() + "@example.com";
        String uniquePhone = "555-" + (1000 + new Random().nextInt(9000));

        Employee newEmployee = new Employee(null, "Phone", "Search",
                "123 Phone St", uniquePhone, uniqueEmail,
                Employee.Position.RECEPTIONIST);

        ServiceResponse<Employee> createResponse = employeeService.createEmployee(newEmployee);
        assertTrue(createResponse.isSuccess());

        try {
            // Find by phone
            Map<String, String> attributes = Map.of("phone", uniquePhone);
            ServiceResponse<Employee> findResponse = employeeService.findEmployeeByAttributes(attributes);

            // Verify success
            assertTrue(findResponse.isSuccess());
            assertEquals(LookupStatus.SUCCESS, findResponse.getStatus());
            assertNotNull(findResponse.getData());
            assertEquals(uniquePhone, findResponse.getData().getPhone());
            assertEquals("Phone", findResponse.getData().getFirstName());
        } finally {
            // Clean up
            employeeService.deleteEmployee(createResponse.getData().getID());
        }
    }

    @Test
    public void testFindEmployeeByAttributes_NotFound() {
        // Try to find by non-existent attributes
        Map<String, String> attributes = Map.of("email", "nonexistent" + System.currentTimeMillis() + "@example.com");
        ServiceResponse<Employee> findResponse = employeeService.findEmployeeByAttributes(attributes);

        // Verify not found
        assertFalse(findResponse.isSuccess());
        assertEquals(LookupStatus.NOT_FOUND, findResponse.getStatus());
        assertNull(findResponse.getData());
        assertTrue(findResponse.getMessage().contains("Employee not found with attributes"));
    }

    // ---------- GET ALL EMPLOYEES TESTS ----------

    @Test
    public void testGetAllEmployees() {
        // Create some test employees
        String baseEmail = "getall." + System.currentTimeMillis();
        String basePhone = "555-" + (1000 + new Random().nextInt(9000));

        Employee employee1 = new Employee(null, "GetAll1", "Test",
                "123 GetAll St", basePhone + "1", baseEmail + "1@example.com",
                Employee.Position.RECEPTIONIST);

        Employee employee2 = new Employee(null, "GetAll2", "Test",
                "456 GetAll St", basePhone + "2", baseEmail + "2@example.com",
                Employee.Position.VETERINARIAN);

        ServiceResponse<Employee> response1 = employeeService.createEmployee(employee1);
        ServiceResponse<Employee> response2 = employeeService.createEmployee(employee2);

        assertTrue(response1.isSuccess());
        assertTrue(response2.isSuccess());

        try {
            // Get all employees
            ServiceResponse<List<Employee>> allResponse = employeeService.getAllEmployees();

            // Verify success
            assertTrue(allResponse.isSuccess());
            assertEquals(LookupStatus.SUCCESS, allResponse.getStatus());
            assertNotNull(allResponse.getData());

            // Check that our test employees are in the list
            List<Employee> allEmployees = allResponse.getData();
            boolean foundEmployee1 = allEmployees.stream()
                    .anyMatch(e -> e.getID().equals(response1.getData().getID()));
            boolean foundEmployee2 = allEmployees.stream()
                    .anyMatch(e -> e.getID().equals(response2.getData().getID()));

            assertTrue(foundEmployee1);
            assertTrue(foundEmployee2);
        } finally {
            // Clean up
            employeeService.deleteEmployee(response1.getData().getID());
            employeeService.deleteEmployee(response2.getData().getID());
        }
    }

    // ---------- FIND EMPLOYEES BY ATTRIBUTES TESTS ----------

    @Test
    public void testFindEmployeesByAttributes() {
        // Create some employees with the same role
        String baseEmail = "findbyattr." + System.currentTimeMillis();
        String basePhone = "555-" + (1000 + new Random().nextInt(9000));

        Employee employee1 = new Employee(null, "FindByAttr1", "Test",
                "123 Attr St", basePhone + "1", baseEmail + "1@example.com",
                Employee.Position.VET_TECH);

        Employee employee2 = new Employee(null, "FindByAttr2", "Test",
                "456 Attr St", basePhone + "2", baseEmail + "2@example.com",
                Employee.Position.VET_TECH);

        Employee employee3 = new Employee(null, "FindByAttr3", "Test",
                "789 Attr St", basePhone + "3", baseEmail + "3@example.com",
                Employee.Position.RECEPTIONIST);

        ServiceResponse<Employee> response1 = employeeService.createEmployee(employee1);
        ServiceResponse<Employee> response2 = employeeService.createEmployee(employee2);
        ServiceResponse<Employee> response3 = employeeService.createEmployee(employee3);

        assertTrue(response1.isSuccess());
        assertTrue(response2.isSuccess());
        assertTrue(response3.isSuccess());

        try {
            // Find all employees with the VET_TECH role
            Map<String, String> attributes = Map.of("role", Employee.Position.VET_TECH.name());
            ServiceResponse<List<Employee>> findResponse = employeeService.findEmployeesByAttributes(attributes);

            // Verify success
            assertTrue(findResponse.isSuccess());
            assertEquals(LookupStatus.SUCCESS, findResponse.getStatus());
            assertNotNull(findResponse.getData());

            // Check that only our VET_TECH employees are in the list
            List<Employee> vetTechs = findResponse.getData();
            boolean foundEmployee1 = vetTechs.stream()
                    .anyMatch(e -> e.getID().equals(response1.getData().getID()));
            boolean foundEmployee2 = vetTechs.stream()
                    .anyMatch(e -> e.getID().equals(response2.getData().getID()));
            boolean foundEmployee3 = vetTechs.stream()
                    .anyMatch(e -> e.getID().equals(response3.getData().getID()));

            assertTrue(foundEmployee1);
            assertTrue(foundEmployee2);
            assertFalse(foundEmployee3); // Should not find the RECEPTIONIST
        } finally {
            // Clean up
            employeeService.deleteEmployee(response1.getData().getID());
            employeeService.deleteEmployee(response2.getData().getID());
            employeeService.deleteEmployee(response3.getData().getID());
        }
    }

    @Test
    public void testFindEmployeesByAttributes_NotFound() {
        // Try to find employees by non-existent attributes
        Map<String, String> attributes = Map.of("firstname", "NonExistentName" + System.currentTimeMillis());
        ServiceResponse<List<Employee>> findResponse = employeeService.findEmployeesByAttributes(attributes);

        // Verify not found
        assertFalse(findResponse.isSuccess());
        assertEquals(LookupStatus.NOT_FOUND, findResponse.getStatus());
        assertNull(findResponse.getData());
        assertEquals("No employees found with given attributes", findResponse.getMessage());
    }

    // ---------- FIND EMPLOYEES BY ROLE TESTS ----------

    @Test
    public void testFindEmployeesByRole() {
        // Create some employees with the same role
        String baseEmail = "findbyrole." + System.currentTimeMillis();
        String basePhone = "555-" + (1000 + new Random().nextInt(9000));

        Employee employee1 = new Employee(null, "FindByRole1", "Test",
                "123 Role St", basePhone + "1", baseEmail + "1@example.com",
                Employee.Position.RECEPTIONIST);

        Employee employee2 = new Employee(null, "FindByRole2", "Test",
                "456 Role St", basePhone + "2", baseEmail + "2@example.com",
                Employee.Position.RECEPTIONIST);

        Employee employee3 = new Employee(null, "FindByRole3", "Test",
                "789 Role St", basePhone + "3", baseEmail + "3@example.com",
                Employee.Position.VETERINARIAN);

        ServiceResponse<Employee> response1 = employeeService.createEmployee(employee1);
        ServiceResponse<Employee> response2 = employeeService.createEmployee(employee2);
        ServiceResponse<Employee> response3 = employeeService.createEmployee(employee3);

        assertTrue(response1.isSuccess());
        assertTrue(response2.isSuccess());
        assertTrue(response3.isSuccess());

        try {
            // Find all employees with the RECEPTIONIST role
            ServiceResponse<List<Employee>> findResponse =
                    employeeService.findEmployeesByRole(Employee.Position.RECEPTIONIST);

            // Verify success
            assertTrue(findResponse.isSuccess());
            assertEquals(LookupStatus.SUCCESS, findResponse.getStatus());
            assertNotNull(findResponse.getData());

            // Check that only our RECEPTIONIST employees are in the list
            List<Employee> receptionists = findResponse.getData();
            boolean foundEmployee1 = receptionists.stream()
                    .anyMatch(e -> e.getID().equals(response1.getData().getID()));
            boolean foundEmployee2 = receptionists.stream()
                    .anyMatch(e -> e.getID().equals(response2.getData().getID()));
            boolean foundEmployee3 = receptionists.stream()
                    .anyMatch(e -> e.getID().equals(response3.getData().getID()));

            assertTrue(foundEmployee1);
            assertTrue(foundEmployee2);
            assertFalse(foundEmployee3); // Should not find the VETERINARIAN
        } finally {
            // Clean up
            employeeService.deleteEmployee(response1.getData().getID());
            employeeService.deleteEmployee(response2.getData().getID());
            employeeService.deleteEmployee(response3.getData().getID());
        }
    }

    @Test
    public void testFindEmployeesByRole_NotFound() {
        // Try to find employees by a role that doesn't exist
        // First, make sure there are no employees with this role
        ServiceResponse<List<Employee>> findAllResponse =
                employeeService.findEmployeesByRole(Employee.Position.VET_TECH);

        if (findAllResponse.isSuccess()) {
            // If there are employees with this role, delete them
            List<Employee> employees = findAllResponse.getData();
            for (Employee employee : employees) {
                employeeService.deleteEmployee(employee.getID());
            }
        }

        // Now try to find employees with this role
        ServiceResponse<List<Employee>> findResponse =
                employeeService.findEmployeesByRole(Employee.Position.VET_TECH);

        // Verify not found
        assertFalse(findResponse.isSuccess());
        assertEquals(LookupStatus.NOT_FOUND, findResponse.getStatus());
        assertNull(findResponse.getData());
        assertEquals("No employees found with role: " + Employee.Position.VET_TECH, findResponse.getMessage());
    }


}