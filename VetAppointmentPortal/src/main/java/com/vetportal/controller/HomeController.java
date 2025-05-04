package com.vetportal.controller;

import com.vetportal.dto.ServiceResponse;
import com.vetportal.model.Appointment;
import com.vetportal.model.Employee;
import com.vetportal.model.Pet;
import com.vetportal.service.AppointmentService;
import com.vetportal.service.ServiceManager;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

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

    @FXML private DatePicker todayDatePicker;
    @FXML private Label todayLabel;

    private AppointmentService appointmentService;

    @FXML
    public void initialize() {
        // Get the AppointmentService instance
        appointmentService = ServiceManager.getInstance().getAppointmentService();

        // Set up the appointment table
        setupAppointmentTable();

        // Set the date picker to today's date
        LocalDate today = LocalDate.now();
        todayDatePicker.setValue(today);

        // Load appointments for today
        loadTodayAppointments(today);

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

    private void loadTodayAppointments(LocalDate date) {
        // Update the label
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
            // Show error message if needed
        }
    }
}