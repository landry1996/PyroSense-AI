#pragma once

#include "sensors/sensor_interface.h"

namespace pyrosense {

class SignalQuality {
public:
    static float compute(const SensorReading& reading);

private:
    static constexpr float MAX_SCORE = 1.0f;
    static constexpr float PENALTY_INVALID_READING = 0.30f;
    static constexpr float PENALTY_HIGH_NOISE = 0.15f;
    static constexpr float PENALTY_ZERO_CURRENT = 0.10f;
    static constexpr float PENALTY_EXTREME_TEMP = 0.10f;
    static constexpr float PENALTY_ARC_ACTIVITY = 0.10f;
};

}  // namespace pyrosense
