#include "config/device_config.h"

namespace pyrosense {

DeviceConfig DeviceConfig::load_defaults() {
    DeviceConfig cfg;

    cfg.identity.device_id = "pyro-dev-001";
    cfg.identity.tenant_id = "tenant-lab-01";
    cfg.identity.firmware_version = "0.1.0";

    cfg.mqtt.broker_uri = "mqtt://localhost";
    cfg.mqtt.port = 1883;
    cfg.mqtt.client_id = "pyro-dev-001";
    cfg.mqtt.username = "";
    cfg.mqtt.password = "";
    cfg.mqtt.use_tls = false;
    cfg.mqtt.keepalive_s = 60;

    cfg.sensors.sampling_rate_hz = 860;
    cfg.sensors.sampling_window_ms = 1000;
    cfg.sensors.rated_current_a = 16.0f;
    cfg.sensors.arc_threshold = 0.3f;
    cfg.sensors.temp_rate_threshold_c_per_min = 2.0f;
    cfg.sensors.overload_ratio = 1.2f;

    cfg.telemetry.feature_interval_ms = 5000;
    cfg.telemetry.heartbeat_interval_ms = 60000;
    cfg.telemetry.health_interval_ms = 300000;
    cfg.telemetry.schema_version = "1.0";

    cfg.wifi.ssid = "";  // Must be provisioned via NVS or serial config
    cfg.wifi.password = "";  // Never hardcoded

    cfg.security.hmac_key = "";
    cfg.security.hmac_enabled = false;

    return cfg;
}

DeviceConfig DeviceConfig::load_from_nvs() {
    // TODO: Implement NVS loading when running on ESP32
    // For now, return defaults
    return load_defaults();
}

void DeviceConfig::save_to_nvs() const {
    // TODO: Implement NVS persistence when running on ESP32
}

}  // namespace pyrosense
