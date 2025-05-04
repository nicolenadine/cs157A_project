package com.vetportal.controller;

import com.vetportal.dto.LookupStatus;
import com.vetportal.dto.ServiceResponse;
import com.vetportal.model.Appointment;
import com.vetportal.model.AppointmentType;
import com.vetportal.model.Customer;
import com.vetportal.model.Employee;
import com.vetportal.model.Pet;
import com.vetportal.service.AppointmentService;
import com.vetportal.service.CustomerService;
import com.vetportal.service.EmployeeService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CreateAppointmentController implements Initializable {

    // Customer Fields
    @FXML private TextField customerFirstName;
    @FXML private TextField customerLastName;
    @FXML private TextField customerEmail;
    @FXML private TextField customerPhone;

    // Pet Fields
    @FXML private TextField petName;
    @FXML private TextField petSpecies;
    @FXML private TextField petBreed;
    @FXML private DatePicker petBirthDate;

    // Appointment Fields
    @FXML private DatePicker appointmentDate;
    @FXML private ComboBox<String> appointmentTime;
    @FXML private ComboBox<String> appointmentType;
    @FXML private ComboBox<Employee> providerSelector;
    @FXML private Label titleLabel;
    @FXML private Button saveButton;

    // Services
    private CustomerService customerService;
    private EmployeeService employeeService;
    private AppointmentService appointmentService;

    // Database connection
    private Connection dbConnection;

    /**
     * Set the database connection and initialize services
     * @param connection active database connection
     */
    public void setConnection(Connection connection) {
        this.dbConnection = connection;
        this.customerService = new CustomerService(connection);
        this.employeeService = new EmployeeService(connection);
        this.appointmentService = new AppointmentService(connection);

        // After setting connection, load data that requires DB access
        loadProviders();
        System.out.println("Controller ready, providers loaded");
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        providerSelector.setPromptText("Select a provider");

        // Setup appointment types
        setupAppointmentTypes();

        // Setup time slots
        setupTimeSlots();

        // Set default date to today
        appointmentDate.setValue(LocalDate.now());
        petBirthDate.setValue(LocalDate.now().minusYears(1)); // Default pet age to 1 year

        // Add listener to the save button
        saveButton.setOnAction(this::handleSaveButton);
    }

    /**
     * Loads providers from the database and populates the provider selector
     */
    private void loadProviders() {
        if (employeeService == null) {
            System.err.println("Error: employeeService is null");
            return;
        }

        try {
            // We might need to adjust this if Employee.Position doesn't have VETERINARIAN
            // Consider using a map lookup instead if needed
            ServiceResponse<List<Employee>> response = employeeService.getAllEmployees();

            if (response.getStatus() == LookupStatus.SUCCESS && response.getData() != null) {
                ObservableList<Employee> providers = FXCollections.observableArrayList(response.getData());
                System.out.println("Loaded " + providers.size() + " providers");

                // Set the items in the ComboBox
                providerSelector.setItems(providers);

                // Set a cell factory to display provider names
                providerSelector.setCellFactory(param -> new ListCell<Employee>() {
                    @Override
                    protected void updateItem(Employee employee, boolean empty) {
                        super.updateItem(employee, empty);
                        if (empty || employee == null) {
                            setText(null);
                        } else {
                            setText(employee.getFirstName() + " " + employee.getLastName());
                        }
                    }
                });

                // Set converter for displaying selected value
                providerSelector.setConverter(new javafx.util.StringConverter<Employee>() {
                    @Override
                    public String toString(Employee employee) {
                        if (employee == null) {
                            return null;
                        }
                        return employee.getFirstName() + " " + employee.getLastName();
                    }

                    @Override
                    public Employee fromString(String string) {
                        // This might not be needed for ComboBox
                        return null;
                    }
                });

                // Select the first provider if available
                if (!providers.isEmpty()) {
                    providerSelector.getSelectionModel().selectFirst();
                }
            } else {
                System.err.println("Error loading providers: " +
                        (response.getMessage() != null ? response.getMessage() : "No data returned"));
                showAlert(AlertType.ERROR, "Error", "Failed to load providers", response.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Exception when loading providers: " + e.getMessage());
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Error", "Exception when loading providers", e.getMessage());
        }
    }

    /**
     * Setup appointment types combobox based on the AppointmentType enum
     */
    private void setupAppointmentTypes() {
        // Convert enum values to strings for the ComboBox
        ObservableList<String> types = FXCollections.observableArrayList();
        for (AppointmentType type : AppointmentType.values()) {
            types.add(type.name());
        }
        appointmentType.setItems(types);
        appointmentType.getSelectionModel().selectFirst();
    }

    /**
     * Setup time slots from 8 AM to 6 PM in 30-minute intervals
     */
    private void setupTimeSlots() {
        List<String> timeSlots = IntStream.rangeClosed(8, 17)
                .boxed()
                .flatMap(hour -> List.of(
                        String.format("%02d:00", hour),
                        String.format("%02d:30", hour)
                ).stream())
                .collect(Collectors.toList());

        ObservableList<String> times = FXCollections.observableArrayList(timeSlots);
        appointmentTime.setItems(times);
        appointmentTime.getSelectionModel().selectFirst();
    }

    /**
     * Handle the save button click event
     * @param event action event
     */
    private void handleSaveButton(ActionEvent event) {
        if (validateForm()) {
            try {
                // Create or find customer
                ServiceResponse<Customer> customerResponse = createOrFindCustomer();
                if (customerResponse.getStatus() != LookupStatus.SUCCESS) {
                    showAlert(AlertType.ERROR, "Customer Error", "Failed to create/find customer",
                            customerResponse.getMessage());
                    return;
                }
                Customer customer = customerResponse.getData();

                // Create or find pet
                ServiceResponse<Pet> petResponse = createOrFindPet(customer);
                if (petResponse.getStatus() != LookupStatus.SUCCESS) {
                    showAlert(AlertType.ERROR, "Pet Error", "Failed to create/find pet",
                            petResponse.getMessage());
                    return;
                }
                Pet pet = petResponse.getData();

                // Create the appointment
                ServiceResponse<Appointment> appointmentResponse = createAppointment(pet);
                if (appointmentResponse.getStatus() != LookupStatus.SUCCESS) {
                    showAlert(AlertType.ERROR, "Appointment Error", "Failed to create appointment",
                            appointmentResponse.getMessage());
                    return;
                }

                showAlert(AlertType.INFORMATION, "Success", "Appointment Created",
                        "The appointment has been successfully created.");
                closeForm();

            } catch (Exception e) {
                showAlert(AlertType.ERROR, "Error", "Failed to create appointment", e.getMessage());
            }
        }
    }

    /**
     * Validate all form fields
     * @return true if all fields are valid
     */
    private boolean validateForm() {
        StringBuilder errorMessage = new StringBuilder();

        // Validate customer fields
        if (customerFirstName.getText().isEmpty()) errorMessage.append("Customer first name is required\n");
        if (customerLastName.getText().isEmpty()) errorMessage.append("Customer last name is required\n");
        if (customerEmail.getText().isEmpty()) errorMessage.append("Customer email is required\n");
        if (customerPhone.getText().isEmpty()) errorMessage.append("Customer phone is required\n");

        // Validate pet fields
        if (petName.getText().isEmpty()) errorMessage.append("Pet name is required\n");
        if (petSpecies.getText().isEmpty()) errorMessage.append("Pet species is required\n");
        if (petBreed.getText().isEmpty()) errorMessage.append("Pet breed is required\n");
        if (petBirthDate.getValue() == null) errorMessage.append("Pet birth date is required\n");

        // Validate appointment fields
        if (appointmentDate.getValue() == null) errorMessage.append("Appointment date is required\n");
        if (appointmentTime.getValue() == null) errorMessage.append("Appointment time is required\n");
        if (appointmentType.getValue() == null) errorMessage.append("Appointment type is required\n");
        if (providerSelector.getValue() == null) errorMessage.append("Provider is required\n");

        // Check if there are any validation errors
        if (errorMessage.length() > 0) {
            showAlert(AlertType.WARNING, "Validation Error",
                    "Please correct the following errors:", errorMessage.toString());
            return false;
        }

        return true;
    }

    /**
     * Create or find an existing customer based on email
     * @return a ServiceResponse containing the Customer object or error
     */
    private ServiceResponse<Customer> createOrFindCustomer() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("email", customerEmail.getText());

        ServiceResponse<Customer> response = customerService.findCustomerByAttributes(attributes);

        if (response.getStatus() == LookupStatus.SUCCESS) {
            // Customer exists, use it
            Customer customer = response.getData();

            // Update customer info if needed
            boolean needsUpdate = false;

            if (!customer.getFirstName().equals(customerFirstName.getText())) {
                customer.setFirstname(customerFirstName.getText());
                needsUpdate = true;
            }

            if (!customer.getLastName().equals(customerLastName.getText())) {
                customer.setLastname(customerLastName.getText());
                needsUpdate = true;
            }

            if (!customer.getPhone().equals(customerPhone.getText())) {
                customer.setPhone(customerPhone.getText());
                needsUpdate = true;
            }

            if (needsUpdate) {
                boolean updated = customerService.updateCustomer(customer);
                if (!updated) {
                    return ServiceResponse.dbError("Failed to update customer information");
                }
            }

            return ServiceResponse.success(customer);
        } else {
            // Create new customer
            Customer customer = new Customer(
                    null,
                    customerFirstName.getText(),
                    customerLastName.getText(),
                    "", // Address is not collected in this form
                    customerPhone.getText(),
                    customerEmail.getText()
            );

            return customerService.createCustomer(customer);
        }
    }

    /**
     * Create or find an existing pet based on name and owner
     * @param owner the pet's owner
     * @return a ServiceResponse containing the Pet object or error
     */
    private ServiceResponse<Pet> createOrFindPet(Customer owner) {
        // First check if this customer already has a pet with this name
        ServiceResponse<List<Pet>> petsResponse = customerService.findPetsByCustomerId(owner.getID());

        if (petsResponse.getStatus() == LookupStatus.SUCCESS) {
            for (Pet existingPet : petsResponse.getData()) {
                if (existingPet.getName().equals(petName.getText())) {
                    // Pet exists, use it
                    Pet pet = existingPet;

                    // Update pet info if needed
                    boolean needsUpdate = false;

                    if (!pet.getSpecies().equals(petSpecies.getText())) {
                        pet.setSpecies(petSpecies.getText());
                        needsUpdate = true;
                    }

                    if (!pet.getBreed().equals(petBreed.getText())) {
                        pet.setBreed(petBreed.getText());
                        needsUpdate = true;
                    }

                    if (!pet.getBirthDate().equals(petBirthDate.getValue())) {
                        pet.setBirthDate(petBirthDate.getValue());
                        needsUpdate = true;
                    }

                    if (needsUpdate) {
                        boolean updated = customerService.updatePet(pet);
                        if (!updated) {
                            return ServiceResponse.dbError("Failed to update pet information");
                        }
                    }

                    return ServiceResponse.success(pet);
                }
            }
        }

        // Pet not found, create a new one
        Pet pet = new Pet(
                null,
                petName.getText(),
                petSpecies.getText(),
                petBreed.getText(),
                petBirthDate.getValue(),
                owner
        );

        return customerService.createPet(pet);
    }

    /**
     * Create a new appointment
     * @param pet the pet for the appointment
     * @return a ServiceResponse containing the Appointment object or error
     */
    private ServiceResponse<Appointment> createAppointment(Pet pet) {
        // Get selected provider
        Employee provider = providerSelector.getValue();
        if (provider == null) {
            return ServiceResponse.notFound("No provider selected");
        }

        // Parse time string to LocalTime
        String timeStr = appointmentTime.getValue();
        String[] timeParts = timeStr.split(":");
        LocalTime time = LocalTime.of(Integer.parseInt(timeParts[0]), Integer.parseInt(timeParts[1]));

        // Convert string appointment type to enum
        AppointmentType type = AppointmentType.valueOf(appointmentType.getValue());

        //create appointment object
        Appointment appointment = new Appointment(
                null,
                appointmentDate.getValue(),
                time,
                provider,
                type,
                pet,
                pet.getOwner()
        );

        //check if provider is qualified
        boolean isSlotTaken = appointmentService.isProviderSlotTaken(
                provider.getID(),
                appointmentDate.getValue(),
                time,
                null // No appointment ID to exclude since we're creating a new one
        );

        if (isSlotTaken) {
            return ServiceResponse.conflict("This time slot is already booked for the selected provider.");
        }

        //create appointment
        return appointmentService.createAppointment(appointment);
    }

    //commit changes
    private void closeForm() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    //alert
    private void showAlert(AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}