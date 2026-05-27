#pragma once

#include <cstdint>
#include <string>

namespace pyrosense {

struct MqttConfig {
    std::string broker_uri;
    uint16_t port;
    std::string client_id;
    std::string username;
    std::string password;
    bool use_tls;
    uint16_t keepalive_s;
};

struct SensorConfig {
    uint16_t sampling_rate_hz;
    uint16_t sampling_window_ms;
    float rated_current_a;
    float arc_threshold;
    float temp_rate_threshold_c_per_min;
    float overload_ratio;
};

struct TelemetryConfig {
    uint16_t feature_interval_ms;
    uint16_t heartbeat_interval_ms;
    uint16_t health_interval_ms;
    std::string schema_version;
};

struct SecurityConfig {
    std::string hmac_key;
    bool hmac_enabled;
};

struct DeviceIdentity {
    std::string device_id;
    std::string tenant_id;
    std::string firmware_version;
};

struct WifiConfig {
    std::string ssid;
    std::string password;
};

struct DeviceConfig {
    DeviceIdentity identity;
    MqttConfig mqtt;
    WifiConfig wifi;
    SensorConfig sensors;
    TelemetryConfig telemetry;
    SecurityConfig security;

    static DeviceConfig load_defaults();
    static DeviceConfig load_from_nvs();
    void save_to_nvs() const;
};

}  // namespace pyrosense
