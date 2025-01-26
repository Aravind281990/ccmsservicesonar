package com.ccms.service.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the InvalidRandomOperationException class.
 */
class InvalidRandomOperationExceptionTest {

    /**
     * Test the constructor that accepts a message.
     */
    @Test
    void testConstructorWithMessage() {
        String errorMessage = "This is a test exception message.";
        
        // Throw and catch the exception to verify the message
        InvalidRandomOperationException exception = assertThrows(InvalidRandomOperationException.class, () -> {
            throw new InvalidRandomOperationException(errorMessage);
        });

        // Verify the exception message
        assertEquals(errorMessage, exception.getMessage(), "The exception message does not match.");
    }

    /**
     * Test the constructor that accepts both a message and a cause.
     */
    @Test
    void testConstructorWithMessageAndCause() {
        String errorMessage = "This is a test exception message with cause.";
        Throwable cause = new Throwable("Test cause");

        // Throw and catch the exception to verify both message and cause
        InvalidRandomOperationException exception = assertThrows(InvalidRandomOperationException.class, () -> {
            throw new InvalidRandomOperationException(errorMessage, cause);
        });

        // Verify the exception message
        assertEquals(errorMessage, exception.getMessage(), "The exception message does not match.");
        
        // Verify the cause of the exception
        assertEquals(cause, exception.getCause(), "The exception cause does not match.");
    }
}
