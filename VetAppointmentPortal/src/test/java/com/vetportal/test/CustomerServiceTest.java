package com.vetportal.test;

import com.vetportal.dto.LookupStatus;
import com.vetportal.dto.ServiceResponse;
import com.vetportal.model.Customer;
import com.vetportal.service.CustomerService;
import com.vetportal.service.ServiceManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerServiceTest {

    private static CustomerService customerService;

    @BeforeAll
    public static void setUp() throws Exception {
        new ServiceManager();  // Initializes the singleton
        customerService = ServiceManager.getInstance().getCustomerService();
    }

    @Test
    public void testFindCustomerByPhone_success() {
        ServiceResponse<Customer> response = customerService.findCustomerByPhone("555-1234");
        printFullResponse(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("Alice", response.getData().getFirstName());
    }

    @Test
    public void testFindCustomerByPhone_notFound() {
        ServiceResponse<Customer> response = customerService.findCustomerByPhone("999-9999");
        printFullResponse(response);
        assertFalse(response.isSuccess());
        assertNull(response.getData());
        assertEquals(LookupStatus.NOT_FOUND, response.getStatus());
    }

    public void printFullResponse(ServiceResponse<?> response){
        System.out.println("response.getStatus(): " + response.getStatus());
        System.out.println("response.getStatus().name(): " + response.getStatus().name());
        System.out.println("isSuccess(): " + response.isSuccess());
    }
}
