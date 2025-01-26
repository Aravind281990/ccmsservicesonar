package com.ccms.service.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class TransactionKafkaProducerTest {

    // Mock the KafkaTemplate to avoid interacting with a real Kafka broker
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    // Inject mocks into the TransactionKafkaProducer
    @InjectMocks
    private TransactionKafkaProducer transactionKafkaProducer;

    // Define a test message
    private static final String TEST_TRANSACTION_LOG = "{\"transactionId\":\"12345\",\"amount\":500}";

    /**
     * Test that the sendTransactionLog method correctly sends the transaction log to Kafka.
     */
    @Test
    void testSendTransactionLog() {
        // Act: Call the method to send the transaction log
        transactionKafkaProducer.sendTransactionLog(TEST_TRANSACTION_LOG);

        // Assert: Verify that KafkaTemplate's send method was called with the correct topic and data
        verify(kafkaTemplate, times(1)).send("transaction-log-topic", TEST_TRANSACTION_LOG);
    }
}
