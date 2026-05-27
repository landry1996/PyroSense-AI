#include "signal_processing/rms_calculator.h"
#include <cmath>

namespace pyrosense {

float RmsCalculator::compute(const float* samples, uint32_t count) {
    if (count == 0) return 0.0f;
    double sum_sq = 0.0;
    for (uint32_t i = 0; i < count; i++) {
        sum_sq += static_cast<double>(samples[i]) * samples[i];
    }
    return static_cast<float>(sqrt(sum_sq / count));
}

float RmsCalculator::average_rms(float l1, float l2, float l3) {
    return (l1 + l2 + l3) / 3.0f;
}

RmsCalculator::RmsCalculator() : sum_squares_(0.0), count_(0) {}

void RmsCalculator::reset() {
    sum_squares_ = 0.0;
    count_ = 0;
}

void RmsCalculator::add_sample(float sample) {
    sum_squares_ += static_cast<double>(sample) * sample;
    count_++;
}

float RmsCalculator::result() const {
    if (count_ == 0) return 0.0f;
    return static_cast<float>(sqrt(sum_squares_ / count_));
}

uint32_t RmsCalculator::sample_count() const {
    return count_;
}

}  // namespace pyrosense
