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
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CreateAppointmentController implements Initializable {

    // Customer Fields
    @FXML private TextField customerFirstName;
    @FXML private TextField customerLastName;
    @FXML private TextField customerEmail;
    @FXML private TextField customerPhone;

    // Pet Selection
    @FXML private ComboBox<Pet> petSelector;

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
    @FXML private Button lookupCustomerButton;

    // Services
    private CustomerService customerService;
    private EmployeeService employeeService;
    private AppointmentService appointmentService;

    // Database connection
    private Connection dbConnection;

    // State tracking
    private Customer selectedCustomer;
    private Pet selectedPet;
    private boolean isCustomerDataModified = false;
    private boolean isPetDataModified = false;

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

        // Setup customer lookup button
        setupCustomerLookup();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Setup appointment types
        setupAppointmentTypes();

        // Setup time slots
        setupTimeSlots();

        // Set default date to today
        appointmentDate.setValue(LocalDate.now());
        petBirthDate.setValue(LocalDate.now().minusYears(1)); // Default pet age to 1 year

        // Disable pet fields until a customer is selected
        setCustomerControlsEnabled(false);
        setPetControlsEnabled(false);

        // Add listeners for form field changes to track modifications
        setupFieldChangeListeners();

        // Add listener for appointment date changes to refresh available times
        appointmentDate.valueProperty().addListener((obs, oldVal, newVal) -> updateAvailableTimes());

        // Add listener for provider selection to refresh available times
        providerSelector.valueProperty().addListener((obs, oldVal, newVal) -> updateAvailableTimes());

        // Add listener for pet selection to populate pet fields
        petSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedPet = newVal;
                populatePetFields(selectedPet);
                setPetControlsEnabled(true);
            }
        });

        // Add listener to the save button
        saveButton.setOnAction(this::handleSaveButton);
    }

    /**
     * Set up the customer lookup functionality
     */
    private void setupCustomerLookup() {
        lookupCustomerButton.setOnAction(event -> showCustomerLookupDialog());
    }

    /**
     * Display the customer lookup dialog
     */
    private void showCustomerLookupDialog() {
        // Create a dialog
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Customer Lookup");
        dialog.setHeaderText("Enter customer phone number");

        // Set the button types
        ButtonType lookupButtonType = new ButtonType("Lookup", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(lookupButtonType, ButtonType.CANCEL);

        // Create the phone field and add it to the dialog
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone number");

        VBox content = new VBox(10);
        content.getChildren().addAll(new Label("Phone:"), phoneField);
        dialog.getDialogPane().setContent(content);

        // Request focus on the phone field by default
        phoneField.requestFocus();

        // Convert the result to a phone number when the lookup button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == lookupButtonType) {
                return phoneField.getText();
            }
            return null;
        });

        // Show the dialog and process the result
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.showAndWait().ifPresent(phoneNumber -> {
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                lookupCustomerByPhone(phoneNumber);
            }
        });
    }

    /**
     * Look up a customer by phone number
     * @param phone the phone number to search for
     */
    private void lookupCustomerByPhone(String phone) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("phone", phone);

        ServiceResponse<Customer> response = customerService.findCustomerByAttributes(attributes);

        if (response.getStatus() == LookupStatus.SUCCESS && response.getData() != null) {
            // Customer found, populate fields
            selectedCustomer = response.getData();
            populateCustomerFields(selectedCustomer);
            loadCustomerPets(selectedCustomer);
            setCustomerControlsEnabled(true);

            // Reset modification tracking
            isCustomerDataModified = false;

            // Display success message
            showAlert(AlertType.INFORMATION, "Customer Found",
                    "Customer information loaded",
                    "Customer " + selectedCustomer.getFirstName() + " " + selectedCustomer.getLastName() + " has been found.");
        } else {
            // Customer not found, allow creating a new one
            showAlert(AlertType.INFORMATION, "Customer Not Found",
                    "No customer found with that phone number",
                    "Please enter the customer information to create a new customer.");

            // Clear fields and enable them for a new customer
            clearCustomerFields();
            clearPetFields();
            setCustomerControlsEnabled(true);
            setPetControlsEnabled(true);
            petSelector.setItems(FXCollections.observableArrayList());
            selectedCustomer = null;
            selectedPet = null;
        }
    }

    /**
     * Load all pets for the selected customer
     * @param customer the customer to load pets for
     */
    private void loadCustomerPets(Customer customer) {
        if (customer == null || customer.getID() == null) {
            petSelector.setItems(FXCollections.observableArrayList());
            return;
        }

        ServiceResponse<List<Pet>> response = customerService.findPetsByCustomerId(customer.getID());

        if (response.getStatus() == LookupStatus.SUCCESS && response.getData() != null) {
            ObservableList<Pet> pets = FXCollections.observableArrayList(response.getData());
            petSelector.setItems(pets);

            // Set up cell factory to display pet names
            petSelector.setCellFactory(param -> new ListCell<Pet>() {
                @Override
                protected void updateItem(Pet pet, boolean empty) {
                    super.updateItem(pet, empty);
                    if (empty || pet == null) {
                        setText(null);
                    } else {
                        setText(pet.getName() + " (" + pet.getSpecies() + ")");
                    }
                }
            });

            // Set converter for displaying selected value
            petSelector.setConverter(new javafx.util.StringConverter<Pet>() {
                @Override
                public String toString(Pet pet) {
                    if (pet == null) {
                        return null;
                    }
                    return pet.getName() + " (" + pet.getSpecies() + ")";
                }

                @Override
                public Pet fromString(String string) {
                    return null;
                }
            });

            // If pets exist, select the first one
            if (!pets.isEmpty()) {
                petSelector.getSelectionModel().selectFirst();
                selectedPet = pets.get(0);
                populatePetFields(selectedPet);
                setPetControlsEnabled(true);
                isPetDataModified = false;
            } else {
                clearPetFields();
                setPetControlsEnabled(true);
                selectedPet = null;
            }
        } else {
            petSelector.setItems(FXCollections.observableArrayList());
            clearPetFields();
            setPetControlsEnabled(true);
            selectedPet = null;
        }
    }

    /**
     * Populate customer fields with data from a Customer object
     * @param customer the customer data to display
     */
    private void populateCustomerFields(Customer customer) {
        if (customer == null) return;

        customerFirstName.setText(customer.getFirstName());
        customerLastName.setText(customer.getLastName());
        customerEmail.setText(customer.getEmail());
        customerPhone.setText(customer.getPhone());
    }

    /**
     * Populate pet fields with data from a Pet object
     * @param pet the pet data to display
     */
    private void populatePetFields(Pet pet) {
        if (pet == null) return;

        petName.setText(pet.getName());
        petSpecies.setText(pet.getSpecies());
        petBreed.setText(pet.getBreed());
        petBirthDate.setValue(pet.getBirthDate());
    }

    /**
     * Clear all customer fields
     */
    private void clearCustomerFields() {
        customerFirstName.clear();
        customerLastName.clear();
        customerEmail.clear();
        customerPhone.clear();
    }

    /**
     * Clear all pet fields
     */
    private void clearPetFields() {
        petName.clear();
        petSpecies.clear();
        petBreed.clear();
        petBirthDate.setValue(LocalDate.now().minusYears(1));
    }

    /**
     * Enable or disable customer-related controls
     * @param enabled true to enable controls, false to disable
     */
    private void setCustomerControlsEnabled(boolean enabled) {
        customerFirstName.setEditable(enabled);
        customerLastName.setEditable(enabled);
        customerEmail.setEditable(enabled);
        customerPhone.setEditable(enabled);
        petSelector.setDisable(!enabled);
    }

    /**
     * Enable or disable pet-related controls
     * @param enabled true to enable controls, false to disable
     */
    private void setPetControlsEnabled(boolean enabled) {
        petName.setEditable(enabled);
        petSpecies.setEditable(enabled);
        petBreed.setEditable(enabled);
        petBirthDate.setDisable(!enabled);
    }

    /**
     * Setup change listeners for all form fields to track modifications
     */
    private void setupFieldChangeListeners() {
        // Customer field listeners
        customerFirstName.textProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedCustomer != null && !newVal.equals(selectedCustomer.getFirstName())) {
                isCustomerDataModified = true;
            }
        });

        customerLastName.textProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedCustomer != null && !newVal.equals(selectedCustomer.getLastName())) {
                isCustomerDataModified = true;
            }
        });

        customerEmail.textProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedCustomer != null && !newVal.equals(selectedCustomer.getEmail())) {
                isCustomerDataModified = true;
            }
        });

        customerPhone.textProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedCustomer != null && !newVal.equals(selectedCustomer.getPhone())) {
                isCustomerDataModified = true;
            }
        });

        // Pet field listeners
        petName.textProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedPet != null && !newVal.equals(selectedPet.getName())) {
                isPetDataModified = true;
            }
        });

        petSpecies.textProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedPet != null && !newVal.equals(selectedPet.getSpecies())) {
                isPetDataModified = true;
            }
        });

        petBreed.textProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedPet != null && !newVal.equals(selectedPet.getBreed())) {
                isPetDataModified = true;
            }
        });

        petBirthDate.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedPet != null && !newVal.equals(selectedPet.getBirthDate())) {
                isPetDataModified = true;
            }
        });
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
     * Setup initial time slots from 8 AM to 6 PM in 30-minute intervals
     * These will be filtered based on availability later
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
     * Update available appointment times based on selected date and provider
     */
    private void updateAvailableTimes() {
        LocalDate date = appointmentDate.getValue();
        Employee provider = providerSelector.getValue();

        if (date == null || provider == null) {
            return;
        }

        // Generate all possible time slots
        List<String> allTimeSlots = IntStream.rangeClosed(8, 17)
                .boxed()
                .flatMap(hour -> List.of(
                        String.format("%02d:00", hour),
                        String.format("%02d:30", hour)
                ).stream())
                .collect(Collectors.toList());

        // Create a filtered list of available times
        List<String> availableTimes = new ArrayList<>();

        for (String timeSlot : allTimeSlots) {
            String[] timeParts = timeSlot.split(":");
            LocalTime time = LocalTime.of(Integer.parseInt(timeParts[0]), Integer.parseInt(timeParts[1]));

            // Check if the slot is available (not taken)
            boolean isSlotTaken = appointmentService.isProviderSlotTaken(
                    provider.getID(),
                    date,
                    time,
                    null // No appointment ID to exclude since we're creating a new one
            );

            if (!isSlotTaken) {
                availableTimes.add(timeSlot);
            }
        }

        // Update the time combobox with available slots
        ObservableList<String> times = FXCollections.observableArrayList(availableTimes);
        appointmentTime.setItems(times);

        // Select the first available time if any exist
        if (!times.isEmpty()) {
            appointmentTime.getSelectionModel().selectFirst();
        }
    }

    /**
     * Handle the save button click event
     * @param event action event
     */
    private void handleSaveButton(ActionEvent event) {
        if (validateForm()) {
            try {
                // Create or update customer if needed
                ServiceResponse<Customer> customerResponse = createOrUpdateCustomer();
                if (customerResponse.getStatus() != LookupStatus.SUCCESS) {
                    showAlert(AlertType.ERROR, "Customer Error", "Failed to create/update customer",
                            customerResponse.getMessage());
                    return;
                }
                Customer customer = customerResponse.getData();

                // Create or update pet if needed
                ServiceResponse<Pet> petResponse = createOrUpdatePet(customer);
                if (petResponse.getStatus() != LookupStatus.SUCCESS) {
                    showAlert(AlertType.ERROR, "Pet Error", "Failed to create/update pet",
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
     * Create a new customer or update an existing one only if data has been modified
     * @return a ServiceResponse containing the Customer object or error
     */
    private ServiceResponse<Customer> createOrUpdateCustomer() {
        // If we have a selected customer and the data hasn't been modified, just return it
        if (selectedCustomer != null && !isCustomerDataModified) {
            return ServiceResponse.success(selectedCustomer);
        }

        // Otherwise, we need to create or update the customer
        Map<String, String> attributes = new HashMap<>();
        attributes.put("email", customerEmail.getText());

        ServiceResponse<Customer> response = customerService.findCustomerByAttributes(attributes);

        if (response.getStatus() == LookupStatus.SUCCESS) {
            // Customer exists, use it
            Customer customer = response.getData();

            // Update customer info if needed
            boolean needsUpdate = false;

            if (!customer.getFirstName().equals(customerFirstName.getText())) {
                customer.setFirstName(customerFirstName.getText());
                needsUpdate = true;
            }

            if (!customer.getLastName().equals(customerLastName.getText())) {
                customer.setLastName(customerLastName.getText());
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
     * Create a new pet or update an existing one only if data has been modified
     * @param owner the pet's owner
     * @return a ServiceResponse containing the Pet object or error
     */
    private ServiceResponse<Pet> createOrUpdatePet(Customer owner) {
        // If we have a selected pet and the data hasn't been modified, just return it
        if (selectedPet != null && !isPetDataModified) {
            return ServiceResponse.success(selectedPet);
        }

        // If a pet is selected from the dropdown, check if it needs updating
        if (selectedPet != null) {
            // Update pet info if needed
            boolean needsUpdate = false;

            if (!selectedPet.getName().equals(petName.getText())) {
                selectedPet.setName(petName.getText());
                needsUpdate = true;
            }

            if (!selectedPet.getSpecies().equals(petSpecies.getText())) {
                selectedPet.setSpecies(petSpecies.getText());
                needsUpdate = true;
            }

            if (!selectedPet.getBreed().equals(petBreed.getText())) {
                selectedPet.setBreed(petBreed.getText());
                needsUpdate = true;
            }

            if (!selectedPet.getBirthDate().equals(petBirthDate.getValue())) {
                selectedPet.setBirthDate(petBirthDate.getValue());
                needsUpdate = true;
            }

            if (needsUpdate) {
                boolean updated = customerService.updatePet(selectedPet);
                if (!updated) {
                    return ServiceResponse.dbError("Failed to update pet information");
                }
            }

            return ServiceResponse.success(selectedPet);
        }

        // Otherwise, check if the customer has a pet with this name already
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

        // Double-check if provider is still available (in case it changed while form was open)
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

    private void closeForm() {
        try {
            // Get only the current stage (appointment form window)
            Stage stage = (Stage) saveButton.getScene().getWindow();

            // Close only this stage, not the entire application otherwise app closes after appointment creation
            stage.close();

            System.out.println("Successfully closed appointment form window");
        } catch (Exception e) {
            System.err.println("Error closing form: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void showAlert(AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}