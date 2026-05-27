#include "signal_processing/signal_quality.h"
#include <cmath>

namespace pyrosense {

float SignalQuality::compute(const SensorReading& reading) {
    float score = MAX_SCORE;

    if (!reading.valid) {
        score -= PENALTY_INVALID_READING;
    }

    if (reading.hf_noise_level > 0.5f) {
        score -= PENALTY_HIGH_NOISE;
    }

    bool all_zero = (reading.current_rms_a[0] < 0.01f) &&
                    (reading.current_rms_a[1] < 0.01f) &&
                    (reading.current_rms_a[2] < 0.01f);
    if (all_zero) {
        score -= PENALTY_ZERO_CURRENT;
    }

    if (reading.temperature_c[0] > 70.0f || reading.temperature_c[0] < -10.0f) {
        score -= PENALTY_EXTREME_TEMP;
    }

    if (reading.micro_arc_count > 5) {
        score -= PENALTY_ARC_ACTIVITY;
    }

    if (score < 0.0f) score = 0.0f;
    return score;
}

}  // namespace pyrosense
