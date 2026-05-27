#pragma once

#include "config/device_config.h"
#include <string>
#include <functional>
#include <cstdint>

namespace pyrosense {

enum class MqttQos : uint8_t {
    AT_MOST_ONCE = 0,
    AT_LEAST_ONCE = 1,
    EXACTLY_ONCE = 2,
};

enum class MqttClientState : uint8_t {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
};

using MqttMessageCallback = std::function<void(const std::string& topic, const std::string& payload)>;

class IMqttClient {
public:
    virtual ~IMqttClient() = default;

    virtual bool init(const MqttConfig& config) = 0;
    virtual bool connect() = 0;
    virtual void disconnect() = 0;
    virtual bool publish(const std::string& topic,
                         const std::string& payload,
                         MqttQos qos,
                         bool retain = false) = 0;
    virtual bool subscribe(const std::string& topic, MqttQos qos, MqttMessageCallback callback) = 0;
    virtual MqttClientState state() const = 0;
    virtual bool is_connected() const = 0;
};

class MqttClient : public IMqttClient {
public:
    MqttClient();
    ~MqttClient() override;

    bool init(const MqttConfig& config) override;
    void set_tenant_id(const std::string& tenant_id);
    bool connect() override;
    void disconnect() override;
    bool publish(const std::string& topic,
                 const std::string& payload,
                 MqttQos qos,
                 bool retain = false) override;
    bool subscribe(const std::string& topic, MqttQos qos, MqttMessageCallback callback) override;
    MqttClientState state() const override;
    bool is_connected() const override;

    std::string build_topic(const std::string& suffix) const;

    uint32_t publish_count() const;
    uint32_t publish_fail_count() const;

private:
    MqttConfig config_;
    MqttClientState state_;
    MqttMessageCallback message_callback_;
    uint32_t publish_count_;
    uint32_t publish_fail_count_;
    std::string topic_prefix_;
    std::string tenant_id_;
};

}  // namespace pyrosense
