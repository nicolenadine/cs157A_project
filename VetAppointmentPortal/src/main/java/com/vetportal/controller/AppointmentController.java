package com.vetportal.controller;

import com.vetportal.model.Appointment;
import com.vetportal.model.AppointmentType;
import com.vetportal.model.Customer;
import com.vetportal.model.Employee;
import com.vetportal.model.Pet;
import com.vetportal.service.AppointmentService;
import com.vetportal.service.EmployeeService;
import com.vetportal.service.ServiceManager;
import com.vetportal.dto.ServiceResponse;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class AppointmentController {

    @FXML private Label titleLabel;
    @FXML private DatePicker appointmentDate;
    @FXML private ComboBox<String> appointmentTime;
    @FXML private ComboBox<AppointmentType> appointmentType;
    @FXML private ComboBox<String> petSelector;
    @FXML private ComboBox<String> providerSelector;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Label customerInfoLabel;

    private Appointment currentAppointment;
    private Customer currentCustomer;
    private AppointmentService appointmentService;
    private EmployeeService employeeService;
    private List<Pet> customerPets;
    private List<Employee> availableProviders;
    private boolean isNewAppointment;

    public void initData(Appointment appointment, Customer customer) {
        this.currentCustomer = customer;
        this.currentAppointment = appointment;
        this.isNewAppointment = (appointment == null);

        // Load services
        appointmentService = ServiceManager.getInstance().getAppointmentService();
        employeeService = ServiceManager.getInstance().getEmployeeService();

        // Set customer information
        customerInfoLabel.setText("Customer: " + customer.getFirstName() + " " + customer.getLastName());

        // Set title based on mode
        if (isNewAppointment) {
            titleLabel.setText("New Appointment");
            currentAppointment = new Appointment(null, LocalDate.now(), LocalTime.of(9, 0), null, null, null, customer);
        } else {
            titleLabel.setText("Edit Appointment");
        }

        // Load options
        loadPets();
        loadProviders();
        loadAppointmentTypes();
        loadTimeSlots();

        // Set initial values if editing
        if (!isNewAppointment) {
            appointmentDate.setValue(currentAppointment.getDate());

            // Select the appropriate time in the combobox
            LocalTime time = currentAppointment.getTime();
            String timeString = String.format("%02d:%02d", time.getHour(), time.getMinute());
            appointmentTime.getSelectionModel().select(timeString);

            // Select the appointment type
            appointmentType.getSelectionModel().select(currentAppointment.getAppointmentType());

            // Select the pet
            if (currentAppointment.getPet() != null) {
                String petName = currentAppointment.getPet().getName();
                for (int i = 0; i < petSelector.getItems().size(); i++) {
                    if (petSelector.getItems().get(i).startsWith(petName + " (")) {
                        petSelector.getSelectionModel().select(i);
                        break;
                    }
                }
            }

            // Select the provider
            if (currentAppointment.getProvider() != null) {
                Employee provider = currentAppointment.getProvider();
                String providerName = provider.getFirstName() + " " + provider.getLastName();
                for (int i = 0; i < providerSelector.getItems().size(); i++) {
                    if (providerSelector.getItems().get(i).startsWith(providerName + " (")) {
                        providerSelector.getSelectionModel().select(i);
                        break;
                    }
                }
            }
        }

        // Set up event handlers
        saveButton.setOnAction(event -> saveAppointment());
        cancelButton.setOnAction(event -> closeDialog());

        // Add listeners to validate date/time availability
        appointmentDate.valueProperty().addListener((obs, oldVal, newVal) -> checkProviderAvailability());
        appointmentTime.valueProperty().addListener((obs, oldVal, newVal) -> checkProviderAvailability());
        providerSelector.valueProperty().addListener((obs, oldVal, newVal) -> checkProviderAvailability());
    }

    private void loadPets() {
        ServiceResponse<List<Pet>> response = ServiceManager.getInstance().getCustomerService()
                .findPetsByCustomerId(currentCustomer.getID());

        if (response.isSuccess()) {
            customerPets = response.getData();
            ObservableList<String> petOptions = FXCollections.observableArrayList();

            for (Pet pet : customerPets) {
                petOptions.add(pet.getName() + " (" + pet.getSpecies() + " - " + pet.getBreed() + ")");
            }

            petSelector.setItems(petOptions);

            // Select first pet by default for new appointments
            if (!petOptions.isEmpty() && isNewAppointment) {
                petSelector.getSelectionModel().select(0);
            }
        }
    }

    private void loadProviders() {
        // Get all veterinarians using the findEmployeesByRole method
        ServiceResponse<List<Employee>> response = employeeService.findEmployeesByRole(Employee.Position.VETERINARIAN);

        if (response.isSuccess()) {
            availableProviders = response.getData();
            ObservableList<String> providerOptions = FXCollections.observableArrayList();

            for (Employee provider : availableProviders) {
                providerOptions.add(provider.getFirstName() + " " + provider.getLastName());
            }

            providerSelector.setItems(providerOptions);

            // Select first provider by default for new appointments
            if (!providerOptions.isEmpty() && isNewAppointment) {
                providerSelector.getSelectionModel().select(0);
            }
        } else {
            // Handle the case when no veterinarians are found
            showAlert("No veterinarians available. Please add veterinarians to the system first.");
        }
    }

    private void loadAppointmentTypes() {
        // Assuming AppointmentType is an enum
        appointmentType.setItems(FXCollections.observableArrayList(AppointmentType.values()));

        // Select first type by default for new appointments
        if (isNewAppointment) {
            appointmentType.getSelectionModel().select(0);
        }
    }

    private void loadTimeSlots() {
        // Create time slots from 8:00 to 17:30 in 30-minute increments
        ObservableList<String> timeSlots = FXCollections.observableArrayList();

        for (int hour = 8; hour < 18; hour++) {
            timeSlots.add(String.format("%02d:00", hour));
            if (hour < 17) {
                timeSlots.add(String.format("%02d:30", hour));
            }
        }

        appointmentTime.setItems(timeSlots);

        // Select a default time for new appointments
        if (isNewAppointment) {
            appointmentTime.getSelectionModel().select("09:00");
        }
    }

    private void checkProviderAvailability() {
        // Skip if not all values are selected
        if (appointmentDate.getValue() == null ||
                appointmentTime.getValue() == null ||
                providerSelector.getSelectionModel().isEmpty()) {
            return;
        }

        // Get selected values
        LocalDate date = appointmentDate.getValue();
        String timeString = appointmentTime.getValue();
        int providerIndex = providerSelector.getSelectionModel().getSelectedIndex();

        if (providerIndex >= 0 && providerIndex < availableProviders.size()) {
            Employee provider = availableProviders.get(providerIndex);

            // Parse time
            String[] timeParts = timeString.split(":");
            LocalTime time = LocalTime.of(
                    Integer.parseInt(timeParts[0]),
                    Integer.parseInt(timeParts[1])
            );

            // Check availability
            Integer excludeAppointmentId = isNewAppointment ? null : currentAppointment.getID();
            boolean isSlotTaken = appointmentService.isProviderSlotTaken(
                    provider.getID(), date, time, excludeAppointmentId
            );

            if (isSlotTaken) {
                showAlert("This time slot is already booked for the selected provider. Please choose another time or provider.");
            }
        }
    }

    private void saveAppointment() {
        // Validate all required fields
        if (appointmentDate.getValue() == null ||
                appointmentTime.getValue() == null ||
                appointmentType.getSelectionModel().isEmpty() ||
                petSelector.getSelectionModel().isEmpty() ||
                providerSelector.getSelectionModel().isEmpty()) {

            showAlert("Please fill in all required fields.");
            return;
        }

        // Get selected values
        LocalDate date = appointmentDate.getValue();
        String timeString = appointmentTime.getValue();
        AppointmentType type = appointmentType.getValue();
        int petIndex = petSelector.getSelectionModel().getSelectedIndex();
        int providerIndex = providerSelector.getSelectionModel().getSelectedIndex();

        // Parse time
        String[] timeParts = timeString.split(":");
        LocalTime time = LocalTime.of(
                Integer.parseInt(timeParts[0]),
                Integer.parseInt(timeParts[1])
        );

        // Set values on appointment
        currentAppointment.setDate(date);
        currentAppointment.setTime(time);
        currentAppointment.setAppointmentType(type);
        currentAppointment.setPet(customerPets.get(petIndex));
        currentAppointment.setProvider(availableProviders.get(providerIndex));
        currentAppointment.setCustomer(currentCustomer);

        // Save appointment
        ServiceResponse<Appointment> response;
        if (isNewAppointment) {
            response = appointmentService.createAppointment(currentAppointment);
        } else {
            response = appointmentService.updateAppointment(currentAppointment);
        }

        if (response.isSuccess()) {
            closeDialog();
        } else {
            showAlert("Error saving appointment: " + response.getMessage());
        }
    }

    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Appointment");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}