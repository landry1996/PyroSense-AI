#include "signal_processing/rms_calculator.h"
#include <cstdio>
#include <cmath>
#include <cassert>

using namespace pyrosense;

static void test_rms_zero_samples() {
    float result = RmsCalculator::compute(nullptr, 0);
    assert(result == 0.0f);
    printf("  PASS: rms_zero_samples\n");
}

static void test_rms_dc_signal() {
    float samples[] = {5.0f, 5.0f, 5.0f, 5.0f};
    float result = RmsCalculator::compute(samples, 4);
    assert(fabsf(result - 5.0f) < 0.001f);
    printf("  PASS: rms_dc_signal (expected=5.0, got=%.4f)\n", result);
}

static void test_rms_sine_wave() {
    // RMS of sin(x) over full period = 1/sqrt(2) ≈ 0.7071
    const int N = 1000;
    float samples[N];
    for (int i = 0; i < N; i++) {
        samples[i] = sinf(2.0f * M_PI * i / N);
    }
    float result = RmsCalculator::compute(samples, N);
    float expected = 1.0f / sqrtf(2.0f);
    assert(fabsf(result - expected) < 0.01f);
    printf("  PASS: rms_sine_wave (expected=%.4f, got=%.4f)\n", expected, result);
}

static void test_rms_known_value() {
    // RMS of {3, -3, 3, -3} = 3
    float samples[] = {3.0f, -3.0f, 3.0f, -3.0f};
    float result = RmsCalculator::compute(samples, 4);
    assert(fabsf(result - 3.0f) < 0.001f);
    printf("  PASS: rms_known_value (expected=3.0, got=%.4f)\n", result);
}

static void test_rms_incremental() {
    RmsCalculator calc;
    calc.add_sample(3.0f);
    calc.add_sample(-3.0f);
    calc.add_sample(3.0f);
    calc.add_sample(-3.0f);
    float result = calc.result();
    assert(fabsf(result - 3.0f) < 0.001f);
    assert(calc.sample_count() == 4);
    printf("  PASS: rms_incremental (expected=3.0, got=%.4f)\n", result);
}

static void test_rms_average() {
    float avg = RmsCalculator::average_rms(10.0f, 11.0f, 12.0f);
    assert(fabsf(avg - 11.0f) < 0.001f);
    printf("  PASS: rms_average (expected=11.0, got=%.4f)\n", avg);
}

int main() {
    printf("=== RMS Calculator Tests ===\n");
    test_rms_zero_samples();
    test_rms_dc_signal();
    test_rms_sine_wave();
    test_rms_known_value();
    test_rms_incremental();
    test_rms_average();
    printf("=== All RMS tests passed ===\n");
    return 0;
}
