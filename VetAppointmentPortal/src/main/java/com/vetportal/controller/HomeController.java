package com.vetportal.controller;

import com.vetportal.model.Appointment;
import com.vetportal.model.Employee;
import com.vetportal.model.Pet;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;


public class HomeController {

    @FXML private TableView<Appointment> AppointmentTable;
    @FXML private TableColumn<Appointment, Integer> Appointment_ID;
    @FXML private TableColumn<Appointment, String> Date;
    @FXML private TableColumn<Appointment, String> Time;
    @FXML private TableColumn<Appointment, Employee> Provider;
    @FXML private TableColumn<Appointment, Pet> pet;

    @FXML private TableView<Appointment> availableTable;
    @FXML private TableColumn<Appointment, Integer> Available_ID;
    @FXML private TableColumn<Appointment, String> Available_Date;
    @FXML private TableColumn<Appointment, String> Available_Time;
    @FXML private TableColumn<Appointment, Employee> Available_Provider;
    @FXML private TableColumn<Appointment, Pet> Available_Pet;



/*
    public void initialize() {
        // Today's Appointments table
        Appointment_ID.setCellValueFactory(new PropertyValueFactory<>("ID"));
        Date.setCellValueFactory(new PropertyValueFactory<>("Date"));
        Time.setCellValueFactory(new PropertyValueFactory<>("Time"));
        Provider.setCellValueFactory(new PropertyValueFactory<>("provider"));
        pet.setCellValueFactory(new PropertyValueFactory<>("pet"));

        // Available Appointments table
        Available_ID.setCellValueFactory(new PropertyValueFactory<>("ID"));
        Available_Date.setCellValueFactory(new PropertyValueFactory<>("Date"));
        Available_Time.setCellValueFactory(new PropertyValueFactory<>("Time"));
        Available_Provider.setCellValueFactory(new PropertyValueFactory<>("provider"));
        Available_Pet.setCellValueFactory(new PropertyValueFactory<>("pet"));


    }
*/

}
