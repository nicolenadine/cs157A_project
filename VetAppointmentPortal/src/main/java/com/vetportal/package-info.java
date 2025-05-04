/**
 * VetPortal - Vet Appointment Portal
 *
 * <h2>Overview</h2>
 * Vet Appointment Portal is a comprehensive application for managing veterinary practice appointment scheduling workflows.
 * It provides functionality for managing customers, their pets, employees, and appointments
 * in a veterinary clinic environment. The system follows a layered architecture pattern
 * with clear separation of concerns and is designed to be extensible for adding new features down the road.
 *
 * <h2>Architecture</h2>
 * The application follows a layered architecture with the following components:
 * <ul>
 *   <li><b>Controller Layer</b>: Interfaces with FXML UI components and routes requests</li>
 *   <li><b>Service Layer</b>: Business logic and transaction management between the Controller and Data Access layers</li>
 *   <li><b>Data Access Layer</b>: DAO classes that handle database operations</li>
 *   <li><b>Model Layer</b>: Data objects that represent core business entities</li>
 *   <li><b>Utility Classes</b>: Supporting classes for database initialization, management, etc.</li>
 * </ul>
 *
 * <h2>Key Packages</h2>
 * <ul>
 *   <li>{@link com.vetportal.model} - Core entities (Customer, Pet, Employee, Appointment)</li>
 *   <li>{@link com.vetportal.dao} - Data Access Objects for CRUD operations</li>
 *   <li>{@link com.vetportal.service} - Business logic and service components</li>
 *   <li>{@link com.vetportal.controller} - UI controllers and request handlers</li>
 *   <li>{@link com.vetportal.mapper} - Maps between ResultSet objects and domain entities</li>
 *   <li>{@link com.vetportal.dto} - Data Transfer Objects for service responses</li>
 *   <li>{@link com.vetportal.exception} - Custom exception classes</li>
 *   <li>{@link com.vetportal.util} - Utility classes like database connection management</li>
 * </ul>
 *
 * <h2>Design Patterns</h2>
 * The application implements several design patterns:
 * <ul>
 *   <li><b>Data Access Object (DAO)</b>: Abstracts and encapsulates database access
 *       through {@link com.vetportal.dao.GenericDAO} and its implementations.</li>
 *   <li><b>Singleton</b>: {@link com.vetportal.service.ServiceManager} maintains a single
 *       instance of service objects and database connection management.</li>
 *   <li><b>Factory Method</b>: Used in {@link com.vetportal.dto.ServiceResponse} to create
 *       standardized response objects.</li>
 *   <li><b>Template Method</b>: {@link com.vetportal.dao.BaseDAO} defines the skeleton of database CRUD
 *       operations with specific steps implemented by subclasses.</li>
 * </ul>
 *
 * <h2>Database Schema</h2>
 * The application uses SQLite with the following main tables:
 * <ul>
 *   <li><b>Customer</b>: Stores pet owner information</li>
 *   <li><b>Pet</b>: Stores animal information with foreign key to owner</li>
 *   <li><b>Employee</b>: Stores staff information with roles (Veterinarian, Vet Tech, Receptionist)</li>
 *   <li><b>Appointment</b>: Links pets, providers, dates, and appointment types</li>
 * </ul>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Customer and pet management</li>
 *   <li>Employee management with different roles</li>
 *   <li>Appointment scheduling with conflict detection</li>
 *   <li>Business rules enforcement (e.g., specifying which provider types can perform various services)</li>
 *   <li>Standardized error handling through ServiceResponse objects with custom exception classes</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * The application follows a consistent error handling approach:
 * <ul>
 *   <li>Database constraint enforcement exceptions like {@link com.vetportal.exception.AppointmentConflictException}</li>
 *   <li>Generic {@link com.vetportal.exception.DataAccessException} for logging specific details related to database operations</li>
 *   <li>Standardized response wrapping with {@link com.vetportal.dto.ServiceResponse}</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>
 * // Initialize services: Singleton ServiceManager instance initialized at entry point in Main.java
 * //                      Manages shared instances of all other Service Classes and their respective DAOs
 * ServiceManager manager = new ServiceManager();
 *
 * // Create a new customer
 * Customer customer = new Customer(null, "John", "Doe", "123 Main St", "555-1234", "john@example.com");
 * ServiceResponse<Customer> response = manager.getCustomerService().createCustomer(customer);
 *
 * if (response.isSuccess()) {
 *     Customer savedCustomer = response.getData();
 *     System.out.println("Created customer with ID: " + savedCustomer.getID());
 * } else {
 *     System.err.println("Error: " + response.getMessage());
 * }
 * </pre>
 *
 * <h3>Retrieving All Appointments for a Given Day</h3>
 * <pre>
 * // Get instance of the service manager
 * ServiceManager manager = ServiceManager.getInstance();
 *
 * // Parse date string into LocalDate object (could be from UI input)
 * LocalDate appointmentDate = LocalDate.parse("2025-05-10");
 *
 * // Retrieve appointments using the AppointmentService
 * ServiceResponse<List<Appointment>> response = manager.getAppointmentService().findAppointmentsByDate(appointmentDate);
 *
 * if (response.isSuccess()) {
 *     List<Appointment> appointments = response.getData();
 *
 *     System.out.println("Appointments for " + appointmentDate + ":");
 *     for (Appointment appointment : appointments) {
 *         System.out.println(appointment.getTime() + " - " +
 *                           appointment.getAppointmentType() + " for " +
 *                           appointment.getPet().getName() + " with Dr. " +
 *                           appointment.getProvider().getLastName());
 *     }
 * } else {
 *     System.out.println("No appointments found: " + response.getMessage());
 * }
 * </pre>
 *
 * @version 1.0
 * @since 2025-05-03
 */
package com.vetportal;