#pragma once

#include <string>
#include <cstdint>
#include <vector>

namespace pyrosense {

class IHmacSigner {
public:
    virtual ~IHmacSigner() = default;

    virtual bool init(const std::string& key_hex) = 0;
    virtual std::string sign(const std::string& payload) = 0;
    virtual bool verify(const std::string& payload, const std::string& signature) = 0;
    virtual bool is_enabled() const = 0;
};

class HmacSigner : public IHmacSigner {
public:
    HmacSigner();

    bool init(const std::string& key_hex) override;
    std::string sign(const std::string& payload) override;
    bool verify(const std::string& payload, const std::string& signature) override;
    bool is_enabled() const override;

private:
    std::vector<uint8_t> key_;
    bool enabled_;

    static std::vector<uint8_t> hex_to_bytes(const std::string& hex);
    static std::string bytes_to_hex(const uint8_t* data, size_t len);
};

}  // namespace pyrosense
