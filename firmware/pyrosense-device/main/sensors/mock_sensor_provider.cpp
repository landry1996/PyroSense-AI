#include "sensors/mock_sensor_provider.h"
#include "diagnostics/logger.h"
#include <cmath>
#include <cstdlib>
#include <ctime>

namespace pyrosense {

MockSensorProvider::MockSensorProvider(const MockSensorConfig& config)
    : config_(config),
      read_count_(0),
      current_temperature_(config.base_temperature_c),
      overload_multiplier_(1.0f),
      injected_arcs_(0) {}

bool MockSensorProvider::init() {
    srand(static_cast<unsigned>(time(nullptr)));
    Logger::info("MOCK", "Mock sensor provider initialized");
    return true;
}

SensorReading MockSensorProvider::read() {
    SensorReading reading = {};
    read_count_++;

    // Current (3 phases with slight imbalance)
    float base = config_.base_current_a * overload_multiplier_;
    reading.current_rms_a[0] = add_noise(base, config_.current_noise_percent);
    reading.current_rms_a[1] = add_noise(base * 0.98f, config_.current_noise_percent);
    reading.current_rms_a[2] = add_noise(base * 1.01f, config_.current_noise_percent);

    // Voltage
    reading.voltage_rms_v = add_noise(config_.base_voltage_v, config_.voltage_noise_percent);

    // Temperature (slow drift simulation)
    float drift = config_.temp_drift_c_per_hour * (static_cast<float>(read_count_) / 3600.0f);
    current_temperature_ = config_.base_temperature_c + drift;
    reading.temperature_c[0] = add_noise(current_temperature_, 0.5f);
    reading.temperature_c[1] = add_noise(current_temperature_ - 2.0f, 0.5f);
    reading.ambient_temp_c = add_noise(22.0f, 1.0f);

    // HF noise
    reading.hf_noise_level = add_noise(config_.hf_noise_base, 20.0f);
    if (reading.hf_noise_level < 0.0f) reading.hf_noise_level = 0.0f;
    if (reading.hf_noise_level > 1.0f) reading.hf_noise_level = 1.0f;

    // Transients (random occurrence)
    float r = static_cast<float>(rand()) / RAND_MAX;
    reading.transient_count = (r < config_.transient_probability) ? (1 + rand() % 3) : 0;

    // Micro-arcs (rare random or injected)
    if (injected_arcs_ > 0) {
        reading.micro_arc_count = injected_arcs_;
        injected_arcs_ = 0;
    } else {
        r = static_cast<float>(rand()) / RAND_MAX;
        reading.micro_arc_count = (r < config_.arc_probability) ? (1 + rand() % 2) : 0;
    }

    reading.timestamp_ms = get_monotonic_ms();
    reading.valid = true;

    // Reset overload after one read
    overload_multiplier_ = 1.0f;

    return reading;
}

bool MockSensorProvider::self_test() {
    Logger::info("MOCK", "Self-test: all channels simulated OK");
    return true;
}

const char* MockSensorProvider::provider_name() const {
    return "MockSensorProvider";
}

void MockSensorProvider::inject_arc_event(uint16_t count) {
    injected_arcs_ = count;
    Logger::debug("MOCK", "Injected %d arc events", count);
}

void MockSensorProvider::inject_overload(float multiplier) {
    overload_multiplier_ = multiplier;
    Logger::debug("MOCK", "Injected overload x%.2f", multiplier);
}

void MockSensorProvider::inject_temperature_spike(float delta_c) {
    current_temperature_ += delta_c;
    Logger::debug("MOCK", "Injected temperature spike +%.1f°C", delta_c);
}

float MockSensorProvider::add_noise(float base, float noise_percent) {
    float noise_range = base * (noise_percent / 100.0f);
    float noise = (static_cast<float>(rand()) / RAND_MAX - 0.5f) * 2.0f * noise_range;
    return base + noise;
}

uint32_t MockSensorProvider::get_monotonic_ms() {
    // Placeholder: in real firmware, use esp_timer_get_time() / 1000
    static uint32_t counter = 0;
    counter += 1000;  // Simulate 1s per read
    return counter;
}

}  // namespace pyrosense
