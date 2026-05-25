# PyroSense AI Platform - Testing Strategy

## 1. Test Pyramid

```
         /  E2E Tests  \         <- Few, slow, high confidence
        / Contract Tests \       <- API compatibility
       / Integration Tests \     <- Testcontainers
      /    Unit Tests       \    <- Many, fast, isolated
     /_______________________\
```

## 2. Unit Tests

### Scope
- Domain model (aggregates, entities, value objects)
- Domain services (baseline learner, anomaly detector, risk calculator)
- Application services (use cases with mocked ports)

### Conventions
- File: `*Test.java`
- Framework: JUnit 5 + AssertJ
- No Spring context loaded
- Mocking: Mockito for port interfaces

### Example Pattern
```java
class AlertTest {
    @Test
    void shouldRejectAcknowledgeOnNonOpenAlert() {
        var alert = createResolvedAlert();
        assertThatThrownBy(() -> alert.acknowledge("user-1"))
            .isInstanceOf(IllegalStateException.class);
    }
}
```

### Coverage Target
- Domain layer: > 90%
- Application layer: > 80%
- Infrastructure: excluded from unit test coverage (tested via integration)

## 3. Integration Tests

### Scope
- Persistence adapters (JPA repositories)
- Kafka producers/consumers
- REST controllers (MockMvc)
- Full service context loading

### Technology
- **Testcontainers**: PostgreSQL, Kafka, Redis
- **Spring Boot Test**: @SpringBootTest with test profiles
- **WireMock**: External service mocking (planned)

### Conventions
- File: `*IT.java` or `*IntegrationTest.java`
- Run via Maven Failsafe plugin
- Separate from unit tests (different phase)

### Test Profile
```yaml
# application-test.yml per service
spring:
  datasource:
    url: jdbc:h2:mem:testdb    # H2 for fast tests
  flyway:
    enabled: false             # Schema via JPA auto-DDL
```

### Testcontainers for Real DB Tests
```java
@Testcontainers
@SpringBootTest
class InstallationPersistenceIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("timescale/timescaledb:latest-pg16");
}
```

## 4. Architecture Tests (ArchUnit)

### Rules Enforced
```java
@ArchTest
static final ArchRule domainShouldNotDependOnSpring =
    noClasses().that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("org.springframework..");

@ArchTest
static final ArchRule useCasesShouldNotDependOnAdapters =
    noClasses().that().resideInAPackage("..application..")
        .should().dependOnClassesThat()
        .resideInAPackage("..adapter..");

@ArchTest
static final ArchRule adaptersShouldNotBeAccessedByDomain =
    noClasses().that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("..adapter..");
```

### Placement
Each service has `src/test/java/.../architecture/HexagonalArchitectureTest.java`

## 5. Contract Tests

### Purpose
Ensure API compatibility between services (producer/consumer contracts).

### Approach
- **Spring Cloud Contract** (planned Phase 2)
- Producer defines contract (REST endpoint schema)
- Consumer verifies against stubs

### Priority Contracts
| Producer | Consumer | API |
|----------|----------|-----|
| Device Service | Gateway | GET /api/v1/installations |
| Alerting Service | Gateway | GET /api/v1/alerts |
| Ingestion Service | Analysis Service | Kafka: signal.ingested |

## 6. Performance Tests

### Tools (Planned Phase 3)
- **Gatling** for HTTP load testing
- **kafka-producer-perf-test** for Kafka throughput

### Benchmarks
| Metric | Target |
|--------|--------|
| Signal ingestion throughput | 1000 msg/s |
| Risk calculation latency (p95) | < 500ms |
| REST API latency (p95) | < 200ms |
| Concurrent sensors supported | 500 |

## 7. Maven Plugin Configuration

### Surefire (Unit Tests)
```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>**/*IT.java</exclude>
            <exclude>**/*IntegrationTest.java</exclude>
        </excludes>
    </configuration>
</plugin>
```

### Failsafe (Integration Tests)
```xml
<plugin>
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/*IT.java</include>
            <include>**/*IntegrationTest.java</include>
        </includes>
    </configuration>
</plugin>
```

### JaCoCo (Coverage)
- Minimum 80% line coverage per module
- Exclusions: config classes, Spring Boot application class
- Report generated at `target/site/jacoco/`

## 8. Running Tests

```bash
# Unit tests only
mvn test

# Unit + integration tests
mvn verify -Ptest

# Specific service
mvn test -pl pyrosense-alerting-service

# With coverage report
mvn verify -Ptest && open target/site/jacoco/index.html
```
