#include "utils/uuid_utils.h"
#include <cstdio>
#include <cstdlib>

namespace pyrosense {

std::string UuidUtils::generate_v4() {
    // Simplified UUID v4 generation
    // On real ESP32: use esp_random() for better entropy
    uint8_t bytes[16];
    for (int i = 0; i < 16; i++) {
        bytes[i] = static_cast<uint8_t>(rand() & 0xFF);
    }

    // Set version 4 and variant bits
    bytes[6] = (bytes[6] & 0x0F) | 0x40;  // version 4
    bytes[8] = (bytes[8] & 0x3F) | 0x80;  // variant 1

    char buf[37];
    snprintf(buf, sizeof(buf),
             "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x",
             bytes[0], bytes[1], bytes[2], bytes[3],
             bytes[4], bytes[5], bytes[6], bytes[7],
             bytes[8], bytes[9], bytes[10], bytes[11],
             bytes[12], bytes[13], bytes[14], bytes[15]);
    return std::string(buf);
}

}  // namespace pyrosense
