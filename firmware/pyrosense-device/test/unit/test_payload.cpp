#include "telemetry/payload_builder.h"
#include "config/device_config.h"
#include <cstdio>
#include <cassert>
#include <cstring>

using namespace pyrosense;

static void test_payload_valid() {
    DeviceConfig config = DeviceConfig::load_defaults();
    PayloadBuilder builder(config);

    ExtractedFeatures features = {};
    features.rms_current_a = 14.5f;
    features.rms_voltage_v = 230.0f;
    features.active_power_w = 3200.0f;
    features.reactive_power_var = 300.0f;
    features.power_factor = 0.95f;
    features.thd_percent = 4.2f;
    features.temperature_c = 39.0f;
    features.hf_noise_level = 0.1f;
    features.micro_arc_count = 0;
    features.transient_count = 1;
    features.signal_quality = 0.97f;
    features.sampling_window_ms = 1000;

    auto payload = builder.build(features, "test_nonce_123", "test_signature_abc");

    assert(payload.valid);
    assert(payload.sequence_number == 1);
    assert(!payload.message_id.empty());
    assert(!payload.json.empty());
    assert(payload.json.find("\"schemaVersion\":\"1.0\"") != std::string::npos);
    assert(payload.json.find("\"messageId\":\"") != std::string::npos);
    assert(payload.json.find("\"deviceId\":\"pyro-dev-001\"") != std::string::npos);
    assert(payload.json.find("\"isDrain\":false") != std::string::npos);
    assert(payload.json.find("\"rmsCurrent\":14.50") != std::string::npos);
    assert(payload.json.find("\"powerFactor\":0.950") != std::string::npos);
    assert(payload.json.find("\"nonce\":\"test_nonce_123\"") != std::string::npos);
    printf("  PASS: payload_valid (size=%zu)\n", payload.json.size());
}

static void test_payload_drain_mode() {
    DeviceConfig config = DeviceConfig::load_defaults();
    PayloadBuilder builder(config);

    ExtractedFeatures features = {};
    features.rms_current_a = 10.0f;
    features.rms_voltage_v = 230.0f;

    auto payload = builder.build(features, "n", "s", true);

    assert(payload.valid);
    assert(payload.json.find("\"isDrain\":true") != std::string::npos);
    printf("  PASS: payload_drain_mode\n");
}

static void test_payload_sequence_increment() {
    DeviceConfig config = DeviceConfig::load_defaults();
    PayloadBuilder builder(config);

    ExtractedFeatures features = {};
    features.rms_current_a = 10.0f;
    features.rms_voltage_v = 230.0f;

    auto p1 = builder.build(features, "n1", "s1");
    auto p2 = builder.build(features, "n2", "s2");
    auto p3 = builder.build(features, "n3", "s3");

    assert(p1.sequence_number == 1);
    assert(p2.sequence_number == 2);
    assert(p3.sequence_number == 3);
    printf("  PASS: payload_sequence_increment\n");
}

static void test_payload_contains_tenant() {
    DeviceConfig config = DeviceConfig::load_defaults();
    PayloadBuilder builder(config);

    ExtractedFeatures features = {};
    auto payload = builder.build(features, "n", "s");

    assert(payload.json.find("\"tenantId\":\"tenant-lab-01\"") != std::string::npos);
    printf("  PASS: payload_contains_tenant\n");
}

static void test_payload_contains_timestamp() {
    DeviceConfig config = DeviceConfig::load_defaults();
    PayloadBuilder builder(config);

    ExtractedFeatures features = {};
    auto payload = builder.build(features, "n", "s");

    assert(payload.json.find("\"timestamp\":\"") != std::string::npos);
    printf("  PASS: payload_contains_timestamp\n");
}

static void test_payload_contains_firmware_version() {
    DeviceConfig config = DeviceConfig::load_defaults();
    PayloadBuilder builder(config);

    ExtractedFeatures features = {};
    auto payload = builder.build(features, "n", "s");

    assert(payload.json.find("\"firmwareVersion\":\"0.1.0\"") != std::string::npos);
    printf("  PASS: payload_contains_firmware_version\n");
}

static void test_payload_message_id_unique() {
    DeviceConfig config = DeviceConfig::load_defaults();
    PayloadBuilder builder(config);

    ExtractedFeatures features = {};
    features.rms_current_a = 10.0f;
    features.rms_voltage_v = 230.0f;

    auto p1 = builder.build(features, "n1", "s1");
    auto p2 = builder.build(features, "n2", "s2");

    assert(!p1.message_id.empty());
    assert(!p2.message_id.empty());
    assert(p1.message_id != p2.message_id);
    printf("  PASS: payload_message_id_unique\n");
}

int main() {
    printf("=== Payload Builder Tests ===\n");
    test_payload_valid();
    test_payload_drain_mode();
    test_payload_message_id_unique();
    test_payload_sequence_increment();
    test_payload_contains_tenant();
    test_payload_contains_timestamp();
    test_payload_contains_firmware_version();
    printf("=== All Payload Builder tests passed ===\n");
    return 0;
}
