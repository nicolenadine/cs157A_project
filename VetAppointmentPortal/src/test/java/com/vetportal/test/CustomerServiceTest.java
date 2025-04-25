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
}
