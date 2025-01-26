package com.ccms.service.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import com.ccms.service.exception.CreditCardNotFoundException;
import com.ccms.service.exception.CustomerNotFoundException;
import com.ccms.service.exception.TransactionNotFoundException;
import com.ccms.service.model.CreditCard.CreditCardDetail;
import com.ccms.service.model.Transaction.CreditCardTransaction;
import com.ccms.service.model.Transaction.TransactionDetail;
import com.ccms.service.model.TransactionWithCardId;
import com.ccms.service.repository.CustomerRepository;
import com.ccms.service.repository.TransactionRepository;
import com.ccms.service.service.CreditCardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.ccms.service.model.TransactionWithCreditCardInfo;
import com.ccms.service.service.TransactionService;
import com.ccms.service.utilities.CreditCardEnDecryption;
import com.ccms.service.utilities.CreditCardFormatter;

/**
 * Service implementation for handling transactions related to customers,
 * including retrieving transaction details from a customer's credit cards and
 * supporting pagination for transaction data.
 * <p>
 * This service interacts with various repositories and external services to
 * fetch customer transaction data, decrypt credit card information, and format
 * transaction details for the user. It also handles pagination of transaction
 * records when queried.
 * </p>
 * 
 * @see TransactionService
 * @see CustomerRepository
 * @see CreditCardService
 * @see TransactionRepository
 * @see CreditCardEnDecryption
 * @see CreditCardFormatter
 * @see MongoTemplate
 */

@Service
public class TransactionServiceimpl implements TransactionService {

	private static final Logger logger = LoggerFactory.getLogger(TransactionServiceimpl.class);
	
	private static final String CREDITCARDS_TRANSACTIONS = "creditcards.transactions";
	private static final String TOTAL_COUNT = "totalCount";


	@Autowired
	public CustomerRepository customerRepository;

	@Autowired
	public CreditCardService cardService; // To fetch credit cards

	@Autowired
	public TransactionRepository transactionRepository;

	@Autowired
	public CreditCardEnDecryption cardEnDecryption;

	@Autowired
	public CreditCardFormatter cardFormatter;

	@Autowired
	public MongoTemplate mongoTemplate;

	@Override
	public Page<TransactionWithCardId> getTransactionsForUser(String username, Pageable pageable) {
		// Create the aggregation pipeline for fetching the paginated results from the
		// Transactions collection
		Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(Criteria.where("username").is(username)), // Match
																															// by
																															// the
																															// username
				Aggregation.unwind("creditcards"), // Unwind the creditcards array
				Aggregation.unwind(CREDITCARDS_TRANSACTIONS), // Unwind the transactions array within creditcards
				Aggregation.project("creditcards.creditCardId", CREDITCARDS_TRANSACTIONS)
						.and("creditcards.creditCardId").as("creditCardId") // Map creditCardId
						.and("creditcards.transactions.transactionId").as("transactionId") // Map transactionId
						.and("creditcards.transactions.transactionDate").as("transactionDate") // Map transactionDate
						.and("creditcards.transactions.transactionTime").as("transactionTime") // Map transactionTime
						.and("creditcards.transactions.transactionType").as("transactionType") // Map transactionType
						.and("creditcards.transactions.transactionAmount").as("transactionAmount") // Map
																									// transactionAmount
						.and("creditcards.transactions.transactionDesc").as("transactionDesc") // Map transactionDesc
						.and(CREDITCARDS_TRANSACTIONS).as("transactionDetail"), // Include entire transaction details
				Aggregation.sort(Sort.by(Sort.Order.desc("transactionDate"))), // Sort by transaction date
				Aggregation.skip((long) pageable.getPageNumber() * pageable.getPageSize()), // Skip the already seen
																							// items
				Aggregation.limit(pageable.getPageSize()) // Limit to the current page size
		);

		// Execute the aggregation to get the paginated results
		AggregationResults<TransactionWithCardId> results = mongoTemplate.aggregate(aggregation, "Transactions",
				TransactionWithCardId.class);

		// Get the paginated results
		List<TransactionWithCardId> transactions = results.getMappedResults();

		// Create the count aggregation to count the total number of matching records
		Aggregation countAggregation = Aggregation.newAggregation(
				Aggregation.match(Criteria.where("username").is(username)), Aggregation.unwind("creditcards"),
				Aggregation.unwind(CREDITCARDS_TRANSACTIONS), Aggregation.count().as(TOTAL_COUNT) // Automatically
																										// counts the
																										// number of
																										// matching
																										// documents
		);

		// Execute the count aggregation to get the total number of matching documents
		AggregationResults<Map> countResults = mongoTemplate.aggregate(countAggregation, "Transactions", Map.class);

		// Retrieve the total count from the map (it will be in the "totalCount" field)
		Map<String, Integer> countMap = countResults.getUniqueMappedResult();
		Integer totalCount = (countMap != null && countMap.containsKey(TOTAL_COUNT)) ? countMap.get(TOTAL_COUNT) : 0;

		// Convert Integer to Long
		Long total = totalCount.longValue(); // Convert to Long if necessary

		// Return the page with transactions and total count for pagination
		return new PageImpl<>(transactions, pageable, total);
	}

	// Customer wants to view last month maximum expenses of all cards
	@Override
	public Page<Map<String, Object>> getMaxExpensesForLastMonth(String username, String statusFilter,
			Pageable pageable) {

		// Retrieve all active credit cards for the user
		logger.info("Entering getMaxExpensesForLastMonth");

		List<CreditCardDetail> activeCreditCards = getActiveCreditCardsForUser(username, statusFilter);

		if (activeCreditCards.isEmpty())
			return Page.empty(pageable);

		List<CreditCardTransaction> cardTransactions = getTransactionsForUser(username);

		if (cardTransactions.isEmpty())
			return Page.empty(pageable);

		// Initialize a map to hold the maximum transaction amount for each card
		Map<Integer, List<TransactionWithCreditCardInfo>> allTransactionDetails = new HashMap<>();

		// Retrieve transactions for each active credit card

		for (CreditCardDetail creditCardDetail : activeCreditCards) {

			List<TransactionWithCreditCardInfo> filteredTransactions = cardTransactions.stream()
					.filter(transaction -> transaction.getCreditCardId() == creditCardDetail.getCreditCardId())
					.flatMap(transaction -> transaction.getTransactions().stream())
					.map(transaction -> new TransactionWithCreditCardInfo(creditCardDetail.getCreditCardNumber(),
							transaction))
					.collect(Collectors.toList());

			if (!filteredTransactions.isEmpty()) {
				allTransactionDetails.put(creditCardDetail.getCreditCardId(), filteredTransactions);
			}
		}

		// List to store the results as specified in the required format
		List<Map<String, Object>> result = new ArrayList<>();

		// Calculate the date range for the last month
		LocalDate now = LocalDate.now();
		LocalDate startOfLastMonth = now.minusMonths(1).withDayOfMonth(1);
		LocalDate endOfLastMonth = now.minusMonths(1).withDayOfMonth(now.minusMonths(1).lengthOfMonth());

		logger.debug("Date range: " + startOfLastMonth + " to " + endOfLastMonth);

		// DateTimeFormatter to extract month name
		DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM");

		// Iterate through each card's transactions to find the max transaction amount
		for (Map.Entry<Integer, List<TransactionWithCreditCardInfo>> entry : allTransactionDetails.entrySet()) {

			List<TransactionWithCreditCardInfo> transactions = entry.getValue();

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");

			// Filter transactions within the date range
			List<TransactionWithCreditCardInfo> maxExpenseTransactions = transactions.stream().filter(t -> {
				LocalDate transactionDate = LocalDate.parse(t.getTransactionDetail().getTransactionDate(), formatter);
				return (transactionDate.isEqual(startOfLastMonth) || transactionDate.isAfter(startOfLastMonth))
						&& (transactionDate.isEqual(endOfLastMonth) || transactionDate.isBefore(endOfLastMonth));
			}).collect(Collectors.toList());

			// Calculate the maximum transaction amount for the last month
			if (!maxExpenseTransactions.isEmpty()) {
				double maxTransactionAmount = maxExpenseTransactions.stream()
						.mapToDouble(t -> t.getTransactionDetail().getTransactionAmount()).max().orElse(0.0);

				String month = startOfLastMonth.format(monthFormatter).toUpperCase();

				Map<String, Object> expenseDetails = new LinkedHashMap<>();

				try {
					String decryptedCreditCardNumber = cardEnDecryption
							.decrypt(entry.getValue().get(0).getCreditCardNumber());

					entry.getValue().get(0).setCreditCardNumber(decryptedCreditCardNumber);

					String formattedCreditCardNumber = cardFormatter
							.maskCreditCardNumber(entry.getValue().get(0).getCreditCardNumber());

					expenseDetails.put("credit_card", formattedCreditCardNumber);
					expenseDetails.put("month", month);
					expenseDetails.put("amount", maxTransactionAmount);

					result.add(expenseDetails);

				} catch (Exception e) {

					logger.error("Error decrypting credit card number", e);
					continue;

				}

			}
		}

		// Calculate the total number of results
		long total = result.size();

		// Apply pagination by skipping and limiting based on the Pageable object
		int start = (int) pageable.getOffset();
		int end = Math.min((start + pageable.getPageSize()), result.size());

		// Get the sublist for the current page
		List<Map<String, Object>> pagedResults = result.subList(start, end);

		// Return the paginated Page object
		return new PageImpl<>(pagedResults, pageable, total);
	}

	// HIGH Value expenses more than the threshold amount (xxxxx)
	@Override
	public Map<String, Page<Map<String, String>>> getHighValueExpensesForUser(String username, int limit,
			String statusFilter, double amountThreshold, Pageable pageable) {

		// Retrieve all active credit cards for the user
		logger.info("Entering getHighValueExpensesForUser");

		List<CreditCardDetail> activeCreditCards = getActiveCreditCardsForUser(username, statusFilter);

		if (activeCreditCards.isEmpty())
			return Collections.emptyMap();

		List<CreditCardTransaction> cardTransactions = getTransactionsForUser(username);

		if (cardTransactions.isEmpty())
			return Collections.emptyMap();

		// Retrieve transactions for each active credit card

		Map<String, Page<Map<String, String>>> allTransactionDetails = new LinkedHashMap();

		for (CreditCardDetail creditCardDetail : activeCreditCards) {

			// Filter the transactions for this particular credit card

			List<Map<String, String>> filteredTransactions = cardTransactions.stream()
					.filter(transaction -> transaction.getCreditCardId() == creditCardDetail.getCreditCardId())
					.flatMap(transaction -> transaction.getTransactions().stream())
					.filter(transaction -> transaction.getTransactionAmount() > amountThreshold).map(
							transaction -> new TransactionWithCreditCardInfo(creditCardDetail.getCreditCardNumber(),
									transaction))
					.sorted(Comparator.comparing((TransactionWithCreditCardInfo transaction) -> transaction
							.getTransactionDetail().getTransactionDate()).reversed())
					.map(transaction -> {
						// Create a map with "transactionDetail" key and the transaction detail data as
						// strings
						Map<String, String> transactionMap = new LinkedHashMap<>();
						TransactionDetail detail = transaction.getTransactionDetail();
						transactionMap.put("transactionId", String.valueOf(detail.getTransactionId()));
						transactionMap.put("transactiondate", String.valueOf(detail.getTransactionDate()));
						transactionMap.put("transactiontime", String.valueOf(detail.getTransactionTime()));
						transactionMap.put("transactiontype", detail.getTransactionType());
						transactionMap.put("transactionamount", String.valueOf(detail.getTransactionAmount()));
						transactionMap.put("transactiondescription", detail.getTransactionDesc());
						return transactionMap;
					}).limit(limit).collect(Collectors.toList());

			if (!filteredTransactions.isEmpty()) {

				try {
					String decryptedCreditCardNumber = cardEnDecryption.decrypt(creditCardDetail.getCreditCardNumber());

					String formattedCreditCardNumber = cardFormatter.maskCreditCardNumber(decryptedCreditCardNumber);

					// Calculate the start and end index for the current page
					int start = (int) pageable.getOffset();
					int end = Math.min((start + pageable.getPageSize()), filteredTransactions.size());

					// Get the current page transactions
					List<Map<String, String>> pageContent = filteredTransactions.subList(start, end);

					// Create a Page object with the paginated transactions
					Page<Map<String, String>> page = new PageImpl<>(pageContent, pageable, filteredTransactions.size());

					// Put the page in the result map with the formatted credit card number as the
					// key
					allTransactionDetails.put(formattedCreditCardNumber, page);

				} catch (Exception e) {

					logger.error("Error decrypting credit card number for user: " + username, e);
					continue;
				}

			}
		}

		if (allTransactionDetails.isEmpty()) {
			logger.info("No high-value transactions found for any card.");
		}

		return allTransactionDetails;

	}

	// Customer wants to view last x number of expenses of all cards
	@Override
	public Map<Integer, Page<TransactionDetail>> getLastXTransactionsForUser(String username, int limit,
			String statusFilter, Pageable pageable) {

		// Retrieve all active credit cards for the user

		logger.info("Entering getLastXTransactionsForUser");

		List<CreditCardDetail> activeCreditCards = getActiveCreditCardsForUser(username, statusFilter);

		if (activeCreditCards.isEmpty())
			return Collections.emptyMap();

		List<CreditCardTransaction> cardTransactions = getTransactionsForUser(username);

		if (cardTransactions.isEmpty())
			return Collections.emptyMap();

		// Retrieve transactions for each active credit card

		Map<Integer, List<TransactionDetail>> allTransactionDetails = new HashMap<>();

		for (CreditCardDetail creditCardDetail : activeCreditCards) {

			List<TransactionDetail> filteredTransactions = cardTransactions.stream()
					.filter(transaction -> transaction.getCreditCardId() == creditCardDetail.getCreditCardId())
					.flatMap(transaction -> transaction.getTransactions().stream())
					.sorted(Comparator.comparing(TransactionDetail::getTransactionDate).reversed()) // Sort before
																									// pagination
					.collect(Collectors.toList());

			if (!filteredTransactions.isEmpty()) {
				allTransactionDetails.put(creditCardDetail.getCreditCardId(), filteredTransactions);
			}

		}

		// Now limit the transactions to the last X for each credit card ID

		Map<Integer, Page<TransactionDetail>> paginatedTransactionDetails = new HashMap<>();

		for (Map.Entry<Integer, List<TransactionDetail>> entry : allTransactionDetails.entrySet()) {

			List<TransactionDetail> transactions = entry.getValue();

			// Sort and limit the transactions for the current credit card ID

			if (!transactions.isEmpty()) {

				List<TransactionDetail> limitedTransactions = transactions.stream().limit(limit) // Limit to the top X
																									// transactions
						.collect(Collectors.toList());

				// Apply pagination based on the Pageable object
				int start = (int) pageable.getOffset();
				int end = Math.min((start + pageable.getPageSize()), limitedTransactions.size());

				// Prevent index out of bounds
				if (start > limitedTransactions.size()) {
					start = limitedTransactions.size();
				}

				List<TransactionDetail> pageContent = limitedTransactions.subList(start, end);

				// Create a Page object for the current credit card's transactions
				Page<TransactionDetail> page = new PageImpl<>(pageContent, pageable, limitedTransactions.size());

				// Add the limited list back to the new map

				// Add the Page to the map, keyed by credit card ID
				paginatedTransactionDetails.put(entry.getKey(), page);
			}

		}

		return paginatedTransactionDetails; // Return the map with paginated transactions

	}

	@Override
	public Map<String, Object> getLastXExpensesForUser(String username, int limit, String statusFilter,
			Pageable pageable) {

		// Step 1: Retrieve active credit cards for the user

		logger.info("Entering getLastXExpensesForUser");
		List<CreditCardDetail> activeCreditCards = getActiveCreditCardsForUser(username, statusFilter);
		if (activeCreditCards.isEmpty()) {
			return Collections.emptyMap(); // No active credit cards
		}

		// Step 2: Retrieve all transactions for the user

		List<CreditCardTransaction> cardTransactions = getTransactionsForUser(username);
		if (cardTransactions.isEmpty()) {
			return Collections.emptyMap(); // No transactions found
		}

		// Step 3: Process and filter transactions for each credit card separately

		List<Map<String, Object>> Maxexpense = new ArrayList<>();

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");

		for (CreditCardDetail creditCardDetail : activeCreditCards) {

			// Step 3.1: Filter transactions specifically for the current credit card
			List<TransactionWithCreditCardInfo> filteredTransactions = cardTransactions.stream()
					.filter(transaction -> transaction.getCreditCardId() == creditCardDetail.getCreditCardId()) // Ensure
																												// card-specific
																												// transactions
					.flatMap(transaction -> transaction.getTransactions().stream()) // Assuming `getTransactions()`
																					// returns the transaction details
					.sorted(Comparator.comparing(TransactionDetail::getTransactionDate).reversed()).limit(limit) // Limit
																													// to
																													// X
																													// number
																													// of
																													// transactions
					.map(transaction -> new TransactionWithCreditCardInfo(creditCardDetail.getCreditCardNumber(),
							transaction))
					.collect(Collectors.toList());

			if (filteredTransactions.isEmpty()) {
				continue; // Skip if there are no transactions for this card
			}

			// Step 3.2: Paginate transactions for each credit card (e.g., limit 100 per
			// page)
			int start = (int) pageable.getOffset();
			int end = Math.min(start + pageable.getPageSize(), filteredTransactions.size());
			List<TransactionWithCreditCardInfo> paginatedTransactions = filteredTransactions.subList(start, end);

			// Step 3.2: Map the filtered transactions to a list of maps
			List<Map<String, Object>> transactionList = paginatedTransactions.stream().map(transaction -> {
				Map<String, Object> transactionMap = new LinkedHashMap<>();
				LocalDate transactionDate = LocalDate.parse(transaction.getTransactionDetail().getTransactionDate(),
						formatter);
				String month = transactionDate.getMonth().name().substring(0, 3); // Get the 3-letter month abbreviation
				transactionMap.put("month", month);
				transactionMap.put("amount", transaction.getTransactionDetail().getTransactionAmount());
				transactionMap.put("description", transaction.getTransactionDetail().getTransactionDesc());
				return transactionMap;
			}).collect(Collectors.toList());

			// Step 3.3: Prepare credit card data with decrypted card number and transaction
			// details
			Map<String, Object> creditCardData = new LinkedHashMap<>();
			try {
				// Decrypt credit card number
				String decryptedCreditCardNumber = cardEnDecryption.decrypt(creditCardDetail.getCreditCardNumber());
				String formattedCreditCardNumber = cardFormatter.maskCreditCardNumber(decryptedCreditCardNumber);
				creditCardData.put("credit_card", formattedCreditCardNumber);
				creditCardData.put("transactions", transactionList); // Add the transaction details for this card
				Maxexpense.add(creditCardData);
			} catch (Exception e) {
				logger.error("Error decrypting credit card number for user: " + username, e);
				continue; // Skip if decryption fails
			}
		}

		// Step 4: Apply pagination to the result
		int start = (int) pageable.getOffset();
		int end = Math.min(start + pageable.getPageSize(), Maxexpense.size());

		// Prevent index out of bounds
		if (start > Maxexpense.size()) {
			start = Maxexpense.size();
		}

		List<Map<String, Object>> paginatedResults = Maxexpense.subList(start, end);

		// Step 5: Prepare the final result
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("content", paginatedResults); // The list of credit cards and their transactions for the current page
		result.put("totalElements", Maxexpense.size()); // Total number of credit cards with transactions
		result.put("totalPages", (int) Math.ceil((double) Maxexpense.size() / pageable.getPageSize())); // Total pages
																										// for
																										// pagination
		result.put("pageable", buildPageableInfo(pageable)); // Pageable info (page size, page number, sort)

		return result; // Return the final result
	}

	// Utility method to get the active credit cards for the user

	List<CreditCardDetail> getActiveCreditCardsForUser(String username, String statusFilter) {
		logger.info("Entering getActiveCreditCardsForUser");
		
		List<CreditCardDetail> activeCreditCards = new ArrayList<>();
		
		try {
			
			activeCreditCards = cardService.getAllCreditCardsForUser(username).getCreditcards();
	
			if (activeCreditCards == null || activeCreditCards.isEmpty()) {
				logger.error("No active credit cards found for user: {}", username);
				throw new CreditCardNotFoundException(username);
			}
		} catch (CustomerNotFoundException e) {
			logger.error(e.getMessage());
			return Collections.emptyList();
		} catch (CreditCardNotFoundException e) {
			logger.error(e.getMessage());
			return Collections.emptyList();
		} catch (DataAccessException e) {
			logger.error("Database error while retrieving creditcards", e);
			return Collections.emptyList();
		} catch (Exception e) {
			logger.error("Error retrieving credit cards for user: {}", username);
			return Collections.emptyList(); // Handle other errors
		}
		return activeCreditCards.stream().filter(card -> {
			switch (statusFilter.toLowerCase()) {
			case "enabled":
				return card.getStatus().equalsIgnoreCase("enabled");
			case "disabled":
				return card.getStatus().equalsIgnoreCase("disabled");
			case "both":
				return true; // Include all cards
			default:
				throw new IllegalArgumentException("Invalid status filter: " + statusFilter);
			}
		}).collect(Collectors.toList());
	}

	// Utility method to get transactions for the user

	List<CreditCardTransaction> getTransactionsForUser(String username) {

		List<CreditCardTransaction> cardTransactions = new ArrayList<>();
		try {
			cardTransactions = transactionRepository.findByUsername(username).getCreditcards().stream()
					.collect(Collectors.toList());
			if (cardTransactions == null || cardTransactions.isEmpty()) {
				logger.error("No transactions found for user: {}", username);
				throw new CreditCardNotFoundException(username);
			}
		} catch (TransactionNotFoundException e) {
			logger.error(e.getMessage());
			return Collections.emptyList(); // Handle user not found exception
		} catch (DataAccessException e) {
			logger.error("Database error while retrieving transactions", e);
			return Collections.emptyList();
		} catch (Exception e) {
			logger.error("Error retrieving Transactions for user: {}", username);
			return Collections.emptyList();
		}
		return cardTransactions;
	}

	// Helper method to build the pageable info object

	private Map<String, Object> buildPageableInfo(Pageable pageable) {
		Map<String, Object> pageableInfo = new LinkedHashMap<>();
		pageableInfo.put("pageSize", pageable.getPageSize());
		pageableInfo.put("pageNumber", pageable.getPageNumber());
		pageableInfo.put("sort",
				Map.of("sorted", pageable.getSort().isSorted(), "unsorted", !pageable.getSort().isSorted()));
		return pageableInfo;
	}

}
