package com.pyrosense.scoring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"analysis-events", "scoring-events"})
class ScoringServiceApplicationTest {

    @Test
    void contextLoads() {
    }
}
