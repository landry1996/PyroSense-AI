#include "signal_processing/signal_quality.h"
#include <cstdio>
#include <cassert>
#include <cmath>

using namespace pyrosense;

static void test_quality_perfect_reading() {
    SensorReading reading = {};
    reading.valid = true;
    reading.current_rms_a[0] = 14.5f;
    reading.current_rms_a[1] = 14.3f;
    reading.current_rms_a[2] = 14.6f;
    reading.voltage_rms_v = 230.0f;
    reading.temperature_c[0] = 38.0f;
    reading.temperature_c[1] = 36.0f;
    reading.ambient_temp_c = 22.0f;
    reading.hf_noise_level = 0.1f;
    reading.transient_count = 0;
    reading.micro_arc_count = 0;

    float score = SignalQuality::compute(reading);
    assert(score == 1.0f);
    printf("  PASS: quality_perfect (score=%.3f)\n", score);
}

static void test_quality_invalid_reading() {
    SensorReading reading = {};
    reading.valid = false;
    reading.current_rms_a[0] = 14.5f;
    reading.hf_noise_level = 0.1f;
    reading.temperature_c[0] = 38.0f;

    float score = SignalQuality::compute(reading);
    assert(score < 1.0f);
    assert(fabsf(score - 0.70f) < 0.01f);
    printf("  PASS: quality_invalid (score=%.3f)\n", score);
}

static void test_quality_high_noise() {
    SensorReading reading = {};
    reading.valid = true;
    reading.current_rms_a[0] = 14.5f;
    reading.hf_noise_level = 0.8f;
    reading.temperature_c[0] = 38.0f;

    float score = SignalQuality::compute(reading);
    assert(score < 1.0f);
    printf("  PASS: quality_high_noise (score=%.3f)\n", score);
}

static void test_quality_zero_current() {
    SensorReading reading = {};
    reading.valid = true;
    reading.current_rms_a[0] = 0.0f;
    reading.current_rms_a[1] = 0.0f;
    reading.current_rms_a[2] = 0.0f;
    reading.hf_noise_level = 0.1f;
    reading.temperature_c[0] = 38.0f;

    float score = SignalQuality::compute(reading);
    assert(score < 1.0f);
    printf("  PASS: quality_zero_current (score=%.3f)\n", score);
}

static void test_quality_extreme_temp() {
    SensorReading reading = {};
    reading.valid = true;
    reading.current_rms_a[0] = 14.5f;
    reading.hf_noise_level = 0.1f;
    reading.temperature_c[0] = 85.0f;  // Too hot

    float score = SignalQuality::compute(reading);
    assert(score < 1.0f);
    printf("  PASS: quality_extreme_temp (score=%.3f)\n", score);
}

static void test_quality_all_degraded() {
    SensorReading reading = {};
    reading.valid = false;
    reading.current_rms_a[0] = 0.0f;
    reading.current_rms_a[1] = 0.0f;
    reading.current_rms_a[2] = 0.0f;
    reading.hf_noise_level = 0.9f;
    reading.temperature_c[0] = 80.0f;
    reading.micro_arc_count = 10;

    float score = SignalQuality::compute(reading);
    assert(score < 0.3f);
    printf("  PASS: quality_all_degraded (score=%.3f)\n", score);
}

int main() {
    printf("=== Signal Quality Tests ===\n");
    test_quality_perfect_reading();
    test_quality_invalid_reading();
    test_quality_high_noise();
    test_quality_zero_current();
    test_quality_extreme_temp();
    test_quality_all_degraded();
    printf("=== All Signal Quality tests passed ===\n");
    return 0;
}
