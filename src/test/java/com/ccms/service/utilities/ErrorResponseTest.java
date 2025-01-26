package com.ccms.service.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class ErrorResponseTest {

    @Test
    void testConstructorAndGetters() {
        // Arrange
        String error = "Not Found";
        String details = "No credit cards found for user: user123";
        
        // Act
        ErrorResponse errorResponse = new ErrorResponse(error, details);
        
        // Assert
        assertEquals(error, errorResponse.getError());
        assertEquals(details, errorResponse.getDetails());
    }

    @Test
    void testToString() {
        // Arrange
        String error = "Bad Request";
        String details = "Credit card details cannot be null";
        ErrorResponse errorResponse = new ErrorResponse(error, details);

        // Act
        String responseString = errorResponse.toString();

        // Assert
        assertEquals("ErrorResponse [error=Bad Request, details=Credit card details cannot be null]", responseString);
    }

    @Test
    void testSetters() {
        // Arrange
        ErrorResponse errorResponse = new ErrorResponse("Not Found", "Some error");
        
        // Act
        errorResponse.setError("Unauthorized");
        errorResponse.setDetails("Invalid API key");
        
        // Assert
        assertEquals("Unauthorized", errorResponse.getError());
        assertEquals("Invalid API key", errorResponse.getDetails());
    }
}
