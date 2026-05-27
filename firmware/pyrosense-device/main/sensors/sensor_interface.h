#pragma once

#include <cstdint>

namespace pyrosense {

struct SensorReading {
    float current_rms_a[3];     // L1, L2, L3 in Amperes
    float voltage_rms_v;        // Volts (NaN if unavailable)
    float temperature_c[2];     // Connection points in Celsius
    float ambient_temp_c;       // Ambient temperature
    float hf_noise_level;       // 0.0 - 1.0 normalized
    uint16_t transient_count;   // Events per sampling window
    uint16_t micro_arc_count;   // Arc pulses per window
    uint32_t timestamp_ms;      // Monotonic timestamp
    bool valid;                 // Overall reading validity
};

class ISensorProvider {
public:
    virtual ~ISensorProvider() = default;

    virtual bool init() = 0;
    virtual SensorReading read() = 0;
    virtual bool self_test() = 0;
    virtual const char* provider_name() const = 0;
};

}  // namespace pyrosense
