package com.ccms.service.kafka;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

/**
 * Unit tests for the AccessLogKafkaProducer class.
 */
@ExtendWith(MockitoExtension.class)
class AccessLogKafkaProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;  // Mock KafkaTemplate

    @InjectMocks
    private AccessLogKafkaProducer accessLogKafkaProducer;  // Inject mocks into the producer

    /**
     * Test that the sendLog method sends a valid log to the Kafka topic.
     */
    @Test
    void testSendLog_validLog() {
        String accessLog = "User accessed /home";

        // Call the method to send the log
        accessLogKafkaProducer.sendLog(accessLog);

        // Verify that the send method was called with the correct topic and log
        verify(kafkaTemplate).send("access-logs", accessLog);
    }

    /**
     * Test that the sendLog method skips logs containing "/actuator/prometheus".
     */
    @Test
    void testSendLog_prometheusLog() {
        String accessLog = "Metrics accessed at /actuator/prometheus";

        // Call the method with a log containing "/actuator/prometheus"
        accessLogKafkaProducer.sendLog(accessLog);

        // Verify that the send method was NOT called (log is skipped)
        verify(kafkaTemplate, never()).send("access-logs", accessLog);
    }

    /**
     * Test that the sendLog method sends other valid logs to the Kafka topic.
     */
    @Test
    void testSendLog_otherLog() {
        String accessLog = "User accessed /dashboard";

        // Call the method to send the log
        accessLogKafkaProducer.sendLog(accessLog);

        // Verify that the send method was called with the correct topic and log
        verify(kafkaTemplate).send("access-logs", accessLog);
    }
}
