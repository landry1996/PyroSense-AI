#include "signal_processing/feature_extractor.h"
#include "signal_processing/rms_calculator.h"
#include "signal_processing/signal_quality.h"
#include <cmath>

namespace pyrosense {

FeatureExtractor::FeatureExtractor()
    : extraction_count_(0), prev_temperature_(NAN) {}

ExtractedFeatures FeatureExtractor::extract(const SensorReading& reading) {
    extraction_count_++;

    ExtractedFeatures features = {};
    features.sampling_window_ms = 1000;

    // RMS current (average of 3 phases for single-value output)
    features.rms_current_a = RmsCalculator::average_rms(
        reading.current_rms_a[0],
        reading.current_rms_a[1],
        reading.current_rms_a[2]);

    // Voltage
    features.rms_voltage_v = reading.voltage_rms_v;

    // Power calculations
    float apparent_power = features.rms_current_a * features.rms_voltage_v;
    features.power_factor = compute_power_factor(apparent_power * 0.94f, apparent_power);
    features.active_power_w = apparent_power * features.power_factor;
    features.reactive_power_var = apparent_power * sinf(acosf(features.power_factor));

    // THD approximation (placeholder — real FFT when ADC raw data available)
    features.thd_percent = compute_thd_approximate(reading);

    // Temperature (use first sensor)
    features.temperature_c = reading.temperature_c[0];
    prev_temperature_ = features.temperature_c;

    // HF and arc detection (pass-through from sensor)
    features.hf_noise_level = reading.hf_noise_level;
    features.micro_arc_count = reading.micro_arc_count;
    features.transient_count = reading.transient_count;

    // Signal quality (composite score)
    features.signal_quality = compute_signal_quality(reading);

    return features;
}

float FeatureExtractor::compute_power_factor(float active_power, float apparent_power) {
    if (apparent_power < 0.001f) return 1.0f;
    float pf = active_power / apparent_power;
    if (pf > 1.0f) pf = 1.0f;
    if (pf < 0.0f) pf = 0.0f;
    return pf;
}

float FeatureExtractor::compute_thd_approximate(const SensorReading& reading) {
    // Placeholder: estimate THD from noise level and transient activity
    // Real implementation will use FFT on raw ADC samples
    float base_thd = 3.0f + reading.hf_noise_level * 10.0f;
    if (reading.transient_count > 0) {
        base_thd += static_cast<float>(reading.transient_count) * 0.5f;
    }
    return base_thd;
}

float FeatureExtractor::compute_signal_quality(const SensorReading& reading) {
    return SignalQuality::compute(reading);
}

}  // namespace pyrosense
