package com.ccms.service.utilities;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthenticationExceptionTest {

    // Test for the constructor that accepts a message
    @Test
    void testAuthenticationExceptionWithMessage() {
        String message = "Authentication failed due to invalid credentials";
        AuthenticationException exception = new AuthenticationException(message);

        // Check that the exception message is correctly set
        assertEquals(message, exception.getMessage());
    }

    // Test for the constructor that accepts both a message and a cause
    @Test
    void testAuthenticationExceptionWithMessageAndCause() {
        String message = "Authentication failed";
        Throwable cause = new Throwable("Cause of failure");
        AuthenticationException exception = new AuthenticationException(message, cause);

        // Check that the exception message is correctly set
        assertEquals(message, exception.getMessage());

        // Check that the cause is correctly set
        assertEquals(cause, exception.getCause());
    }

    // Test that the exception is an instance of RuntimeException
    @Test
    void testIsInstanceOfRuntimeException() {
        String message = "Authentication error";
        AuthenticationException exception = new AuthenticationException(message);

        // Check that AuthenticationException is a subclass of RuntimeException
        assertTrue(exception instanceof RuntimeException);
    }
}
