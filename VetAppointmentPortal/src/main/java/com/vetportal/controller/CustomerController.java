package com.vetportal.controller;

import com.vetportal.model.*;
import com.vetportal.service.AppointmentService;
import com.vetportal.service.ServiceManager;
import com.vetportal.service.CustomerService;
import com.vetportal.dto.ServiceResponse;
import com.vetportal.util.FXUtil;
import com.vetportal.util.CommonUtil;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;


import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CustomerController {
    @FXML private TextField customerLookupField;
    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;
    @FXML private Label petsLabel;
    @FXML private Label addressLabel;
    @FXML private Label appointmentsLabel;
    @FXML private Label enterPrompt;
    @FXML private Button searchButton;
    @FXML private Button editPetButton;
    @FXML private Button editAppointmentButton;
    @FXML private Circle customerPhoto;
    @FXML private TableView<Pet> petsTableView;
    @FXML private TableView<Appointment> appointmentsTableView;
    @FXML private TableColumn<Pet, String> nameColumn;
    @FXML private TableColumn<Pet, String> speciesColumn;
    @FXML private TableColumn<Pet, String> breedColumn;
    @FXML private TableColumn<Pet, LocalDate> birthDateColumn;
    @FXML private TableColumn<Appointment, LocalDate> dateColumn;
    @FXML private TableColumn<Appointment, LocalTime> timeColumn;
    @FXML private TableColumn<Appointment, String> typeColumn;
    @FXML private TableColumn<Appointment, String> petColumn;
    @FXML private TableColumn<Appointment, String> providerColumn;
    @FXML private VBox customerInfoContainer;
    @FXML private Circle profileImage;

    private CustomerService customerService;
    private Customer currentCustomer;

    @FXML
    public void initialize() {
        customerService = ServiceManager.getInstance().getCustomerService();

        // Set up double-click handler for appointments table
        appointmentsTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // Double click
                handleAppointmentClick();
            }
        });

        // Set up double-click handler for pets table
        petsTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // Double click
                handlePetClick();
            }
        });

        // Hide customer information initially
        hideCustomerInfo();
        setProfileImage();
    }

    private void handlePetClick() {
        Pet selectedPet = petsTableView.getSelectionModel().getSelectedItem();
        if (selectedPet != null) {
            // Open pet details dialog
            showPetDetails(selectedPet);
        }
    }

    // Add method to show pet details in a dialog
    private void showPetDetails(Pet pet) {
        // Create a dialog to show pet details
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Pet Details");
        alert.setHeaderText("Pet Information");

        String content = String.format(
                "Name: %s\nSpecies: %s\nBreed: %s\nBirth Date: %s\nOwner: %s %s",
                pet.getName(),
                pet.getSpecies(),
                pet.getBreed(),
                pet.getBirthDate(),
                currentCustomer.getFirstName(),
                currentCustomer.getLastName()
        );

        alert.setContentText(content);
        alert.showAndWait();
    }

    // Update the existing appointment click handler
    private void handleAppointmentClick() {
        Appointment selectedAppointment = appointmentsTableView.getSelectionModel().getSelectedItem();
        if (selectedAppointment != null) {
            // Open appointment details dialog
            showAppointmentDetails(selectedAppointment);
        }
    }

    // Update the existing appointment details method to use a simple alert dialog
    private void showAppointmentDetails(Appointment appointment) {
        // Create a dialog to show appointment details
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
        String imagePath = "/images/profile.png";

        // Create an image from the resource path
        Image image = new Image(getClass().getResourceAsStream("/media/icon.png"));

        profileImage.setFill(new ImagePattern(image));
    }

    private void hideCustomerInfo() {
        //make customer info elements invisible until a customer is found
        if (nameLabel != null) nameLabel.setVisible(false);
        if (emailLabel != null) emailLabel.setVisible(false);
        if (phoneLabel != null) phoneLabel.setVisible(false);
        if (petsLabel != null) petsLabel.setVisible(false);
        if (addressLabel != null) addressLabel.setVisible(false);
        if (appointmentsLabel != null) appointmentsLabel.setVisible(false);
        if (petsTableView != null) petsTableView.setVisible(false);
        if (appointmentsTableView != null) appointmentsTableView.setVisible(false);
        if (editAppointmentButton != null) editAppointmentButton.setVisible(false);
        if (editPetButton != null) editPetButton.setVisible(false);
        if (profileImage != null) profileImage.setVisible(false);
    }

    @FXML
    private void handleCustomerByPhone() {
        Customer customer = fetchCustomerByPhone();
        if (customer != null) {
            currentCustomer = customer;
            displayCustomerInfo(customer);
        } else {
            hideCustomerInfo();
        }
    }

    private Customer fetchCustomerByPhone() {
        String phone = customerLookupField.getText();

        if (phone == null || phone.trim().isEmpty()) {
            showAlert("Please enter a phone number");
            return null;
        }

        // Create a map with the phone field
        Map<String, String> fields = new HashMap<>();
        fields.put("phone", phone);

        ServiceResponse<Customer> response = customerService.findCustomerByAttributes(fields);

        if (response.isSuccess()) {
            return response.getData();  // gets the customer object if it exists
        } else {
            showAlert(response.getMessage());  // show the error
            return null;
        }
    }

    private void loadPets(int customerID) {
        ServiceResponse<List<Pet>> response = customerService.findPetsByCustomerId(customerID);

        if (response.isSuccess()) {
            List<Pet> pets = response.getData();
            ObservableList<Pet> petData = FXCollections.observableArrayList(pets);
            petsTableView.setItems(petData);

            // Show a message if no pets are found
            if (pets.isEmpty()) {
                petsLabel.setText("No pets found for this customer");
            } else {
                petsLabel.setText("Pets:");
            }
        } else {
            // Clear the table if there was an error
            petsTableView.setItems(FXCollections.observableArrayList());
            petsLabel.setText("Error loading pets");
            showAlert("Error loading pets: " + response.getMessage());
        }
    }

    private void fillPetsTable() {
        //set up cell value factories for pets table
        TableColumn<Pet, String> nameColumn = (TableColumn<Pet, String>) petsTableView.getColumns().get(0);
        TableColumn<Pet, String> speciesColumn = (TableColumn<Pet, String>) petsTableView.getColumns().get(1);
        TableColumn<Pet, String> breedColumn = (TableColumn<Pet, String>) petsTableView.getColumns().get(2);
        TableColumn<Pet, LocalDate> birthDateColumn = (TableColumn<Pet, LocalDate>) petsTableView.getColumns().get(3);

        nameColumn.setCellValueFactory(cellData -> {
            Pet pet = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(pet.getName());
        });

        speciesColumn.setCellValueFactory(cellData -> {
            Pet pet = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(pet.getSpecies());
        });

        breedColumn.setCellValueFactory(cellData -> {
            Pet pet = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(pet.getBreed());
        });

        birthDateColumn.setCellValueFactory(cellData -> {
            Pet pet = cellData.getValue();
            return new javafx.beans.property.SimpleObjectProperty<>(pet.getBirthDate());
        });
    }

    private void loadAppointmentsForCustomer(int CustomerID) {
        ServiceResponse<List<Pet>> petsResponse = customerService.findPetsByCustomerId(CustomerID);

        if (!petsResponse.isSuccess() || petsResponse.getData().isEmpty()) {
            //no pets or error getting pets
            appointmentsTableView.setItems(FXCollections.observableArrayList());
            appointmentsLabel.setText("No appointments found");
            return;
        }

        //initialize appointment list
        ObservableList<Appointment> allAppointments = FXCollections.observableArrayList();

        //get AppointmentService from ServiceManager
        AppointmentService appointmentService = ServiceManager.getInstance().getAppointmentService();

        //for each pet, get appointments and add to the list
        for (Pet pet : petsResponse.getData()) {
            ServiceResponse<List<Appointment>> response = appointmentService.findAppointmentsByPetId(pet.getID());

            if (response.isSuccess()) {
                allAppointments.addAll(response.getData());
            }
        }

        // Set items to the table
        appointmentsTableView.setItems(allAppointments);

        // Show a message if no appointments are found
        if (allAppointments.isEmpty()) {
            appointmentsLabel.setText("No appointments found for this customer");
        } else {
            appointmentsLabel.setText("Appointments:");
        }
    }

    private void fillAppointmentTable() {
        TableColumn<Appointment, LocalDate> dateColumn =
                (TableColumn<Appointment, LocalDate>) appointmentsTableView.getColumns().get(0);
        TableColumn<Appointment, LocalTime> timeColumn =
                (TableColumn<Appointment, LocalTime>) appointmentsTableView.getColumns().get(1);
        TableColumn<Appointment, String> petColumn =
                (TableColumn<Appointment, String>) appointmentsTableView.getColumns().get(2);
        TableColumn<Appointment, String> typeColumn =
                (TableColumn<Appointment, String>) appointmentsTableView.getColumns().get(3);
        TableColumn<Appointment, String> providerColumn =
                (TableColumn<Appointment, String>) appointmentsTableView.getColumns().get(4);

        //set up cell value factories for existing columns
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

    private void displayCustomerInfo(Customer customer) {
        //make customer elements visible
        nameLabel.setVisible(true);
        emailLabel.setVisible(true);
        phoneLabel.setVisible(true);
        petsLabel.setVisible(true);
        addressLabel.setVisible(true);
        appointmentsLabel.setVisible(true);
        petsTableView.setVisible(true);
        appointmentsTableView.setVisible(true);
        editPetButton.setVisible(true);
        editAppointmentButton.setVisible(true);
        profileImage.setVisible(true);

        //remove lookup field, label, and button
        enterPrompt.setVisible(false);
        customerLookupField.setVisible(false);
        searchButton.setVisible(false);

        //update labels with customer data
        nameLabel.setText("Name: " + customer.getFirstName() + " " + customer.getLastName());
        emailLabel.setText("Email: " + customer.getEmail());
        phoneLabel.setText("Phone #: " + customer.getPhone());
        addressLabel.setText("Address: " + customer.getAddress());

        //load customer info
        loadPets(customer.getID());
        loadAppointmentsForCustomer(customer.getID());

        fillPetsTable();
        fillAppointmentTable();
    }


    private void showAlert(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Customer Lookup");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
