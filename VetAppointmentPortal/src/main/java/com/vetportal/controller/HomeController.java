package com.vetportal.controller;

import com.vetportal.dto.ServiceResponse;
import com.vetportal.model.Appointment;
import com.vetportal.model.Customer;
import com.vetportal.model.Employee;
import com.vetportal.model.Pet;
import com.vetportal.service.AppointmentService;
import com.vetportal.service.CustomerService;
import com.vetportal.service.EmployeeService;
import com.vetportal.service.ServiceManager;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class HomeController {

    @FXML private TableView<Appointment> todayTable;
    @FXML private TableColumn<Appointment, Integer> Appointment_ID;
    @FXML private TableColumn<Appointment, LocalDate> Date;
    @FXML private TableColumn<Appointment, LocalTime> Time;
    @FXML private TableColumn<Appointment, String> Provider;
    @FXML private TableColumn<Appointment, String> pet;

    @FXML private TableView<Customer> customersTable;
    @FXML private TableColumn<Customer, String> customerNameColumn;
    @FXML private TableColumn<Customer, String> customerPhoneColumn;
    @FXML private TableColumn<Customer, String> customerEmailColumn;

    @FXML private TableView<Employee> employeesTable;
    @FXML private TableColumn<Employee, String> employeeNameColumn;
    @FXML private TableColumn<Employee, String> roleColumn;
    @FXML private TableColumn<Employee, String> employeePhoneColumn;

    @FXML private DatePicker todayDatePicker;
    @FXML private Label todayLabel;

    private AppointmentService appointmentService;
    private CustomerService customerService;
    private EmployeeService employeeService;

    @FXML
    public void initialize() {
        // Get the AppointmentService instance
        ServiceManager serviceManager = ServiceManager.getInstance();
        appointmentService = serviceManager.getAppointmentService();

        // Get connection and create services for customer and employee
        Connection connection = serviceManager.getConnection();
        customerService = new CustomerService(connection);
        employeeService = new EmployeeService(connection);

        // Set up all tables
        setupAppointmentTable();
        setupCustomerTable();
        setupEmployeeTable();

        // Set the date picker to today's date
        LocalDate today = LocalDate.now();
        todayDatePicker.setValue(today);

        // Load data for all tables
        loadTodayAppointments(today);
        loadCustomers();
        loadEmployees();

        // Add listener to date picker
        todayDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                loadTodayAppointments(newValue);
            }
        });
    }

    private void setupAppointmentTable() {
        // Today's Appointments table
        Appointment_ID.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getID()));

        Date.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getDate()));

        Time.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getTime()));

        Provider.setCellValueFactory(cellData -> {
            Employee provider = cellData.getValue().getProvider();
            return new SimpleStringProperty(provider != null ?
                    provider.getFirstName() + " " + provider.getLastName() : "");
        });

        pet.setCellValueFactory(cellData -> {
            Pet p = cellData.getValue().getPet();
            return new SimpleStringProperty(p != null ? p.getName() : "");
        });
    }

    private void setupCustomerTable() {
        // Set up customer table columns to display the relevant data
        customerNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFirstName() + " " + cellData.getValue().getLastName()));
        customerPhoneColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPhone()));
        customerEmailColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getEmail()));
    }

    private void setupEmployeeTable() {
        // Set up employee table columns
        employeeNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFirstName() + " " + cellData.getValue().getLastName()));
        roleColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getRole().toString()));
        employeePhoneColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPhone()));
    }

    private void loadTodayAppointments(LocalDate date) {
        todayLabel.setText("Appointments for " + date);

        // Load scheduled appointments
        ServiceResponse<List<Appointment>> response =
                appointmentService.findAppointmentsByDate(date);

        if (response.isSuccess()) {
            List<Appointment> appointments = response.getData();
            ObservableList<Appointment> appointmentData =
                    FXCollections.observableArrayList(appointments);
            todayTable.setItems(appointmentData);
        } else {
            todayTable.setItems(FXCollections.observableArrayList());
        }
    }

    private void loadCustomers() {
        try {
            // Get all employees to use their Service Response pattern
            ServiceResponse<List<Employee>> employeeResponse = employeeService.getAllEmployees();

            if (employeeResponse.isSuccess()) {
                List<Customer> allCustomers = customerService.getAllCustomers();
                if (allCustomers != null) {
                    ObservableList<Customer> customerData =
                            FXCollections.observableArrayList(allCustomers);
                    customersTable.setItems(customerData);
                } else {
                    System.err.println("Failed to load customers: No customers found");
                    customersTable.setItems(FXCollections.observableArrayList());
                }
            } else {
                System.err.println("Failed to initialize services");
                customersTable.setItems(FXCollections.observableArrayList());
            }
        } catch (Exception e) {
            System.err.println("Error loading customers: " + e.getMessage());
            e.printStackTrace();
            customersTable.setItems(FXCollections.observableArrayList());
        }
    }

    private void loadEmployees() {
        try {
            // Use the getAllEmployees method from EmployeeService
            ServiceResponse<List<Employee>> response = employeeService.getAllEmployees();

            if (response.isSuccess()) {
                List<Employee> employees = response.getData();
                ObservableList<Employee> employeeData =
                        FXCollections.observableArrayList(employees);
                employeesTable.setItems(employeeData);
            } else {
                System.err.println("Failed to load employees: " + response.getMessage());
                employeesTable.setItems(FXCollections.observableArrayList());
            }
        } catch (Exception e) {
            System.err.println("Error loading employees: " + e.getMessage());
            e.printStackTrace();
            employeesTable.setItems(FXCollections.observableArrayList());
        }
    }
}