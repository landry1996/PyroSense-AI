#pragma once

#include <cstdint>

namespace pyrosense {

class RmsCalculator {
public:
    static float compute(const float* samples, uint32_t count);
    static float average_rms(float l1, float l2, float l3);

    RmsCalculator();
    void reset();
    void add_sample(float sample);
    float result() const;
    uint32_t sample_count() const;

private:
    double sum_squares_;
    uint32_t count_;
};

}  // namespace pyrosense
