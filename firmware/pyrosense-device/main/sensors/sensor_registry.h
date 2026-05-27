#pragma once

#include "sensors/sensor_interface.h"
#include <memory>

namespace pyrosense {

class SensorRegistry {
public:
    static SensorRegistry& instance();

    void register_provider(std::unique_ptr<ISensorProvider> provider);
    ISensorProvider* provider();
    bool has_provider() const;

private:
    SensorRegistry() = default;
    std::unique_ptr<ISensorProvider> provider_;
};

}  // namespace pyrosense
