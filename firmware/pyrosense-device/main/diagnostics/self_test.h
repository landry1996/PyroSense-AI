#pragma once

#include <cstdint>

namespace pyrosense {

struct SelfTestResult {
    bool sensors_ok;
    bool storage_ok;
    bool connectivity_ok;
    bool security_ok;
    bool all_pass;
    uint32_t duration_ms;
};

class SelfTest {
public:
    static SelfTestResult run();
    static void print_report(const SelfTestResult& result);

private:
    static bool test_sensors();
    static bool test_storage();
    static bool test_connectivity();
    static bool test_security();
};

}  // namespace pyrosense
