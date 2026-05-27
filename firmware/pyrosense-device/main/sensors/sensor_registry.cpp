#include "sensors/sensor_registry.h"
#include "diagnostics/logger.h"

namespace pyrosense {

SensorRegistry& SensorRegistry::instance() {
    static SensorRegistry registry;
    return registry;
}

void SensorRegistry::register_provider(std::unique_ptr<ISensorProvider> provider) {
    provider_ = std::move(provider);
    Logger::info("SENS", "Registered provider: %s", provider_->provider_name());
}

ISensorProvider* SensorRegistry::provider() {
    return provider_.get();
}

bool SensorRegistry::has_provider() const {
    return provider_ != nullptr;
}

}  // namespace pyrosense
