package com.vetportal.controller;

import com.vetportal.model.*;
import com.vetportal.service.AppointmentService;
import com.vetportal.service.ServiceManager;
import com.vetportal.service.CustomerService;
import com.vetportal.dto.ServiceResponse;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.application.Platform;

import java.time.LocalDate;
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
    @FXML private Button addNewCustomerButton;
    @FXML private Button addPetButton;
    @FXML private Button editPetButton;
    @FXML private Button editCustomerButton;

    @FXML private TableView<Pet> petsTableView;
    @FXML private TableView<Appointment> appointmentsTableView;

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

        // Set default styling for section headers
        petsLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        appointmentsLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Set up action handlers for pet buttons
        if (editPetButton != null) {
            editPetButton.setOnAction(event -> handleEditPet());
        }

        if (addPetButton != null) {
            addPetButton.setOnAction(event -> handleAddPet());
        }

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

    @FXML
    private void handleAddPet() {
        if (currentCustomer == null) {
            showAlert("No customer selected");
            return;
        }

        Pet newPet = showPetDialog(null, "Add New Pet");
        if (newPet != null) {
            // Set owner for the new pet
            newPet.setOwner(currentCustomer);

            // Create the pet using the service
            ServiceResponse<Pet> response = customerService.createPet(newPet);

            if (response.isSuccess()) {
                // Refresh the pets table to display the new pet
                loadPets(currentCustomer.getID());
                showAlert("Pet added successfully!");
            } else {
                showAlert("Error adding pet: " + response.getMessage());
            }
        }
    }

    @FXML
    private void handleEditPet() {
        if (currentCustomer == null) {
            showAlert("No customer selected");
            return;
        }

        // Get all pets for the dropdown
        ServiceResponse<List<Pet>> petsResponse = customerService.findPetsByCustomerId(currentCustomer.getID());

        if (!petsResponse.isSuccess() || petsResponse.getData().isEmpty()) {
            showAlert("No pets found for this customer");
            return;
        }

        // Show pet selection dialog
        Pet selectedPet = showPetSelectionDialog(petsResponse.getData());

        if (selectedPet != null) {
            // Show the edit dialog with the selected pet's information
            Pet updatedPet = showPetDialog(selectedPet, "Edit Pet");

            if (updatedPet != null) {
                // Keep the ID and owner from the selected pet since this doesn't change
                updatedPet.setID(selectedPet.getID());
                updatedPet.setOwner(selectedPet.getOwner());

                // Update the pet using the CustomerService
                boolean success = customerService.updatePet(updatedPet);

                if (success) {
                    // Refresh the pets table
                    loadPets(currentCustomer.getID());
                    showAlert("Pet updated successfully!");
                } else {
                    showAlert("Error updating pet information");
                }
            }
        }
    }

    private Pet showPetSelectionDialog(List<Pet> pets) {
        // Create the custom dialog
        Dialog<Pet> dialog = new Dialog<>();
        dialog.setTitle("Select Pet");
        dialog.setHeaderText("Choose a pet to edit:");

        // Set the button types
        ButtonType selectButtonType = new ButtonType("Select", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);

        // Create the pet ComboBox and set the dialog pane
        ComboBox<Pet> petComboBox = new ComboBox<>();
        petComboBox.setItems(FXCollections.observableArrayList(pets));
        petComboBox.setPromptText("Select a pet");

        // Create cell factory to display pet's name in the dropdown
        petComboBox.setCellFactory(param -> new ListCell<Pet>() {
            @Override
            protected void updateItem(Pet pet, boolean empty) {
                super.updateItem(pet, empty);
                if (empty || pet == null) {
                    setText(null);
                } else {
                    setText(pet.getName());
                }
            }
        });

        // Set the same cell factory for the button cell to display the selected pet's name
        petComboBox.setButtonCell(new ListCell<Pet>() {
            @Override
            protected void updateItem(Pet pet, boolean empty) {
                super.updateItem(pet, empty);
                if (empty || pet == null) {
                    setText(null);
                } else {
                    setText(pet.getName());
                }
            }
        });

        // Create a grid for the ComboBox
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Pet:"), 0, 0);
        grid.add(petComboBox, 1, 0);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the ComboBox
        Platform.runLater(() -> petComboBox.requestFocus());

        // Enable or Disable the Select button depending on whether a pet has been selected
        Button selectButton = (Button) dialog.getDialogPane().lookupButton(selectButtonType);
        selectButton.setDisable(true);

        petComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectButton.setDisable(newValue == null);
        });

        // Convert the result when the select button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType) {
                return petComboBox.getValue();
            }
            return null;
        });

        // Show the dialog and wait for user action
        Optional<Pet> result = dialog.showAndWait();
        return result.orElse(null);
    }


    private Pet showPetDialog(Pet pet, String title) {
        boolean isEdit = pet != null;

        // Create the custom dialog
        Dialog<Pet> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(isEdit ? "Edit pet information:" : "Enter pet information:");

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create the grid and set the dialog pane
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Create text fields for pet info
        TextField nameField = new TextField();
        nameField.setPromptText("Pet Name");
        TextField speciesField = new TextField();
        speciesField.setPromptText("Species");
        TextField breedField = new TextField();
        breedField.setPromptText("Breed");
        DatePicker birthDatePicker = new DatePicker();
        birthDatePicker.setPromptText("Birth Date");

        // If editing, populate fields with current values
        if (isEdit) {
            nameField.setText(pet.getName());
            speciesField.setText(pet.getSpecies());
            breedField.setText(pet.getBreed());
            birthDatePicker.setValue(pet.getBirthDate());
        }

        // Add labels and fields to the grid
        grid.add(new Label("Name:*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Species:*"), 0, 1);
        grid.add(speciesField, 1, 1);
        grid.add(new Label("Breed:*"), 0, 2);
        grid.add(breedField, 1, 2);
        grid.add(new Label("Birth Date:*"), 0, 3);
        grid.add(birthDatePicker, 1, 3);

        //  Required fields notification
        Label requiredFieldsLabel = new Label("* All fields are required");
        requiredFieldsLabel.setStyle("-fx-font-size: 10px; -fx-font-style: italic;");
        grid.add(requiredFieldsLabel, 0, 4, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the first field
        Platform.runLater(() -> nameField.requestFocus());

        // Validation
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            // Check if any fields are empty
            if (nameField.getText().trim().isEmpty() ||
                    speciesField.getText().trim().isEmpty() ||
                    breedField.getText().trim().isEmpty() ||
                    birthDatePicker.getValue() == null) {

                // Highlight empty fields with red borders
                if (nameField.getText().trim().isEmpty()) {
                    nameField.setStyle("-fx-border-color: red;");
                } else {
                    nameField.setStyle("");
                }

                if (speciesField.getText().trim().isEmpty()) {
                    speciesField.setStyle("-fx-border-color: red;");
                } else {
                    speciesField.setStyle("");
                }

                if (breedField.getText().trim().isEmpty()) {
                    breedField.setStyle("-fx-border-color: red;");
                } else {
                    breedField.setStyle("");
                }

                if (birthDatePicker.getValue() == null) {
                    birthDatePicker.setStyle("-fx-border-color: red;");
                } else {
                    birthDatePicker.setStyle("");
                }

                // Show the error message
                showAlert("All fields must be filled!");

                // Consume the event to prevent the dialog from closing
                event.consume();
            }
        });

        // Convert the result to a pet when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // Create pet object - validation was already done
                Pet result = new Pet(null,
                        nameField.getText().trim(),
                        speciesField.getText().trim(),
                        breedField.getText().trim(),
                        birthDatePicker.getValue(),
                        currentCustomer);

                return result;
            }
            return null;
        });

        // Show the dialog and wait for user action
        Optional<Pet> result = dialog.showAndWait();
        return result.orElse(null);
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
        Image image = new Image(getClass().getResourceAsStream("/images/icon.png"));

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
        if (editPetButton != null) editPetButton.setVisible(false);
        if (addPetButton != null) addPetButton.setVisible(false);
        if (editCustomerButton != null) editCustomerButton.setVisible(false);
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

    @FXML
    private void handleAddNewCustomer() {
        Customer newCustomer = showCustomerDialog(null, "Add New Customer");
        if (newCustomer != null) {
            // Check if customer already exists with this phone or email
            Map<String, String> phoneCheck = new HashMap<>();
            phoneCheck.put("phone", newCustomer.getPhone());

            Map<String, String> emailCheck = new HashMap<>();
            emailCheck.put("email", newCustomer.getEmail());

            ServiceResponse<Customer> phoneResponse = customerService.findCustomerByAttributes(phoneCheck);
            ServiceResponse<Customer> emailResponse = customerService.findCustomerByAttributes(emailCheck);

            if (phoneResponse.isSuccess()) {
                showAlert("A customer with this phone number already exists!");
                return;
            }

            if (emailResponse.isSuccess()) {
                showAlert("A customer with this email already exists!");
                return;
            }

            // Create the customer using the service
            ServiceResponse<Customer> response = customerService.createCustomer(newCustomer);

            if (response.isSuccess()) {
                // If successful, set as current customer and display info
                currentCustomer = response.getData();
                displayCustomerInfo(currentCustomer);

                // Show success message
                showAlert("Customer added successfully!");
            } else {
                // Show error message
                showAlert("Error adding customer: " + response.getMessage());
            }
        }
    }

    @FXML
    private void handleEditCustomer() {
        if (currentCustomer == null) {
            showAlert("No customer selected");
            return;
        }

        Customer updatedCustomer = showCustomerDialog(currentCustomer, "Edit Customer");
        if (updatedCustomer != null) {
            // Preserve the ID from the current customer
            updatedCustomer.setID(currentCustomer.getID());

            // Update the customer using the service
            boolean success = customerService.updateCustomer(updatedCustomer);

            if (success) {
                // Update the current customer reference
                currentCustomer = updatedCustomer;

                // Refresh the customer info display
                displayCustomerInfo(currentCustomer);

                // Show success message
                showAlert("Customer updated successfully!");
            } else {
                // Show error message
                showAlert("Error updating customer information");
            }
        }
    }

    private Customer showCustomerDialog(Customer customer, String title) {
        boolean isEdit = customer != null;

        // Create the custom dialog
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(isEdit ? "Edit customer information:" : "Enter customer information:");

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create the grid and set the dialog pane
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Create text fields for customer info
        TextField firstNameField = new TextField();
        firstNameField.setPromptText("First Name");
        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Last Name");
        TextField addressField = new TextField();
        addressField.setPromptText("Address");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone (XXX-XXXX)");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        // If editing, populate fields with current values
        if (isEdit) {
            firstNameField.setText(customer.getFirstName());
            lastNameField.setText(customer.getLastName());
            addressField.setText(customer.getAddress());
            phoneField.setText(customer.getPhone());
            emailField.setText(customer.getEmail());
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

        // Add a label explaining required fields
        Label requiredFieldsLabel = new Label("* All fields are required");
        requiredFieldsLabel.setStyle("-fx-font-size: 10px; -fx-font-style: italic;");
        grid.add(requiredFieldsLabel, 0, 5, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the first field
        firstNameField.requestFocus();

        // Add validation directly to the Save button
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            // Check if any fields are empty
            if (firstNameField.getText().trim().isEmpty() ||
                    lastNameField.getText().trim().isEmpty() ||
                    addressField.getText().trim().isEmpty() ||
                    phoneField.getText().trim().isEmpty() ||
                    emailField.getText().trim().isEmpty()) {

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

                // Show the error message
                showAlert("All fields must be filled!");

                // Consume the event to prevent the dialog from closing
                event.consume();
            }
        });

        // Convert the result to a customer when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // Create customer object - validation is already handled by the event filter
                Customer result = new Customer();
                result.setFirstName(firstNameField.getText().trim());
                result.setLastName(lastNameField.getText().trim());
                result.setAddress(addressField.getText().trim());
                result.setPhone(phoneField.getText().trim());
                result.setEmail(emailField.getText().trim());

                return result;
            }
            return null;
        });

        // Show the dialog and wait for response
        Optional<Customer> result = dialog.showAndWait();
        return result.orElse(null);
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

            // Show a message if no pets are found with smaller font size
            if (pets.isEmpty()) {
                petsLabel.setText("No pets found for this customer");
                petsLabel.setStyle("-fx-font-size: 14px;");
            } else {
                petsLabel.setText("Pets:");
                petsLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            }
        } else {
            // Clear the table if there was an error
            petsTableView.setItems(FXCollections.observableArrayList());
            petsLabel.setText("Error loading pets");
            petsLabel.setStyle("-fx-font-size: 14px;");
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
            appointmentsLabel.setStyle("-fx-font-size: 14px;");
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
            appointmentsLabel.setStyle("-fx-font-size: 14px;");
        } else {
            appointmentsLabel.setText("Appointments:");
            appointmentsLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
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
        addPetButton.setVisible(true);
        editPetButton.setVisible(true);
        editCustomerButton.setVisible(true);
        profileImage.setVisible(true);

        //remove lookup field, label, and button
        enterPrompt.setVisible(false);
        customerLookupField.setVisible(false);
        searchButton.setVisible(false);
        addNewCustomerButton.setVisible(false);

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
        alert.setTitle("Customer Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}