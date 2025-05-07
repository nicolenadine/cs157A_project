package com.vetportal.controller;

import com.vetportal.model.*;
import com.vetportal.service.AppointmentService;
import com.vetportal.service.EmployeeService;
import com.vetportal.service.ServiceManager;
import com.vetportal.dto.ServiceResponse;

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
import javafx.scene.control.Dialog;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;


import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;


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
    @FXML private Button addNewEmployeeButton;

    @FXML private Button editEmployeeButton;
    @FXML private Button deleteEmployeeButton;

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

        if (addNewEmployeeButton != null) {
            addNewEmployeeButton.setOnAction(event -> handleAddNewEmployee());
        }


        if (editEmployeeButton != null) {
            editEmployeeButton.setOnAction(event -> handleEditEmployee());
        }

        if (deleteEmployeeButton != null) {
            deleteEmployeeButton.setOnAction(event -> handleDeleteEmployee());
        }



        // Hide employee information initially
        hideEmployeeInfo();

        // Set profile image
        setProfileImage();

        // Configure table columns
        configureAppointmentTable();
    }

    @FXML
    private void handleAddNewEmployee() {
        Employee newEmployee = showEmployeeDialog(null, "Add New Employee");
        if (newEmployee != null) {
            // Create the employee using the service
            ServiceResponse<Employee> response = employeeService.createEmployee(newEmployee);

            if (response.isSuccess()) {
                // If successful, set as current employee and display info
                currentEmployee = response.getData();
                displayEmployeeInfo(currentEmployee);

                // Show success message
                showAlert("Employee added successfully!");
            } else {
                // Show error message
                showAlert("Error adding employee: " + response.getMessage());
            }
        }
    }

    @FXML
    private void handleEditEmployee() {
        if (currentEmployee == null) {
            showAlert("No employee selected");
            return;
        }

        Employee updatedEmployee = showEmployeeDialog(currentEmployee, "Edit Employee");
        if (updatedEmployee != null) {
            // Preserve the ID from the current employee
            updatedEmployee.setID(currentEmployee.getID());

            // Update the employee using the service
            boolean success = employeeService.updateEmployee(updatedEmployee);

            if (success) {
                // Update the current employee reference
                currentEmployee = updatedEmployee;

                // Refresh the employee info display
                displayEmployeeInfo(currentEmployee);

                // Show success message
                showAlert("Employee updated successfully!");
            } else {
                // Show error message
                showAlert("Error updating employee information");
            }
        }
    }

    @FXML
    private void handleDeleteEmployee() {
        if (currentEmployee == null) {
            showAlert("No employee selected");
            return;
        }

        // Show confirmation dialog
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Employee");
        confirmDialog.setHeaderText("Are you sure you want to delete " +
                currentEmployee.getFirstName() + " " + currentEmployee.getLastName() + "?");
        confirmDialog.setContentText("This action cannot be undone.");

        java.util.Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Call the delete method in the service
            boolean success = employeeService.deleteEmployee(currentEmployee.getID());

            if (success) {
                // Reset UI to initial state
                currentEmployee = null;
                hideEmployeeInfo();

                // Show the lookup field again
                enterPrompt.setVisible(true);
                employeeLookupField.setVisible(true);
                searchButton.setVisible(true);

                // Clear the lookup field
                employeeLookupField.setText("");

                showAlert("Employee deleted successfully!");
            } else {
                showAlert("Error deleting employee");
            }
        }
    }

    private Employee showEmployeeDialog(Employee employee, String title) {
        boolean isEdit = employee != null;

        // Create the custom dialog
        Dialog<Employee> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(isEdit ? "Edit employee information:" : "Enter employee information:");

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create the grid and set the dialog pane
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        // Create text fields for employee info
        TextField firstNameField = new TextField();
        firstNameField.setPromptText("First Name");
        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Last Name");
        TextField addressField = new TextField();
        addressField.setPromptText("Address");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        // Create role dropdown
        ComboBox<Employee.Position> roleComboBox = new ComboBox<>();
        roleComboBox.setItems(FXCollections.observableArrayList(Employee.Position.values()));
        roleComboBox.setPromptText("Role");

        // If editing, populate fields with current values
        if (isEdit) {
            firstNameField.setText(employee.getFirstName());
            lastNameField.setText(employee.getLastName());
            addressField.setText(employee.getAddress());
            phoneField.setText(employee.getPhone());
            emailField.setText(employee.getEmail());
            roleComboBox.setValue(employee.getRole());
        }

        // Add labels and fields to the grid
        grid.add(new Label("First Name:*"), 0, 0);
        grid.add(firstNameField, 1, 0);
        grid.add(new Label("Last Name:*"), 0, 1);
        grid.add(lastNameField, 1, 1);
        grid.add(new Label("Address:*"), 0, 2);
        grid.add(addressField, 1, 2);
        grid.add(new Label("Phone:*"), 0, 3);
        grid.add(phoneField, 1, 3);
        grid.add(new Label("Email:*"), 0, 4);
        grid.add(emailField, 1, 4);
        grid.add(new Label("Role:*"), 0, 5);
        grid.add(roleComboBox, 1, 5);

        // Add a label explaining required fields
        Label requiredFieldsLabel = new Label("* All fields are required");
        requiredFieldsLabel.setStyle("-fx-font-size: 10px; -fx-font-style: italic;");
        grid.add(requiredFieldsLabel, 0, 6, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the first field
        javafx.application.Platform.runLater(() -> firstNameField.requestFocus());

        // Add validation to the Save button
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            // Check if any fields are empty
            if (firstNameField.getText().trim().isEmpty() ||
                    lastNameField.getText().trim().isEmpty() ||
                    addressField.getText().trim().isEmpty() ||
                    phoneField.getText().trim().isEmpty() ||
                    emailField.getText().trim().isEmpty() ||
                    roleComboBox.getValue() == null) {

                // Highlight empty fields with red borders
                if (firstNameField.getText().trim().isEmpty()) {
                    firstNameField.setStyle("-fx-border-color: red;");
                } else {
                    firstNameField.setStyle("");
                }

                if (lastNameField.getText().trim().isEmpty()) {
                    lastNameField.setStyle("-fx-border-color: red;");
                } else {
                    lastNameField.setStyle("");
                }

                if (addressField.getText().trim().isEmpty()) {
                    addressField.setStyle("-fx-border-color: red;");
                } else {
                    addressField.setStyle("");
                }

                if (phoneField.getText().trim().isEmpty()) {
                    phoneField.setStyle("-fx-border-color: red;");
                } else {
                    phoneField.setStyle("");
                }

                if (emailField.getText().trim().isEmpty()) {
                    emailField.setStyle("-fx-border-color: red;");
                } else {
                    emailField.setStyle("");
                }

                if (roleComboBox.getValue() == null) {
                    roleComboBox.setStyle("-fx-border-color: red;");
                } else {
                    roleComboBox.setStyle("");
                }

                // Show the error message
                showAlert("All fields must be filled!");

                // Consume the event to prevent the dialog from closing
                event.consume();
            }
        });

        // Convert the result to an employee when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // Create employee object - validation is already handled by the event filter
                Employee result = new Employee(
                        null, // ID will be set later if this is an edit
                        firstNameField.getText().trim(),
                        lastNameField.getText().trim(),
                        addressField.getText().trim(),
                        phoneField.getText().trim(),
                        emailField.getText().trim(),
                        roleComboBox.getValue()
                );
                return result;
            }
            return null;
        });

        // Show the dialog and wait for response
        java.util.Optional<Employee> result = dialog.showAndWait();
        return result.orElse(null);
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

        Label noAppointmentsLabel = new Label("No appointments found for this employee");
        noAppointmentsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #757575;");
        appointmentsTableView.setPlaceholder(noAppointmentsLabel);
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
            Image image = new Image(getClass().getResourceAsStream("/images/icon.png"));
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
        profileImage.setVisible(false);
        editEmployeeButton.setVisible(false);
        deleteEmployeeButton.setVisible(false);
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
        profileImage.setVisible(true);
        editEmployeeButton.setVisible(true);
        deleteEmployeeButton.setVisible(true);

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

    private void showAlert(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Employee Lookup");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}