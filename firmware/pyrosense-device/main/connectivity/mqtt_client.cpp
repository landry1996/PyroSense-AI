#include "connectivity/mqtt_client.h"
#include "diagnostics/logger.h"

namespace pyrosense {

MqttClient::MqttClient()
    : state_(MqttClientState::DISCONNECTED),
      publish_count_(0),
      publish_fail_count_(0) {}

MqttClient::~MqttClient() {
    disconnect();
}

bool MqttClient::init(const MqttConfig& config) {
    config_ = config;
    topic_prefix_ = "pyrosense/v1/";
    Logger::info("MQTT", "Client initialized, broker=%s:%d",
                 config_.broker_uri.c_str(), config_.port);
    return true;
}

void MqttClient::set_tenant_id(const std::string& tenant_id) {
    tenant_id_ = tenant_id;
}

bool MqttClient::connect() {
    state_ = MqttClientState::CONNECTING;
    Logger::info("MQTT", "Connecting to %s...", config_.broker_uri.c_str());

    // TODO: Replace with esp_mqtt_client_start() on real ESP32
    // For now, simulate successful connection
    state_ = MqttClientState::CONNECTED;
    Logger::info("MQTT", "Connected successfully");
    return true;
}

void MqttClient::disconnect() {
    if (state_ == MqttClientState::CONNECTED) {
        // TODO: esp_mqtt_client_stop()
        state_ = MqttClientState::DISCONNECTED;
        Logger::info("MQTT", "Disconnected");
    }
}

bool MqttClient::publish(const std::string& topic,
                          const std::string& payload,
                          MqttQos qos,
                          bool retain) {
    if (state_ != MqttClientState::CONNECTED) {
        publish_fail_count_++;
        Logger::warn("MQTT", "Publish failed: not connected");
        return false;
    }

    // TODO: Replace with esp_mqtt_client_publish()
    // For now, log the publication
    Logger::debug("MQTT", "PUB [%s] qos=%d len=%zu",
                  topic.c_str(), static_cast<int>(qos), payload.size());

    publish_count_++;
    return true;
}

bool MqttClient::subscribe(const std::string& topic, MqttQos qos, MqttMessageCallback callback) {
    if (state_ != MqttClientState::CONNECTED) {
        Logger::warn("MQTT", "Subscribe failed: not connected");
        return false;
    }

    message_callback_ = callback;
    // TODO: esp_mqtt_client_subscribe()
    Logger::info("MQTT", "SUB [%s] qos=%d", topic.c_str(), static_cast<int>(qos));
    return true;
}

MqttClientState MqttClient::state() const {
    return state_;
}

bool MqttClient::is_connected() const {
    return state_ == MqttClientState::CONNECTED;
}

std::string MqttClient::build_topic(const std::string& suffix) const {
    return topic_prefix_ + tenant_id_ + "/" + config_.client_id + "/" + suffix;
}

uint32_t MqttClient::publish_count() const {
    return publish_count_;
}

uint32_t MqttClient::publish_fail_count() const {
    return publish_fail_count_;
}

}  // namespace pyrosense
