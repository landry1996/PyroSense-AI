#include "storage/offline_queue.h"
#include <cstdio>
#include <cassert>

using namespace pyrosense;

static void test_queue_init() {
    OfflineQueue queue;
    queue.init(10);
    assert(queue.is_empty());
    assert(queue.count() == 0);
    assert(queue.usage_percent() == 0);
    printf("  PASS: queue_init\n");
}

static void test_queue_enqueue_dequeue() {
    OfflineQueue queue;
    queue.init(10);

    bool ok = queue.enqueue("topic/test", "{\"value\":1}");
    assert(ok);
    assert(queue.count() == 1);

    QueueEntry entry;
    ok = queue.dequeue(entry);
    assert(ok);
    assert(entry.topic == "topic/test");
    assert(entry.payload == "{\"value\":1}");
    assert(queue.is_empty());
    printf("  PASS: queue_enqueue_dequeue\n");
}

static void test_queue_fifo_order() {
    OfflineQueue queue;
    queue.init(10);

    queue.enqueue("t1", "first");
    queue.enqueue("t2", "second");
    queue.enqueue("t3", "third");

    QueueEntry entry;
    queue.dequeue(entry);
    assert(entry.payload == "first");
    queue.dequeue(entry);
    assert(entry.payload == "second");
    queue.dequeue(entry);
    assert(entry.payload == "third");
    printf("  PASS: queue_fifo_order\n");
}

static void test_queue_priority() {
    OfflineQueue queue;
    queue.init(10);

    queue.enqueue("normal", "msg_normal", QueuePriority::NORMAL);
    queue.enqueue("high", "msg_high", QueuePriority::HIGH);
    queue.enqueue("critical", "msg_critical", QueuePriority::CRITICAL);

    QueueEntry entry;
    queue.dequeue(entry);
    assert(entry.payload == "msg_critical");
    queue.dequeue(entry);
    assert(entry.payload == "msg_high");
    queue.dequeue(entry);
    assert(entry.payload == "msg_normal");
    printf("  PASS: queue_priority\n");
}

static void test_queue_full() {
    OfflineQueue queue;
    queue.init(3);

    queue.enqueue("t1", "a");
    queue.enqueue("t2", "b");
    queue.enqueue("t3", "c");
    assert(queue.is_full());

    // Normal priority should be rejected when full
    bool ok = queue.enqueue("t4", "d", QueuePriority::NORMAL);
    assert(!ok);
    assert(queue.count() == 3);
    printf("  PASS: queue_full\n");
}

static void test_queue_critical_eviction() {
    OfflineQueue queue;
    queue.init(3);

    queue.enqueue("t1", "normal1", QueuePriority::NORMAL);
    queue.enqueue("t2", "normal2", QueuePriority::NORMAL);
    queue.enqueue("t3", "normal3", QueuePriority::NORMAL);

    // Critical should evict oldest normal
    bool ok = queue.enqueue("t4", "critical", QueuePriority::CRITICAL);
    assert(ok);
    assert(queue.count() == 3);

    QueueEntry entry;
    queue.dequeue(entry);
    assert(entry.payload == "critical");
    printf("  PASS: queue_critical_eviction\n");
}

static void test_queue_usage_percent() {
    OfflineQueue queue;
    queue.init(10);

    for (int i = 0; i < 5; i++) {
        queue.enqueue("t", "p");
    }
    assert(queue.usage_percent() == 50);
    printf("  PASS: queue_usage_percent\n");
}

static void test_queue_clear() {
    OfflineQueue queue;
    queue.init(10);

    queue.enqueue("t1", "a");
    queue.enqueue("t2", "b");
    queue.clear();
    assert(queue.is_empty());
    assert(queue.count() == 0);
    printf("  PASS: queue_clear\n");
}

int main() {
    printf("=== Offline Queue Tests ===\n");
    test_queue_init();
    test_queue_enqueue_dequeue();
    test_queue_fifo_order();
    test_queue_priority();
    test_queue_full();
    test_queue_critical_eviction();
    test_queue_usage_percent();
    test_queue_clear();
    printf("=== All Offline Queue tests passed ===\n");
    return 0;
}
