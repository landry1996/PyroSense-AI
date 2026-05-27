#include "connectivity/wifi_manager.h"
#include "diagnostics/logger.h"

namespace pyrosense {

WifiManager::WifiManager()
    : state_(WifiState::DISCONNECTED), rssi_(-50) {}

bool WifiManager::init(const std::string& ssid, const std::string& password) {
    ssid_ = ssid;
    password_ = password;
    Logger::info("WIFI", "Initialized, SSID=%s", ssid_.c_str());
    return true;
}

bool WifiManager::connect() {
    state_ = WifiState::CONNECTING;
    Logger::info("WIFI", "Connecting to %s...", ssid_.c_str());

    // TODO: Replace with esp_wifi_connect() on real ESP32
    state_ = WifiState::CONNECTED;
    rssi_ = -42;
    Logger::info("WIFI", "Connected, RSSI=%d dBm", rssi_);
    return true;
}

void WifiManager::disconnect() {
    state_ = WifiState::DISCONNECTED;
    Logger::info("WIFI", "Disconnected");
}

WifiState WifiManager::state() const {
    return state_;
}

bool WifiManager::is_connected() const {
    return state_ == WifiState::CONNECTED;
}

int8_t WifiManager::rssi() const {
    return rssi_;
}

std::string WifiManager::ip_address() const {
    // TODO: Get real IP from esp_netif
    return "192.168.1.100";
}

}  // namespace pyrosense
