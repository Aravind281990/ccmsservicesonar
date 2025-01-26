package com.ccms.service.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;


@ExtendWith(MockitoExtension.class)
class CustomerKafkaProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;  // Mock KafkaTemplate

    @InjectMocks
    private CustomerKafkaProducer customerKafkaProducer;  // The producer to be tested

    private static final String CUSTOMER_TOPIC = "customer-log-topic";  // Kafka topic
    private static final String TEST_CUSTOMER_LOG = "Customer action: Login successful";  // Sample log message

    /**
     * Before each test, you can initialize necessary mock objects if needed.
     */
    @BeforeEach
    void setup() {
        // Any setup can be done here, like initializing mock objects if necessary.
    }

    /**
     * Test that the sendCustomerLog method sends a message to the correct Kafka topic.
     */
    @Test
    void testSendCustomerLog() {
        // Act: Call the method under test
        customerKafkaProducer.sendCustomerLog(TEST_CUSTOMER_LOG);

        // Assert: Verify that the KafkaTemplate's send method is called with the correct topic and message
        verify(kafkaTemplate, times(1)).send(CUSTOMER_TOPIC, TEST_CUSTOMER_LOG);
    }
}
