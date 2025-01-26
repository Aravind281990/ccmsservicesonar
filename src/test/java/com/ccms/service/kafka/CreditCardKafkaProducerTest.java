package com.ccms.service.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;

/**
 * Unit tests for the CreditCardKafkaProducer class.
 */
@ExtendWith(MockitoExtension.class)
class CreditCardKafkaProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;  // Mock KafkaTemplate

    @InjectMocks
    private CreditCardKafkaProducer creditCardKafkaProducer;  // Inject the mock into the producer

    /**
     * Setup before each test.
     */
    @BeforeEach
    void setup() {
        // No special setup is needed since we're using mocks
    }

    /**
     * Test that the sendCreditCardLog method sends a message to the Kafka topic.
     */
    @Test
    void testSendCreditCardLog() {
        // Arrange: Prepare the credit card log message
        String creditCardData = "Credit Card transaction event: User made a payment of $100";

        // Act: Call the send method on the producer
        creditCardKafkaProducer.sendCreditCardLog(creditCardData);

        // Assert: Verify that the KafkaTemplate's send method is called with the correct topic and message
        verify(kafkaTemplate, times(1)).send("creditcard-log-topic", creditCardData);  // Topic and message should match
    }
}
