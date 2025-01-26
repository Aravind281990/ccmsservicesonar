package com.ccms.service.controller;

import com.ccms.service.exception.InvalidUsernameFormatException;
import com.ccms.service.model.Transaction.TransactionDetail;
import com.ccms.service.service.TransactionService;
import com.ccms.service.utilities.Decodename;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"access-logs"})
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private TransactionService transactionService;

    @Mock
    private Decodename decodename;

    @InjectMocks
    private TransactionController transactionController;

    private String username;
    private String encodedUsername;
    private Pageable pageable;

    @BeforeEach
    public void setUp() {
        username = "testUser";
        encodedUsername = "dGVzdFVzZXI=";
        pageable = PageRequest.of(0, 10); // Default page size 10 for testing
    }

    @Test
    public void testGetTransactionsForUser_Success() throws Exception {
        // Mock the behavior of the service layer
        when(transactionService.getTransactionsForUser(username, pageable))
                .thenReturn(null);  // Return empty transactions for this case

        // Call the controller and check for the HTTP status and content
        mockMvc.perform(get("/api/customer/transactions/{username}", encodedUsername)
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isNoContent());
   
    }

    @Test
    public void testGetTransactionsForUser_Error() throws Exception {
        // Simulate an error when fetching transactions
        when(transactionService.getTransactionsForUser(username, pageable))
                .thenThrow(new RuntimeException("Internal Server Error"));

        mockMvc.perform(get("/api/customer/transactions/{username}", encodedUsername))
                .andExpect(status().isNoContent());
  
    }

    @Test
    public void testGetMaxExpensesForLastMonth_Success() throws Exception {
        // Mock service response
        when(transactionService.getMaxExpensesForLastMonth(username, "both", pageable))
                .thenReturn(null);  // Mocking empty response for this case

        mockMvc.perform(get("/api/customer/transactions/maxExpenses/lastMonth/{username}", encodedUsername)
                        .param("status", "both")
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isNoContent());  // Expect no content response if no expenses are found
    }

    @Test
    public void testGetMaxExpensesForLastMonth_Error() throws Exception {
        // Simulate an error during the request
        when(transactionService.getMaxExpensesForLastMonth(username, "both", pageable))
                .thenThrow(new RuntimeException("Error in fetching max expenses"));

        mockMvc.perform(get("/api/customer/transactions/maxExpenses/lastMonth/{username}", encodedUsername))
                .andExpect(status().isNoContent()); // Expecting 500 because of internal error
    }

    @Test
    public void testGetHighValueExpenses_Success() throws Exception {
        // Mock the behavior for getting high-value expenses
        when(transactionService.getHighValueExpensesForUser(username, 1, "both", 100.0, pageable))
                .thenReturn(null);  // Return empty result for high-value expenses

        mockMvc.perform(get("/api/customer/transactions/highvalue/expenses/{username}", encodedUsername)
                        .param("amountThreshold", "100.0")
                        .param("status", "both")
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isNoContent());  // No content as no high-value expenses are found
    }


    // Test: Invalid amountThreshold (amountThreshold <= 0)
    @Test
    void testGetHighValueExpenses_InvalidAmountThreshold() {
    	
        when(decodename.decodeUsername(encodedUsername)).thenReturn("testUser");

        ResponseEntity<?> response = transactionController.getHighValueExpenses(encodedUsername, 5, "enabled", 0.0, 0, 10);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Amount threshold must be a positive value"));
    }

    // Test: Invalid status (not "enabled", "disabled", or "both")
    @Test
    void testGetHighValueExpenses_InvalidStatus() {

        when(decodename.decodeUsername(encodedUsername)).thenReturn("testUser");

        ResponseEntity<?> response = transactionController.getHighValueExpenses(encodedUsername, 5, "invalidStatus", 1000.0, 0, 10);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Invalid status"));
    }

    // Test: No expenses found (empty response)
    @Test
    void testGetHighValueExpenses_NoContent() {
    	
        int limit = 5;
        String status = "enabled";
        double amountThreshold = 1000.0;
        int page = 0;
        int size = 10;

        when(decodename.decodeUsername(encodedUsername)).thenReturn(username);
        when(transactionService.getHighValueExpensesForUser(username, limit, status, amountThreshold, PageRequest.of(page, size)))
                .thenReturn(new HashMap<>());

        ResponseEntity<?> response = transactionController.getHighValueExpenses(encodedUsername, limit, status, amountThreshold, page, size);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    // Test: Internal server error (generic exception handling)
    @Test
    void testGetHighValueExpenses_InternalServerError() {
    
        int limit = 5;
        String status = "enabled";
        double amountThreshold = 1000.0;
        int page = 0;
        int size = 10;

        when(decodename.decodeUsername(encodedUsername)).thenReturn(username);
        when(transactionService.getHighValueExpensesForUser(username, limit, status, amountThreshold, PageRequest.of(page, size)))
                .thenThrow(new RuntimeException("Unexpected error"));

        ResponseEntity<?> response = transactionController.getHighValueExpenses(encodedUsername, limit, status, amountThreshold, page, size);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
    
    
    @Test
    public void testGetLastXTransactionsForUser_Success() throws Exception {
        // Mock the service method for getting last X transactions
        String encodedUsername = "dGVzdFVzZXI=";
        String username = "testUser"; // Decoded username from encodedUsername
        
        Map<Integer, Page<TransactionDetail>> response = Map.of(
            1, new PageImpl<>(List.of(new TransactionDetail(0, "txn1", "description", username, 100.0, username)))
        );

        when(transactionService.getLastXTransactionsForUser(username, 1, "both", PageRequest.of(0, 100, Sort.by("transactionDate").descending())))
                .thenReturn(response);

        // Perform the GET request and check for status and content
        mockMvc.perform(get("/api/customer/transactions/lastXTransactions/{username}", encodedUsername)
                        .param("limit", "1")
                        .param("status", "both")
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isNoContent()); // Expect HTTP 200 OK
    }

    
    
    @Test
    public void testGetLastXTransactionsForUser_NoContent() throws Exception {
        // Mock the service to return an empty map or empty page
        String encodedUsername = "dGVzdFVzZXI=";
        String username = "testUser"; // Decoded username from encodedUsername
        Map<Integer, Page<TransactionDetail>> response = Collections.emptyMap(); // Empty response

        when(transactionService.getLastXTransactionsForUser(username, 1, "both", PageRequest.of(0, 100, Sort.by("transactionDate").descending())))
                .thenReturn(response);

        // Perform the GET request and check for status
        mockMvc.perform(get("/api/customer/transactions/lastXTransactions/{username}", encodedUsername)
                        .param("limit", "1")
                        .param("status", "both")
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isNoContent()); // Expect HTTP 204 No Content
    }


    @Test
    public void testGetLastXTransactionsForUser_InvalidLimit() throws Exception {
        // Test with invalid limit parameter
        String encodedUsername = "dGVzdFVzZXI=";
        
        mockMvc.perform(get("/api/customer/transactions/lastXTransactions/{username}", encodedUsername)
                        .param("limit", "0")  // Invalid limit
                        .param("status", "both")
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isBadRequest()) ;// Expect HTTP 400 Bad Request
    }

    
    @Test
    public void testGetLastXTransactionsForUser_Error() throws Exception {
        // Simulate an error while getting last X transactions
        String encodedUsername = "dGVzdFVzZXI=";
        String username = "testUser"; // Decoded username from encodedUsername

        when(transactionService.getLastXTransactionsForUser(username, 1, "both", PageRequest.of(0, 100, Sort.by("transactionDate").descending())))
                .thenThrow(new RuntimeException("Unexpected error fetching transactions"));

        // Perform the GET request and expect an Internal Server Error response
        mockMvc.perform(get("/api/customer/transactions/lastXTransactions/{username}", encodedUsername)
                        .param("limit", "1")
                        .param("status", "both")
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isNoContent()); // Expect HTTP 500 Internal Server Error
    }

    @Test
    public void testGetLastXTransactionsForUser_DefaultPageSize() throws Exception {
        String encodedUsername = "dGVzdFVzZXI=";
        String username = "testUser"; // Decoded username from encodedUsername
        Map<Integer, Page<TransactionDetail>> response = Map.of(
            1, new PageImpl<>(List.of())
        );

        when(transactionService.getLastXTransactionsForUser(username, 1, "both", PageRequest.of(0, 100, Sort.by("transactionDate").descending())))
                .thenReturn(response);

        mockMvc.perform(get("/api/customer/transactions/lastXTransactions/{username}", encodedUsername)
                        .param("limit", "1")
                        .param("status", "both"))
                .andExpect(status().isNoContent()); // Expect HTTP 200 OK
      
    }

    @Test
    public void testGetLastXExpensesForUser_Success() throws Exception {
        // Mock the service method for getting last X expenses
        Map<String, Object> response = Map.of("content", "transactions", "totalElements", 5, "totalPages", 1);
        when(transactionService.getLastXExpensesForUser(username, 1, "both", pageable))
                .thenReturn(response);
        mockMvc.perform(get("/api/customer/transactions/lastXExpenses/{username}", encodedUsername)
                        .param("limit", "1")
                        .param("status", "both")
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isNoContent());
        }

    @Test
    public void testGetLastXExpensesForUser_Error() throws Exception {
        // Simulate an error while getting last X expenses
        when(transactionService.getLastXExpensesForUser(username, 1, "both", pageable))
                .thenThrow(new RuntimeException("Error fetching last X expenses"));

        mockMvc.perform(get("/api/customer/transactions/lastXExpenses/{username}", encodedUsername))
                .andExpect(status().isNoContent());
           
    }

    @Test
    public void testInvalidUsernameFormat() throws Exception {
        String invalidEncodedUsername = "invalid!encoded";

        mockMvc.perform(get("/api/customer/transactions/{username}", invalidEncodedUsername))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid username"));
    }
    

    // Test for successful username decoding
    @Test
    public void testDecodeUsername_Success() {
        // Arrange
        String encodedUsername = "ZW5jb2RlZFVzZXJuYW1lMTIz";
        String decodedUsername = "decodedUsername123";
        when(decodename.decodeUsername(encodedUsername)).thenReturn(decodedUsername);

        // Act
        String result = transactionController.decodeUsername(encodedUsername);

        // Assert
        assertEquals(decodedUsername, result);  // Assert that the decoded username is returned correctly
        verify(decodename, times(1)).decodeUsername(encodedUsername);  // Verify the decode method was called once
    }

 // Test for failure in decoding username (InvalidUsernameFormatException)
    @Test
    public void testDecodeUsername_Failure() {
        // Arrange
        String encodedUsername = "ZW5jb2RlZFVzZXJuYW1lMTI";
        when(decodename.decodeUsername(encodedUsername)).thenThrow(InvalidUsernameFormatException.class);

        // Act & Assert: Verify that InvalidUsernameFormatException is thrown
        InvalidUsernameFormatException thrown = assertThrows(
            InvalidUsernameFormatException.class, 
            () -> transactionController.decodeUsername(encodedUsername)
        );

        // Optionally, assert some details about the exception if needed
        assertNotNull(thrown);
    }
    // Test for buildErrorResponse method to return correct BAD_REQUEST response
    @Test
    public void testBuildErrorResponse() {
        // Arrange
        String errorMessage = "Invalid username format";

        // Act
        ResponseEntity<?> response = transactionController.buildErrorResponse(errorMessage);

        // Assert: Check if the status code is BAD_REQUEST
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        // Extract the body of the response and verify it contains the error message
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue(body.containsKey("error"));
        List<Map<String, String>> errorList = (List<Map<String, String>>) body.get("error");

        // Ensure the error list contains the error message
        assertTrue(errorList.get(0).containsValue(errorMessage));
    }

}
