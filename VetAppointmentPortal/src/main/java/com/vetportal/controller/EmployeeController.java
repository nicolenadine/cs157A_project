package com.vetportal.controller;

import com.vetportal.model.*;
import com.vetportal.service.AppointmentService;
import com.vetportal.service.EmployeeService;
import com.vetportal.service.ServiceManager;
import com.vetportal.dto.ServiceResponse;
import com.vetportal.util.FXUtil;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EmployeeController {
    @FXML private TextField employeeLookupField;
    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;
    @FXML private Label addressLabel;
    @FXML private Label roleLabel;
    @FXML private Label appointmentsLabel;
    @FXML private Label enterPrompt;
    @FXML private Button searchButton;
    @FXML private Button editAppointmentButton;
    @FXML private Circle profileImage;
    @FXML private TableView<Appointment> appointmentsTableView;
    @FXML private TableColumn<Appointment, LocalDate> dateColumn;
    @FXML private TableColumn<Appointment, LocalTime> timeColumn;
    @FXML private TableColumn<Appointment, String> typeColumn;
    @FXML private TableColumn<Appointment, String> petColumn;
    @FXML private TableColumn<Appointment, String> providerColumn;

    private EmployeeService employeeService;
    private AppointmentService appointmentService;
    private Employee currentEmployee;

    @FXML
    public void initialize() {
        // Get service instances
        employeeService = ServiceManager.getInstance().getEmployeeService();
        appointmentService = ServiceManager.getInstance().getAppointmentService();

        // Set up double-click handler for appointments
        appointmentsTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // Double click
                handleAppointmentClick();
            }
        });

        // Hide employee information initially
        hideEmployeeInfo();

        // Set profile image
        setProfileImage();

        // Configure table columns
        configureAppointmentTable();
    }

    private void configureAppointmentTable() {
        // Set up cell value factories for appointment columns
        dateColumn.setCellValueFactory(cellData -> {
            Appointment appointment = cellData.getValue();
            return new SimpleObjectProperty<>(appointment.getDate());
        });

        timeColumn.setCellValueFactory(cellData -> {
            Appointment appointment = cellData.getValue();
            return new SimpleObjectProperty<>(appointment.getTime());
        });

        petColumn.setCellValueFactory(cellData -> {
            Appointment appointment = cellData.getValue();
            Pet pet = appointment.getPet();
            return new SimpleStringProperty(pet != null ? pet.getName() : "");
        });

        typeColumn.setCellValueFactory(cellData -> {
            Appointment appointment = cellData.getValue();
            AppointmentType type = appointment.getAppointmentType();
            return new SimpleStringProperty(type != null ? type.toString() : "");
        });

        providerColumn.setCellValueFactory(cellData -> {
            Appointment appointment = cellData.getValue();
            Employee provider = appointment.getProvider();
            return new SimpleStringProperty(provider != null ?
                    provider.getFirstName() + " " + provider.getLastName() : "");
        });
    }

    private void handleAppointmentClick() {
        Appointment selectedAppointment = appointmentsTableView.getSelectionModel().getSelectedItem();
        if (selectedAppointment != null) {
            // Open appointment details or edit dialog
            showAppointmentDetails(selectedAppointment);
        }
    }

    private void showAppointmentDetails(Appointment appointment) {
        // Create a dialog to show appointment details
        // This is a placeholder for now - you would implement similar to the CustomerController example
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Appointment Details");
        alert.setHeaderText("Appointment Information");

        String content = String.format(
                "Date: %s\nTime: %s\nPet: %s\nType: %s\nProvider: %s %s",
                appointment.getDate(),
                appointment.getTime(),
                appointment.getPet() != null ? appointment.getPet().getName() : "N/A",
                appointment.getAppointmentType() != null ? appointment.getAppointmentType().toString() : "N/A",
                appointment.getProvider() != null ? appointment.getProvider().getFirstName() : "",
                appointment.getProvider() != null ? appointment.getProvider().getLastName() : ""
        );

        alert.setContentText(content);
        alert.showAndWait();
    }

    private void setProfileImage() {
        // Load default profile image
        try {
            Image image = new Image(getClass().getResourceAsStream("/media/icon.png"));
            profileImage.setFill(new ImagePattern(image));
        } catch (Exception e) {
            System.err.println("Error loading profile image: " + e.getMessage());
            // Set a default color as fallback
            profileImage.setFill(javafx.scene.paint.Color.DODGERBLUE);
        }
    }

    private void hideEmployeeInfo() {
        // Make employee info elements invisible until an employee is found
        nameLabel.setVisible(false);
        emailLabel.setVisible(false);
        phoneLabel.setVisible(false);
        roleLabel.setVisible(false);
        addressLabel.setVisible(false);
        appointmentsLabel.setVisible(false);
        appointmentsTableView.setVisible(false);
        editAppointmentButton.setVisible(false);
        profileImage.setVisible(false);
    }

    @FXML
    private void handleEmployeeById() {
        // Get the employee ID from the text field
        String employeeIdText = employeeLookupField.getText();

        if (employeeIdText == null || employeeIdText.trim().isEmpty()) {
            showAlert("Please enter an Employee ID");
            return;
        }

        try {
            int employeeId = Integer.parseInt(employeeIdText);
            fetchEmployeeById(employeeId);
        } catch (NumberFormatException e) {
            showAlert("Please enter a valid numeric Employee ID");
        }
    }

    private void fetchEmployeeById(int employeeId) {
        ServiceResponse<Employee> response = employeeService.findEmployeeByID(employeeId);

        if (response.isSuccess()) {
            currentEmployee = response.getData();
            displayEmployeeInfo(currentEmployee);
        } else {
            showAlert(response.getMessage());
            hideEmployeeInfo();
        }
    }

    private void loadAppointmentsForEmployee(int employeeId) {
        ServiceResponse<List<Appointment>> response = appointmentService.findAppointmentsByProviderId(employeeId);

        if (response.isSuccess()) {
            List<Appointment> appointments = response.getData();
            ObservableList<Appointment> appointmentData = FXCollections.observableArrayList(appointments);
            appointmentsTableView.setItems(appointmentData);

            // Show a message if no appointments are found
            if (appointments.isEmpty()) {
                appointmentsLabel.setText("This employee has no appointments.");
            } else {
                appointmentsLabel.setText("Appointments:");
            }
        } else {
            // Clear the table if there was an error
            appointmentsTableView.setItems(FXCollections.observableArrayList());
            showAlert("Error loading appointments: " + response.getMessage());
        }
    }

    private void displayEmployeeInfo(Employee employee) {
        // Make employee elements visible
        nameLabel.setVisible(true);
        emailLabel.setVisible(true);
        phoneLabel.setVisible(true);
        roleLabel.setVisible(true);
        addressLabel.setVisible(true);
        appointmentsLabel.setVisible(true);
        appointmentsTableView.setVisible(true);
        editAppointmentButton.setVisible(true);
        profileImage.setVisible(true);

        // Remove lookup field, label, and button
        enterPrompt.setVisible(false);
        employeeLookupField.setVisible(false);
        searchButton.setVisible(false);

        // Update labels with employee data
        nameLabel.setText("Name: " + employee.getFirstName() + " " + employee.getLastName());
        emailLabel.setText("Email: " + employee.getEmail());
        phoneLabel.setText("Phone #: " + employee.getPhone());
        roleLabel.setText("Role: " + employee.getRole().toString());
        addressLabel.setText("Address: " + employee.getAddress());

        // Load appointments for this employee
        loadAppointmentsForEmployee(employee.getID());
    }

    //might remove
    @FXML
    private void handleEditAppointment() {
        Appointment selectedAppointment = appointmentsTableView.getSelectionModel().getSelectedItem();
        if (selectedAppointment != null) {
            showAppointmentDetails(selectedAppointment);
        } else {
            showAlert("Please select an appointment to edit");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Employee Lookup");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}