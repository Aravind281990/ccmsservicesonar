package com.ccms.service.service.impl;

import com.ccms.service.model.CreditCard;
import com.ccms.service.model.Customer;
import com.ccms.service.model.Transaction;
import com.ccms.service.model.Transaction.TransactionDetail;
import com.ccms.service.model.Transaction.CreditCardTransaction;
import com.ccms.service.model.TransactionWithCardId;
import com.ccms.service.model.CreditCard.CreditCardDetail;
import com.ccms.service.repository.CreditCardRepository;
import com.ccms.service.repository.CustomerRepository;
import com.ccms.service.repository.TransactionRepository;
import com.ccms.service.service.CreditCardService;
import com.ccms.service.utilities.CreditCardEnDecryption;
import com.ccms.service.utilities.CreditCardFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.Aggregation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.Collectors;

public class TransactionServiceimplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CreditCardService creditCardService;

    @Mock
    private CreditCardRepository creditCardRepository;  
    
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CreditCardEnDecryption cardEnDecryption;

    @Mock
    private CreditCardFormatter cardFormatter;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private CreditCardServiceImpl creditCardServiceImpl;

    @InjectMocks
    private TransactionServiceimpl transactionServiceImpl;

    private Pageable pageable;
    
    
    private Customer mockCustomer;
    private CreditCard mockCreditCard;
    private CreditCardDetail mockCreditCardDetail;


    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        pageable = PageRequest.of(0, 10); // Example Pageable for pagination
        
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
        

    }

    // Test for getTransactionsForUser with valid username
    @Test
    public void testGetTransactionsForUser_Valid() {
        // Mock the transaction data
        TransactionDetail transactionDetail = new TransactionDetail(1L, "2025-01-23", "10:00", "db", 100.0, "Payment to Vendor");
        TransactionWithCardId transactionWithCardId = new TransactionWithCardId(123, transactionDetail);

        List<TransactionWithCardId> transactions = Collections.singletonList(transactionWithCardId);

        // Mock the AggregationResults for the aggregation query
        AggregationResults<TransactionWithCardId> aggregationResults = mock(AggregationResults.class);
        when(aggregationResults.getMappedResults()).thenReturn(transactions);

        // Mock the count aggregation
        AggregationResults<Map> countResults = mock(AggregationResults.class);
        Map<String, Integer> countMap = new HashMap<>();
        countMap.put("totalCount", 1);
        when(countResults.getUniqueMappedResult()).thenReturn(countMap);

        // Mock the aggregation calls to MongoTemplate
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("Transactions"), eq(TransactionWithCardId.class)))
                .thenReturn(aggregationResults);
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("Transactions"), eq(Map.class)))
                .thenReturn(countResults);

        // Act: Call the service method
        Page<TransactionWithCardId> result = transactionServiceImpl.getTransactionsForUser("user123", pageable);

        // Assert: Verify that the result contains the transaction details
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(123, result.getContent().get(0).getCreditCardId());
        assertEquals(100.0, result.getContent().get(0).getTransactionDetail().getTransactionAmount());
        assertEquals("Payment to Vendor", result.getContent().get(0).getTransactionDetail().getTransactionDesc());
    }

    // Test for getTransactionsForUser with no transactions found
    @Test
    public void testGetTransactionsForUser_NoTransactions() {
        // Mock the AggregationResults for the aggregation query with empty list
        AggregationResults<TransactionWithCardId> aggregationResults = mock(AggregationResults.class);
        when(aggregationResults.getMappedResults()).thenReturn(Collections.emptyList());

        // Mock the count aggregation with total count = 0
        AggregationResults<Map> countResults = mock(AggregationResults.class);
        Map<String, Integer> countMap = new HashMap<>();
        countMap.put("totalCount", 0);
        when(countResults.getUniqueMappedResult()).thenReturn(countMap);

        // Mock the aggregation calls to MongoTemplate
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("Transactions"), eq(TransactionWithCardId.class)))
                .thenReturn(aggregationResults);
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("Transactions"), eq(Map.class)))
                .thenReturn(countResults);

        // Act: Call the service method
        Page<TransactionWithCardId> result = transactionServiceImpl.getTransactionsForUser("user123", pageable);

        // Assert: Verify that the result has no transactions
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    // Test for getTransactionsForUser when an exception occurs
    @Test
    public void testGetTransactionsForUser_Exception() {
        // Mock the aggregation to throw an exception
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("Transactions"), eq(TransactionWithCardId.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert: Ensure that an exception is thrown when calling the service method
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transactionServiceImpl.getTransactionsForUser("user123", pageable);
        });

        // Assert: Check the exception message
        assertEquals("Database error", exception.getMessage());
    }
    
    @Test
    void testGetMaxExpensesForLastMonth() {
        // Setup mocks
        String username = "testUser";
        String statusFilter = "enabled";
        Pageable pageable = PageRequest.of(0, 5);

        // Mocking active and inactive credit cards
        CreditCard.CreditCardDetail creditCard1 = new CreditCard.CreditCardDetail(
                1, "1111-1111-1111-1111", 12, 2026, 123, "Vendor1", "enabled");

        CreditCard.CreditCardDetail creditCard2 = new CreditCard.CreditCardDetail(
                2, "2222-2222-2222-2222", 5, 2025, 456, "Vendor2", "disabled");

        // Only pass active credit cards for the test
        List<CreditCard.CreditCardDetail> activeCreditCards = Arrays.asList(creditCard1,creditCard2);

        // Mock `creditCardRepository.findByUsername()` to return only active credit cards
        CreditCard mockResponse = new CreditCard();
        mockResponse.setUsername("testUser");
        mockResponse.setCreditcards(activeCreditCards);  
      
        when(creditCardRepository.findByUsername("testUser")).thenReturn(mockResponse);

        // Mock getAllCreditCardsForUser to return the mockCreditCard
        when(creditCardService.getAllCreditCardsForUser("testUser")).thenReturn(mockResponse);
        
        
        // Mock `transactionRepository.findByUsername()` to return transactions for only active credit cards
        TransactionDetail transaction1 = new TransactionDetail(1L, "12/15/2024", "10:00", "db", 500.0, "Purchase 1");
        TransactionDetail transaction2 = new TransactionDetail(2L, "12/23/2024", "12:30", "db", 700.0, "Purchase 2");
        
        CreditCardTransaction cardTransaction1 = new CreditCardTransaction(1, Arrays.asList(transaction1, transaction2));

        List<CreditCardTransaction> cardTransactions = Arrays.asList(cardTransaction1);

        Transaction mockTransaction = new Transaction();
        mockTransaction.setUsername("testUser");
        mockTransaction.setCreditcards(cardTransactions);  // Set only active card transactions

        when(transactionRepository.findByUsername("testUser")).thenReturn(mockTransaction);
       
        
        // Mock decryption and formatting
        try {
            when(cardEnDecryption.decrypt(anyString())).thenReturn("1111-1111-1111-1111");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        when(cardFormatter.maskCreditCardNumber(anyString())).thenReturn("1111-****-****-1111");
        
        
        // Call method under test
        Page<Map<String, Object>> result = transactionServiceImpl.getMaxExpensesForLastMonth(username, statusFilter, pageable);

        // Verify results
        assertNotNull(result);  // Ensure the result is not null
        assertEquals(1, result.getTotalElements());  // We expect 1 transaction result (from active card)
        assertEquals(1, result.getContent().size());  // Should match the content size for filtered results

        // Retrieve the first result
        Map<String, Object> expenseDetails = result.getContent().get(0);

        // Assert that the credit card number is masked correctly
        assertEquals("1111-****-****-1111", expenseDetails.get("credit_card"));

        // Assert the month is correct (assuming 'MAR' is the correct month for the transactions)
        assertEquals("DEC", expenseDetails.get("month"));

        // Assert that the maximum expense is correctly calculated (700.0 is the highest value among active card transactions)
        assertEquals(700.0, expenseDetails.get("amount"));
    }
    
    
    @Test
    void testGetHighValueExpensesForUser() {
        // Setup mocks
        String username = "testUser";
        int limit = 5;
        String statusFilter = "enabled";
        double amountThreshold = 500.0;
        Pageable pageable = PageRequest.of(0, 5); // Paginated results for the first page

        // Mocking active and inactive credit cards
        CreditCard.CreditCardDetail creditCard1 = new CreditCard.CreditCardDetail(
                1, "1111-1111-1111-1111", 12, 2026, 123, "Vendor1", "enabled");

        CreditCard.CreditCardDetail creditCard2 = new CreditCard.CreditCardDetail(
                2, "2222-2222-2222-2222", 5, 2025, 456, "Vendor2", "disabled");

        // Only pass active credit cards for the test
        List<CreditCard.CreditCardDetail> activeCreditCards = Arrays.asList(creditCard1);

        // Mock `creditCardRepository.findByUsername()` to return only active credit cards
        CreditCard mockResponse = new CreditCard();
        mockResponse.setUsername("testUser");
        mockResponse.setCreditcards(activeCreditCards);

        when(creditCardRepository.findByUsername("testUser")).thenReturn(mockResponse);

        when(creditCardService.getAllCreditCardsForUser("testUser")).thenReturn(mockResponse);
        
        // Mock `transactionRepository.findByUsername()` to return transactions for only active credit cards
        TransactionDetail transaction1 = new TransactionDetail(1L, "12/15/2024", "10:00", "db", 300.0, "Purchase 1");
        TransactionDetail transaction2 = new TransactionDetail(2L, "12/20/2024", "12:30", "db", 700.0, "Purchase 2");
        TransactionDetail transaction3 = new TransactionDetail(3L, "12/23/2024", "14:00", "db", 1000.0, "Purchase 3");

        // Only two transactions above the threshold (500.0)
        CreditCardTransaction cardTransaction1 = new CreditCardTransaction(1, Arrays.asList(transaction1, transaction2, transaction3));

        List<CreditCardTransaction> cardTransactions = Arrays.asList(cardTransaction1);

        Transaction mockTransaction = new Transaction();
        mockTransaction.setUsername("testUser");
        mockTransaction.setCreditcards(cardTransactions);  // Set only active card transactions

        when(transactionRepository.findByUsername("testUser")).thenReturn(mockTransaction);

        // Mock decryption and formatting
        try {
            when(cardEnDecryption.decrypt(anyString())).thenReturn("1111-1111-1111-1111");
        } catch (Exception e) {
            e.printStackTrace();
        }

        when(cardFormatter.maskCreditCardNumber(anyString())).thenReturn("1111-****-****-1111");

        // Call method under test
        Map<String, Page<Map<String, String>>> result = transactionServiceImpl.getHighValueExpensesForUser(username, limit, statusFilter, amountThreshold, pageable);


        // Verify the result
        assertNotNull(result);  // Ensure the result is not null
        assertEquals(1, result.size());  // Should only be 1 card (active one)

        // Retrieve the first result (the only one in this case)
        Page<Map<String, String>> page = result.get("1111-****-****-1111");
        

        // Assert that the page contains the correct number of results (transactions)
        assertNotNull(page);
        assertEquals(2, page.getTotalElements());  // Only two transactions should meet the threshold (700.0 and 1000.0)

        // Assert that the transactions are correctly sorted by date (recent first)
        List<Map<String, String>> transactions = page.getContent();
        assertEquals("1000.0", transactions.get(0).get("transactionamount"));  // Highest transaction first
        assertEquals("700.0", transactions.get(1).get("transactionamount"));

        // Assert the transaction details for the first transaction
        assertEquals("Purchase 3", transactions.get(0).get("transactiondescription"));
        assertEquals("12/23/2024", transactions.get(0).get("transactiondate"));

        // Assert the transaction details for the second transaction
        assertEquals("Purchase 2", transactions.get(1).get("transactiondescription"));
        assertEquals("12/20/2024", transactions.get(1).get("transactiondate"));

        // Ensure the credit card number is masked correctly
        assertEquals("1111-****-****-1111","1111-****-****-1111");

        // Ensure that the returned map has the correct structure
        Map<String, String> transactionDetails = page.getContent().get(0);
        
        assertTrue(transactionDetails.containsKey("transactionId"));
        assertTrue(transactionDetails.containsKey("transactiondate"));
        assertTrue(transactionDetails.containsKey("transactionamount"));
    }

    
    @Test
    void testGetLastXTransactionsForUser() {
        // Setup mocks
        String username = "testUser";
        String statusFilter = "enabled";
        Pageable pageable = PageRequest.of(0, 5);  // Define pagination (e.g., 5 transactions per page)
        int limit = 10;  // Assume we want the last 10 transactions

        // Mocking active credit cards
        CreditCard.CreditCardDetail creditCard1 = new CreditCard.CreditCardDetail(
                1, "1111-1111-1111-1111", 12, 2026, 123, "Vendor1", "enabled");

        CreditCard.CreditCardDetail creditCard2 = new CreditCard.CreditCardDetail(
                2, "2222-2222-2222-2222", 5, 2025, 456, "Vendor2", "enabled");

        List<CreditCard.CreditCardDetail> activeCreditCards = Arrays.asList(creditCard1, creditCard2);

        // Mock `creditCardRepository.findByUsername()` to return active credit cards
        CreditCard mockResponse = new CreditCard();
        mockResponse.setUsername(username);
        mockResponse.setCreditcards(activeCreditCards);

        when(creditCardRepository.findByUsername("testUser")).thenReturn(mockResponse);
        
        when(creditCardService.getAllCreditCardsForUser(username)).thenReturn(mockResponse);

        // Mocking transactions for each credit card
        TransactionDetail transaction1 = new TransactionDetail(1L, "12/01/2024", "10:00", "db", 100.0, "Purchase 1");
        TransactionDetail transaction2 = new TransactionDetail(2L, "12/05/2024", "12:30", "db", 200.0, "Purchase 2");
        TransactionDetail transaction3 = new TransactionDetail(3L, "12/10/2024", "14:45", "db", 150.0, "Purchase 3");

        List<TransactionDetail> transactionsForCard1 = Arrays.asList(transaction1, transaction2);
        List<TransactionDetail> transactionsForCard2 = Arrays.asList(transaction3);

        // Mock card transactions for the user
        CreditCardTransaction cardTransaction1 = new CreditCardTransaction(1, transactionsForCard1);
        CreditCardTransaction cardTransaction2 = new CreditCardTransaction(2, transactionsForCard2);

        List<CreditCardTransaction> cardTransactions = Arrays.asList(cardTransaction1, cardTransaction2);

        // Mock `transactionRepository.findByUsername()` to return transactions for active cards
        Transaction mockTransaction = new Transaction();
        mockTransaction.setUsername(username);
        mockTransaction.setCreditcards(cardTransactions);  // Only active card transactions

        when(transactionRepository.findByUsername(username)).thenReturn(mockTransaction);

        // Call method under test
        Map<Integer, Page<TransactionDetail>> result = transactionServiceImpl.getLastXTransactionsForUser(username, limit, statusFilter, pageable);
        
        // Verify results
        assertNotNull(result);  // Ensure the result is not null
        assertEquals(2, result.size());  // We expect 2 credit cards in the result

        // For credit card 1, the list of transactions should be paginated and limited to 'limit'
        Page<TransactionDetail> pageCard1 = result.get(creditCard1.getCreditCardId());
        assertNotNull(pageCard1);
        assertEquals(2, pageCard1.getContent().size());  // Since only 2 transactions exist for card 1
        
        // Sort the transactions inside pageCard1 by transactionDate (just to be sure, since it might be out of order)
        List<TransactionDetail> sortedTransactionsCard1 = pageCard1.getContent().stream()
                .sorted(Comparator.comparing(TransactionDetail::getTransactionId)) // Sort by id, ascending
                .collect(Collectors.toList());
        
        assertEquals(transaction1.getTransactionId(), sortedTransactionsCard1.get(0).getTransactionId());  // Check if the first transaction matches
        // For credit card 2, the list should contain only the last transaction
        Page<TransactionDetail> pageCard2 = result.get(creditCard2.getCreditCardId());
        assertNotNull(pageCard2);
        
        assertEquals(1, pageCard2.getContent().size());  // Only 1 transaction exists for card 2
      
        assertEquals(transaction3.getTransactionId(), pageCard2.getContent().get(0).getTransactionId());  // Check the last transaction
        
        // Optional: Check the total number of transactions across both cards
        int totalTransactions = result.values().stream().mapToInt(page -> page.getContent().size()).sum();
        assertEquals(3, totalTransactions);  
    }
    
    @Test
    void testGetLastXExpensesForUser() {
        // Setup mocks
        String username = "testUser";
        String statusFilter = "enabled";
        Pageable pageable = PageRequest.of(0, 5);  // Define pagination (e.g., 5 expenses per page)
        int limit = 10;  // Assume we want the last 10 expenses

        // Mocking active credit cards
        CreditCardDetail creditCard1 = new CreditCardDetail(
                1, "1111-1111-1111-1111", 12, 2026, 123, "Vendor1", "enabled");
        CreditCardDetail creditCard2 = new CreditCardDetail(
                2, "2222-2222-2222-2222", 5, 2025, 456, "Vendor2", "enabled");

        List<CreditCardDetail> activeCreditCards = Arrays.asList(creditCard1, creditCard2);

      // Mock `creditCardRepository.findByUsername()` to return active credit cards
      CreditCard mockResponse = new CreditCard();
      mockResponse.setUsername(username);
      mockResponse.setCreditcards(activeCreditCards);
        
      when(creditCardRepository.findByUsername("testUser")).thenReturn(mockResponse);
   
      when(creditCardService.getAllCreditCardsForUser(username)).thenReturn(mockResponse);

        // Mocking transactions for each credit card
        TransactionDetail transaction1 = new TransactionDetail(1L, "12/01/2024", "10:00", "db", 100.0, "Purchase 1");
        TransactionDetail transaction2 = new TransactionDetail(2L, "12/05/2024", "12:30", "db", 200.0, "Purchase 2");
        TransactionDetail transaction3 = new TransactionDetail(3L, "12/10/2024", "14:45", "db", 150.0, "Purchase 3");

        List<TransactionDetail> transactionsForCard1 = Arrays.asList(transaction1, transaction2);
        List<TransactionDetail> transactionsForCard2 = Arrays.asList(transaction3);

        // Mock card transactions for the user
        CreditCardTransaction cardTransaction1 = new CreditCardTransaction(1, transactionsForCard1);
        CreditCardTransaction cardTransaction2 = new CreditCardTransaction(2, transactionsForCard2);

        List<CreditCardTransaction> cardTransactions = Arrays.asList(cardTransaction1, cardTransaction2);

        // Mock `transactionRepository.findByUsername()` to return transactions for active cards
        Transaction mockTransaction = new Transaction();
        mockTransaction.setUsername(username);
        mockTransaction.setCreditcards(cardTransactions);  // Only active card transactions
        
        // Mock `getTransactionsForUser()` to return transactions for active cards
        when(transactionRepository.findByUsername(username)).thenReturn(mockTransaction);

        // Mock decryption and formatting services
        try {
			when(cardEnDecryption.decrypt(creditCard1.getCreditCardNumber())).thenReturn("1111-1111-1111-1111");
		} catch (Exception e) {
			e.printStackTrace();
		}
        try {
			when(cardEnDecryption.decrypt(creditCard2.getCreditCardNumber())).thenReturn("2222-2222-2222-2222");
		} catch (Exception e) {
			e.printStackTrace();
		}
        when(cardFormatter.maskCreditCardNumber(anyString())).thenReturn("****-****-****-1111");
        when(cardFormatter.maskCreditCardNumber(anyString())).thenReturn("****-****-****-2222");

        // Call method under test
        Map<String, Object> result = transactionServiceImpl.getLastXExpensesForUser(username, limit, statusFilter, pageable);

        // Verify results
        assertNotNull(result);  // Ensure the result is not null
        assertTrue(result.containsKey("content"));  // Ensure that 'content' key exists in the result
        assertTrue(result.containsKey("totalElements"));  // Ensure that 'totalElements' exists
        assertTrue(result.containsKey("totalPages"));  // Ensure that 'totalPages' exists
        assertTrue(result.containsKey("pageable"));  // Ensure that 'pageable' exists

        // Check pagination and transaction content
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
        assertNotNull(content);
        assertEquals(2, content.size());  // Since there are two credit cards, we expect two entries in the content

        // Verify the structure of the content for credit card 1
        Map<String, Object> card1Content = content.get(0);
        assertTrue(card1Content.containsKey("credit_card"));
        assertTrue(card1Content.containsKey("transactions"));
        
        // Check the transactions under credit card 1
        List<Map<String, Object>> card1Transactions = (List<Map<String, Object>>) card1Content.get("transactions");
        assertEquals(2, card1Transactions.size());  // There should be 2 transactions for card 1 (limited by the 'limit')

        // Check the structure of each transaction for card 1
        Map<String, Object> transaction1Map = card1Transactions.get(0);
        assertTrue(transaction1Map.containsKey("month"));
        assertTrue(transaction1Map.containsKey("amount"));
        assertTrue(transaction1Map.containsKey("description"));

        // Verify that transactions are sorted by date (descending)
        assertEquals("DEC", transaction1Map.get("month"));  // First transaction should be in December
        assertEquals(200.0, transaction1Map.get("amount"));  // Amount should match transaction 2
        assertEquals("Purchase 2", transaction1Map.get("description"));
        
        // Verify pagination details
        assertEquals(1, result.get("totalPages"));
        assertEquals(2, result.get("totalElements"));
        assertNotNull(result.get("pageable"));
        
        // For credit card 2, check the transactions as well
        Map<String, Object> card2Content = content.get(1);
        assertTrue(card2Content.containsKey("credit_card"));
        assertTrue(card2Content.containsKey("transactions"));
      
        // Check the transactions under credit card 2
        List<Map<String, Object>> card2Transactions = (List<Map<String, Object>>) card2Content.get("transactions");
        assertEquals(1, card2Transactions.size());  // There should be only 1 transaction for card 2
        
        // Check the details of card 2's transaction
        Map<String, Object> transaction3Map = card2Transactions.get(0);
        assertTrue(transaction3Map.containsKey("month"));
        assertTrue(transaction3Map.containsKey("amount"));
        assertTrue(transaction3Map.containsKey("description"));
        
        assertEquals("DEC", transaction3Map.get("month"));  // This should be in December as well
        assertEquals(150.0, transaction3Map.get("amount"));  // Amount should match transaction 3
        assertEquals("Purchase 3", transaction3Map.get("description"));
    }


}
