package com.ccms.service.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import com.ccms.service.model.Customer.Address;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

public class CustomerTest {

    // Set up the validator factory and validator
    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    // Test Case 1: Valid Customer Object
    @Test
    void testValidCustomerCreation() {
        Customer.Name name = new Customer.Name("John", "Doe");
        Customer.Address address = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");
        
        Customer customer = new Customer(
            "1", 
            "johndoe", 
            "password123", 
            name, 
            "1990-01-01", 
            "M", 
            "johndoe@example.com", 
            1001, 
            address, 
            true, 
            new Date()
        );

        assertNotNull(customer);
        assertEquals("John", customer.getName().getFirst());
        assertEquals("Doe", customer.getName().getLast());
        assertTrue(customer.isActive());
        assertEquals("johndoe@example.com", customer.getEmail());
    }

    // Test Case 2: Invalid Email
    @Test
    void testInvalidEmail() {
        Customer.Name name = new Customer.Name("John", "Doe");
        Customer.Address address = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");
        
        Customer customer = new Customer(
            "1", 
            "johndoe", 
            "password123", 
            name, 
            "1990-01-01", 
            "M", 
            "invalid-email", // Invalid email format
            1001, 
            address, 
            true, 
            new Date()
        );

        Set<ConstraintViolation<Customer>> violations = validator.validate(customer);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("email")));
    }

    // Test Case 3: Null Username (Validation Failure)
    @Test
    void testNullUsername() {
        Customer.Name name = new Customer.Name("John", "Doe");
        Customer.Address address = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");
        
        Customer customer = new Customer(
            "1", 
            null, // Null username should fail validation
            "password123", 
            name, 
            "1990-01-01", 
            "M", 
            "johndoe@example.com", 
            1001, 
            address, 
            true, 
            new Date()
        );

        Set<ConstraintViolation<Customer>> violations = validator.validate(customer);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must not be null")));
    }

    // Test Case 4: Empty Name Fields
    @Test
    void testEmptyNameFields() {
        Customer.Name name = new Customer.Name("", ""); // Empty first and last name
        Customer.Address address = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");
        
        Customer customer = new Customer(
            "1", 
            "johndoe", 
            "password123", 
            name, 
            "1990-01-01", 
            "M", 
            "johndoe@example.com", 
            1001, 
            address, 
            true, 
            new Date()
        );

        assertNotNull(customer);
        assertEquals("", customer.getName().getFirst());
        assertEquals("", customer.getName().getLast());
    }

    // Test Case 6: Customer with Inactive Status
    @Test
    void testInactiveCustomer() {
        Customer.Name name = new Customer.Name("John", "Doe");
        Customer.Address address = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");
        
        Customer customer = new Customer(
            "1", 
            "johndoe", 
            "password123", 
            name, 
            "1990-01-01", 
            "M", 
            "johndoe@example.com", 
            1001, 
            address, 
            false, // Inactive customer
            new Date()
        );

        assertNotNull(customer);
        assertFalse(customer.isActive()); // Check if customer is inactive
    }

    // Test Case 7: Customer with No Address
    @Test
    void testCustomerWithNoAddress() {
        Customer.Name name = new Customer.Name("John", "Doe");
        
        Customer customer = new Customer(
            "1", 
            "johndoe", 
            "password123", 
            name, 
            "1990-01-01", 
            "M", 
            "johndoe@example.com", 
            1001, 
            null, // No address provided
            true, 
            new Date()
        );

        assertNotNull(customer);
        assertNull(customer.getAddress()); // Address should be null
    }

    // Test Case 8: Customer Creation with Missing Password
    @Test
    void testCustomerWithMissingPassword() {
        Customer.Name name = new Customer.Name("John", "Doe");
        Customer.Address address = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");
        
        Customer customer = new Customer(
            "1", 
            "johndoe", 
            null, // Missing password
            name, 
            "1990-01-01", 
            "M", 
            "johndoe@example.com", 
            1001, 
            address, 
            true, 
            new Date()
        );

        assertNotNull(customer);
        assertNull(customer.getPassword()); // Password should be null
    }

    // Test Case 9: Customer DOB Format Check
    @Test
    void testCustomerDOBFormat() {
        String validDob = "1990-01-01";
        Customer.Name name = new Customer.Name("John", "Doe");
        Customer.Address address = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");
        
        Customer customer = new Customer(
            "1", 
            "johndoe", 
            "password123", 
            name, 
            validDob, 
            "M", 
            "johndoe@example.com", 
            1001, 
            address, 
            true, 
            new Date()
        );

        assertEquals(validDob, customer.getDob());
    }

    // Test Case 10: Customer Creation with CreatedAt Date
    @Test
    void testCustomerCreationWithCreatedAt() {
        Date createdAt = new Date();
        Customer.Name name = new Customer.Name("John", "Doe");
        Customer.Address address = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");

        Customer customer = new Customer(
            "1", 
            "johndoe", 
            "password123", 
            name, 
            "1990-01-01", 
            "M", 
            "johndoe@example.com", 
            1001, 
            address, 
            true, 
            createdAt
        );

        assertNotNull(customer);
        assertEquals(createdAt, customer.getCreatedAt()); // Ensure createdAt is set correctly
    }
    
    @Test
    void testEquals() {
        Customer.Name name1 = new Customer.Name("John", "Doe");
        Customer.Address address1 = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");
        Customer customer1 = new Customer(
            "1", "johndoe", "password123", name1, "1990-01-01", "M",
            "johndoe@example.com", 1001, address1, true, new Date()
        );

        Customer.Name name2 = new Customer.Name("John", "Doe");
        Customer.Address address2 = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");
        Customer customer2 = new Customer(
            "1", "johndoe", "password123", name2, "1990-01-01", "M",
            "johndoe@example.com", 1001, address2, true, new Date()
        );

        assertTrue(customer1.equals(customer2), "The two customers should be equal based on the same ID.");
    }

    
    @Test
    void testHashCode() {
        Customer.Name name1 = new Customer.Name("John", "Doe");
        Customer.Address address1 = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");
        Customer customer1 = new Customer(
            "1", "johndoe", "password123", name1, "1990-01-01", "M",
            "johndoe@example.com", 1001, address1, true, new Date()
        );

        Customer.Name name2 = new Customer.Name("John", "Doe");
        Customer.Address address2 = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");
        Customer customer2 = new Customer(
            "1", "johndoe", "password123", name2, "1990-01-01", "M",
            "johndoe@example.com", 1001, address2, true, new Date()
        );

        assertEquals(customer1.hashCode(), customer2.hashCode(), "The hash codes should be equal for two customers with the same ID.");
    }
    
    
    @Test
    void testToString() {
        Customer.Name name = new Customer.Name("John", "Doe");
        Customer.Address address = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");
        Customer customer = new Customer(
            "1", "johndoe", "password123", name, "1990-01-01", "M",
            "johndoe@example.com", 1001, address, true, new Date()
        );

        // Format the createdAt field using SimpleDateFormat to ensure consistency
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
        String formattedDate = dateFormat.format(customer.getCreatedAt());

        // Update expected string to include password
        String expected = "Customer(id=1, username=johndoe, password=password123, name=Customer.Name(first=John, last=Doe), dob=1990-01-01, sex=M, email=johndoe@example.com, customerId=1001, address=Customer.Address(street=123 Main St, city=Anytown, state=CA, zip=12345, country=USA), active=true, createdAt=" + formattedDate + ")";

        assertEquals(expected, customer.toString(), "The toString method should return the correct string representation.");
    }

    
    @Test
    void testSetId() {
        Customer.Name name = new Customer.Name("John", "Doe");
        Customer.Address address = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");
        Customer customer = new Customer(
            "1", "johndoe", "password123", name, "1990-01-01", "M",
            "johndoe@example.com", 1001, address, true, new Date()
        );

        customer.setId("2");
        assertEquals("2", customer.getId(), "The id should be updated to 2.");
    }
    
    
    @Test
    void testSetPassword() {
        Customer.Name name = new Customer.Name("John", "Doe");
        Customer.Address address = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");
        Customer customer = new Customer(
            "1", "johndoe", "password123", name, "1990-01-01", "M",
            "johndoe@example.com", 1001, address, true, new Date()
        );

        customer.setPassword("newPassword123");
        assertEquals("newPassword123", customer.getPassword(), "The password should be updated.");
    }
    
    
    @Test
    void testSetDob() {
        Customer.Name name = new Customer.Name("John", "Doe");
        Customer.Address address = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");
        Customer customer = new Customer(
            "1", "johndoe", "password123", name, "1990-01-01", "M",
            "johndoe@example.com", 1001, address, true, new Date()
        );

        customer.setDob("1992-05-15");
        assertEquals("1992-05-15", customer.getDob(), "The date of birth should be updated.");
    }
    
    
    @Test
    void testSetSex() {
        Customer.Name name = new Customer.Name("John", "Doe");
        Customer.Address address = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");
        Customer customer = new Customer(
            "1", "johndoe", "password123", name, "1990-01-01", "M",
            "johndoe@example.com", 1001, address, true, new Date()
        );

        customer.setSex("F");
        assertEquals("F", customer.getSex(), "The sex should be updated to F.");
    }
    
    
    @Test
    void testSetEmail() {
        Customer.Name name = new Customer.Name("John", "Doe");
        Customer.Address address = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");
        Customer customer = new Customer(
            "1", "johndoe", "password123", name, "1990-01-01", "M",
            "johndoe@example.com", 1001, address, true, new Date()
        );

        customer.setEmail("newemail@example.com");
        assertEquals("newemail@example.com", customer.getEmail(), "The email should be updated.");
    }

    
    
    @Test
    void testSetCustomerId() {
        Customer.Name name = new Customer.Name("John", "Doe");
        Customer.Address address = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");
        Customer customer = new Customer(
            "1", "johndoe", "password123", name, "1990-01-01", "M",
            "johndoe@example.com", 1001, address, true, new Date()
        );

        customer.setCustomerId(2002);
        assertEquals(2002, customer.getCustomerId(), "The customer ID should be updated.");
    }

    
    @Test
    void testSetAddress() {
        Customer.Name name = new Customer.Name("John", "Doe");
        Customer.Address address = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");
        Customer customer = new Customer(
            "1", "johndoe", "password123", name, "1990-01-01", "M",
            "johndoe@example.com", 1001, address, true, new Date()
        );

        Customer.Address newAddress = new Customer.Address("456 Another St", "Othertown", "NY", 67890, "USA");
        customer.setAddress(newAddress);
        assertEquals(newAddress, customer.getAddress(), "The address should be updated.");
    }

    @Test
    void testSetActive() {
        // Create a new Customer object
        Customer.Name name = new Customer.Name("John", "Doe");
        Customer.Address address = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");
        Customer customer = new Customer(
            "1", "johndoe", "password123", name, "1990-01-01", "M",
            "johndoe@example.com", 1001, address, true, new Date()
        );

        // Set the 'active' status to false
        customer.setActive(false);

        // Assert that the 'active' status has been updated correctly
        assertFalse(customer.isActive(), "The active status should be updated to false.");

        // Set the 'active' status to true
        customer.setActive(true);

        // Assert that the 'active' status has been updated correctly
        assertTrue(customer.isActive(), "The active status should be updated to true.");
    }

    
    @Test
    void testSetCreatedAt() {
        // Create a new Customer object
        Customer.Name name = new Customer.Name("John", "Doe");
        Customer.Address address = new Customer.Address("123 Main St", "Anytown", "CA", 12345, "USA");
        Customer customer = new Customer(
            "1", "johndoe", "password123", name, "1990-01-01", "M",
            "johndoe@example.com", 1001, address, true, new Date()
        );

        // Create a new date to set as the createdAt value
        Date newCreatedAt = new Date(1672531199000L); // January 1st, 2023, 12:00:00 AM GMT

        // Set the createdAt date
        customer.setCreatedAt(newCreatedAt);

        // Assert that the createdAt date has been updated correctly
        assertEquals(newCreatedAt, customer.getCreatedAt(), "The createdAt date should be updated.");
    }


    @Test
    void testEquals1() {
        Customer.Name name1 = new Customer.Name("John", "Doe");
        Customer.Name name2 = new Customer.Name("John", "Doe");
        
        // Two objects with the same first and last name should be equal
        assertEquals(name1, name2, "The names should be equal.");
        
        // Modify the second name to make them different
        name2.setLast("Smith");
        
        // Assert that the names are not equal
        assertNotEquals(name1, name2, "The names should not be equal after last name change.");
    }
    
    
    @Test
    void testHashCode1() {
        Customer.Name name1 = new Customer.Name("John", "Doe");
        Customer.Name name2 = new Customer.Name("John", "Doe");
        
        // Assert that equal objects have the same hash code
        assertEquals(name1.hashCode(), name2.hashCode(), "Hash codes should be equal for equal names.");
        
        // Modify the second name and ensure hash codes are different
        name2.setLast("Smith");
        
        // Assert that different names produce different hash codes
        assertNotEquals(name1.hashCode(), name2.hashCode(), "Hash codes should be different for different names.");
    }

    
    @Test
    void testSetFirst() {
        // Create a new name object
        Customer.Name name = new Customer.Name();
        
        // Set first name
        name.setFirst("Jane");
        
        // Assert that the first name is correctly set
        assertEquals("Jane", name.getFirst(), "First name should be set correctly.");
    }

    
    @Test
    void testSetLast() {
        // Create a new name object
        Customer.Name name = new Customer.Name();
        
        // Set last name
        name.setLast("Doe");
        
        // Assert that the last name is correctly set
        assertEquals("Doe", name.getLast(), "Last name should be set correctly.");
    }

    
    @Test
    void testCustomerNameConstructor() {
        // Test the no-argument constructor
        Customer.Name name = new Customer.Name();
        
        // Assert that the object is created and the fields are null or empty
        assertNull(name.getFirst(), "First name should be null.");
        assertNull(name.getLast(), "Last name should be null.");
        
        // Test the parameterized constructor
        Customer.Name nameWithValues = new Customer.Name("John", "Doe");
        
        // Assert that the fields are set correctly
        assertEquals("John", nameWithValues.getFirst(), "First name should be 'John'.");
        assertEquals("Doe", nameWithValues.getLast(), "Last name should be 'Doe'.");
    }

    
    @Test
    void testCanEqual() {
        Customer.Name name1 = new Customer.Name("John", "Doe");
        Customer.Name name2 = new Customer.Name("John", "Doe");
        
        // Assert that canEqual returns true for objects of the same class
        assertTrue(name1.canEqual(name2), "CanEqual should return true for objects of the same class.");
        
        // Assert that canEqual returns false for objects of different classes
        String otherObject = "Some String";
        assertFalse(name1.canEqual(otherObject), "CanEqual should return false for objects of different types.");
    }

      @Test
        void testSettersAndGetters() {
        	
        	Address address = new Address();
            // Set new values and test getters
        	address.setStreet("456 Elm St");
        	address.setCity("Chicago");
        	address.setState("IL");
        	address.setZip(60601);
        	address.setCountry("USA");

            assertEquals("456 Elm St", address.getStreet());
            assertEquals("Chicago", address.getCity());
            assertEquals("IL", address.getState());
            assertEquals(60601, address.getZip());
            assertEquals("USA", address.getCountry());
        }

}
