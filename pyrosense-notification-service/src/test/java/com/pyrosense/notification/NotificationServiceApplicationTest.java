package com.pyrosense.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"alerting-events", "scoring-events", "maintenance-events", "reporting-events", "device-events", "notification-dlq"})
class NotificationServiceApplicationTest {

    @Test
    void contextLoads() {
    }
}
