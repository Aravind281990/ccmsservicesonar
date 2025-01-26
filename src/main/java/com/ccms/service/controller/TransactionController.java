package com.ccms.service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccms.service.exception.InvalidUsernameFormatException;
import com.ccms.service.model.Transaction.TransactionDetail;
import com.ccms.service.model.TransactionWithCardId;
import com.ccms.service.service.TransactionService;
import com.ccms.service.utilities.Decodename;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller responsible for managing credit card transactions related to
 * customers. This includes fetching transaction history, expenses, and
 * high-value transactions for a given customer.
 */

@Tag(name = "Transaction Controller", description = "Controller for managing Customers Creditcard Transactions")
@RestController
@Validated
@RequestMapping("/api/customer/transactions")
public class TransactionController {

	private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);
	
	 private static final String TRANSACTION_DATE = "transactionDate";
	 private static final String ERROR = "error";
	 private static final List<String> VALID_STATUSES = List.of("enabled", "disabled", "both");
	 private static final String INVALID_STATUS_MESSAGE = "Invalid status value. Valid values are 'enabled', 'disabled', or 'both'";
	 private static final String INVALID_ARGUMENT_MESSAGE = "Invalid argument provided";
	 private static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred. Please try again later";
	 private static final String LIMIT_PARAMETER_ERROR_MESSAGE = "The 'limit' parameter must be a positive integer greater than 0.";

	@Autowired
	TransactionService transactionService;

	@Autowired
	private Decodename decodename;


	/**
	 * Fetches all transactions for a given customer, including pagination details.
	 * 
	 * @param encodedusername The encoded username of the customer.
	 * @param page            The page number to retrieve (optional, defaults to 0).
	 * @param size            The number of transactions per page (optional,
	 *                        defaults to 100).
	 * @return A ResponseEntity containing the paginated list of transactions or an
	 *         error response.
	 */

	@Operation(summary = "Get all Transactions", description = "Show all transactions for every card associated with the given customer")
	@GetMapping("/{username}")
	public ResponseEntity<?> gettransactionsforuser(@PathVariable("username") String encodedusername,
			@RequestParam(required = false) Integer page, // No default value set
			@RequestParam(required = false) Integer size) { // No default value set

		String username = decodeUsername(encodedusername);

		logger.info("Fetching transactions for user: {}, page: {}, limit: {}", username, page, size);

		if (page == null) {
			page = 0; // Default to the first page
		}
		if (size == null) {
			size = 100; // Default to 100 transactions per page
		}

		try {
			// Set pagination details

			Pageable pageable = PageRequest.of(page, size, Sort.by(TRANSACTION_DATE).descending());

			// Fetch paginated transactions for the given user
			Page<TransactionWithCardId> transactions = transactionService.getTransactionsForUser(username, pageable);

			// Check if transactions are found

			if (transactions == null || transactions.isEmpty()) {

				return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
			}

			// Prepare paginated response

			Map<String, Object> response = Map.of("content", transactions.getContent(), "totalElements",
					transactions.getTotalElements(), "totalPages", transactions.getTotalPages(), "currentPage",
					transactions.getNumber(), "size", transactions.getSize());

			return ResponseEntity.ok(response);

		} catch (Exception e) {

			logger.error("An error occurred while fetching the credit card transactions", e);

			// Return a 500 Internal Server Error with the exception message

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(ERROR,
					List.of(Map.of(ERROR, "An error occurred while fetching transactions: " + e.getMessage()))));
		}
	}

	/**
	 * Retrieves the maximum expenses for all cards of the given customer in the
	 * last month.
	 * 
	 * @param encodedusername The encoded username of the customer.
	 * @param status          The status of the transactions to filter by:
	 *                        'enabled', 'disabled', or 'both' (optional, default is
	 *                        'both').
	 * @param page            The page number to retrieve (optional, defaults to 0).
	 * @param size            The number of transactions per page (optional,
	 *                        defaults to 100).
	 * @return A ResponseEntity containing the paginated max expense records or an
	 *         error response.
	 */

	@Operation(summary = "Retrieve the maximum expenses", description = "View the maximum expenses for all cards of the given customer in the last month")
	@GetMapping("/maxExpenses/lastMonth/{username}")
	public ResponseEntity<?> getMaxExpensesForLastMonth(@PathVariable("username") String encodedusername,
			@RequestParam(required = false, defaultValue = "both") String status,
			@RequestParam(required = false) Integer page, // No default value set
			@RequestParam(required = false) Integer size) { // No default value set)

		// Username validation: Check if it's null, empty, or exceeds max length

		String username = decodeUsername(encodedusername);

		// Set defaults for page and limit if they are not provided
		if (page == null) {
			page = 0; // Default to the first page
		}
		if (size == null) {
			size = 100; // Default to 100 transactions per page
		}

		// Status validation: Valid statuses are "enabled", "disabled", and "both"
		if (!VALID_STATUSES.contains(status)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(List
					.of(Map.of(ERROR, INVALID_STATUS_MESSAGE)));

		}

		try {

			// Set pagination details

			Pageable pageable = PageRequest.of(page, size, Sort.by(TRANSACTION_DATE).descending());

			// Fetch max expenses for the last month
			Page<Map<String, Object>> maxExpenses = transactionService.getMaxExpensesForLastMonth(username, status,
					pageable);

			// If no expenses are found, return a 204 No Content response
			if (maxExpenses.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
			}

			// Return the max expenses with a 200 OK response

			return ResponseEntity.ok(maxExpenses);

		} catch (IllegalArgumentException e) {

			return ResponseEntity.badRequest().body(List.of(Map.of(ERROR, INVALID_ARGUMENT_MESSAGE)));
		} catch (Exception e) {

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(List.of(Map.of(ERROR, UNEXPECTED_ERROR_MESSAGE)));
		}
	}

	/**
	 * Retrieves the high-value expenses for the given customer that exceed the
	 * specified threshold.
	 * 
	 * @param encodedusername The encoded username of the customer.
	 * @param limit           The number of high-value expenses to retrieve
	 *                        (optional, defaults to 1).
	 * @param status          The status of the transactions to filter by:
	 *                        'enabled', 'disabled', or 'both' (optional, default is
	 *                        'both').
	 * @param amountThreshold The threshold above which transactions are considered
	 *                        high-value.
	 * @param page            The page number to retrieve (optional, defaults to 0).
	 * @param size            The number of transactions per page (optional,
	 *                        defaults to 100).
	 * @return A ResponseEntity containing the high-value expenses or an error
	 *         response.
	 */

	@Operation(summary = "Retrieve the high-value expenses", description = "View the high-value expenses for all cards of the given customer that exceed the specified threshold")
	@GetMapping("/highvalue/expenses/{username}")
	public ResponseEntity<?> getHighValueExpenses(@PathVariable("username") String encodedusername,
			@RequestParam(required = false, defaultValue = "1") int limit,
			@RequestParam(required = false, defaultValue = "both") String status, @RequestParam double amountThreshold,
			@RequestParam(required = false) Integer page, // No default value set
			@RequestParam(required = false) Integer size) {

		// Validate username

		// Set defaults for page and limit if they are not provided
		if (page == null) {
			page = 0; // Default to the first page
		}

		if (size == null) {
			size = 100; // Default to 100 transactions per page
		}

		String username = decodeUsername(encodedusername);

		// Validate the limit to ensure it's a positive integer greater than 0
		if (limit <= 0) {
			return buildErrorResponse(LIMIT_PARAMETER_ERROR_MESSAGE);
		}
		// Validate amountThreshold
		if (amountThreshold <= 0) {
			return buildErrorResponse("Amount threshold must be a positive value");
		}

		// Validate status (should be either "enabled", "disabled", or "both")

		if (!VALID_STATUSES.contains(status)) {
			return buildErrorResponse(INVALID_STATUS_MESSAGE);
		}

		try {
			// Call the service to get high-value expenses

			// Set pagination details

			Pageable pageable = PageRequest.of(page, size, Sort.by(TRANSACTION_DATE).descending());

			Map<String, Page<Map<String, String>>> highValueExpenses = transactionService
					.getHighValueExpensesForUser(username, limit, status, amountThreshold, pageable);

			// If no expenses are found, return 204 No Content
			if (highValueExpenses.isEmpty()) {

				return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
			}

			// Return the high-value expenses with a 200 OK response
			return ResponseEntity.ok(highValueExpenses);

		} catch (IllegalArgumentException e) {
			// Log exception if needed
			return buildErrorResponse(INVALID_ARGUMENT_MESSAGE);
		} catch (Exception e) {
			// Log exception for debugging
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
					Map.of(ERROR, List.of(Map.of(ERROR, UNEXPECTED_ERROR_MESSAGE))));
		}
	}

	/**
	 * Retrieves the last X expenses for the given customer.
	 * 
	 * @param encodedusername The encoded username of the customer.
	 * @param limit           The number of expenses to retrieve (optional, defaults
	 *                        to 1).
	 * @param status          The status of the transactions to filter by:
	 *                        'enabled', 'disabled', or 'both' (optional, default is
	 *                        'both').
	 * @param page            The page number to retrieve (optional, defaults to 0).
	 * @param size            The number of transactions per page (optional,
	 *                        defaults to
	 */

	@Operation(summary = "Retrieve the last X expenses for all cards.", description = "View the last X expenses for all cards associated with the given customer")
	@GetMapping("/lastXTransactions/{username}")
	public ResponseEntity<?> getLastXTransactionsForUser(@PathVariable("username") String encodedusername,
			@RequestParam(required = false, defaultValue = "1") int limit,
			@RequestParam(required = false, defaultValue = "both") String status,
			@RequestParam(required = false) Integer page, // No default value set
			@RequestParam(required = false) Integer size) {

		// Validate the username

		String username = decodeUsername(encodedusername);

		// Set defaults for page and limit if they are not provided
		if (page == null) {
			page = 0; // Default to the first page
		}

		if (size == null) {
			size = 100; // Default to 100 transactions per page
		}

		// Validate the limit to ensure it's a positive integer greater than 0
		if (limit <= 0) {
			return buildErrorResponse(LIMIT_PARAMETER_ERROR_MESSAGE);
		}
		// Validate status (should be either "enabled", "disabled", or "both")

		if (!VALID_STATUSES.contains(status)) {
			return buildErrorResponse(INVALID_STATUS_MESSAGE);
		}

		try {

			// Set pagination details

			Pageable pageable = PageRequest.of(page, size, Sort.by(TRANSACTION_DATE).descending());

			Map<Integer, Page<TransactionDetail>> transactions = transactionService
					.getLastXTransactionsForUser(username, limit, status, pageable);

			if (transactions.isEmpty() || transactions == null) {

				return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

			}

			return ResponseEntity.ok(transactions);

		} catch (

		IllegalArgumentException e) {
			return buildErrorResponse(INVALID_ARGUMENT_MESSAGE);

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(UNEXPECTED_ERROR_MESSAGE);
		}

	}

	/**
	 * Retrieves the last X expenses for the given customer - only for Backend.
	 * 
	 * @param encodedusername The encoded username of the customer.
	 * @param limit           The number of expenses to retrieve (optional, defaults
	 *                        to 1).
	 * @param status          The status of the transactions to filter by:
	 *                        'enabled', 'disabled', or 'both' (optional, default is
	 *                        'both').
	 * @param page            The page number to retrieve (optional, defaults to 0).
	 * @param size            The number of transactions per page (optional,
	 *                        defaults to 100).
	 * @return A ResponseEntity containing the last X expenses or an error response.
	 */

	@Operation(summary = "For Backend - Retrieve the last X expenses for all cards.", description = "View the last X expenses for all cards associated with the given customer")
	@GetMapping("/lastXExpenses/{username}")
	public ResponseEntity<?> getLastXExpensesForUser(@PathVariable("username") String encodedusername,
			@RequestParam(required = false, defaultValue = "1") int limit,
			@RequestParam(required = false, defaultValue = "both") String status,
			@RequestParam(required = false) Integer page, // No default value set
			@RequestParam(required = false) Integer size) {

		// Validate the username

		String username = decodeUsername(encodedusername);

		// Set defaults for page and limit if they are not provided
		if (page == null) {
			page = 0; // Default to the first page
		}

		if (size == null) {
			size = 100; // Default to 100 transactions per page
		}

		// Validate the limit to ensure it's a positive integer greater than 0
		if (limit <= 0) {
			return buildErrorResponse(LIMIT_PARAMETER_ERROR_MESSAGE);
		}

		// Validate status (should be either "enabled", "disabled", or "both")
		if (!VALID_STATUSES.contains(status)) {
			return buildErrorResponse(INVALID_STATUS_MESSAGE);
		}

		try {

			// Set pagination details
			Pageable pageable = PageRequest.of(page, size, Sort.by(TRANSACTION_DATE).descending());

			// Call the service to get the last X expenses
			Map<String, Object> transactions = transactionService.getLastXExpensesForUser(username, limit, status,
					pageable);

			// If no transactions are found, return 204 No Content
			if (transactions.isEmpty() || transactions == null) {
				return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
			}

			// Return the last X transactions with a 200 OK response
			return ResponseEntity.ok(transactions);

		} catch (IllegalArgumentException e) {
			// Log exception if needed
			return buildErrorResponse(INVALID_ARGUMENT_MESSAGE);

		} catch (Exception e) {
			// Log exception for debugging
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(List.of(Map.of(ERROR, UNEXPECTED_ERROR_MESSAGE)));
		}
	}

	// Helper method to decode username

	public String decodeUsername(String encodedusername) {
		try {
			return decodename.decodeUsername(encodedusername);
		} catch (InvalidUsernameFormatException e) {
			logger.error("Failed to decode username: {}...", encodedusername.substring(0, 3), e);
			throw e;
		}
	}

	// Helper method to standardize error responses
	public ResponseEntity<?> buildErrorResponse(String errorMessage) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of(ERROR, List.of(Map.of(ERROR, errorMessage))));
	}

}