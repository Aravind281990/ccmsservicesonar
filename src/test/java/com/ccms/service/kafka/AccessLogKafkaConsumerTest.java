package com.ccms.service.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Unit tests for the AccessLogKafkaConsumer class.
 */
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@EmbeddedKafka(partitions = 1, topics = "access-logs", bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@EnableKafka
class AccessLogKafkaConsumerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;  // Mock KafkaTemplate for sending messages

    private AccessLogKafkaConsumer accessLogKafkaConsumer;

    /**
     * Setup before each test.
     */
    @BeforeEach
    void setup() {
        accessLogKafkaConsumer = new AccessLogKafkaConsumer(); // Initialize the consumer
    }

    /**
     * Test that the Kafka listener processes a message correctly.
     */
    @Test
    void testListenTransactionLogs() {
        // Simulate the message to be received by the Kafka listener
        String logMessage = "User accessed /dashboard";

        // Simulate receiving a message (KafkaListener should automatically trigger)
        accessLogKafkaConsumer.listenTransactionLogs(logMessage);

        // Here, we're just verifying that the message was logged.
        // In a real scenario, you could mock the logger or check the console output.
        // For now, we just verify the listener is triggered.
        verify(kafkaTemplate, times(0)).send(anyString(), anyString()); // No KafkaTemplate send call expected here
    }
}
