#pragma once

#include <cstdint>
#include <functional>

namespace pyrosense {

enum class DeviceState : uint8_t {
    BOOTING = 0,
    PROVISIONING = 1,
    CONNECTING = 2,
    ACTIVE = 3,
    OFFLINE_BUFFERING = 4,
    DEGRADED = 5,
    ERROR = 6,
    REVOKED = 7,
};

enum class StateEvent : uint8_t {
    BOOT_COMPLETE,
    CREDENTIALS_FOUND,
    NO_CREDENTIALS,
    ENROLLED,
    CONNECTED,
    WIFI_LOST,
    MQTT_LOST,
    RECONNECTED,
    BUFFER_CRITICAL,
    SENSOR_FAIL,
    FATAL_ERROR,
    REVOKE_RECEIVED,
    RECOVERY_SUCCESS,
    PROVISIONING_TIMEOUT,
};

const char* state_to_string(DeviceState state);
const char* event_to_string(StateEvent event);

using StateChangeCallback = std::function<void(DeviceState old_state, DeviceState new_state)>;

class StateMachine {
public:
    StateMachine();

    void init(DeviceState initial_state);
    bool transition(StateEvent event);
    DeviceState current_state() const;
    void on_state_change(StateChangeCallback callback);

private:
    DeviceState state_;
    StateChangeCallback callback_;
    DeviceState resolve_transition(DeviceState from, StateEvent event) const;
};

}  // namespace pyrosense
