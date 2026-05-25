package com.pyrosense.simulator.domain;

import com.pyrosense.simulator.config.SimulatorConfig;
import com.pyrosense.simulator.publisher.TelemetryPublisher;
import com.pyrosense.simulator.scenario.ScenarioEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulationEngine {

    private static final Logger log = LoggerFactory.getLogger(SimulationEngine.class);

    private final SimulatorConfig config;
    private final List<TelemetryPublisher> publishers;
    private final ScenarioEngine scenarioEngine;
    private final List<SimulatedTenant> tenants;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger totalPublished = new AtomicInteger(0);
    private ScheduledExecutorService scheduler;
    private ScenarioType activeScenario = ScenarioType.NORMAL;
    private final Map<String, ScenarioType> deviceScenarios = new ConcurrentHashMap<>();

    public SimulationEngine(SimulatorConfig config, List<TelemetryPublisher> publishers) {
        this.config = config;
        this.publishers = publishers;
        this.scenarioEngine = new ScenarioEngine();
        this.tenants = buildTenants();
    }

    private List<SimulatedTenant> buildTenants() {
        List<SimulatedTenant> result = new ArrayList<>();
        for (int i = 0; i < config.tenantCount(); i++) {
            result.add(SimulatedTenant.create(
                    "tenant-" + (i + 1),
                    config.buildingsPerTenant(),
                    config.devicesPerBuilding()));
        }
        return result;
    }

    public void start() {
        if (running.getAndSet(true)) {
            log.warn("Simulation already running");
            return;
        }

        publishers.forEach(TelemetryPublisher::connect);
        scheduler = Executors.newScheduledThreadPool(2);

        long intervalMs = config.publishInterval().toMillis() / config.timeAccelerationFactor();
        long totalDurationMs = Duration.ofDays(config.simulationDurationDays()).toMillis() / config.timeAccelerationFactor();

        AtomicInteger tick = new AtomicInteger(0);
        Instant simulationStart = Instant.now();

        scheduler.scheduleAtFixedRate(() -> {
            if (!running.get()) return;

            int currentTick = tick.getAndIncrement();
            Instant simulatedTime = simulationStart.plus(
                    Duration.ofMillis((long) currentTick * config.publishInterval().toMillis()));

            for (SimulatedTenant tenant : tenants) {
                for (SimulatedDevice device : tenant.allDevices()) {
                    ScenarioType scenario = deviceScenarios.getOrDefault(device.deviceId(), activeScenario);

                    if (scenario == ScenarioType.DEVICE_OFFLINE) {
                        log.debug("Device {} is offline, skipping", device.name());
                        continue;
                    }

                    TelemetryReading reading = scenarioEngine.generate(device, scenario, currentTick, simulatedTime);
                    if (reading != null) {
                        for (TelemetryPublisher publisher : publishers) {
                            publisher.publish(reading);
                        }
                        totalPublished.incrementAndGet();
                    }
                }
            }

            if (currentTick % 10 == 0) {
                log.info("Tick {} | Published: {} | Scenario: {} | Simulated time: {}",
                        currentTick, totalPublished.get(), activeScenario, simulatedTime);
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS);

        if (config.simulationDurationDays() > 0) {
            scheduler.schedule(this::stop, totalDurationMs, TimeUnit.MILLISECONDS);
        }

        int totalDevices = tenants.stream().mapToInt(t -> t.allDevices().size()).sum();
        log.info("Simulation started: {} tenants, {} devices, interval={}ms (x{} acceleration), duration={} days",
                tenants.size(), totalDevices, intervalMs, config.timeAccelerationFactor(), config.simulationDurationDays());
    }

    public void stop() {
        if (!running.getAndSet(false)) {
            log.warn("Simulation not running");
            return;
        }

        if (scheduler != null) {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        publishers.forEach(TelemetryPublisher::disconnect);
        log.info("Simulation stopped. Total published: {}", totalPublished.get());
    }

    public void setScenario(ScenarioType scenario) {
        this.activeScenario = scenario;
        log.info("Active scenario changed to: {}", scenario);
    }

    public void setDeviceScenario(String deviceId, ScenarioType scenario) {
        deviceScenarios.put(deviceId, scenario);
        log.info("Device {} scenario set to: {}", deviceId, scenario);
    }

    public boolean isRunning() {
        return running.get();
    }

    public int getTotalPublished() {
        return totalPublished.get();
    }

    public ScenarioType getActiveScenario() {
        return activeScenario;
    }

    public List<SimulatedTenant> getTenants() {
        return Collections.unmodifiableList(tenants);
    }

    public int getTotalDeviceCount() {
        return tenants.stream().mapToInt(t -> t.allDevices().size()).sum();
    }
}
