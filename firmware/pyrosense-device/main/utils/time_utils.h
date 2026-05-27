#pragma once

#include <string>
#include <cstdint>

namespace pyrosense {

class TimeUtils {
public:
    static uint32_t unix_timestamp();
    static uint32_t monotonic_ms();
    static std::string iso8601_now();
    static void set_time_offset(int32_t offset_s);

private:
    static int32_t time_offset_s_;
};

}  // namespace pyrosense
