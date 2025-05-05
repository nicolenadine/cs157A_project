package com.vetportal.controller;

import com.vetportal.dto.ServiceResponse;
import com.vetportal.model.Appointment;
import com.vetportal.model.Employee;
import com.vetportal.service.AppointmentService;
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
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.LocalDate;
import java.util.*;
import javafx.util.StringConverter;
import com.vetportal.dto.LookupStatus;

/**
 * Controller for searching, editing, and deleting existing appointments.
 */
public class AppointmentSearchController implements Initializable {

    @FXML
    private DatePicker datePicker;

    @FXML
    private ComboBox<Employee> providerComboBox;

    @FXML
    private TextField petIdTextField;

    @FXML
    private Button searchButton;

    @FXML
    private Button clearButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private TableView<Appointment> appointmentTable;

    @FXML
    private TableColumn<Appointment, Integer> idColumn;

    @FXML
    private TableColumn<Appointment, String> dateColumn;

    @FXML
    private TableColumn<Appointment, String> timeColumn;

    @FXML
    private TableColumn<Appointment, String> petNameColumn;

    @FXML
    private TableColumn<Appointment, String> ownerNameColumn;

    @FXML
    private TableColumn<Appointment, String> providerColumn;

    private AppointmentService appointmentService;
    private EmployeeService employeeService;
    private ObservableList<Appointment> appointmentList;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialize services
        ServiceManager serviceManager = ServiceManager.getInstance();
        appointmentService = serviceManager.getAppointmentService();
        employeeService = serviceManager.getEmployeeService();
        appointmentList = FXCollections.observableArrayList();

        // Initialize table columns
        idColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getID()).asObject());
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDate().toString()));
        timeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTime().toString()));
        petNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPet().getName()));
        ownerNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPet().getOwner().getLastName() + ", " +
                        cellData.getValue().getPet().getOwner().getFirstName()));
        providerColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getProvider().getLastName() + ", " +
                        cellData.getValue().getProvider().getFirstName()));

        // Set up table selection listener
        appointmentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            editButton.setDisable(!hasSelection);
            deleteButton.setDisable(!hasSelection);
        });

        // Disable edit and delete buttons initially
        editButton.setDisable(true);
        deleteButton.setDisable(true);

        // Load providers for combo box
        loadProviders();

        // Load all appointments initially
        loadAllAppointments();
    }

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
     * Loads all appointments into the table.
     */
    private void loadAllAppointments() {
        ServiceResponse<List<Appointment>> response = appointmentService.getAllAppointments();
        if (response.isSuccess()) {
            appointmentList.clear();
            appointmentList.addAll(response.getData());
            appointmentTable.setItems(appointmentList);
        } else {
            // If no appointments found, clear the table but don't show error
            if (response.getStatus() == LookupStatus.NOT_FOUND) {
                appointmentList.clear();
                appointmentTable.setItems(appointmentList);
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to load appointments", response.getMessage());
            }
        }
    }

    /**
     * Handles the search button action.
     */
    @FXML
    private void handleSearch(ActionEvent event) {
        LocalDate date = datePicker.getValue();
        Employee provider = providerComboBox.getValue();
        String petIdStr = petIdTextField.getText().trim();

        // Clear existing results
        appointmentList.clear();

        // Perform search based on populated fields
        if (date != null) {
            searchByDate(date);
        } else if (provider != null) {
            searchByProvider(provider.getID());
        } else if (!petIdStr.isEmpty()) {
            try {
                int petId = Integer.parseInt(petIdStr);
                searchByPet(petId);
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Input Error", "Invalid Pet ID",
                        "Please enter a valid numeric Pet ID.");
            }
        } else {
            // If no search criteria provided, load all appointments
            loadAllAppointments();
        }
    }

    /**
     * Searches appointments by date.
     */
    private void searchByDate(LocalDate date) {
        ServiceResponse<List<Appointment>> response = appointmentService.findAppointmentsByDate(date);
        handleSearchResponse(response, "date");
    }

    /**
     * Searches appointments by provider ID.
     */
    private void searchByProvider(int providerId) {
        ServiceResponse<List<Appointment>> response = appointmentService.findAppointmentsByProviderId(providerId);
        handleSearchResponse(response, "provider");
    }

    /**
     * Searches appointments by pet ID.
     */
    private void searchByPet(int petId) {
        ServiceResponse<List<Appointment>> response = appointmentService.findAppointmentsByPetId(petId);
        handleSearchResponse(response, "pet");
    }

    /**
     * Handles search response and updates the UI accordingly.
     */
    private void handleSearchResponse(ServiceResponse<List<Appointment>> response, String searchType) {
        if (response.isSuccess()) {
            appointmentList.addAll(response.getData());
            appointmentTable.setItems(appointmentList);
        } else {
            if (response.getStatus() == LookupStatus.NOT_FOUND) {
                showAlert(Alert.AlertType.INFORMATION, "No Results",
                        "No appointments found",
                        "No appointments found for the selected " + searchType + ".");
            } else {
                showAlert(Alert.AlertType.ERROR, "Search Error",
                        "Failed to search appointments",
                        response.getMessage());
            }
        }
    }

    /**
     * Handles the clear button action.
     */
    @FXML
    private void handleClear(ActionEvent event) {
        datePicker.setValue(null);
        providerComboBox.setValue(null);
        petIdTextField.clear();
        loadAllAppointments();
    }

    /**
     * Handles the edit button action.
     */
    @FXML
    private void handleEdit(ActionEvent event) {
        Appointment selectedAppointment = appointmentTable.getSelectionModel().getSelectedItem();
        if (selectedAppointment != null) {
            // Store the selected appointment ID in a singleton or static field that the edit controller can access
            AppointmentEditManager.setAppointmentToEdit(selectedAppointment);

            // Navigate to the edit appointment view
            FXUtil.setPage("/fxml/AppointmentEdit.fxml");
        } else {
            showAlert(Alert.AlertType.WARNING, "Selection Required",
                    "No Appointment Selected",
                    "Please select an appointment to edit.");
        }
    }

    /**
     * Handles the delete button action.
     */
    @FXML
    private void handleDelete(ActionEvent event) {
        Appointment selectedAppointment = appointmentTable.getSelectionModel().getSelectedItem();
        if (selectedAppointment != null) {
            // Confirm deletion
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Confirm Deletion");
            confirmDialog.setHeaderText("Delete Appointment");
            confirmDialog.setContentText("Are you sure you want to delete the appointment for " +
                    selectedAppointment.getPet().getName() + " on " +
                    selectedAppointment.getDate() + " at " +
                    selectedAppointment.getTime() + "?");

            Optional<ButtonType> result = confirmDialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Delete the appointment
                ServiceResponse<Boolean> response = appointmentService.deleteAppointment(selectedAppointment.getID());
                if (response.isSuccess() && response.getData()) {
                    appointmentList.remove(selectedAppointment);
                    showAlert(Alert.AlertType.INFORMATION, "Success",
                            "Appointment Deleted",
                            "The appointment has been successfully deleted.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error",
                            "Failed to Delete Appointment",
                            response.getMessage());
                }
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "Selection Required",
                    "No Appointment Selected",
                    "Please select an appointment to delete.");
        }
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

    /**
     * Static utility class to manage appointment editing between controllers.
     */
    public static class AppointmentEditManager {
        private static Appointment appointmentToEdit;

        public static void setAppointmentToEdit(Appointment appointment) {
            appointmentToEdit = appointment;
        }

        public static Appointment getAppointmentToEdit() {
            return appointmentToEdit;
        }
    }
}