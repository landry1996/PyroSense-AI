#include "config/state_machine.h"
#include "diagnostics/logger.h"

namespace pyrosense {

const char* state_to_string(DeviceState state) {
    switch (state) {
        case DeviceState::BOOTING:           return "BOOTING";
        case DeviceState::PROVISIONING:      return "PROVISIONING";
        case DeviceState::CONNECTING:        return "CONNECTING";
        case DeviceState::ACTIVE:            return "ACTIVE";
        case DeviceState::OFFLINE_BUFFERING: return "OFFLINE_BUFFERING";
        case DeviceState::DEGRADED:          return "DEGRADED";
        case DeviceState::ERROR:             return "ERROR";
        case DeviceState::REVOKED:           return "REVOKED";
    }
    return "UNKNOWN";
}

const char* event_to_string(StateEvent event) {
    switch (event) {
        case StateEvent::BOOT_COMPLETE:        return "BOOT_COMPLETE";
        case StateEvent::CREDENTIALS_FOUND:    return "CREDENTIALS_FOUND";
        case StateEvent::NO_CREDENTIALS:       return "NO_CREDENTIALS";
        case StateEvent::ENROLLED:             return "ENROLLED";
        case StateEvent::CONNECTED:            return "CONNECTED";
        case StateEvent::WIFI_LOST:            return "WIFI_LOST";
        case StateEvent::MQTT_LOST:            return "MQTT_LOST";
        case StateEvent::RECONNECTED:          return "RECONNECTED";
        case StateEvent::BUFFER_CRITICAL:      return "BUFFER_CRITICAL";
        case StateEvent::SENSOR_FAIL:          return "SENSOR_FAIL";
        case StateEvent::FATAL_ERROR:          return "FATAL_ERROR";
        case StateEvent::REVOKE_RECEIVED:      return "REVOKE_RECEIVED";
        case StateEvent::RECOVERY_SUCCESS:     return "RECOVERY_SUCCESS";
        case StateEvent::PROVISIONING_TIMEOUT: return "PROVISIONING_TIMEOUT";
    }
    return "UNKNOWN";
}

StateMachine::StateMachine() : state_(DeviceState::BOOTING), callback_(nullptr) {}

void StateMachine::init(DeviceState initial_state) {
    state_ = initial_state;
    Logger::info("SM", "Initialized in state: %s", state_to_string(state_));
}

bool StateMachine::transition(StateEvent event) {
    DeviceState new_state = resolve_transition(state_, event);
    if (new_state == state_) {
        Logger::warn("SM", "Invalid transition: %s + %s → ignored",
                     state_to_string(state_), event_to_string(event));
        return false;
    }

    DeviceState old_state = state_;
    state_ = new_state;
    Logger::info("SM", "Transition: %s → %s (event: %s)",
                 state_to_string(old_state), state_to_string(new_state), event_to_string(event));

    if (callback_) {
        callback_(old_state, new_state);
    }
    return true;
}

DeviceState StateMachine::current_state() const {
    return state_;
}

void StateMachine::on_state_change(StateChangeCallback callback) {
    callback_ = callback;
}

DeviceState StateMachine::resolve_transition(DeviceState from, StateEvent event) const {
    switch (from) {
        case DeviceState::BOOTING:
            if (event == StateEvent::CREDENTIALS_FOUND) return DeviceState::CONNECTING;
            if (event == StateEvent::NO_CREDENTIALS) return DeviceState::PROVISIONING;
            if (event == StateEvent::FATAL_ERROR) return DeviceState::ERROR;
            break;

        case DeviceState::PROVISIONING:
            if (event == StateEvent::ENROLLED) return DeviceState::CONNECTING;
            if (event == StateEvent::PROVISIONING_TIMEOUT) return DeviceState::ERROR;
            break;

        case DeviceState::CONNECTING:
            if (event == StateEvent::CONNECTED) return DeviceState::ACTIVE;
            if (event == StateEvent::WIFI_LOST) return DeviceState::OFFLINE_BUFFERING;
            if (event == StateEvent::MQTT_LOST) return DeviceState::OFFLINE_BUFFERING;
            if (event == StateEvent::FATAL_ERROR) return DeviceState::ERROR;
            break;

        case DeviceState::ACTIVE:
            if (event == StateEvent::WIFI_LOST) return DeviceState::OFFLINE_BUFFERING;
            if (event == StateEvent::MQTT_LOST) return DeviceState::OFFLINE_BUFFERING;
            if (event == StateEvent::REVOKE_RECEIVED) return DeviceState::REVOKED;
            if (event == StateEvent::SENSOR_FAIL) return DeviceState::DEGRADED;
            if (event == StateEvent::FATAL_ERROR) return DeviceState::ERROR;
            break;

        case DeviceState::OFFLINE_BUFFERING:
            if (event == StateEvent::RECONNECTED) return DeviceState::ACTIVE;
            if (event == StateEvent::BUFFER_CRITICAL) return DeviceState::DEGRADED;
            if (event == StateEvent::FATAL_ERROR) return DeviceState::ERROR;
            break;

        case DeviceState::DEGRADED:
            if (event == StateEvent::RECOVERY_SUCCESS) return DeviceState::CONNECTING;
            if (event == StateEvent::FATAL_ERROR) return DeviceState::ERROR;
            break;

        case DeviceState::ERROR:
            if (event == StateEvent::RECOVERY_SUCCESS) return DeviceState::CONNECTING;
            break;

        case DeviceState::REVOKED:
            if (event == StateEvent::ENROLLED) return DeviceState::PROVISIONING;
            break;
    }
    return from;  // No valid transition
}

}  // namespace pyrosense
