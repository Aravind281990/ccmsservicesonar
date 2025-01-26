package com.ccms.service.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class SuccessResponseTest {

    @Test
    void testConstructorAndGetters() {
        // Arrange
        String data = "Test Success";
        
        // Act
        SuccessResponse<String> successResponse = new SuccessResponse<>(data);
        
        // Assert
        assertEquals("success", successResponse.getStatus());  // default status should be "success"
        assertEquals(data, successResponse.getData());
    }
    
    @Test
    void testToString() {
        // Arrange
        String data = "Test Data";
        SuccessResponse<String> successResponse = new SuccessResponse<>(data);

        // Act
        String responseString = successResponse.toString();

        // Assert
        assertEquals("SuccessResponse [status=success, data=Test Data]", responseString);
    }

    @Test
    void testSetters() {
        // Arrange
        SuccessResponse<String> successResponse = new SuccessResponse<>("Initial Data");
        
        // Act
        successResponse.setStatus("success");
        successResponse.setData("Updated Data");
        
        // Assert
        assertEquals("success", successResponse.getStatus());
        assertEquals("Updated Data", successResponse.getData());
    }
}
