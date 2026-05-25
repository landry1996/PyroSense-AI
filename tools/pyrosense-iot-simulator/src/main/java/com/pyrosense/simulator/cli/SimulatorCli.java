package com.pyrosense.simulator.cli;

import com.pyrosense.simulator.config.SimulatorConfig;
import com.pyrosense.simulator.domain.ScenarioType;
import com.pyrosense.simulator.domain.SimulatedTenant;
import com.pyrosense.simulator.domain.SimulationEngine;
import com.pyrosense.simulator.publisher.MqttTelemetryPublisher;
import com.pyrosense.simulator.publisher.RestTelemetryPublisher;
import com.pyrosense.simulator.publisher.TelemetryPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SimulatorCli {

    private static final Logger log = LoggerFactory.getLogger(SimulatorCli.class);

    private SimulationEngine engine;
    private SimulatorConfig config;

    public static void main(String[] args) {
        new SimulatorCli().run(args);
    }

    public void run(String[] args) {
        config = parseArgs(args);
        List<TelemetryPublisher> publishers = buildPublishers(config);
        engine = new SimulationEngine(config, publishers);

        printBanner();
        printConfig();

        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            printMenu();
            System.out.print("> ");
            String input = scanner.nextLine().trim().toLowerCase();

            switch (input) {
                case "1", "start" -> startSimulation();
                case "2", "stop" -> stopSimulation();
                case "3", "scenario" -> chooseScenario(scanner);
                case "4", "status" -> printStatus();
                case "5", "devices" -> listDevices();
                case "6", "config" -> printConfig();
                case "7", "exit", "quit" -> {
                    stopSimulation();
                    exit = true;
                }
                default -> System.out.println("Unknown command: " + input);
            }
        }

        System.out.println("Goodbye!");
    }

    private void startSimulation() {
        if (engine.isRunning()) {
            System.out.println("Simulation already running.");
            return;
        }
        try {
            engine.start();
            System.out.println("Simulation started.");
        } catch (Exception e) {
            System.out.println("Failed to start: " + e.getMessage());
            log.error("Start failed", e);
        }
    }

    private void stopSimulation() {
        if (!engine.isRunning()) {
            System.out.println("Simulation not running.");
            return;
        }
        engine.stop();
        System.out.println("Simulation stopped. Total published: " + engine.getTotalPublished());
    }

    private void chooseScenario(Scanner scanner) {
        System.out.println("\nAvailable scenarios:");
        ScenarioType[] scenarios = ScenarioType.values();
        for (int i = 0; i < scenarios.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, scenarios[i]);
        }
        System.out.print("Choose scenario (number): ");
        try {
            int choice = Integer.parseInt(scanner.nextLine().trim());
            if (choice >= 1 && choice <= scenarios.length) {
                engine.setScenario(scenarios[choice - 1]);
                System.out.println("Scenario set to: " + scenarios[choice - 1]);
            } else {
                System.out.println("Invalid choice.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
        }
    }

    private void printStatus() {
        System.out.println("\n--- Simulation Status ---");
        System.out.println("Running:        " + engine.isRunning());
        System.out.println("Scenario:       " + engine.getActiveScenario());
        System.out.println("Total devices:  " + engine.getTotalDeviceCount());
        System.out.println("Total published:" + engine.getTotalPublished());
        System.out.println("Publisher:      " + (config.useMqtt() ? "MQTT" : "") + (config.useRest() ? " REST" : ""));
        System.out.println("Acceleration:   x" + config.timeAccelerationFactor());
        System.out.println("Duration:       " + config.simulationDurationDays() + " day(s)");
    }

    private void listDevices() {
        System.out.println("\n--- Devices ---");
        for (SimulatedTenant tenant : engine.getTenants()) {
            System.out.printf("Tenant: %s (%s)%n", tenant.name(), tenant.tenantId().substring(0, 8) + "...");
            for (var building : tenant.buildings()) {
                System.out.printf("  Building: %s%n", building.name());
                for (var device : building.devices()) {
                    System.out.printf("    Device: %s [%s]%n", device.name(), device.deviceId().substring(0, 8) + "...");
                }
            }
        }
    }

    private void printMenu() {
        System.out.println("\n--- PyroSense IoT Simulator ---");
        System.out.println("  1. start    - Start simulation");
        System.out.println("  2. stop     - Stop simulation");
        System.out.println("  3. scenario - Choose scenario");
        System.out.println("  4. status   - Show status");
        System.out.println("  5. devices  - List devices");
        System.out.println("  6. config   - Show configuration");
        System.out.println("  7. exit     - Exit");
    }

    private void printBanner() {
        System.out.println("""

                ╔══════════════════════════════════════════╗
                ║     PyroSense IoT Device Simulator      ║
                ║     Testing without real ESP32 hardware  ║
                ╚══════════════════════════════════════════╝
                """);
    }

    private void printConfig() {
        System.out.println("\n--- Configuration ---");
        System.out.println("MQTT:             " + config.mqttHost() + ":" + config.mqttPort() + " (enabled=" + config.useMqtt() + ")");
        System.out.println("REST:             " + config.restBaseUrl() + " (enabled=" + config.useRest() + ")");
        System.out.println("Tenants:          " + config.tenantCount());
        System.out.println("Buildings/tenant: " + config.buildingsPerTenant());
        System.out.println("Devices/building: " + config.devicesPerBuilding());
        System.out.println("Publish interval: " + config.publishInterval().toSeconds() + "s");
        System.out.println("Time acceleration:" + config.timeAccelerationFactor() + "x");
        System.out.println("Duration:         " + config.simulationDurationDays() + " day(s)");
    }

    private List<TelemetryPublisher> buildPublishers(SimulatorConfig config) {
        List<TelemetryPublisher> publishers = new ArrayList<>();
        if (config.useMqtt()) {
            publishers.add(new MqttTelemetryPublisher(config));
        }
        if (config.useRest()) {
            publishers.add(new RestTelemetryPublisher(config));
        }
        if (publishers.isEmpty()) {
            publishers.add(new MqttTelemetryPublisher(config));
        }
        return publishers;
    }

    private SimulatorConfig parseArgs(String[] args) {
        SimulatorConfig.Builder builder = SimulatorConfig.builder();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--mqtt-host" -> builder.mqttHost(args[++i]);
                case "--mqtt-port" -> builder.mqttPort(Integer.parseInt(args[++i]));
                case "--rest-url" -> builder.restBaseUrl(args[++i]);
                case "--tenants" -> builder.tenantCount(Integer.parseInt(args[++i]));
                case "--buildings" -> builder.buildingsPerTenant(Integer.parseInt(args[++i]));
                case "--devices" -> builder.devicesPerBuilding(Integer.parseInt(args[++i]));
                case "--interval" -> builder.publishInterval(Duration.ofSeconds(Long.parseLong(args[++i])));
                case "--acceleration" -> builder.timeAccelerationFactor(Integer.parseInt(args[++i]));
                case "--days" -> builder.simulationDurationDays(Integer.parseInt(args[++i]));
                case "--use-mqtt" -> builder.useMqtt(Boolean.parseBoolean(args[++i]));
                case "--use-rest" -> builder.useRest(Boolean.parseBoolean(args[++i]));
                case "--help", "-h" -> {
                    printUsage();
                    System.exit(0);
                }
            }
        }

        return builder.build();
    }

    private void printUsage() {
        System.out.println("""
                Usage: pyrosense-iot-simulator [OPTIONS]

                Options:
                  --mqtt-host HOST       MQTT broker host (default: localhost)
                  --mqtt-port PORT       MQTT broker port (default: 1883)
                  --rest-url URL         REST ingestion URL (default: http://localhost:8083)
                  --tenants N            Number of tenants (default: 2)
                  --buildings N          Buildings per tenant (default: 2)
                  --devices N            Devices per building (default: 3)
                  --interval SECONDS     Publish interval in seconds (default: 5)
                  --acceleration FACTOR  Time acceleration factor (default: 1)
                  --days N               Simulation duration in days (default: 1)
                  --use-mqtt true|false  Enable MQTT publishing (default: true)
                  --use-rest true|false  Enable REST publishing (default: false)
                  -h, --help             Show this help
                """);
    }
}
