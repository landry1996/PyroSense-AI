#pragma once

#include "sensors/sensor_interface.h"
#include <cstdint>

namespace pyrosense {

struct MockSensorConfig {
    float base_current_a = 14.5f;
    float current_noise_percent = 2.0f;
    float base_voltage_v = 230.0f;
    float voltage_noise_percent = 1.0f;
    float base_temperature_c = 38.0f;
    float temp_drift_c_per_hour = 0.5f;
    float hf_noise_base = 0.05f;
    float arc_probability = 0.02f;
    float transient_probability = 0.05f;
};

class MockSensorProvider : public ISensorProvider {
public:
    explicit MockSensorProvider(const MockSensorConfig& config = MockSensorConfig());

    bool init() override;
    SensorReading read() override;
    bool self_test() override;
    const char* provider_name() const override;

    void inject_arc_event(uint16_t count);
    void inject_overload(float multiplier);
    void inject_temperature_spike(float delta_c);

private:
    MockSensorConfig config_;
    uint32_t read_count_;
    float current_temperature_;
    float overload_multiplier_;
    uint16_t injected_arcs_;

    float add_noise(float base, float noise_percent);
    uint32_t get_monotonic_ms();
};

}  // namespace pyrosense
