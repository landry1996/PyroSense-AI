#include "security/nonce_generator.h"
#include "utils/time_utils.h"
#include <cstdio>
#include <cstdlib>

namespace pyrosense {

uint32_t NonceGenerator::counter_ = 0;

std::string NonceGenerator::generate() {
    // Format: timestamp_hex + counter_hex + random_hex
    // Provides uniqueness (counter), time-binding (timestamp), unpredictability (random)
    uint32_t ts = TimeUtils::unix_timestamp();
    uint32_t rnd = static_cast<uint32_t>(rand());
    counter_++;

    char buf[32];
    snprintf(buf, sizeof(buf), "%08x%08x%08x", ts, counter_, rnd);
    return std::string(buf);
}

bool NonceGenerator::validate_freshness(const std::string& nonce, uint32_t max_age_s) {
    if (nonce.size() < 8) return false;

    // Extract timestamp from first 8 hex chars
    uint32_t nonce_ts = 0;
    for (int i = 0; i < 8; i++) {
        nonce_ts <<= 4;
        char c = nonce[i];
        if (c >= '0' && c <= '9') nonce_ts |= (c - '0');
        else if (c >= 'a' && c <= 'f') nonce_ts |= (c - 'a' + 10);
        else return false;
    }

    uint32_t now = TimeUtils::unix_timestamp();
    if (now < nonce_ts) return false;
    return (now - nonce_ts) <= max_age_s;
}

}  // namespace pyrosense
