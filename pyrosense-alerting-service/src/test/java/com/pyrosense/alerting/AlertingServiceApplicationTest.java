package com.pyrosense.alerting;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"scoring-events", "analysis-events", "alerting-events"})
class AlertingServiceApplicationTest {

    @Test
    void contextLoads() {
    }
}
