package com.vetportal.controller;

import com.vetportal.dto.ServiceResponse;
import com.vetportal.model.Appointment;
import com.vetportal.model.Employee;
import com.vetportal.model.Pet;
import com.vetportal.service.AppointmentService;
import com.vetportal.service.CustomerService;
import com.vetportal.service.EmployeeService;
import com.vetportal.service.ServiceManager;
import com.vetportal.util.FXUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for editing appointments.
 */
public class AppointmentEditController implements Initializable {

    @FXML
    private TextField appointmentIdField;

    @FXML
    private DatePicker datePicker;

    @FXML
    private ComboBox<String> timeComboBox;

    @FXML
    private ComboBox<Pet> petComboBox;

    @FXML
    private ComboBox<Employee> providerComboBox;

    // Status and notes fields removed as they are not supported

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private AppointmentService appointmentService;
    private EmployeeService employeeService;
    private CustomerService customerService;
    private Appointment appointment;
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialize services
        ServiceManager serviceManager = ServiceManager.getInstance();
        appointmentService = serviceManager.getAppointmentService();
        employeeService = serviceManager.getEmployeeService();
        customerService = serviceManager.getCustomerService();

        // Set up time combo box with appointment time slots (30 minute increments)
        setupTimeComboBox();

        // Load providers
        loadProviders();

        // Load pets
        loadPets();

        // Get appointment to edit
        appointment = AppointmentSearchController.AppointmentEditManager.getAppointmentToEdit();
        if (appointment != null) {
            populateFields();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "No Appointment Selected",
                    "No appointment was selected for editing.");
            handleCancel(null);
        }
    }

    /**
     * Sets up the time combo box with 30-minute increments.
     */
    private void setupTimeComboBox() {
        ObservableList<String> timeSlots = FXCollections.observableArrayList();

        // Create time slots from 8:00 to 18:00 in 30-minute increments
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime endTime = LocalTime.of(18, 0);

        while (!startTime.isAfter(endTime)) {
            timeSlots.add(startTime.format(timeFormatter));
            startTime = startTime.plusMinutes(30);
        }

        timeComboBox.setItems(timeSlots);
    }

    // Status combo box setup method removed as it is not supported

    /**
     * Loads all providers into the provider combo box.
     */
    private void loadProviders() {
        ServiceResponse<List<Employee>> response = employeeService.getAllEmployees();
        if (response.isSuccess()) {
            providerComboBox.setItems(FXCollections.observableArrayList(response.getData()));
            providerComboBox.setConverter(new StringConverter<Employee>() {
                @Override
                public String toString(Employee employee) {
                    if (employee == null) {
                        return null;
                    }
                    return employee.getLastName() + ", " + employee.getFirstName();
                }

                @Override
                public Employee fromString(String string) {
                    return null; // Not needed for this implementation
                }
            });
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load providers", response.getMessage());
        }
    }

    /**
     * Loads all pets into the pet combo box.
     */
    private void loadPets() {
        // This would typically be filtered based on the customer,
        // but for now we'll load all pets for simplicity
        ObservableList<Pet> pets = FXCollections.observableArrayList();

        // For now, use the pet from the existing appointment
        if (appointment != null && appointment.getPet() != null) {
            pets.add(appointment.getPet());
        }

        petComboBox.setItems(pets);
        petComboBox.setConverter(new StringConverter<Pet>() {
            @Override
            public String toString(Pet pet) {
                if (pet == null) {
                    return null;
                }
                return pet.getName() + " (" + pet.getOwner().getLastName() + ", " + pet.getOwner().getFirstName() + ")";
            }

            @Override
            public Pet fromString(String string) {
                return null; // Not needed for this implementation
            }
        });
    }

    /**
     * Populates form fields with data from the appointment to edit.
     */
    private void populateFields() {
        appointmentIdField.setText(String.valueOf(appointment.getID()));
        datePicker.setValue(appointment.getDate());
        timeComboBox.setValue(appointment.getTime().format(timeFormatter));

        // Select the correct pet
        for (Pet pet : petComboBox.getItems()) {
            if (pet.getID().equals(appointment.getPet().getID())) {
                petComboBox.setValue(pet);
                break;
            }
        }

        // Select the correct provider
        for (Employee provider : providerComboBox.getItems()) {
            if (provider.getID().equals(appointment.getProvider().getID())) {
                providerComboBox.setValue(provider);
                break;
            }
        }

        // Status and notes fields removed as they are not supported
    }

    /**
     * Handles the save button action.
     */
    @FXML
    private void handleSave(ActionEvent event) {
        // Validate input fields
        if (!validateInputs()) {
            return;
        }

        // Update the appointment with form data
        LocalDate date = datePicker.getValue();
        LocalTime time = LocalTime.parse(timeComboBox.getValue(), timeFormatter);
        Pet pet = petComboBox.getValue();
        Employee provider = providerComboBox.getValue();

        // Check if the provider is already booked at this time (excluding this appointment)
        if (appointmentService.isProviderSlotTaken(provider.getID(), date, time, appointment.getID())) {
            showAlert(Alert.AlertType.ERROR, "Scheduling Conflict",
                    "Provider Already Booked",
                    "The selected provider already has an appointment at this time.");
            return;
        }

        // Update appointment object
        appointment.setDate(date);
        appointment.setTime(time);
        appointment.setPet(pet);
        appointment.setProvider(provider);

        // Save the updated appointment
        ServiceResponse<Appointment> response = appointmentService.updateAppointment(appointment);
        if (response.isSuccess()) {
            showAlert(Alert.AlertType.INFORMATION, "Success",
                    "Appointment Updated",
                    "The appointment has been successfully updated.");

            // Return to the appointment search view
            FXUtil.setPage("/fxml/AppointmentSearch.fxml");
        } else {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to Update Appointment",
                    response.getMessage());
        }
    }

    /**
     * Validates the input fields.
     *
     * @return true if all inputs are valid, false otherwise
     */
    private boolean validateInputs() {
        StringBuilder errors = new StringBuilder();

        if (datePicker.getValue() == null) {
            errors.append("- Date is required\n");
        }

        if (timeComboBox.getValue() == null) {
            errors.append("- Time is required\n");
        }

        if (petComboBox.getValue() == null) {
            errors.append("- Pet is required\n");
        }

        if (providerComboBox.getValue() == null) {
            errors.append("- Provider is required\n");
        }

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.ERROR, "Validation Error",
                    "Please correct the following errors:",
                    errors.toString());
            return false;
        }

        return true;
    }

    /**
     * Handles the cancel button action.
     */
    @FXML
    private void handleCancel(ActionEvent event) {
        // Return to the appointment search view
        FXUtil.setPage("/fxml/AppointmentSearch.fxml");
    }

    /**
     * Shows an alert dialog with the specified parameters.
     */
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}