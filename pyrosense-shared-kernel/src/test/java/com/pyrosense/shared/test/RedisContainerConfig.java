package com.pyrosense.shared.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Redis container for integration tests.
 * Uses Redis 7 Alpine for a lightweight footprint.
 */
public class RedisContainerConfig {

    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    static {
        REDIS.start();
    }

    private RedisContainerConfig() {}

    /**
     * Registers Redis connection properties from the running container.
     * Call this from @DynamicPropertySource in test classes.
     */
    public static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    public static String getHost() {
        return REDIS.getHost();
    }

    public static int getPort() {
        return REDIS.getMappedPort(6379);
    }
}
