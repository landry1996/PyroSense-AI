package com.pyrosense.alerting.adapter.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.alerting.domain.model.*;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.util.ClockProvider;
import com.pyrosense.shared.valueobject.AlertSeverity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JdbcAlertRepository.class, JdbcAlertRepositoryTest.TestConfig.class})
class JdbcAlertRepositoryTest {

    @Configuration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private JdbcAlertRepository repository;

    private final Instant now = Instant.parse("2025-01-15T10:00:00Z");
    private final TenantId tenantId = TenantId.generate();
    private final DeviceId deviceId = DeviceId.generate();

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(now, ZoneId.of("UTC")));
    }

    @AfterEach
    void tearDown() {
        ClockProvider.reset();
    }

    private Alert createAndSave(AlertType type, AlertSeverity severity) {
        Alert alert = Alert.create(tenantId, deviceId, type, severity, "Test " + type, "Desc", SlaPolicy.defaults());
        alert.clearDomainEvents();
        return repository.save(alert);
    }

    @Test
    void shouldSaveAndFindById() {
        Alert saved = createAndSave(AlertType.OVERHEATING, AlertSeverity.CRITICAL);

        Optional<Alert> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().type()).isEqualTo(AlertType.OVERHEATING);
        assertThat(found.get().severity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(found.get().status()).isEqualTo(AlertStatus.OPEN);
    }

    @Test
    void shouldUpdateStatus() {
        Alert alert = createAndSave(AlertType.OVERHEATING, AlertSeverity.CRITICAL);
        alert.acknowledge(UserId.generate());
        alert.clearDomainEvents();
        repository.save(alert);

        Optional<Alert> found = repository.findById(alert.getId());
        assertThat(found.get().status()).isEqualTo(AlertStatus.ACKNOWLEDGED);
    }

    @Test
    void shouldFindByTenantId() {
        createAndSave(AlertType.OVERHEATING, AlertSeverity.CRITICAL);
        createAndSave(AlertType.HARMONIC_DISTORTION, AlertSeverity.WARNING);

        var results = repository.findByTenantId(tenantId);

        assertThat(results).hasSize(2);
    }

    @Test
    void shouldFindActiveByDeduplicationKey() {
        createAndSave(AlertType.OVERHEATING, AlertSeverity.CRITICAL);

        DeduplicationKey key = new DeduplicationKey(deviceId, AlertType.OVERHEATING);
        Optional<Alert> found = repository.findActiveByDeduplicationKey(key);

        assertThat(found).isPresent();
    }

    @Test
    void shouldNotFindResolvedByDeduplicationKey() {
        Alert alert = createAndSave(AlertType.OVERHEATING, AlertSeverity.CRITICAL);
        alert.resolve(UserId.generate(), "done");
        alert.clearDomainEvents();
        repository.save(alert);

        DeduplicationKey key = new DeduplicationKey(deviceId, AlertType.OVERHEATING);
        Optional<Alert> found = repository.findActiveByDeduplicationKey(key);

        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindByStatus() {
        createAndSave(AlertType.OVERHEATING, AlertSeverity.CRITICAL);

        var results = repository.findByStatus(AlertStatus.OPEN);

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(a -> a.status() == AlertStatus.OPEN);
    }

    @Test
    void shouldCountByTenantAndStatus() {
        createAndSave(AlertType.OVERHEATING, AlertSeverity.CRITICAL);
        createAndSave(AlertType.HARMONIC_DISTORTION, AlertSeverity.WARNING);

        long count = repository.countByTenantAndStatus(tenantId, AlertStatus.OPEN);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldPersistComments() {
        Alert alert = createAndSave(AlertType.OVERHEATING, AlertSeverity.CRITICAL);
        UserId author = UserId.generate();
        alert.addComment(author, "First comment");
        alert.addComment(author, "Second comment");
        repository.save(alert);

        Optional<Alert> found = repository.findById(alert.getId());
        assertThat(found.get().comments()).hasSize(2);
        assertThat(found.get().comments().get(0).content()).isEqualTo("First comment");
    }
}
