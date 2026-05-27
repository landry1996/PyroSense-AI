#include "utils/time_utils.h"
#include <ctime>
#include <cstdio>

namespace pyrosense {

int32_t TimeUtils::time_offset_s_ = 0;

uint32_t TimeUtils::unix_timestamp() {
    // On real ESP32: use esp_timer + NTP synced time
    return static_cast<uint32_t>(time(nullptr)) + time_offset_s_;
}

uint32_t TimeUtils::monotonic_ms() {
    // On real ESP32: use esp_timer_get_time() / 1000
    // On host: clock_gettime or similar
    static uint32_t counter = 0;
    counter++;
    return counter;
}

std::string TimeUtils::iso8601_now() {
    time_t now = static_cast<time_t>(unix_timestamp());
    struct tm* tm_info = gmtime(&now);
    char buf[32];
    strftime(buf, sizeof(buf), "%Y-%m-%dT%H:%M:%SZ", tm_info);
    return std::string(buf);
}

void TimeUtils::set_time_offset(int32_t offset_s) {
    time_offset_s_ = offset_s;
}

}  // namespace pyrosense
