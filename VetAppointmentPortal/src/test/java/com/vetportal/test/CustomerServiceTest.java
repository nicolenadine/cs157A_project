package com.vetportal.test;

import com.vetportal.dto.LookupStatus;
import com.vetportal.dto.ServiceResponse;
import com.vetportal.model.Customer;
import com.vetportal.model.Pet;
import com.vetportal.service.CustomerService;
import com.vetportal.service.ServiceManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerServiceTest {

    private static CustomerService customerService;

    @BeforeAll
    public static void setUp() throws Exception {
        new ServiceManager();  // Initializes the singleton
        customerService = ServiceManager.getInstance().getCustomerService();
    }

    // ---------- TESTS FOR RETRIEVING CUSTOMERS BY FIELDS ----------
    @Test
    public void testFindCustomerByPhone_success() {
        Map<String, String> fields = Map.of("phone", "555-0001");
        ServiceResponse<Customer> response = customerService.findCustomerByAttributes(fields);
        printFullResponse(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("Alice", response.getData().getFirstName());
    }

    @Test
    public void testFindCustomerByPhone_notFound() {
        Map<String, String> fields = Map.of("phone", "999-9999");
        ServiceResponse<Customer> response = customerService.findCustomerByAttributes(fields);
        printFullResponse(response);
        assertFalse(response.isSuccess());
        assertNull(response.getData());
        assertEquals(LookupStatus.NOT_FOUND, response.getStatus());
    }

    @Test
    public void testFindCustomerByFirstAndLastName_success() {
        Map<String, String> fields = Map.of("firstName", "Bob", "lastName", "Jones");
        ServiceResponse<Customer> response = customerService.findCustomerByAttributes(fields);
        printFullResponse(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("Bob", response.getData().getFirstName());
        assertEquals("Jones", response.getData().getLastName());
    }

    @Test
    public void testFindCustomerByFirstAndLastName_notFound() {
        Map<String, String> fields = Map.of("firstName", "Nonexistent", "lastName", "User");
        ServiceResponse<Customer> response = customerService.findCustomerByAttributes(fields);
        printFullResponse(response);
        assertFalse(response.isSuccess());
        assertNull(response.getData());
        assertEquals(LookupStatus.NOT_FOUND, response.getStatus());
    }

    // ------ TESTS FOR RETRIEVING PETS BY CUSTOMER ----------
    @Test
    public void testFindPetsByCustomerId_withPets() {
        ServiceResponse<List<Pet>> response = customerService.findPetsByCustomerId(1); // assuming Alice has pets
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertFalse(response.getData().isEmpty());
    }

    @Test
    public void testFindPetsByCustomerId_noPets() {
        ServiceResponse<List<Pet>> response = customerService.findPetsByCustomerId(15); // Owen has no pets
        assertFalse(response.isSuccess());
        assertEquals(LookupStatus.NOT_FOUND, response.getStatus());
    }

    //  ---------- PRINT RESPONSES -------------------
    public void printFullResponse(ServiceResponse<?> response){
        System.out.println("response.getStatus(): " + response.getStatus());
        System.out.println("response.getStatus().name(): " + response.getStatus().name());
        System.out.println("isSuccess(): " + response.isSuccess());
        if (response.getData() != null) {
            System.out.println("Customer: " + response.getData());
        }
    }




    // Tests for creating customers
    @Test
    public void testCreateCustomer_success() {
        // Create a customer with all required fields
        Customer newCustomer = new Customer(null, "John", "Doe", "123 Test St", "555-1234", "john.doe@example.com");

        ServiceResponse<Customer> response = customerService.createCustomer(newCustomer);
        printFullResponse(response);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertNotNull(response.getData().getID()); // ID should be populated now
        assertEquals("John", response.getData().getFirstName());
        assertEquals("Doe", response.getData().getLastName());

        // Clean up the created customer
        assertTrue(customerService.deleteCustomer(response.getData().getID()));
    }

    @Test
    public void testCreateCustomer_duplicateEmail() {
        // Create a customer with a duplicate email (assuming Alice's email is already in use)
        Customer newCustomer = new Customer(null, "Duplicate", "User", "456 Test Ave", "555-9999", "alice@example.com");

        ServiceResponse<Customer> response = customerService.createCustomer(newCustomer);
        printFullResponse(response);

        assertFalse(response.isSuccess());
        assertEquals(LookupStatus.DB_ERROR, response.getStatus());
    }

    // Tests for updating customers
    @Test
    public void testUpdateCustomer_success() {
        // First create a customer to update
        Customer newCustomer = new Customer(null, "Jane", "Smith", "789 Update Ln", "555-5678", "jane.smith@example.com");

        ServiceResponse<Customer> createResponse = customerService.createCustomer(newCustomer);
        assertTrue(createResponse.isSuccess());

        // Now update the customer - use the returned object which has the ID
        Customer customerToUpdate = createResponse.getData();
        customerToUpdate.setAddress("789 Changed Ave");
        customerToUpdate.setPhone("555-8765");

        boolean updateResult = customerService.updateCustomer(customerToUpdate);
        assertTrue(updateResult);

        // Verify the update was successful
        Map<String, String> fields = Map.of("email", "jane.smith@example.com");
        ServiceResponse<Customer> response = customerService.findCustomerByAttributes(fields);
        assertTrue(response.isSuccess());
        assertEquals("789 Changed Ave", response.getData().getAddress());
        assertEquals("555-8765", response.getData().getPhone());

        // Clean up
        assertTrue(customerService.deleteCustomer(customerToUpdate.getID()));
    }

    @Test
    public void testUpdateCustomer_nonExistent() {
        // Create a customer with a non-existent ID
        Customer nonExistentCustomer = new Customer(9999, "Nobody", "NoWhere", "123 Nowhere St", "555-0000", "nobody@example.com");

        boolean updateResult = customerService.updateCustomer(nonExistentCustomer);
        assertFalse(updateResult);
    }

    // Tests for deleting customers
    @Test
    public void testDeleteCustomer_success() {
        // First create a customer to delete
        Customer newCustomer = new Customer(null, "Delete", "Me", "321 Delete Rd", "555-3333", "delete.me@example.com");

        ServiceResponse<Customer> createResponse = customerService.createCustomer(newCustomer);
        assertTrue(createResponse.isSuccess());

        // Now delete the customer
        int customerId = createResponse.getData().getID();
        boolean deleteResult = customerService.deleteCustomer(customerId);
        assertTrue(deleteResult);

        // Verify the customer was deleted
        Map<String, String> fields = Map.of("email", "delete.me@example.com");
        ServiceResponse<Customer> response = customerService.findCustomerByAttributes(fields);
        assertFalse(response.isSuccess());
        assertEquals(LookupStatus.NOT_FOUND, response.getStatus());
    }

    @Test
    public void testDeleteCustomer_nonExistent() {
        boolean deleteResult = customerService.deleteCustomer(9999); // Assuming this ID doesn't exist
        assertFalse(deleteResult);
    }

    // Tests for creating pets
    @Test
    public void testCreatePet_success() {
        // First find an existing customer to be the pet owner
        Map<String, String> fields = Map.of("phone", "555-0001"); // Alice
        ServiceResponse<Customer> customerResponse = customerService.findCustomerByAttributes(fields);
        assertTrue(customerResponse.isSuccess());

        Pet newPet = new Pet(null, "Fluffy", "Cat", "Persian", "2020-01-15", customerResponse.getData());

        ServiceResponse<Pet> response = customerService.createPet(newPet);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertNotNull(response.getData().getID()); // ID should be populated now
        assertEquals("Fluffy", response.getData().getName());
        assertEquals("Cat", response.getData().getSpecies());

        // Clean up
        assertTrue(customerService.deletePet(response.getData().getID()));
    }

    @Test
    public void testCreatePet_invalidOwner() {
        // Create a customer with a non-existent ID
        Customer nonExistentOwner = new Customer(9999, "Invalid", "Owner", "999 Nowhere St", "555-9999", "invalid@nowhere.com");

        Pet newPet = new Pet(null, "Max", "Dog", "Labrador", "2019-06-10", nonExistentOwner);

        ServiceResponse<Pet> response = customerService.createPet(newPet);

        assertFalse(response.isSuccess());
        // The specific error might depend on your implementation
        // It could be NOT_FOUND or DB_ERROR
    }

    // Tests for updating pets
    @Test
    public void testUpdatePet_success() {
        // First find an existing customer to be the pet owner
        Map<String, String> fields = Map.of("phone", "555-0001"); // Alice
        ServiceResponse<Customer> customerResponse = customerService.findCustomerByAttributes(fields);
        assertTrue(customerResponse.isSuccess());

        // Create a pet to update
        Pet newPet = new Pet(null, "Rover", "Dog", "Beagle", "2018-03-20", customerResponse.getData());

        ServiceResponse<Pet> createResponse = customerService.createPet(newPet);
        assertTrue(createResponse.isSuccess());

        // Now update the pet - use the returned object which has the ID
        Pet petToUpdate = createResponse.getData();
        petToUpdate.setName("Rex");
        petToUpdate.setBreed("German Shepherd");

        boolean updateResult = customerService.updatePet(petToUpdate);
        assertTrue(updateResult);

        // Verify the update by getting the pet list for the owner
        ServiceResponse<List<Pet>> petsResponse = customerService.findPetsByCustomerId(customerResponse.getData().getID());
        assertTrue(petsResponse.isSuccess());

        boolean foundUpdatedPet = petsResponse.getData().stream()
                .anyMatch(pet -> pet.getID() == petToUpdate.getID()
                        && "Rex".equals(pet.getName())
                        && "German Shepherd".equals(pet.getBreed()));

        assertTrue(foundUpdatedPet);

        // Clean up
        assertTrue(customerService.deletePet(petToUpdate.getID()));
    }

    @Test
    public void testUpdatePet_nonExistent() {
        // First find an existing customer to be the pet owner
        Map<String, String> fields = Map.of("phone", "555-0001"); // Alice
        ServiceResponse<Customer> customerResponse = customerService.findCustomerByAttributes(fields);
        assertTrue(customerResponse.isSuccess());

        // Create a non-existent pet with a valid owner
        Pet nonExistentPet = new Pet(9999, "Ghost", "Ghost", "Ghostly", "2010-10-31", customerResponse.getData());

        boolean updateResult = customerService.updatePet(nonExistentPet);
        assertFalse(updateResult);
    }

    // Tests for deleting pets
    @Test
    public void testDeletePet_success() {
        // First find an existing customer to be the pet owner
        Map<String, String> fields = Map.of("phone", "555-0001"); // Alice
        ServiceResponse<Customer> customerResponse = customerService.findCustomerByAttributes(fields);
        assertTrue(customerResponse.isSuccess());

        // Create a pet to delete
        Pet newPet = new Pet(null, "DeleteMe", "Bird", "Canary", "2021-05-01", customerResponse.getData());

        ServiceResponse<Pet> createResponse = customerService.createPet(newPet);
        assertTrue(createResponse.isSuccess());

        // Now delete the pet
        int petId = createResponse.getData().getID();
        boolean deleteResult = customerService.deletePet(petId);
        assertTrue(deleteResult);

        // Attempt to find pets by the customer ID
        ServiceResponse<List<Pet>> petsResponse = customerService.findPetsByCustomerId(customerResponse.getData().getID());

        // If the customer had no other pets, this would return NOT_FOUND
        // Otherwise, we need to check that the deleted pet is not in the list
        if (petsResponse.isSuccess()) {
            boolean petStillExists = petsResponse.getData().stream()
                    .anyMatch(pet -> pet.getID() == petId);
            assertFalse(petStillExists);
        } else {
            assertEquals(LookupStatus.NOT_FOUND, petsResponse.getStatus());
        }
    }

    @Test
    public void testDeletePet_nonExistent() {
        boolean deleteResult = customerService.deletePet(9999); // Assuming this ID doesn't exist
        assertFalse(deleteResult);
    }

    // Test cascading delete - check that when a customer is deleted, their pets are also deleted
    @Test
    public void testDeleteCustomer_cascadeDeletesPets() {
        // Create a new customer
        Customer newCustomer = new Customer(null, "Cascade", "Test", "123 Cascade Ave", "555-4444", "cascade.test@example.com");

        ServiceResponse<Customer> customerResponse = customerService.createCustomer(newCustomer);
        assertTrue(customerResponse.isSuccess());
        int customerId = customerResponse.getData().getID();

        // Create a pet owned by this customer
        Pet newPet = new Pet(null, "CascadePet", "Hamster", "Syrian", "2022-02-02", customerResponse.getData());

        ServiceResponse<Pet> petResponse = customerService.createPet(newPet);
        assertTrue(petResponse.isSuccess());
        int petId = petResponse.getData().getID();

        // Verify the pet exists
        ServiceResponse<List<Pet>> petsBeforeResponse = customerService.findPetsByCustomerId(customerId);
        assertTrue(petsBeforeResponse.isSuccess());

        // Delete the customer
        boolean deleteResult = customerService.deleteCustomer(customerId);
        assertTrue(deleteResult);

        // Verify the customer is gone
        Map<String, String> fields = Map.of("email", "cascade.test@example.com");
        ServiceResponse<Customer> customerAfterResponse = customerService.findCustomerByAttributes(fields);
        assertFalse(customerAfterResponse.isSuccess());
        assertEquals(LookupStatus.NOT_FOUND, customerAfterResponse.getStatus());

        // Now try to get pets for this (now deleted) customer
        // This should return a NOT_FOUND status since the customer no longer exists
        ServiceResponse<List<Pet>> petsAfterResponse = customerService.findPetsByCustomerId(customerId);
        assertFalse(petsAfterResponse.isSuccess());
    }


}
