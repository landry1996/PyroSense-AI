#include "config/state_machine.h"
#include <cstdio>
#include <cassert>

using namespace pyrosense;

static void test_initial_state() {
    StateMachine sm;
    sm.init(DeviceState::BOOTING);
    assert(sm.current_state() == DeviceState::BOOTING);
    printf("  PASS: initial_state\n");
}

static void test_boot_to_connecting() {
    StateMachine sm;
    sm.init(DeviceState::BOOTING);
    bool ok = sm.transition(StateEvent::CREDENTIALS_FOUND);
    assert(ok);
    assert(sm.current_state() == DeviceState::CONNECTING);
    printf("  PASS: boot_to_connecting\n");
}

static void test_boot_to_provisioning() {
    StateMachine sm;
    sm.init(DeviceState::BOOTING);
    bool ok = sm.transition(StateEvent::NO_CREDENTIALS);
    assert(ok);
    assert(sm.current_state() == DeviceState::PROVISIONING);
    printf("  PASS: boot_to_provisioning\n");
}

static void test_connecting_to_active() {
    StateMachine sm;
    sm.init(DeviceState::CONNECTING);
    bool ok = sm.transition(StateEvent::CONNECTED);
    assert(ok);
    assert(sm.current_state() == DeviceState::ACTIVE);
    printf("  PASS: connecting_to_active\n");
}

static void test_active_to_offline() {
    StateMachine sm;
    sm.init(DeviceState::ACTIVE);
    bool ok = sm.transition(StateEvent::WIFI_LOST);
    assert(ok);
    assert(sm.current_state() == DeviceState::OFFLINE_BUFFERING);
    printf("  PASS: active_to_offline\n");
}

static void test_offline_to_active() {
    StateMachine sm;
    sm.init(DeviceState::OFFLINE_BUFFERING);
    bool ok = sm.transition(StateEvent::RECONNECTED);
    assert(ok);
    assert(sm.current_state() == DeviceState::ACTIVE);
    printf("  PASS: offline_to_active\n");
}

static void test_active_to_revoked() {
    StateMachine sm;
    sm.init(DeviceState::ACTIVE);
    bool ok = sm.transition(StateEvent::REVOKE_RECEIVED);
    assert(ok);
    assert(sm.current_state() == DeviceState::REVOKED);
    printf("  PASS: active_to_revoked\n");
}

static void test_invalid_transition() {
    StateMachine sm;
    sm.init(DeviceState::ACTIVE);
    // ENROLLED is not valid from ACTIVE
    bool ok = sm.transition(StateEvent::ENROLLED);
    assert(!ok);
    assert(sm.current_state() == DeviceState::ACTIVE);
    printf("  PASS: invalid_transition_rejected\n");
}

static void test_degraded_recovery() {
    StateMachine sm;
    sm.init(DeviceState::DEGRADED);
    bool ok = sm.transition(StateEvent::RECOVERY_SUCCESS);
    assert(ok);
    assert(sm.current_state() == DeviceState::CONNECTING);
    printf("  PASS: degraded_recovery\n");
}

static void test_state_change_callback() {
    StateMachine sm;
    sm.init(DeviceState::BOOTING);

    DeviceState captured_old = DeviceState::ERROR;
    DeviceState captured_new = DeviceState::ERROR;
    sm.on_state_change([&](DeviceState old_s, DeviceState new_s) {
        captured_old = old_s;
        captured_new = new_s;
    });

    sm.transition(StateEvent::CREDENTIALS_FOUND);
    assert(captured_old == DeviceState::BOOTING);
    assert(captured_new == DeviceState::CONNECTING);
    printf("  PASS: state_change_callback\n");
}

int main() {
    printf("=== State Machine Tests ===\n");
    test_initial_state();
    test_boot_to_connecting();
    test_boot_to_provisioning();
    test_connecting_to_active();
    test_active_to_offline();
    test_offline_to_active();
    test_active_to_revoked();
    test_invalid_transition();
    test_degraded_recovery();
    test_state_change_callback();
    printf("=== All State Machine tests passed ===\n");
    return 0;
}
