#include "diagnostics/self_test.h"
#include "diagnostics/logger.h"
#include "sensors/sensor_registry.h"
#include "utils/time_utils.h"

namespace pyrosense {

SelfTestResult SelfTest::run() {
    Logger::info("TEST", "=== Self-test starting ===");
    uint32_t start = TimeUtils::monotonic_ms();

    SelfTestResult result = {};
    result.sensors_ok = test_sensors();
    result.storage_ok = test_storage();
    result.connectivity_ok = test_connectivity();
    result.security_ok = test_security();
    result.all_pass = result.sensors_ok && result.storage_ok &&
                      result.connectivity_ok && result.security_ok;
    result.duration_ms = TimeUtils::monotonic_ms() - start;

    print_report(result);
    return result;
}

void SelfTest::print_report(const SelfTestResult& result) {
    Logger::info("TEST", "=== Self-test results ===");
    Logger::info("TEST", "  Sensors:      %s", result.sensors_ok ? "PASS" : "FAIL");
    Logger::info("TEST", "  Storage:      %s", result.storage_ok ? "PASS" : "FAIL");
    Logger::info("TEST", "  Connectivity: %s", result.connectivity_ok ? "PASS" : "FAIL");
    Logger::info("TEST", "  Security:     %s", result.security_ok ? "PASS" : "FAIL");
    Logger::info("TEST", "  Overall:      %s (%u ms)",
                 result.all_pass ? "ALL PASS" : "FAILURES DETECTED",
                 result.duration_ms);
}

bool SelfTest::test_sensors() {
    auto& registry = SensorRegistry::instance();
    if (!registry.has_provider()) {
        Logger::warn("TEST", "No sensor provider registered");
        return false;
    }
    return registry.provider()->self_test();
}

bool SelfTest::test_storage() {
    // TODO: Verify SPIFFS mount, write/read cycle
    Logger::debug("TEST", "Storage: placeholder (always pass)");
    return true;
}

bool SelfTest::test_connectivity() {
    // TODO: Verify WiFi driver init, DNS resolution
    Logger::debug("TEST", "Connectivity: placeholder (always pass)");
    return true;
}

bool SelfTest::test_security() {
    // TODO: Verify NVS key access, HMAC computation
    Logger::debug("TEST", "Security: placeholder (always pass)");
    return true;
}

}  // namespace pyrosense
