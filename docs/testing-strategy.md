# PyroSense AI Platform - Testing Strategy

## 1. Test Pyramid

```
         /  E2E Tests  \         <- Few, slow, high confidence
        / Performance   \       <- Smoke tests (throughput, latency)
       / Contract Tests  \      <- API compatibility (Spring Cloud Contract)
      / Integration Tests \     <- Testcontainers (PostgreSQL, Kafka, Redis)
     / Architecture Tests  \    <- ArchUnit (hexagonal, naming, no cycles)
    /    Security Tests     \   <- Tenant isolation, JWT validation, RBAC
   /      Unit Tests         \  <- Many, fast, isolated (domain + use cases)
  /___________________________\
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
- Location: `src/test/java/.../domain/` and `src/test/java/.../application/`

### Example Pattern
```java
class RiskScoreTest {
    @Test
    void shouldClampScoreAt100() {
        var score = RiskScore.of(150);
        assertThat(score.value()).isEqualTo(100);
    }

    @Test
    void shouldRejectNegativeScore() {
        assertThatThrownBy(() -> RiskScore.of(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

### Coverage Targets (enforced by JaCoCo)

| Layer | Minimum | Rationale |
|-------|---------|-----------|
| Domain | 90% | Pure logic, no excuses |
| Application (use cases) | 85% | Orchestration with mocked ports |
| Adapters | 60-70% | Tested via integration tests |
| Config | Excluded | Wiring only, verified by context load |

### Exclusions from Coverage
- `*Application.java` (Spring Boot entry points)
- `*Config.java` / `*Configuration.java`
- `adapter/in/rest/dto/**` (records/DTOs)

## 3. Integration Tests

### Scope
- Persistence adapters (JDBC repositories with real PostgreSQL)
- Kafka producers/consumers (real broker)
- Redis adapters (real Redis)
- Full service context loading
- REST controllers (WebTestClient)

### Technology
- **Testcontainers**: PostgreSQL/TimescaleDB, Kafka, Redis
- **Spring Boot Test**: @SpringBootTest with test profiles
- **WireMock**: External service mocking

### Conventions
- File: `*IT.java` or `*IntegrationTest.java`
- Run via Maven Failsafe plugin (separate `verify` phase)
- Use shared container configs (singleton pattern for speed)

### Shared Container Infrastructure
```java
public class PostgresContainerConfig {
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("timescale/timescaledb:latest-pg16")
            .withDatabaseName("pyrosense_test")
            .withUsername("test")
            .withPassword("test");

    static { POSTGRES.start(); }

    public static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
```

### Test Profile
```yaml
# application-test.yml per service
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
  flyway:
    enabled: true  # Use real migrations in IT tests
```

## 4. Architecture Tests (ArchUnit)

### Hexagonal Rules (per service)
Each service has `src/test/java/.../architecture/HexagonalArchitectureTest.java`:

| Rule | Description |
|------|-------------|
| Domain ↛ Spring | No Spring imports in domain package |
| Domain ↛ Adapters | Domain never references adapter code |
| Domain ↛ Application | Domain is self-contained |
| Application ↛ Adapters | Use cases depend only on ports |
| Application ↛ Config | No config awareness in use cases |
| Ports are interfaces | port.in and port.out contain only interfaces |
| Layered architecture | Proper layer access rules |
| Domain events implement DomainEvent | Contract enforcement |
| No @Service on use cases | Use cases are POJOs wired via config |
| No field injection | Constructor injection only |

### Platform-Wide Rules
`PlatformArchitectureRulesTest.java` enforces cross-cutting concerns:

| Rule | Description |
|------|-------------|
| No package cycles | Slices are cycle-free |
| Controllers ↛ Repositories | Must go through use cases |
| No cross-context imports | e.g., alerting ↛ ingestion.domain |
| Value objects are immutable | No setters in domain |
| Naming conventions | UseCase, Port, Adapter, Event, Exception suffixes |

### Naming Conventions
`NamingConventionTest.java` enforces:

| Pattern | Convention |
|---------|-----------|
| Port-in interfaces | `*UseCase` or `*Query` |
| Port-out interfaces | `*Port` |
| Adapter classes | Contains "Adapter" |
| Domain events | `*Event` |
| Exceptions | `*Exception` |
| Config classes | `*Config` or `*Configuration` |

## 5. Security Tests

### Scope
- JWT validation (401 without token)
- Tenant isolation (cannot access other tenant's data)
- Header sanitization (spoofed headers stripped)
- RBAC enforcement (role-based endpoint access)
- Actuator protection (ADMIN only)

### Test Types

| Test | Verifies |
|------|----------|
| TenantSecurityTest | Tenant header stripping, JWT extraction, cross-tenant denial |
| ActuatorSecurityTest | /actuator/** requires ADMIN, health/info public |
| RateLimitTest | Rate limiting enforcement per endpoint type |
| CorsTest | CORS headers present, origins enforced |

### Example
```java
@Test
void requestWithSpoofedTenantHeaderShouldBeStripped() {
    webTestClient.get().uri("/api/v1/devices")
        .header("X-Tenant-Id", "spoofed-tenant")
        // No JWT = unauthenticated
        .exchange()
        .expectStatus().isUnauthorized();
}
```

## 6. Contract Tests

### Purpose
Ensure API compatibility between services (producer/consumer contracts).

### Approach
- **Spring Cloud Contract** (Phase 2)
- Producer defines contract (REST endpoint schema)
- Consumer verifies against stubs

### Priority Contracts
| Producer | Consumer | API |
|----------|----------|-----|
| Device Service | Gateway | GET /api/v1/devices |
| Alerting Service | Gateway | GET /api/v1/alerts |
| Ingestion Service | Analysis Service | Kafka: telemetry-events |
| Scoring Service | Alerting Service | Kafka: scoring-events |

## 7. Performance Smoke Tests

### Purpose
Detect obvious performance regressions in CI. Not a substitute for load testing.

### Approach
- JUnit 5 tests (run with unit tests via Surefire)
- Mocked external dependencies
- Assert throughput/latency bounds
- No external infrastructure needed

### Benchmarks

| Service | Test | Threshold |
|---------|------|-----------|
| Ingestion | 1000 messages processed | < 5s total, < 10ms avg |
| Risk Scoring | 500 calculations | < 3s total, < 5ms avg |
| Signal Analysis | 200 windows analyzed | < 4s total, < 20ms avg |

### Example
```java
@Test
void shouldProcess1000MessagesWithinPerformanceBudget() {
    var start = System.nanoTime();
    for (int i = 0; i < 1000; i++) {
        useCase.ingest(buildReading(i));
    }
    var elapsed = Duration.ofNanos(System.nanoTime() - start);
    assertThat(elapsed).isLessThan(Duration.ofSeconds(5));
}
```

## 8. Maven Plugin Configuration

### Surefire (Unit + Arch + Performance Tests)
```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>@{argLine} --add-opens java.base/java.lang=ALL-UNNAMED</argLine>
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
        <argLine>@{argLine} --add-opens java.base/java.lang=ALL-UNNAMED</argLine>
        <includes>
            <include>**/*IT.java</include>
            <include>**/*IntegrationTest.java</include>
        </includes>
    </configuration>
</plugin>
```

### JaCoCo (Coverage)
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>**/config/**</exclude>
            <exclude>**/*Application.*</exclude>
            <exclude>**/adapter/in/rest/dto/**</exclude>
        </excludes>
    </configuration>
</plugin>
```

### Spotless (Code Formatting)
```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <configuration>
        <java>
            <importOrder/>
            <removeUnusedImports/>
            <palantirJavaFormat>
                <version>2.47.0</version>
            </palantirJavaFormat>
        </java>
    </configuration>
</plugin>
```

## 9. CI Integration (GitHub Actions)

### Pipeline Jobs

| Job | Trigger | Scope |
|-----|---------|-------|
| code-quality | All pushes/PRs | Spotless check |
| build-and-test | All pushes/PRs | Compile + unit tests |
| integration-tests | After unit tests | Testcontainers (Docker required) |
| coverage | After unit tests | JaCoCo report + enforcement |
| security-scan | After unit tests | OWASP dependency-check |
| docker-build | Main branch only | Verify images build |

### Concurrency
Cancel in-progress runs for same branch to save resources.

### Artifacts Preserved
- Test results (JUnit XML)
- JaCoCo coverage reports
- OWASP vulnerability reports

## 10. Running Tests

```bash
# Unit tests only (fast, no Docker needed)
mvn test

# Unit + integration tests (needs Docker for Testcontainers)
mvn verify -Ptest

# Specific service
mvn test -pl pyrosense-alerting-service

# With coverage report
mvn verify -Djacoco.skip=false
open target/site/jacoco/index.html

# Code formatting check
mvn spotless:check

# Fix formatting
mvn spotless:apply

# OWASP security scan
mvn dependency-check:check -Powasp

# Run architecture tests only
mvn test -pl pyrosense-shared-kernel -Dtest="*ArchitectureTest*"
```

## 11. Test Data Conventions

### Builders / Fixtures
Each service should have test fixtures in `src/test/java/.../support/`:
- `TestDataBuilder.java` - Creates domain objects with sensible defaults
- `TestFixtures.java` - Common test constants (tenant IDs, device IDs)

### Naming
- Test methods: `shouldDoSomethingWhenCondition()` or `verbWhenConditionExpectsOutcome()`
- Test classes: `{ClassUnderTest}Test.java` for units, `{Adapter}IT.java` for integration
- Nested classes: `@Nested class WhenCondition { ... }` for grouping

### Assertions
- Always use AssertJ (no Hamcrest, no raw JUnit assertions)
- Custom assertions for domain objects when patterns repeat
- Soft assertions for multi-field verification

## 12. Rules

1. **Never disable tests to make the build pass.** Fix the test or fix the code.
2. **Never ignore errors.** Investigate and resolve properly.
3. **Tests are production code.** Same quality standards apply.
4. **No `@Disabled` without a tracking issue.** Every disabled test needs a TODO with ticket reference.
5. **Test behavior, not implementation.** Tests should survive refactoring.
6. **One assertion concept per test.** Multiple asserts are fine if they verify one logical concept.
7. **Tests must be deterministic.** No flaky tests. No `Thread.sleep()` in assertions.
8. **Fast feedback.** Unit tests < 100ms each. Integration tests < 30s each.
