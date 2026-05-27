#pragma once

#include "config/device_config.h"
#include "config/state_machine.h"
#include <string>
#include <cstdint>

namespace pyrosense {

struct HeartbeatPayload {
    std::string json;
    uint32_t sequence_number;
    bool valid;
};

class HeartbeatBuilder {
public:
    explicit HeartbeatBuilder(const DeviceConfig& config);

    HeartbeatPayload build(DeviceState state,
                           uint32_t uptime_s,
                           uint32_t free_heap,
                           int8_t wifi_rssi,
                           uint8_t buffer_percent,
                           float signal_quality);

private:
    const DeviceConfig& config_;
    uint32_t sequence_counter_;
};

}  // namespace pyrosense
