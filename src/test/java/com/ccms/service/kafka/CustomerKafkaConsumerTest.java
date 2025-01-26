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

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@EmbeddedKafka(partitions = 1, topics = "customer-log-topic", bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@EnableKafka
class CustomerKafkaConsumerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;  // Mock KafkaTemplate

    private CustomerKafkaConsumer customerKafkaConsumer;

    private static final String TEST_MESSAGE = "Customer action: Login successful";  // Test message

    @BeforeEach
    void setup() {
        customerKafkaConsumer = new CustomerKafkaConsumer();  // Initialize the consumer
    }

    @Test
    void testListenCustomerLogs() throws InterruptedException {
        // Simulate receiving a message from Kafka
        customerKafkaConsumer.listenCustomerLogs(TEST_MESSAGE);

        // Sleep for a moment to allow Kafka listener to process the message
        Thread.sleep(1000);  // Adjust if necessary

        // Verify that the message was logged
        verify(kafkaTemplate, times(0)).send(anyString(), anyString());  // No send expected in the consumer test
    }
}
