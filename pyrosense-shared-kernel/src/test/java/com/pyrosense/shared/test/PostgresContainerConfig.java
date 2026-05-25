package com.pyrosense.shared.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared PostgreSQL (TimescaleDB) container for integration tests.
 * Uses a singleton pattern so only one container is started per JVM.
 */
public class PostgresContainerConfig {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("timescale/timescaledb:latest-pg16")
                    .withDatabaseName("pyrosense_test")
                    .withUsername("test")
                    .withPassword("test");

    static {
        POSTGRES.start();
    }

    private PostgresContainerConfig() {}

    /**
     * Registers datasource properties from the running container.
     * Call this from @DynamicPropertySource in test classes.
     */
    public static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
    }
}
