#include "telemetry/payload_builder.h"
#include "utils/time_utils.h"
#include "utils/uuid_utils.h"
#include <cstdio>

namespace pyrosense {

PayloadBuilder::PayloadBuilder(const DeviceConfig& config)
    : config_(config), sequence_counter_(0) {}

TelemetryPayload PayloadBuilder::build(const ExtractedFeatures& features,
                                        const std::string& nonce,
                                        const std::string& signature,
                                        bool is_drain) {
    TelemetryPayload payload;
    payload.sequence_number = ++sequence_counter_;
    payload.timestamp_unix = TimeUtils::unix_timestamp();
    payload.message_id = UuidUtils::generate_v4();
    payload.valid = true;

    std::string ts = get_timestamp_iso8601();

    char buf[1024];
    int len = snprintf(buf, sizeof(buf),
        "{"
        "\"schemaVersion\":\"%s\","
        "\"messageId\":\"%s\","
        "\"deviceId\":\"%s\","
        "\"tenantId\":\"%s\","
        "\"timestamp\":\"%s\","
        "\"sequenceNumber\":%u,"
        "\"firmwareVersion\":\"%s\","
        "\"samplingWindowMs\":%u,"
        "\"isDrain\":%s,"
        "\"features\":{"
            "\"rmsCurrent\":%.2f,"
            "\"rmsVoltage\":%.1f,"
            "\"activePower\":%.1f,"
            "\"reactivePower\":%.1f,"
            "\"powerFactor\":%.3f,"
            "\"thd\":%.1f,"
            "\"temperatureCelsius\":%.1f,"
            "\"hfNoiseLevel\":%.3f,"
            "\"microArcCount\":%u,"
            "\"transientCount\":%u,"
            "\"signalQuality\":%.3f"
        "},"
        "\"security\":{"
            "\"nonce\":\"%s\","
            "\"signature\":\"%s\""
        "}"
        "}",
        config_.telemetry.schema_version.c_str(),
        payload.message_id.c_str(),
        config_.identity.device_id.c_str(),
        config_.identity.tenant_id.c_str(),
        ts.c_str(),
        payload.sequence_number,
        config_.identity.firmware_version.c_str(),
        features.sampling_window_ms,
        is_drain ? "true" : "false",
        features.rms_current_a,
        features.rms_voltage_v,
        features.active_power_w,
        features.reactive_power_var,
        features.power_factor,
        features.thd_percent,
        features.temperature_c,
        features.hf_noise_level,
        features.micro_arc_count,
        features.transient_count,
        features.signal_quality,
        nonce.c_str(),
        signature.c_str()
    );

    if (len > 0 && len < static_cast<int>(sizeof(buf))) {
        payload.json = std::string(buf, len);
    } else {
        payload.valid = false;
    }

    return payload;
}

uint32_t PayloadBuilder::last_sequence() const {
    return sequence_counter_;
}

std::string PayloadBuilder::get_timestamp_iso8601() {
    return TimeUtils::iso8601_now();
}

}  // namespace pyrosense
