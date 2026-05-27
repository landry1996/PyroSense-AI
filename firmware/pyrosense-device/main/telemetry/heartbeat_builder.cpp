#include "telemetry/heartbeat_builder.h"
#include "utils/time_utils.h"
#include <cstdio>

namespace pyrosense {

HeartbeatBuilder::HeartbeatBuilder(const DeviceConfig& config)
    : config_(config), sequence_counter_(0) {}

HeartbeatPayload HeartbeatBuilder::build(DeviceState state,
                                          uint32_t uptime_s,
                                          uint32_t free_heap,
                                          int8_t wifi_rssi,
                                          uint8_t buffer_percent,
                                          float signal_quality) {
    HeartbeatPayload payload;
    payload.sequence_number = ++sequence_counter_;
    payload.valid = true;

    char buf[512];
    int len = snprintf(buf, sizeof(buf),
        "{"
        "\"schemaVersion\":\"%s\","
        "\"deviceId\":\"%s\","
        "\"tenantId\":\"%s\","
        "\"timestamp\":\"%s\","
        "\"sequenceNumber\":%u,"
        "\"firmwareVersion\":\"%s\","
        "\"state\":\"%s\","
        "\"uptimeSeconds\":%u,"
        "\"freeHeapBytes\":%u,"
        "\"wifiRssiDbm\":%d,"
        "\"bufferUsagePercent\":%u,"
        "\"signalQuality\":%.3f"
        "}",
        config_.telemetry.schema_version.c_str(),
        config_.identity.device_id.c_str(),
        config_.identity.tenant_id.c_str(),
        TimeUtils::iso8601_now().c_str(),
        payload.sequence_number,
        config_.identity.firmware_version.c_str(),
        state_to_string(state),
        uptime_s,
        free_heap,
        static_cast<int>(wifi_rssi),
        static_cast<unsigned>(buffer_percent),
        signal_quality
    );

    if (len > 0 && len < static_cast<int>(sizeof(buf))) {
        payload.json = std::string(buf, len);
    } else {
        payload.valid = false;
    }

    return payload;
}

}  // namespace pyrosense
