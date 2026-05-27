#pragma once

#include <string>
#include <cstdint>

namespace pyrosense {

class NonceGenerator {
public:
    static std::string generate();
    static bool validate_freshness(const std::string& nonce, uint32_t max_age_s);

private:
    static uint32_t counter_;
};

}  // namespace pyrosense
