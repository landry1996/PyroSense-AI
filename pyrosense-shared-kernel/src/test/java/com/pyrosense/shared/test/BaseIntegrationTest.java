package com.pyrosense.shared.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Abstract base class for integration tests.
 * Each test class declares the containers it needs via composition.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
    // No containers here - each test class declares what it needs
}
