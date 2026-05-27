#pragma once

#include <string>
#include <cstdint>

namespace pyrosense {

enum class WifiState : uint8_t {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
};

class WifiManager {
public:
    WifiManager();

    bool init(const std::string& ssid, const std::string& password);
    bool connect();
    void disconnect();
    WifiState state() const;
    bool is_connected() const;
    int8_t rssi() const;
    std::string ip_address() const;

private:
    std::string ssid_;
    std::string password_;
    WifiState state_;
    int8_t rssi_;
};

}  // namespace pyrosense
