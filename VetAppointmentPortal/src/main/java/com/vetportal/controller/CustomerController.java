package com.vetportal.controller;

import com.vetportal.model.Customer;
import com.vetportal.service.ServiceManager;
import com.vetportal.service.CustomerService;
import com.vetportal.dto.ServiceResponse;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.shape.Circle;
import javafx.scene.control.TableView;
import javafx.scene.control.Button;

import java.util.HashMap;
import java.util.Map;

public class CustomerController {


    @FXML private TextField customerLookupField;
    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;
    @FXML private Label petsLabel;
    @FXML private Label appointmentsLabel;
    @FXML private Label enterPrompt;
    @FXML private Button searchButton;
    @FXML private Button addPetButton;
    @FXML private Button addAppointmentButton;
    @FXML private Circle customerPhoto;
    @FXML private TableView petsTableView;
    @FXML private TableView appointmentsTableView;
    @FXML private VBox customerInfoContainer;

    private CustomerService customerService;
    private Customer currentCustomer;

    @FXML
    public void initialize() {
        customerService = ServiceManager.getInstance().getCustomerService();

        // Hide customer information initially
        hideCustomerInfo();
    }

    private void hideCustomerInfo() {
        // Make customer info elements invisible until a customer is found
        if (nameLabel != null) nameLabel.setVisible(false);
        if (emailLabel != null) emailLabel.setVisible(false);
        if (phoneLabel != null) phoneLabel.setVisible(false);
        if (petsLabel != null) petsLabel.setVisible(false);
        if (appointmentsLabel != null) appointmentsLabel.setVisible(false);
        if (customerPhoto != null) customerPhoto.setVisible(false);
        if (petsTableView != null) petsTableView.setVisible(false);
        if (appointmentsTableView != null) appointmentsTableView.setVisible(false);
        if (addAppointmentButton != null) addAppointmentButton.setVisible(false);
        if (addPetButton != null) addPetButton.setVisible(false);
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

    private void displayCustomerInfo(Customer customer) {
        // Make all customer info elements visible
        nameLabel.setVisible(true);
        emailLabel.setVisible(true);
        phoneLabel.setVisible(true);
        petsLabel.setVisible(true);
        appointmentsLabel.setVisible(true);
        customerPhoto.setVisible(true);
        petsTableView.setVisible(true);
        appointmentsTableView.setVisible(true);
        addPetButton.setVisible(true);
        addAppointmentButton.setVisible(true);

        //Remove lookup field and button
        enterPrompt.setVisible(false);
        customerLookupField.setVisible(false);
        searchButton.setVisible(false);


        // Update labels with customer data
        nameLabel.setText("Name: " + customer.getFirstName() + " " + customer.getLastName());
        emailLabel.setText("Email: " + customer.getEmail());
        phoneLabel.setText("Phone #: " + customer.getPhone());

        // Here you would also populate the pets and appointments tables
        // loadPetsForCustomer(customer.getID());
        // loadAppointmentsForCustomer(customer.getID());
    }

    private void showAlert(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Customer Lookup");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Methods to load pets and appointments would be added here
}