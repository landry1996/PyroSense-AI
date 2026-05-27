#pragma once

#include "sensors/sensor_interface.h"
#include <cstdint>

namespace pyrosense {

struct ExtractedFeatures {
    float rms_current_a;
    float rms_voltage_v;
    float active_power_w;
    float reactive_power_var;
    float power_factor;
    float thd_percent;
    float temperature_c;
    float hf_noise_level;
    uint16_t micro_arc_count;
    uint16_t transient_count;
    float signal_quality;
    uint32_t sampling_window_ms;
};

class FeatureExtractor {
public:
    FeatureExtractor();

    ExtractedFeatures extract(const SensorReading& reading);

private:
    float compute_power_factor(float active_power, float apparent_power);
    float compute_thd_approximate(const SensorReading& reading);
    float compute_signal_quality(const SensorReading& reading);

    uint32_t extraction_count_;
    float prev_temperature_;
};

}  // namespace pyrosense
