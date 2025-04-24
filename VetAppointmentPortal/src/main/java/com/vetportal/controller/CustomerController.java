package com.vetportal.controller;

import com.vetportal.model.Customer;
import com.vetportal.service.ServiceManager;
import com.vetportal.service.CustomerService;
import com.vetportal.dto.ServiceResponse;


import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.util.HashMap;
import java.util.Map;

public class CustomerController {

    @FXML
    private TextField customerLookupField;

    private CustomerService customerService;
    private Customer currentCustomer;

    @FXML
    public void initialize() {
        customerService = ServiceManager.getInstance().getCustomerService();
    }

    @FXML
    private void handleCustomerByPhone() {
        Customer customer = fetchCustomerByPhone();
        if (customer != null) {
            displayCustomerInfo(customer);
        }
    }

    private Customer fetchCustomerByPhone() {
        String phone = customerLookupField.getText();

        // Create a map with the phone field
        Map<String, String> fields = new HashMap<>();
        fields.put("phone", phone);

        ServiceResponse<Customer> response = customerService.findCustomerByFields(fields);

        if (response.isSuccess()) {
            return response.getData();  // gets the customer object if it exists
        } else {
            showAlert(response.getMessage());  // show the error
            return null;
        }
    }


    private void displayCustomerInfo(Customer customer) {
        // @Miguel you can change the name of this to be whatever you want
        //         the idea is just that the handleSearchByPhone method will return a
        //         Service response object and to get the Customer object from that you use
        //         response.getData()  to retrieve.
    }

    private void showAlert(String message) {
        // Show an alert to the user
    }
}
