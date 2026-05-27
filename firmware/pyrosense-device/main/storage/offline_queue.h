#pragma once

#include <string>
#include <cstdint>
#include <deque>

namespace pyrosense {

enum class QueuePriority : uint8_t {
    NORMAL = 0,
    HIGH = 1,
    CRITICAL = 2,
};

struct QueueEntry {
    std::string topic;
    std::string payload;
    QueuePriority priority;
    uint32_t timestamp;
    uint8_t retry_count;
};

struct QueueStats {
    uint32_t total_entries;
    uint32_t total_bytes;
    uint32_t max_capacity;
    uint8_t usage_percent;
    uint32_t dropped_count;
};

class IOfflineQueue {
public:
    virtual ~IOfflineQueue() = default;

    virtual bool init(uint32_t max_entries) = 0;
    virtual bool enqueue(const std::string& topic,
                         const std::string& payload,
                         QueuePriority priority = QueuePriority::NORMAL) = 0;
    virtual bool dequeue(QueueEntry& entry) = 0;
    virtual bool peek(QueueEntry& entry) const = 0;
    virtual uint32_t count() const = 0;
    virtual bool is_empty() const = 0;
    virtual bool is_full() const = 0;
    virtual uint8_t usage_percent() const = 0;
    virtual QueueStats stats() const = 0;
    virtual void clear() = 0;
};

class OfflineQueue : public IOfflineQueue {
public:
    OfflineQueue();

    bool init(uint32_t max_entries) override;
    bool enqueue(const std::string& topic,
                 const std::string& payload,
                 QueuePriority priority = QueuePriority::NORMAL) override;
    bool dequeue(QueueEntry& entry) override;
    bool peek(QueueEntry& entry) const override;
    uint32_t count() const override;
    bool is_empty() const override;
    bool is_full() const override;
    uint8_t usage_percent() const override;
    QueueStats stats() const override;
    void clear() override;

private:
    std::deque<QueueEntry> queue_;
    uint32_t max_entries_;
    uint32_t dropped_count_;
    uint32_t total_bytes_;

    void evict_oldest_normal();
};

}  // namespace pyrosense
