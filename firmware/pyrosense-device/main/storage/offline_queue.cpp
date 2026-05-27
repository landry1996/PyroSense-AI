#include "storage/offline_queue.h"
#include "diagnostics/logger.h"
#include "utils/time_utils.h"

namespace pyrosense {

OfflineQueue::OfflineQueue()
    : max_entries_(100), dropped_count_(0), total_bytes_(0) {}

bool OfflineQueue::init(uint32_t max_entries) {
    max_entries_ = max_entries;
    Logger::info("STOR", "Offline queue initialized, capacity=%u", max_entries_);
    return true;
}

bool OfflineQueue::enqueue(const std::string& topic,
                            const std::string& payload,
                            QueuePriority priority) {
    if (queue_.size() >= max_entries_) {
        if (priority == QueuePriority::CRITICAL) {
            evict_oldest_normal();
        } else {
            dropped_count_++;
            Logger::warn("STOR", "Queue full, dropped message (total dropped=%u)", dropped_count_);
            return false;
        }
    }

    QueueEntry entry;
    entry.topic = topic;
    entry.payload = payload;
    entry.priority = priority;
    entry.timestamp = TimeUtils::unix_timestamp();
    entry.retry_count = 0;

    // Insert by priority (critical first, then high, then normal)
    if (priority == QueuePriority::CRITICAL) {
        queue_.push_front(entry);
    } else if (priority == QueuePriority::HIGH) {
        // Insert after critical entries
        auto it = queue_.begin();
        while (it != queue_.end() && it->priority == QueuePriority::CRITICAL) {
            ++it;
        }
        queue_.insert(it, entry);
    } else {
        queue_.push_back(entry);
    }

    total_bytes_ += payload.size() + topic.size();
    return true;
}

bool OfflineQueue::dequeue(QueueEntry& entry) {
    if (queue_.empty()) return false;
    entry = queue_.front();
    total_bytes_ -= (entry.payload.size() + entry.topic.size());
    queue_.pop_front();
    return true;
}

bool OfflineQueue::peek(QueueEntry& entry) const {
    if (queue_.empty()) return false;
    entry = queue_.front();
    return true;
}

uint32_t OfflineQueue::count() const {
    return static_cast<uint32_t>(queue_.size());
}

bool OfflineQueue::is_empty() const {
    return queue_.empty();
}

bool OfflineQueue::is_full() const {
    return queue_.size() >= max_entries_;
}

uint8_t OfflineQueue::usage_percent() const {
    if (max_entries_ == 0) return 100;
    return static_cast<uint8_t>((queue_.size() * 100) / max_entries_);
}

QueueStats OfflineQueue::stats() const {
    return {
        .total_entries = static_cast<uint32_t>(queue_.size()),
        .total_bytes = total_bytes_,
        .max_capacity = max_entries_,
        .usage_percent = usage_percent(),
        .dropped_count = dropped_count_,
    };
}

void OfflineQueue::clear() {
    queue_.clear();
    total_bytes_ = 0;
    Logger::info("STOR", "Queue cleared");
}

void OfflineQueue::evict_oldest_normal() {
    for (auto it = queue_.rbegin(); it != queue_.rend(); ++it) {
        if (it->priority == QueuePriority::NORMAL) {
            total_bytes_ -= (it->payload.size() + it->topic.size());
            queue_.erase(std::next(it).base());
            dropped_count_++;
            return;
        }
    }
    // If no normal entries, evict oldest high
    for (auto it = queue_.rbegin(); it != queue_.rend(); ++it) {
        if (it->priority == QueuePriority::HIGH) {
            total_bytes_ -= (it->payload.size() + it->topic.size());
            queue_.erase(std::next(it).base());
            dropped_count_++;
            return;
        }
    }
}

}  // namespace pyrosense
