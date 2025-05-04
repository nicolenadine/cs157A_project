package com.vetportal.service;

import com.vetportal.dao.EmployeeDAO;
import com.vetportal.dto.ServiceResponse;
import com.vetportal.exception.DataAccessException;
import com.vetportal.model.Employee;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service layer responsible for employee-related business logic.
 * <p>
 * Delegates database operations to the {@link EmployeeDAO} and wraps results in
 * {@link ServiceResponse} objects to include status and error handling.
 */
public class EmployeeService {

    private final EmployeeDAO employeeDAO;

    /**
     * Constructs a new EmployeeService using the given database connection.
     *
     * @param conn an active SQL database connection
     */
    public EmployeeService(Connection conn) {
        this.employeeDAO = new EmployeeDAO(conn);
    }

    // -------------- CREATE UPDATE AND DELETE METHODS ------------
    /**
     * Creates a new employee in the database.
     *
     * @param employee the employee to create
     * @return a service response containing the created employee or an error
     */
    public ServiceResponse<Employee> createEmployee(Employee employee) {
        try {
            Optional<Employee> result = employeeDAO.createEmployee(employee);
            return result.map(ServiceResponse::success)
                    .orElseGet(() -> ServiceResponse.notFound("Employee could not be created"));
        } catch (DataAccessException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                return ServiceResponse.dbError("Error: Email or phone already exists");
            }
            return ServiceResponse.dbError("Error: " + e.getMessage());
        }
    }

    /**
     * Updates an existing employee in the database.
     *
     * @param employee the employee to update
     * @return true if the employee was updated, false otherwise
     */
    public boolean updateEmployee(Employee employee) {
        try {
            return employeeDAO.update(employee);
        } catch (DataAccessException e) {
            System.err.println("Error updating employee: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes an employee from the database.
     *
     * @param employeeID the ID of the employee to delete
     * @return true if the employee was deleted, false otherwise
     */
    public boolean deleteEmployee(int employeeID) {
        try {
            // First verify the employee exists
            Optional<Employee> employee = employeeDAO.findByID(employeeID);
            if (employee.isEmpty()) {
                System.out.println("Employee with ID " + employeeID + " not found");
                return false;
            }

            return employeeDAO.delete(employeeID);
        } catch (DataAccessException e) {
            System.err.println("Error deleting employee: " + e.getMessage());
            return false;
        }
    }

    // -----------------  QUERY METHODS ------------------------

    /**
     * Looks up an employee by attributes (e.g., phone, email, role).
     *
     * @param attributes a Map of Strings where the first string corresponds to the entity attribute and
     *      *                   the second String corresponds to the value to search for in the db
     * @return a service response containing the employee or an error
     */
    public ServiceResponse<Employee> findEmployeeByAttributes(Map<String, String> attributes) {
        try {
            Optional<Employee> result = employeeDAO.findByAttributes(attributes);
            return result.map(ServiceResponse::success)
                    .orElseGet(() -> ServiceResponse.notFound("Employee not found with attributes: " + attributes));
        } catch (DataAccessException e) {
            return ServiceResponse.dbError("Database error: " + e.getMessage());
        }
    }

    /**
     * Finds an employee by ID.
     *
     * @param employeeID the ID of the employee to find
     * @return a service response containing the employee or an error
     */
    public ServiceResponse<Employee> findEmployeeByID(int employeeID) {
        try {
            Optional<Employee> result = employeeDAO.findByID(employeeID);
            return result.map(ServiceResponse::success)
                    .orElseGet(() -> ServiceResponse.notFound("Employee not found with ID: " + employeeID));
        } catch (DataAccessException e) {
            return ServiceResponse.dbError("Database error: " + e.getMessage());
        }
    }

    /**
     * Retrieves all employees from the database.
     *
     * @return a service response containing the list of employees or an error
     */
    public ServiceResponse<List<Employee>> getAllEmployees() {
        try {
            List<Employee> employees = employeeDAO.findAll();
            if (employees.isEmpty()) {
                return ServiceResponse.notFound("No employees found");
            }
            return ServiceResponse.success(employees);
        } catch (DataAccessException e) {
            return ServiceResponse.dbError("Error retrieving employees: " + e.getMessage());
        }
    }

    /**
     * Finds all employees matching certain attributes.
     *
     * @param attributes a Map of Strings where the first string corresponds to the entity attribute and
     *                   the second String corresponds to the value to filter by in the db
     * @return a service response containing the filtered list of employees or an error
     */
    public ServiceResponse<List<Employee>> findEmployeesByAttributes(Map<String, String> attributes) {
        try {
            List<Employee> employees = employeeDAO.findAllByAttributes(attributes);
            if (employees.isEmpty()) {
                return ServiceResponse.notFound("No employees found with given attributes");
            }
            return ServiceResponse.success(employees);
        } catch (DataAccessException e) {
            return ServiceResponse.dbError("Error retrieving employees: " + e.getMessage());
        }
    }

    /**
     * Find employees by their role.
     *
     * @param role the role to filter by
     * @return a service response containing employees with the specified role
     */
    public ServiceResponse<List<Employee>> findEmployeesByRole(Employee.Position role) {
        try {
            Map<String, String> attributes = Map.of("role", role.name());
            List<Employee> employees = employeeDAO.findAllByAttributes(attributes);
            if (employees.isEmpty()) {
                return ServiceResponse.notFound("No employees found with role: " + role);
            }
            return ServiceResponse.success(employees);
        } catch (DataAccessException e) {
            return ServiceResponse.dbError("Error retrieving employees by role: " + e.getMessage());
        }
    }
}