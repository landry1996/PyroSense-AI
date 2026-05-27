#include "config/device_config.h"
#include "config/state_machine.h"
#include "sensors/sensor_registry.h"
#include "sensors/mock_sensor_provider.h"
#include "signal_processing/feature_extractor.h"
#include "telemetry/payload_builder.h"
#include "telemetry/heartbeat_builder.h"
#include "connectivity/mqtt_client.h"
#include "connectivity/wifi_manager.h"
#include "security/hmac_signer.h"
#include "security/nonce_generator.h"
#include "storage/offline_queue.h"
#include "diagnostics/self_test.h"
#include "diagnostics/logger.h"
#include "utils/time_utils.h"

// On real ESP32, this would be:
// extern "C" void app_main(void)
// For host compilation, we use standard main()

namespace pyrosense {

class Application {
public:
    int run() {
        Logger::set_level(LogLevel::DEBUG);
        Logger::info("MAIN", "PyroSense Device Firmware v%s", "0.1.0");
        Logger::info("MAIN", "Starting boot sequence...");

        // Phase 1: Load configuration
        config_ = DeviceConfig::load_defaults();
        Logger::info("MAIN", "Config loaded: device=%s tenant=%s",
                     config_.identity.device_id.c_str(),
                     config_.identity.tenant_id.c_str());

        // Phase 2: Initialize state machine
        state_machine_.init(DeviceState::BOOTING);

        // Phase 3: Register sensor provider (mock for now)
        auto mock_provider = std::make_unique<MockSensorProvider>();
        if (!mock_provider->init()) {
            Logger::error("MAIN", "Sensor provider init failed");
            state_machine_.transition(StateEvent::FATAL_ERROR);
            return 1;
        }
        SensorRegistry::instance().register_provider(std::move(mock_provider));

        // Phase 4: Self-test
        auto test_result = SelfTest::run();
        if (!test_result.all_pass) {
            Logger::warn("MAIN", "Self-test had failures, continuing in degraded mode");
        }

        // Phase 5: Initialize security
        hmac_signer_.init(config_.security.hmac_key);

        // Phase 6: Initialize storage
        offline_queue_.init(1000);

        // Phase 7: Connect (credentials loaded from NVS, never hardcoded)
        state_machine_.transition(StateEvent::CREDENTIALS_FOUND);
        wifi_manager_.init(config_.wifi.ssid, config_.wifi.password);

        if (wifi_manager_.connect()) {
            mqtt_client_.init(config_.mqtt);
            if (mqtt_client_.connect()) {
                state_machine_.transition(StateEvent::CONNECTED);
            } else {
                state_machine_.transition(StateEvent::MQTT_LOST);
            }
        } else {
            state_machine_.transition(StateEvent::WIFI_LOST);
        }

        // Phase 8: Main loop
        Logger::info("MAIN", "Entering main loop (state=%s)",
                     state_to_string(state_machine_.current_state()));

        FeatureExtractor extractor;
        PayloadBuilder payload_builder(config_);
        HeartbeatBuilder heartbeat_builder(config_);

        uint32_t loop_count = 0;
        uint32_t last_heartbeat = 0;

        while (loop_count < 20) {  // Limited iterations for skeleton demo
            loop_count++;

            // Read sensors
            auto reading = SensorRegistry::instance().provider()->read();

            // Extract features
            auto features = extractor.extract(reading);

            // Build and publish telemetry
            if (state_machine_.current_state() == DeviceState::ACTIVE) {
                std::string nonce = NonceGenerator::generate();
                std::string signature = hmac_signer_.sign(nonce);

                auto payload = payload_builder.build(features, nonce, signature);
                if (payload.valid) {
                    std::string topic = mqtt_client_.build_topic("features/periodic");
                    if (!mqtt_client_.publish(topic, payload.json, MqttQos::AT_LEAST_ONCE)) {
                        offline_queue_.enqueue(topic, payload.json, QueuePriority::NORMAL);
                    }
                }

                // Heartbeat every N iterations
                if (loop_count - last_heartbeat >= 12) {
                    auto heartbeat = heartbeat_builder.build(
                        state_machine_.current_state(),
                        loop_count * 5,  // simulated uptime
                        200000,          // simulated free heap
                        wifi_manager_.rssi(),
                        offline_queue_.usage_percent(),
                        features.signal_quality);

                    if (heartbeat.valid) {
                        std::string hb_topic = mqtt_client_.build_topic("status/heartbeat");
                        mqtt_client_.publish(hb_topic, heartbeat.json, MqttQos::AT_MOST_ONCE);
                    }
                    last_heartbeat = loop_count;
                }
            } else if (state_machine_.current_state() == DeviceState::OFFLINE_BUFFERING) {
                // Buffer locally
                std::string nonce = NonceGenerator::generate();
                std::string signature = hmac_signer_.sign(nonce);
                auto payload = payload_builder.build(features, nonce, signature);
                if (payload.valid) {
                    std::string topic = mqtt_client_.build_topic("features/periodic");
                    offline_queue_.enqueue(topic, payload.json, QueuePriority::NORMAL);
                }
            }

            // Drain offline queue when connected
            if (state_machine_.current_state() == DeviceState::ACTIVE &&
                !offline_queue_.is_empty()) {
                QueueEntry entry;
                if (offline_queue_.dequeue(entry)) {
                    mqtt_client_.publish(entry.topic, entry.payload, MqttQos::AT_LEAST_ONCE);
                }
            }

            // Simulate delay (on real ESP32: vTaskDelay)
            // In skeleton: just loop
        }

        Logger::info("MAIN", "Main loop complete. Published %u messages, queue=%u",
                     mqtt_client_.publish_count(), offline_queue_.count());
        Logger::info("MAIN", "Shutdown.");
        return 0;
    }

private:
    DeviceConfig config_;
    StateMachine state_machine_;
    WifiManager wifi_manager_;
    MqttClient mqtt_client_;
    HmacSigner hmac_signer_;
    OfflineQueue offline_queue_;
};

}  // namespace pyrosense

#ifdef PYRO_HOST_BUILD
int main() {
    pyrosense::Application app;
    return app.run();
}
#else
extern "C" void app_main(void) {
    pyrosense::Application app;
    app.run();
}
#endif
