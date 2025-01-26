package com.ccms.service.controller;

import com.ccms.service.exception.DuplicateCreditCardException;
import com.ccms.service.exception.InvalidUsernameFormatException;
import com.ccms.service.kafka.CreditCardKafkaProducer;
import com.ccms.service.model.CreditCard;
import com.ccms.service.model.CreditCard.CreditCardDetail;
import com.ccms.service.service.CreditCardService;
import com.ccms.service.service.impl.CreditCardServiceImpl;
import com.ccms.service.utilities.Decodename;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;


@ExtendWith(MockitoExtension.class)
public class CreditCardControllerTest {

    @InjectMocks
    private CreditCardController creditCardController;

    @Mock
    private CreditCardService creditCardService;

    @Mock
    private Decodename decodename;

    @Mock
    private CreditCardKafkaProducer creditCardKafkaProducer;  // Mock Kafka producer

    private MockMvc mockMvc;
    
  @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(creditCardController).build();
        MockitoAnnotations.openMocks(this);
    }

    // Test for adding a new credit card - Success scenario
    @Test
    public void testAddCreditCard_Success() throws Exception {
        String encodedUsername = "encodedUser123";
        String username = "user123";
        
        CreditCard.CreditCardDetail creditCardDetail = new CreditCard.CreditCardDetail(
                123, "1234-5678-9876-5432", 12, 2025, 123, "Visa", "Active"
        );

        when(decodename.decodeUsername(encodedUsername)).thenReturn(username);
        when(creditCardService.addCreditCard(username, creditCardDetail)).thenReturn(creditCardDetail);
        doNothing().when(creditCardKafkaProducer).sendCreditCardLog(anyString());  // Mocking Kafka producer's method

        mockMvc.perform(post("/api/customer/creditcard/addcreditcard/{username}", encodedUsername)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"creditCardId\": 123, \"creditCardNumber\": \"1234-5678-9876-5432\", \"expiryMonth\": 12, \"expiryYear\": 2025, \"cvv\": 123, \"wireTransactionVendor\": \"Visa\", \"status\": \"Active\" }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creditCardId").value(123))
                .andExpect(jsonPath("$.creditCardNumber").value("1234-5678-9876-5432"))
                .andExpect(jsonPath("$.expiryMonth").value(12))
                .andExpect(jsonPath("$.expiryYear").value(2025))
                .andExpect(jsonPath("$.cvv").value(123))
                .andExpect(jsonPath("$.wireTransactionVendor").value("Visa"))
                .andExpect(jsonPath("$.status").value("Active"));
    }


    // Test for adding a duplicate credit card
    @Test
    public void testAddCreditCard_Duplicate() throws Exception {
        String encodedUsername = "encodedUser123";
        String username = "user123";
        
        CreditCard.CreditCardDetail creditCardDetail = new CreditCard.CreditCardDetail(
                123, "1234-5678-9876-5432", 12, 2025, 123, "Visa", "Active"
        );

        when(decodename.decodeUsername(encodedUsername)).thenReturn(username);
        when(creditCardService.addCreditCard(username, creditCardDetail)).thenThrow(new DuplicateCreditCardException("Card already exists"));
        doNothing().when(creditCardKafkaProducer).sendCreditCardLog(anyString());  // Mocking Kafka producer's method

        mockMvc.perform(post("/api/customer/creditcard/addcreditcard/{username}", encodedUsername)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"creditCardId\": 123, \"creditCardNumber\": \"1234-5678-9876-5432\", \"expiryMonth\": 12, \"expiryYear\": 2025, \"cvv\": 123, \"wireTransactionVendor\": \"Visa\", \"status\": \"Active\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").value("Card already exists"));
    }


    // Test for toggling credit card status - Success scenario
    @Test
    public void testToggleCreditCardStatus_Success() throws Exception {
        String encodedUsername = "encodedUser123";
        String username = "user123";
        int creditCardId = 123;

        when(decodename.decodeUsername(encodedUsername)).thenReturn(username);
        when(creditCardService.toggleCreditCardStatus(username, creditCardId)).thenReturn(true);

        mockMvc.perform(put("/api/customer/creditcard/togglecreditcard/{username}/{creditCardId}/toggle", encodedUsername, creditCardId))
                .andExpect(status().isOk())
                .andExpect(content().string("Credit card status toggled successfully"));
    }
    
    @Test
    public void testGetCreditCardsForUser_Success() throws Exception {
        String encodedUsername = "encodedUser123";
        boolean showFullNumber = false;

        // Mocking the decoded username and a valid response
        when(decodename.decodeUsername(encodedUsername)).thenReturn("user123");

        // Creating mock CreditCard objects to return
        CreditCard.CreditCardDetail cardDetail1 = new CreditCard.CreditCardDetail(123, "4111111111111111", 12, 2025, 123, "Vendor1", "ACTIVE");
        CreditCard.CreditCardDetail cardDetail2 = new CreditCard.CreditCardDetail(124, "4111111111112222", 5, 2024, 456, "Vendor2", "INACTIVE");

        CreditCard creditCard = new CreditCard();
        creditCard.setUsername("user123");
        creditCard.setNameOnTheCard("John Doe");
        creditCard.setCreditcards(List.of(cardDetail1, cardDetail2));

        // Mock the service call
        when(creditCardService.getCreditCardForUser(anyString(), anyBoolean())).thenReturn(creditCard);

        // Perform the GET request and verify the result
        mockMvc.perform(get("/api/customer/creditcard/listcreditcards/{username}", encodedUsername)
                .param("showFullNumber", String.valueOf(showFullNumber)))
            .andExpect(status().isOk())  // HTTP status 200
            .andExpect(jsonPath("$.creditcards").isArray())  // Ensure 'creditcards' is an array
            .andExpect(jsonPath("$.creditcards.length()").value(2))  // Verify we get 2 credit cards
            .andExpect(jsonPath("$.creditcards[0].creditCardNumber").value("4111111111111111"))  // First card number
            .andExpect(jsonPath("$.creditcards[1].creditCardNumber").value("4111111111112222"));  // Second card number
    }


    @Test
    public void testGetCreditCardsForUser_NotFound() throws Exception {
        String encodedUsername = "encodedUser123";
        boolean showFullNumber = false;

        when(decodename.decodeUsername(encodedUsername)).thenReturn("user123");
        when(creditCardService.getCreditCardForUser(anyString(), anyBoolean()))
            .thenReturn(null);

        mockMvc.perform(get("/api/customer/creditcard/listcreditcards/{username}", encodedUsername)
                .param("showFullNumber", String.valueOf(showFullNumber)))
            .andExpect(status().isNotFound());
    }
    
    @Test
    public void testToggleCreditCardStatus_NotFound() throws Exception {
        String encodedUsername = "encodedUser123";
        int creditCardId = 999;

        when(decodename.decodeUsername(encodedUsername)).thenReturn("user123");
        when(creditCardService.toggleCreditCardStatus(anyString(), anyInt())).thenReturn(false);

        mockMvc.perform(put("/api/customer/creditcard/togglecreditcard/{username}/{creditCardId}/toggle", encodedUsername, creditCardId))
            .andExpect(status().isNotFound());
 
    }
    
    
    @Test
    public void testAddCreditCard_Success1() throws Exception {
        String encodedUsername = "encodedUser123";
        String username = "user123";
        
        CreditCard.CreditCardDetail creditCardDetail = new CreditCard.CreditCardDetail(
                123, "1234-5678-9876-5432", 12, 2025, 123, "Visa", "Active"
        );

        when(decodename.decodeUsername(encodedUsername)).thenReturn(username);
        when(creditCardService.addCreditCard(username, creditCardDetail)).thenReturn(creditCardDetail);
        doNothing().when(creditCardKafkaProducer).sendCreditCardLog(anyString());

        mockMvc.perform(post("/api/customer/creditcard/addcreditcard/{username}", encodedUsername)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"creditCardId\": 123, \"creditCardNumber\": \"1234-5678-9876-5432\", \"expiryMonth\": 12, \"expiryYear\": 2025, \"cvv\": 123, \"wireTransactionVendor\": \"Visa\", \"status\": \"Active\" }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creditCardId").value(123))
                .andExpect(jsonPath("$.creditCardNumber").value("1234-5678-9876-5432"))
                .andExpect(jsonPath("$.expiryMonth").value(12))
                .andExpect(jsonPath("$.expiryYear").value(2025))
                .andExpect(jsonPath("$.cvv").value(123))
                .andExpect(jsonPath("$.wireTransactionVendor").value("Visa"))
                .andExpect(jsonPath("$.status").value("Active"));
    }
    
    @Test
    public void testToggleCreditCardStatus_Success1() throws Exception {
        String encodedUsername = "encodedUser123";
        String username = "user123";
        int creditCardId = 123;

        when(decodename.decodeUsername(encodedUsername)).thenReturn(username);
        when(creditCardService.toggleCreditCardStatus(username, creditCardId)).thenReturn(true);

        mockMvc.perform(put("/api/customer/creditcard/togglecreditcard/{username}/{creditCardId}/toggle", encodedUsername, creditCardId))
                .andExpect(status().isOk())
                .andExpect(content().string("Credit card status toggled successfully"));
    }

    
    @Test
    public void testAddCreditCard_Duplicate1() throws Exception {
        String encodedUsername = "encodedUser123";
        String username = "user123";
        
        CreditCard.CreditCardDetail creditCardDetail = new CreditCard.CreditCardDetail(
                123, "1234-5678-9876-5432", 12, 2025, 123, "Visa", "Active"
        );

        when(decodename.decodeUsername(encodedUsername)).thenReturn(username);
        when(creditCardService.addCreditCard(username, creditCardDetail)).thenThrow(new DuplicateCreditCardException("Card already exists"));
        doNothing().when(creditCardKafkaProducer).sendCreditCardLog(anyString());

        mockMvc.perform(post("/api/customer/creditcard/addcreditcard/{username}", encodedUsername)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"creditCardId\": 123, \"creditCardNumber\": \"1234-5678-9876-5432\", \"expiryMonth\": 12, \"expiryYear\": 2025, \"cvv\": 123, \"wireTransactionVendor\": \"Visa\", \"status\": \"Active\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").value("Card already exists"));
    }

    
    
    @Test
    public void testToggleCreditCardStatus_NotFound1() throws Exception {
        String encodedUsername = "encodedUser123";
        int creditCardId = 999;

        when(decodename.decodeUsername(encodedUsername)).thenReturn("user123");
        when(creditCardService.toggleCreditCardStatus(anyString(), anyInt())).thenReturn(false);

        mockMvc.perform(put("/api/customer/creditcard/togglecreditcard/{username}/{creditCardId}/toggle", encodedUsername, creditCardId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetCreditCardsForUser_Success1() throws Exception {
        String encodedUsername = "encodedUser123";
        boolean showFullNumber = false;

        when(decodename.decodeUsername(encodedUsername)).thenReturn("user123");

        CreditCard.CreditCardDetail cardDetail1 = new CreditCard.CreditCardDetail(123, "4111111111111111", 12, 2025, 123, "Vendor1", "ACTIVE");
        CreditCard.CreditCardDetail cardDetail2 = new CreditCard.CreditCardDetail(124, "4111111111112222", 5, 2024, 456, "Vendor2", "INACTIVE");

        CreditCard creditCard = new CreditCard();
        creditCard.setUsername("user123");
        creditCard.setNameOnTheCard("John Doe");
        creditCard.setCreditcards(List.of(cardDetail1, cardDetail2));

        when(creditCardService.getCreditCardForUser(anyString(), anyBoolean())).thenReturn(creditCard);

        mockMvc.perform(get("/api/customer/creditcard/listcreditcards/{username}", encodedUsername)
                .param("showFullNumber", String.valueOf(showFullNumber)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.creditcards").isArray())
            .andExpect(jsonPath("$.creditcards.length()").value(2))
            .andExpect(jsonPath("$.creditcards[0].creditCardNumber").value("4111111111111111"))
            .andExpect(jsonPath("$.creditcards[1].creditCardNumber").value("4111111111112222"));
    }

    
    @Test
    public void testGetCreditCardsForUser_NotFound1() throws Exception {
        String encodedUsername = "encodedUser123";
        boolean showFullNumber = false;

        when(decodename.decodeUsername(encodedUsername)).thenReturn("user123");
        when(creditCardService.getCreditCardForUser(anyString(), anyBoolean())).thenReturn(null);

        mockMvc.perform(get("/api/customer/creditcard/listcreditcards/{username}", encodedUsername)
                .param("showFullNumber", String.valueOf(showFullNumber)))
            .andExpect(status().isNotFound());
  
    }

    
    @Test
    public void testDecodeUsername_Success() {
        String encodedUsername = "encodedUser123";
        String decodedUsername = "user123";

        when(decodename.decodeUsername(encodedUsername)).thenReturn(decodedUsername);

        String result = decodename.decodeUsername(encodedUsername);

        assertEquals(decodedUsername, result);
    }

    
    @Test
    public void testDecodeUsername_Invalid() {
        String encodedUsername = "invalidEncodedUser";

        when(decodename.decodeUsername(encodedUsername)).thenThrow(new IllegalArgumentException("Invalid encoded username"));

        assertThrows(IllegalArgumentException.class, () -> {
            decodename.decodeUsername(encodedUsername);
        });
    }
  
}
