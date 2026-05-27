#include "sensors/mock_sensor_provider.h"
#include "signal_processing/feature_extractor.h"
#include "telemetry/payload_builder.h"
#include "security/hmac_signer.h"
#include "security/nonce_generator.h"
#include "storage/offline_queue.h"
#include "connectivity/mqtt_client.h"
#include "config/device_config.h"
#include <cstdio>
#include <cassert>

using namespace pyrosense;

static void test_full_pipeline_mock_to_publish() {
    // Setup
    DeviceConfig config = DeviceConfig::load_defaults();
    MockSensorProvider sensor;
    sensor.init();
    FeatureExtractor extractor;
    PayloadBuilder builder(config);
    HmacSigner signer;
    signer.init("");  // disabled
    MqttClient mqtt;
    mqtt.init(config.mqtt);
    mqtt.set_tenant_id(config.identity.tenant_id);
    mqtt.connect();

    // Execute pipeline
    auto reading = sensor.read();
    assert(reading.valid);

    auto features = extractor.extract(reading);
    assert(features.rms_current_a > 0.0f);
    assert(features.signal_quality > 0.0f);

    std::string nonce = NonceGenerator::generate();
    std::string signature = signer.sign(nonce);

    auto payload = builder.build(features, nonce, signature);
    assert(payload.valid);
    assert(!payload.json.empty());
    assert(!payload.message_id.empty());
    assert(payload.sequence_number == 1);

    std::string topic = mqtt.build_topic("telemetry");
    assert(topic.find("pyrosense/v1/") == 0);
    bool published = mqtt.publish(topic, payload.json, MqttQos::AT_LEAST_ONCE);
    assert(published);
    assert(mqtt.publish_count() == 1);

    printf("  PASS: full_pipeline (sensor → features → payload → mqtt)\n");
    printf("        topic: %s\n", topic.c_str());
    printf("        payload size: %zu bytes\n", payload.json.size());
}

static void test_pipeline_offline_then_drain() {
    DeviceConfig config = DeviceConfig::load_defaults();
    MockSensorProvider sensor;
    sensor.init();
    FeatureExtractor extractor;
    PayloadBuilder builder(config);
    OfflineQueue queue;
    queue.init(100);
    MqttClient mqtt;
    mqtt.init(config.mqtt);
    mqtt.set_tenant_id(config.identity.tenant_id);

    // Simulate offline: buffer 5 messages (isDrain=true for buffered payloads)
    for (int i = 0; i < 5; i++) {
        auto reading = sensor.read();
        auto features = extractor.extract(reading);
        auto payload = builder.build(features, "n", "s", true);
        assert(payload.json.find("\"isDrain\":true") != std::string::npos);
        queue.enqueue("topic", payload.json);
    }
    assert(queue.count() == 5);

    // Simulate reconnection: drain queue
    mqtt.connect();
    uint32_t drained = 0;
    while (!queue.is_empty()) {
        QueueEntry entry;
        queue.dequeue(entry);
        mqtt.publish(entry.topic, entry.payload, MqttQos::AT_LEAST_ONCE);
        drained++;
    }
    assert(drained == 5);
    assert(mqtt.publish_count() == 5);
    assert(queue.is_empty());

    printf("  PASS: pipeline_offline_drain (buffered=5, drained=5, isDrain=true)\n");
}

static void test_pipeline_with_arc_injection() {
    MockSensorProvider sensor;
    sensor.init();
    FeatureExtractor extractor;

    // Inject arc event
    sensor.inject_arc_event(3);
    auto reading = sensor.read();
    auto features = extractor.extract(reading);

    assert(features.micro_arc_count == 3);
    assert(features.signal_quality < 1.0f);  // Degraded due to arcs

    printf("  PASS: pipeline_arc_injection (arcs=%d, quality=%.3f)\n",
           features.micro_arc_count, features.signal_quality);
}

int main() {
    printf("=== Integration Pipeline Tests ===\n");
    test_full_pipeline_mock_to_publish();
    test_pipeline_offline_then_drain();
    test_pipeline_with_arc_injection();
    printf("=== All Integration tests passed ===\n");
    return 0;
}
