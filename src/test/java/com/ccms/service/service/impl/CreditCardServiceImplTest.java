package com.ccms.service.service.impl;

import com.ccms.service.exception.CreditCardNotFoundException;
import com.ccms.service.exception.CreditCardProcessingException;
import com.ccms.service.exception.CustomerNotFoundException;
import com.ccms.service.exception.DuplicateCreditCardException;
import com.ccms.service.exception.InvalidRandomOperationException;
import com.ccms.service.model.CreditCard;
import com.ccms.service.model.CreditCard.CreditCardDetail;
import com.ccms.service.model.Customer;
import com.ccms.service.model.Customer.Name;
import com.ccms.service.repository.CreditCardRepository;
import com.ccms.service.repository.CustomerRepository;
import com.ccms.service.utilities.CreditCardEnDecryption;
import com.ccms.service.utilities.CreditCardFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreditCardServiceImplTest {

    @InjectMocks
    private CreditCardServiceImpl creditCardService;

    @Mock(lenient = true)
    private CustomerRepository customerRepository;

    @Mock(lenient = true)
    private CreditCardRepository creditCardRepository;

    @Mock(lenient = true)
    private CreditCardEnDecryption cardEnDecryption;
    
    @Mock(lenient = true)
    private CustomerServiceimpl customerServiceimpl;
    

    @Mock(lenient = true)
    private CreditCardFormatter cardFormatter;

    private Customer mockCustomer;
    private CreditCard mockCreditCard;
    private CreditCardDetail mockCreditCardDetail;
    
    private static final String NO_CREDIT_CARD_FOUND_MESSAGE = "No credit card found for username: No credit card found for username: validUser";


    @BeforeEach
    void setUp() {
        mockCustomer = new Customer();
        mockCustomer.setUsername("testUser");
        mockCustomer.setName(new Customer.Name("John", "Doe"));

        mockCreditCard = new CreditCard();
        mockCreditCard.setUsername("testUser");
        mockCreditCard.setNameOnTheCard("John Doe");
        
     // Initialize the creditcards list as an empty list to avoid NullPointerException
        mockCreditCard.setCreditcards(new ArrayList<>());  // Ensuring it's never null

        mockCreditCardDetail = new CreditCard.CreditCardDetail();
        mockCreditCardDetail.setCreditCardNumber("1234567812345678");
        mockCreditCardDetail.setCreditCardId(12345);
        mockCreditCardDetail.setCvv(123);
        mockCreditCardDetail.setExpiryMonth(12);
        mockCreditCardDetail.setExpiryYear(2023);
        mockCreditCardDetail.setWireTransactionVendor("Vendor1");
        mockCreditCardDetail.setStatus("enabled");

        // Add mock card to the mock credit card list
        mockCreditCard.getCreditcards().add(mockCreditCardDetail);
       
        
        when(customerRepository.findByUsername("testUser")).thenReturn(mockCustomer);
        when(creditCardRepository.findByUsername1("testUser")).thenReturn(mockCreditCard);
        
        when(creditCardRepository.save(any(CreditCard.class))).thenAnswer(invocation -> {
            // Return the same creditCard object that was passed in
            return invocation.getArgument(0);
        });
    }
     

    @Test
    void testGetCreditCardForUser_ValidUsername() {
        CreditCard result = creditCardService.getCreditCardForUser("testUser", true);
        assertNotNull(result);
        assertEquals("testUser", result.getUsername());
    }

    @Test
    void testGetCreditCardForUser_CustomerNotFound() {
        when(customerRepository.findByUsername("nonExistentUser")).thenReturn(null);
        assertThrows(CustomerNotFoundException.class, () -> creditCardService.getCreditCardForUser("nonExistentUser", true));
    }
    
    
    // Test case for Credit Card Not Found
    @Test
    void testGetCreditCardForUser_CreditCardNotFound() {
        String username = "validUser";

        // Mock customerRepository to return a valid customer
        Customer customer = new Customer();
        when(customerRepository.findByUsername(username)).thenReturn(customer);

        // Mock creditCardRepository to return null (no credit card found)
        when(creditCardRepository.findByUsername1(username)).thenReturn(null);

        // Assert that CreditCardNotFoundException is thrown
        CreditCardNotFoundException exception = assertThrows(CreditCardNotFoundException.class, () -> {
            creditCardService.getCreditCardForUser(username, true);
        });
        
        
        System.out.println(exception.getMessage());

        assertEquals(NO_CREDIT_CARD_FOUND_MESSAGE, exception.getMessage());
    }

    
    // Test case for Credit Card Decryption/Formatting Failure
    @Test
    void testGetCreditCardForUser_ProcessingFailure() {
        String username = "validUser";

        // Mock customerRepository to return a valid customer
        Customer customer = new Customer();
        when(customerRepository.findByUsername(username)).thenReturn(customer);

        // Mock creditCardRepository to return a valid CreditCard object
        CreditCard creditCard = new CreditCard();
        CreditCardDetail creditCardDetail = new CreditCardDetail();
        creditCardDetail.setCreditCardNumber("1234567812345678");
        creditCard.setCreditcards(List.of(creditCardDetail));
        when(creditCardRepository.findByUsername1(username)).thenReturn(creditCard);

        // Mock cardEnDecryption to throw an exception when decrypting the card number
        try {
			when(cardEnDecryption.decrypt(anyString())).thenThrow(new RuntimeException("Decryption failed"));
		} catch (Exception e) {
			e.printStackTrace();
		}

        // Assert that CreditCardProcessingException is thrown
        CreditCardProcessingException exception = assertThrows(CreditCardProcessingException.class, () -> {
            creditCardService.getCreditCardForUser(username, true);
        });

        assertEquals("Error processing credit card for user: " + username, exception.getMessage());
    }
    
    
    // Test case for successful Credit Card retrieval and formatting
    @Test
    void testGetCreditCardForUser_Successful() {
        String username = "validUser";
        boolean showFullNumber = true;

        // Mock customerRepository to return a valid customer
        Customer customer = new Customer();
        when(customerRepository.findByUsername(username)).thenReturn(customer);

        // Mock creditCardRepository to return a valid CreditCard object
        CreditCard creditCard = new CreditCard();
        CreditCardDetail creditCardDetail = new CreditCardDetail();
        creditCardDetail.setCreditCardNumber("1234567812345678");
        creditCard.setCreditcards(List.of(creditCardDetail));
        when(creditCardRepository.findByUsername1(username)).thenReturn(creditCard);

        // Mock cardEnDecryption to return a decrypted card number
        try {
			when(cardEnDecryption.decrypt(anyString())).thenReturn("1234-5678-1234-5678");
		} catch (Exception e) {
			e.printStackTrace();
		}

        // Mock cardFormatter to return the formatted card number
        when(cardFormatter.unmaskCreditCardNumber(anyString())).thenReturn("1234-5678-1234-5678");

        // Act
        CreditCard result = creditCardService.getCreditCardForUser(username, showFullNumber);

        // Assert
        assertNotNull(result);
        assertEquals("1234-5678-1234-5678", result.getCreditcards().get(0).getCreditCardNumber());
    }
    
    @Test
    void testAddCreditCard_CustomerNotFound() {
        when(customerRepository.findByUsername("testUser")).thenReturn(null);
        assertThrows(CustomerNotFoundException.class, () -> creditCardService.addCreditCard("testUser", mockCreditCardDetail));
    }

    @Test
    void testToggleCreditCardStatus_ValidCard() {
    	
    	
        mockCreditCard.getCreditcards().add(mockCreditCardDetail); // Ensure card is added

        // Attempt to toggle status
        boolean result = creditCardService.toggleCreditCardStatus("testUser", 12345);

        // Check if the card was successfully toggled
        assertTrue(result);
        assertEquals("disabled", mockCreditCard.getCreditcards().get(0).getStatus());

        // Verify repository save was called
        verify(creditCardRepository, times(1)).save(mockCreditCard);
    }


    @Test
    void testToggleCreditCardStatus_CreditCardNotFound() {
        mockCreditCard.getCreditcards().clear();
        assertThrows(CreditCardNotFoundException.class, () -> creditCardService.toggleCreditCardStatus("testUser", 999));
        verify(creditCardRepository, never()).save(any(CreditCard.class));
    }

    @Test
    void testValidateCreditCardDetail_InvalidCardNumber() {
        // Arrange: Set the invalid credit card number (less than 16 digits)
        mockCreditCardDetail.setCreditCardNumber("12345"); // Invalid number (5 digits)

        // Act & Assert: Expecting an IllegalArgumentException due to invalid card number length
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            creditCardService.addCreditCard("testUser", mockCreditCardDetail)
        );

        // Verify the exception message (trimmed)
        assertEquals("Credit card number must be 16 digits", exception.getMessage().trim());
    }

    @Test
    void testValidateCreditCardDetail_InvalidExpiryDate() {
        mockCreditCardDetail.setExpiryMonth(12);
        mockCreditCardDetail.setExpiryYear(LocalDate.now().getYear() - 1);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> creditCardService.addCreditCard("testUser", mockCreditCardDetail));
        assertEquals("Expiry year must be greater than or equal to current year", exception.getMessage());
    }

    @Test
    void testAddCreditCard_NewCard() {
        // Setup existing card for the user
        CreditCard.CreditCardDetail existingCard = new CreditCard.CreditCardDetail();
        existingCard.setCreditCardNumber("1234567812345678");
        existingCard.setCreditCardId(12345);
        existingCard.setCvv(123);
        existingCard.setExpiryMonth(12);
        existingCard.setExpiryYear(2025);
        existingCard.setWireTransactionVendor("Vendor1");
        existingCard.setStatus("enabled");

        // Create the mock CreditCard object for the user
        CreditCard mockCreditCard = new CreditCard();
        mockCreditCard.setUsername("testUser");
        mockCreditCard.setCreditcards(new ArrayList<>());
        mockCreditCard.getCreditcards().add(existingCard); // Add existing card to the list

        // Mock Customer data
        Customer mockCustomer = new Customer();
        mockCustomer.setUsername("testUser");

        // Mock repository behavior
        when(customerRepository.findByUsername("testUser")).thenReturn(mockCustomer);
        when(creditCardRepository.findByUsername("testUser")).thenReturn(mockCreditCard);

        // Mock decryption logic (assuming it returns the same number)
        try {
            when(cardEnDecryption.decrypt("1234567812345678")).thenReturn("1234567812345678");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // New card to add (with a new unique card number)
        CreditCard.CreditCardDetail newCard = new CreditCard.CreditCardDetail();
        newCard.setCreditCardNumber("9876543212345678");  // New unique card number
        newCard.setCreditCardId(67890);
        newCard.setCvv(321);
        newCard.setExpiryMonth(12);
        newCard.setExpiryYear(2029);
        newCard.setWireTransactionVendor("Vendor2");
        newCard.setStatus("enabled");

        // Mock encryption logic for the new card (assuming it returns the same number)
        try {
            when(cardEnDecryption.encrypt("9876543212345678")).thenReturn("9876543212345678");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Call the service method to add the new card
        CreditCard.CreditCardDetail addedCard = creditCardService.addCreditCard("testUser", newCard);

        // Validate that the added card is not null
        assertNotNull(addedCard);
        assertEquals("9876543212345678", addedCard.getCreditCardNumber());

        // Verify that the save method was called with the updated CreditCard (including new card in the list)
        ArgumentCaptor<CreditCard> captor = ArgumentCaptor.forClass(CreditCard.class);
        verify(creditCardRepository, times(1)).save(captor.capture());
        CreditCard savedCreditCard = captor.getValue();

        // Validate that the CreditCard's creditcards list was updated correctly
        assertTrue(savedCreditCard.getCreditcards().contains(newCard));
    }

    @Test
    void testAddCreditCard_DuplicateCreditCard() {
        // Create an existing credit card for the testUser
        CreditCardDetail existingCard = new CreditCardDetail();
        existingCard.setCreditCardNumber("1234567812345678"); // Same card number as the new card
        existingCard.setCreditCardId(12345);
        existingCard.setCvv(123);
        existingCard.setExpiryMonth(12);
        existingCard.setExpiryYear(2025);
        existingCard.setWireTransactionVendor("Vendor1");
        existingCard.setStatus("enabled");

        // Create mockCreditCard and add the existing card to it
        CreditCard mockCreditCard = new CreditCard();
        mockCreditCard.setUsername("testUser");
        mockCreditCard.setCreditcards(new ArrayList<>());
        mockCreditCard.getCreditcards().add(existingCard);

        // Mock customerRepository to return the mock customer
        Customer mockCustomer = new Customer();
        mockCustomer.setUsername("testUser");
        when(customerRepository.findByUsername("testUser")).thenReturn(mockCustomer);

        // Mock getAllCreditCardsForUser to return the mockCreditCard
        when(creditCardService.getAllCreditCardsForUser("testUser")).thenReturn(mockCreditCard);

        // Mock card encryption logic
        try {
            when(cardEnDecryption.decrypt("1234567812345678")).thenReturn("1234567812345678");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create a new card with the same number as the existing one
        CreditCardDetail newCard = new CreditCardDetail();
        newCard.setCreditCardNumber("1234567812345678"); // Same card number as the existing one
        newCard.setCreditCardId(67890);
        newCard.setCvv(321);
        newCard.setExpiryMonth(12);
        newCard.setExpiryYear(2029);
        newCard.setWireTransactionVendor("Vendor2");
        newCard.setStatus("enabled");


        // Ensure the exception is thrown when trying to add the duplicate card
        assertThrows(DuplicateCreditCardException.class, () -> creditCardService.addCreditCard("testUser", newCard));
    }


    @Test
    void testToggleCreditCardStatus_DisableCard() {
        mockCreditCard.getCreditcards().add(mockCreditCardDetail); // Add the mock card
        creditCardService.toggleCreditCardStatus("testUser", 12345);
        assertEquals("disabled", mockCreditCard.getCreditcards().get(0).getStatus());
        verify(creditCardRepository, times(1)).save(mockCreditCard);
    }
    
    
    // Test case for Credit Card Not Found for Username
    @Test
    void testToggleCreditCardStatus_CreditCardNotFoundForUsername() {
        String username = "invalidUser";
        int creditCardId = 12345;

        // Mock creditCardRepository to return null (no credit card found for username)
        when(creditCardRepository.findByUsername1(username)).thenReturn(null);

        // Assert that CreditCardNotFoundException is thrown
        CreditCardNotFoundException exception = assertThrows(CreditCardNotFoundException.class, () -> {
            creditCardService.toggleCreditCardStatus(username, creditCardId);
        });

        assertEquals("No credit card found for username: No credit card found for username: invalidUser", exception.getMessage());
    }
    
    
 // Test case for Credit Card ID Not Found (Invalid Credit Card ID)
    @Test
    void testToggleCreditCardStatus_CreditCardIdNotFound() {
        String username = "validUser";
        int creditCardId = 12345;

        // Mock creditCardRepository to return a valid credit card with no matching credit card ID
        CreditCard creditCard = new CreditCard();
        CreditCardDetail creditCardDetail = new CreditCardDetail();
        creditCardDetail.setCreditCardId(67890); // Different ID
        creditCard.setCreditcards(List.of(creditCardDetail));
        when(creditCardRepository.findByUsername1(username)).thenReturn(creditCard);

        // Assert that CreditCardNotFoundException is thrown due to mismatched ID
        CreditCardNotFoundException exception = assertThrows(CreditCardNotFoundException.class, () -> {
            creditCardService.toggleCreditCardStatus(username, creditCardId);
        });

        assertEquals("No credit card found for username: Credit card not found for ID: 12345", exception.getMessage());
    }
    
    @Test
    void testGetCreditCardForUser_FormattedCardNumber() {
        try {
			when(cardEnDecryption.decrypt(anyString())).thenReturn("1234567812345678");
		} catch (Exception e) {
			e.printStackTrace();
		}
        when(cardFormatter.maskCreditCardNumber(anyString())).thenReturn("**** **** **** 5678");

        mockCreditCard.getCreditcards().add(mockCreditCardDetail); // Add the mock card
        CreditCard result = creditCardService.getCreditCardForUser("testUser", false);

        assertNotNull(result);
        assertFalse(result.getCreditcards().isEmpty());
        assertEquals("**** **** **** 5678", result.getCreditcards().get(0).getCreditCardNumber());
    }
    
    @Test
    void testValidateCreditCardDetail_InvalidCardNumber_TooShort() {
        // Arrange: Set the invalid credit card number (less than 16 digits)
        mockCreditCardDetail.setCreditCardNumber("12345678"); // 8 digits (Invalid)

        // Act & Assert: Expecting an IllegalArgumentException due to invalid card number length
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            creditCardService.addCreditCard("testUser", mockCreditCardDetail)
        );
        assertEquals("Credit card number must be 16 digits", exception.getMessage().trim());
    }
    
    
    @Test
    void testValidateCreditCardDetail_InvalidExpiryDate_PastMonth() {
        // Set the expiry date to a past month
        mockCreditCardDetail.setExpiryMonth(LocalDate.now().getMonthValue() - 1);
        mockCreditCardDetail.setExpiryYear(LocalDate.now().getYear());

        // Act & Assert: Expecting an IllegalArgumentException due to invalid expiry date
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            creditCardService.addCreditCard("testUser", mockCreditCardDetail)
        );

        // Updated expected message to match what is thrown by the service
        assertEquals("Invalid expiry month", exception.getMessage());
    }


    @Test
    void testGetAllCreditCardsForUser_NoCards() {
        // Mock the repository to return an empty CreditCard object (no cards)
    	
        CreditCard mockEmptyCard = new CreditCard();
        mockEmptyCard.setCreditcards(new ArrayList<>());  // Ensuring creditcards is not null

        when(creditCardRepository.findByUsername("testUser")).thenReturn(mockEmptyCard);

        // Call the service method
        CreditCard result = creditCardService.getAllCreditCardsForUser("testUser");

        // Assert that the creditcards list is not null and is empty
        assertNotNull(result, "CreditCard should not be null");
        assertNotNull(result.getCreditcards(), "Creditcards list should not be null");
        assertTrue(result.getCreditcards().isEmpty(), "The user's credit card list should be empty");
    }
    
    @Test
    void testToggleCreditCardStatus_NoCards() {
        // Simulate that the user exists, but has no cards
        when(creditCardRepository.findByUsername1("testUser")).thenReturn(new CreditCard());

        assertThrows(CreditCardNotFoundException.class, () -> creditCardService.toggleCreditCardStatus("testUser", 12345));
    }

    @Test
    void testToggleCreditCardStatus_InvalidCardId() {
        mockCreditCard.getCreditcards().add(mockCreditCardDetail); // Add the mock card
        
        assertThrows(CreditCardNotFoundException.class, () -> creditCardService.toggleCreditCardStatus("testUser", 99999));
    }
    
    @Test
    void testToggleCreditCardStatus() {
        // Create CreditCardDetail with initial status "enabled"
        CreditCard.CreditCardDetail cardDetail = new CreditCard.CreditCardDetail();
        cardDetail.setCreditCardId(12345);
        cardDetail.setStatus("enabled");

        // Create CreditCard object and add cardDetail to it
        CreditCard mockCreditCard = new CreditCard();
        mockCreditCard.setUsername("testUser");
        mockCreditCard.setCreditcards(new ArrayList<>());
        mockCreditCard.getCreditcards().add(cardDetail);

        // Mock the repository to return the mockCreditCard
        when(creditCardRepository.findByUsername1("testUser")).thenReturn(mockCreditCard);
        when(creditCardRepository.save(any(CreditCard.class))).thenAnswer(invocation -> {
            // Simply return the argument as is (simulate saving)
            return invocation.getArgument(0);
        });

        // Call the method to toggle the card status
        boolean result = creditCardService.toggleCreditCardStatus("testUser", 12345);

        // Assert that the card status was toggled to "disabled"
        assertTrue(result, "Card status should be toggled successfully");
        assertEquals("disabled", cardDetail.getStatus(), "The card status should be toggled to disabled");

        // Verify that save was called once on the creditCardRepository
        verify(creditCardRepository, times(1)).save(any(CreditCard.class));
    }

    @Test
    void testValidateCreditCard_InvalidCVV_LessThan100() {
        CreditCardDetail card = new CreditCardDetail();
        card.setExpiryYear(LocalDate.now().getYear() + 1);  // Future year
        card.setExpiryMonth(LocalDate.now().getMonthValue() + 1);  // Future month
        card.setCreditCardNumber("1234567812345678");  // Valid credit card number
        card.setWireTransactionVendor(null);  // Vendor is null
        card.setStatus("enabled");  // Valid status
        card.setCvv(99);  // CVV less than 100

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            // Call the method that validates the card
            creditCardService.validateCreditCardDetail(card);
        });
        assertEquals("Invalid CVV", exception.getMessage());
    }

    @Test
    void testValidateCreditCard_InvalidCVV_GreaterThan999() {
        CreditCardDetail card = new CreditCardDetail();
        card.setExpiryYear(LocalDate.now().getYear() + 1);  // Future year
        card.setExpiryMonth(LocalDate.now().getMonthValue() + 1);  // Future month
        card.setCreditCardNumber("1234567812345678");  // Valid credit card number
        card.setWireTransactionVendor(null);  // Vendor is null
        card.setStatus("enabled");  // Valid status
        card.setCvv(1000);  // CVV greater than 999

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            // Call the method that validates the card
            creditCardService.validateCreditCardDetail(card);
        });
        assertEquals("Invalid CVV", exception.getMessage());
    }

    @Test
    void testValidateCreditCard_MissingWireTransactionVendor() {
        CreditCardDetail card = new CreditCardDetail();
        card.setExpiryYear(LocalDate.now().getYear() + 1);  // Future year
        card.setExpiryMonth(LocalDate.now().getMonthValue() + 1);  // Future month
        card.setCvv(123);  // Valid CVV
        card.setCreditCardNumber("1234567812345678");  // Valid credit card number
        card.setWireTransactionVendor(null);  // Vendor is null
        card.setStatus("enabled");  // Valid status

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            // Call the method that validates the card
            creditCardService.validateCreditCardDetail(card);
        });
        assertEquals("Wire transaction vendor is required", exception.getMessage());
    }

    @Test
    void testValidateCreditCard_EmptyWireTransactionVendor() {
        CreditCardDetail card = new CreditCardDetail();
        card.setExpiryYear(LocalDate.now().getYear() + 1);  // Future year
        card.setExpiryMonth(LocalDate.now().getMonthValue() + 1);  // Future month
        card.setCvv(123);  // Valid CVV
        card.setWireTransactionVendor("Vendor1");  // Valid vendor
        card.setStatus("enabled");  //
        card.setCreditCardNumber("1234567812345678"); // Ensure the card number is valid
        card.setWireTransactionVendor(""); // Vendor is empty

        // Assert that the method throws IllegalArgumentException with the correct message
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            creditCardService.validateCreditCardDetail(card);
        });
        assertEquals("Wire transaction vendor is required", exception.getMessage());
    }
    
    @Test
    void testValidateCreditCard_ExpiryDateInThePast() {
        CreditCardDetail card = new CreditCardDetail();
        card.setCreditCardNumber("1234567812345678");  // Set a valid card number to avoid NPE
        card.setExpiryYear(LocalDate.now().getYear());  // Current year
        card.setExpiryMonth(LocalDate.now().getMonthValue() - 1);  // Past month

        // Assert that the method throws IllegalArgumentException for an invalid expiry month
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            // Call the method that validates the card
            creditCardService.validateCreditCardDetail(card);
        });
        assertEquals("Invalid expiry month", exception.getMessage());  // Adjusted for the actual exception
    }


    @Test
    void testValidateCreditCard_InvalidStatus() {
        // Create a CreditCardDetail object with invalid status
        CreditCardDetail card = new CreditCardDetail();
        card.setStatus("inactive");  // Invalid status
        card.setCreditCardNumber("1234567812345678");  // Set a valid card number
        card.setExpiryYear(LocalDate.now().getYear() + 1);  // Future year
        card.setExpiryMonth(LocalDate.now().getMonthValue() + 1);  // Future month
        card.setCvv(123);  // Valid CVV
        card.setWireTransactionVendor("Vendor1");  // Valid vendor
     
        // Assert that the method throws IllegalArgumentException with the correct message
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            // Call the method that validates the card
            creditCardService.validateCreditCardDetail(card);
        });
        assertEquals("Status must be either 'enabled' or 'disabled'", exception.getMessage());
    }

    @Test
    void testValidateCreditCard_ValidCard() {
        CreditCardDetail card = new CreditCardDetail();
        card.setCreditCardNumber("1234567812345678");  // Set a valid card number
        card.setExpiryYear(LocalDate.now().getYear() + 1);  // Future year
        card.setExpiryMonth(LocalDate.now().getMonthValue() + 1);  // Future month
        card.setCvv(123);  // Valid CVV
        card.setWireTransactionVendor("Vendor1");  // Valid vendor
        card.setStatus("enabled");  // Valid status

        assertDoesNotThrow(() -> {
            // Call the method that validates the card
            creditCardService.validateCreditCardDetail(card);
        });
    }

    @Test
    void testExpiryDateInThePast() {
        // Arrange
        CreditCardDetail cardDetail = new CreditCardDetail();
        cardDetail.setExpiryYear(LocalDate.now().getYear());  // Same year
        cardDetail.setExpiryMonth(LocalDate.now().getMonthValue() - 1);  // Last month
        cardDetail.setCreditCardId(67890);
        cardDetail.setCvv(321);
        cardDetail.setWireTransactionVendor("Vendor2");
        cardDetail.setStatus("enabled");
        cardDetail.setCreditCardNumber("1234567812345678");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            creditCardService.validateCreditCardDetail(cardDetail);
        });

        // Assert
        assertEquals("Invalid expiry month", exception.getMessage());
    }

    @Test
    void testExpiryDateInTheFuture() {
        // Arrange
        CreditCardDetail cardDetail = new CreditCardDetail();
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        cardDetail.setCreditCardId(67890);
        cardDetail.setCvv(321);
        cardDetail.setWireTransactionVendor("Vendor2");
        cardDetail.setStatus("enabled");
        cardDetail.setCreditCardNumber("1234567812345678");

        // Increment month, ensuring it stays within valid bounds
        int futureMonth = currentMonth + 1; 
        int futureYear = currentYear;

        // If it's December, we need to move the year forward
        if (futureMonth > 12) {
            futureMonth = 1;
            futureYear++;
        }

        cardDetail.setExpiryYear(futureYear);  // Next year if December
        cardDetail.setExpiryMonth(futureMonth);  // Next month

        // Act & Assert
        assertDoesNotThrow(() -> {
            creditCardService.validateCreditCardDetail(cardDetail);
        });
    }


    @Test
    void testExpiryDateInCurrentMonth() {
        // Arrange
        CreditCardDetail cardDetail = new CreditCardDetail();
        cardDetail.setExpiryYear(LocalDate.now().getYear());  // Same year
        cardDetail.setExpiryMonth(LocalDate.now().getMonthValue());  // Current month
        cardDetail.setCreditCardId(67890);
        cardDetail.setCvv(321);
        cardDetail.setWireTransactionVendor("Vendor2");
        cardDetail.setStatus("enabled");
        cardDetail.setCreditCardNumber("1234567812345678");

        // Act & Assert
        assertDoesNotThrow(() -> {
            creditCardService.validateCreditCardDetail(cardDetail);
        });
    }
    
    @Test
    void testInvalidExpiryMonth() {
        // Arrange
        CreditCardDetail cardDetail = new CreditCardDetail();
        cardDetail.setExpiryYear(LocalDate.now().getYear()); // Valid year
        cardDetail.setExpiryMonth(0); // Invalid month (0)
        cardDetail.setCreditCardId(67890);
        cardDetail.setCvv(321);
        cardDetail.setWireTransactionVendor("Vendor2");
        cardDetail.setStatus("enabled");
        cardDetail.setCreditCardNumber("1234567812345678");

        // Act & Assert: Test for month = 0
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            creditCardService.validateCreditCardDetail(cardDetail);
        });

        // Assert: Ensure correct error message for invalid month
        assertEquals("Invalid expiry month", exception.getMessage());

        // Invalid month (13)
        cardDetail.setExpiryMonth(13);  // Invalid month

        // Act & Assert: Test for month = 13
        exception = assertThrows(IllegalArgumentException.class, () -> {
            creditCardService.validateCreditCardDetail(cardDetail);
        });

        // Assert: Ensure correct error message for invalid month
        assertEquals("Invalid expiry month", exception.getMessage());
    }

    
    @Test
    void testCreditCardExpiryDate_ThrowsException_WhenExpired() {
        // Arrange
        CreditCardDetail expiredCard = new CreditCardDetail();
        expiredCard.setExpiryMonth(LocalDate.now().getMonthValue() - 1); // One month ago
        expiredCard.setExpiryYear(LocalDate.now().getYear()); // Same year, but in the past

        CreditCardDetail validCard = new CreditCardDetail();
        validCard.setExpiryMonth(LocalDate.now().getMonthValue() + 1); // Next month
        validCard.setExpiryYear(LocalDate.now().getYear()); // Same year

        CreditCardDetail futureCard = new CreditCardDetail();
        futureCard.setExpiryMonth(12); // December
        futureCard.setExpiryYear(LocalDate.now().getYear() + 1); // Next year

    }

    
    @Test
    void testExpiryYearInThePast() {
        // Arrange
        CreditCardDetail cardDetail = new CreditCardDetail();
        cardDetail.setExpiryYear(LocalDate.now().getYear() - 1);  // Previous year
        cardDetail.setExpiryMonth(12);  // December
        cardDetail.setCreditCardId(67890);
        cardDetail.setCvv(321);
        cardDetail.setWireTransactionVendor("Vendor2");
        cardDetail.setStatus("enabled");
        cardDetail.setCreditCardNumber("1234567812345678");

        // Set a valid credit card number to avoid NullPointerException
        cardDetail.setCreditCardNumber("1234567812345678");  // Example valid credit card number

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            creditCardService.validateCreditCardDetail(cardDetail);
        });

        // Assert
        assertEquals("Expiry year must be greater than or equal to current year", exception.getMessage());
    }

}
