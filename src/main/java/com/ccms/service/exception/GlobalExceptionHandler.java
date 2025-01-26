package com.ccms.service.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ccms.service.utilities.ErrorResponse;

/**
 * GlobalExceptionHandler is responsible for handling exceptions thrown by the
 * application. It catches specific exceptions like InvalidUsernameException and
 * DuplicateCreditCardException, and returns custom error responses with
 * appropriate HTTP status codes.
 * <p>
 * The class uses @RestControllerAdvice to handle exceptions globally and
 * ensures that all API responses follow a consistent format.
 * </p>
 */

@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * Handles InvalidUsernameException and returns a custom error response with a
	 * 400 Bad Request status.
	 * 
	 * @param ex The InvalidUsernameException thrown by the application.
	 * @return A ResponseEntity containing an ErrorResponse object with error
	 *         details and a 400 status.
	 */

	@ExceptionHandler(InvalidUsernameException.class)
	public ResponseEntity<ErrorResponse> handleInvalidUsernameException(InvalidUsernameException ex) {
		// Log the error or any additional processing
		// Return a custom error response
		ErrorResponse errorResponse = new ErrorResponse("Invalid username", ex.getMessage());
		return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handles DuplicateCreditCardException and returns a custom error response with
	 * a 400 Bad Request status.
	 * 
	 * @param ex The DuplicateCreditCardException thrown by the application.
	 * @return A ResponseEntity containing an ErrorResponse object with error
	 *         details and a 400 status.
	 */

	@ExceptionHandler(DuplicateCreditCardException.class)
	public ResponseEntity<ErrorResponse> handleDuplicateCreditCardException(DuplicateCreditCardException ex) {
		// Log the error or any additional processing
		// Return a custom error response
		ErrorResponse errorResponse = new ErrorResponse("Credit card already associated with this user",
				ex.getMessage());
		return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Global exception handler for any unhandled exceptions that may occur in the
	 * application. This method provides a generic error response for all unexpected
	 * errors.
	 * 
	 * @param ex The Exception thrown by the application.
	 * @return A ResponseEntity containing an ErrorResponse object with a generic
	 *         error message and a 500 status.
	 */

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {

		// Create a custom error response
		ErrorResponse errorResponse = new ErrorResponse("Internal Server Error", "An unexpected error occurred");

		// Return the error response with a 500 Internal Server Error status
		return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	}
	

}


