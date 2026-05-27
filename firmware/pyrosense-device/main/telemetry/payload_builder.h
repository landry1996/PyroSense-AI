#pragma once

#include "signal_processing/feature_extractor.h"
#include "config/device_config.h"
#include <string>
#include <cstdint>

namespace pyrosense {

struct TelemetryPayload {
    std::string json;
    std::string message_id;
    uint32_t sequence_number;
    uint32_t timestamp_unix;
    bool valid;
};

class PayloadBuilder {
public:
    explicit PayloadBuilder(const DeviceConfig& config);

    TelemetryPayload build(const ExtractedFeatures& features,
                           const std::string& nonce,
                           const std::string& signature,
                           bool is_drain = false);

    uint32_t last_sequence() const;

private:
    const DeviceConfig& config_;
    uint32_t sequence_counter_;

    std::string get_timestamp_iso8601();
};

}  // namespace pyrosense
