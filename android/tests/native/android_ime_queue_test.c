#include "android_ime_queue.h"

#include <assert.h>
#include <stdio.h>

static struct android_ime_key received[2048];
static size_t count;
static uint32_t current_time;

static void emit(void *context, uint8_t scancode, bool down) {
    (void)context;
    assert(count < sizeof(received) / sizeof(received[0]));
    received[count++] = (struct android_ime_key){current_time, scancode, down};
}

static void process(struct android_ime_queue *queue, uint32_t at) {
    current_time = at;
    android_ime_queue_process(queue, at, emit, NULL);
}

static void tap(struct android_ime_queue *queue, uint32_t at, uint8_t code) {
    assert(android_ime_queue_key(queue, at, code, true, false));
    assert(android_ime_queue_key(queue, at, code, false, false));
}

static void reset(struct android_ime_queue *queue) {
    android_ime_queue_init(queue, 50, 30);
    count = 0;
}

int main(void) {
    struct android_ime_queue queue;
    reset(&queue);
    tap(&queue, 0, 4);
    process(&queue, 0);
    assert(count == 1 && received[0].down && queue.pressed[4]);
    process(&queue, 49);
    assert(count == 1 && queue.pressed[4]);
    process(&queue, 50);
    assert(count == 2 && !received[1].down && !queue.pressed[4]);

    reset(&queue);
    tap(&queue, 100, 4);
    tap(&queue, 100, 4);
    process(&queue, 100);
    process(&queue, 150);
    process(&queue, 179);
    assert(count == 2 && !queue.pressed[4]);
    process(&queue, 180);
    assert(count == 3 && queue.pressed[4]);
    process(&queue, 230);
    assert(count == 4 && !queue.pressed[4]);

    reset(&queue);
    assert(android_ime_queue_key(&queue, 0, 225, true, true));
    tap(&queue, 0, 4);
    assert(android_ime_queue_key(&queue, 0, 225, false, true));
    tap(&queue, 0, 5);
    process(&queue, 0);
    assert(count == 2 && queue.pressed[225] && queue.pressed[4]);
    process(&queue, 50);
    assert(count == 4 && !queue.pressed[225] && !queue.pressed[4]);
    process(&queue, 80);
    assert(count == 5 && !queue.pressed[225] && queue.pressed[5]);
    process(&queue, 130);
    assert(count == 6 && !queue.pressed[5]);

    reset(&queue);
    tap(&queue, 0, 40); // Enter
    tap(&queue, 0, 44); // Space
    tap(&queue, 0, 42); // Delete
    for (uint32_t time = 0; time <= 210; time += 10) process(&queue, time);
    assert(count == 6 && !queue.pressed[40] && !queue.pressed[44] && !queue.pressed[42]);
    assert(received[0].scancode == 40 && received[2].scancode == 44
            && received[4].scancode == 42);

    reset(&queue);
    assert(android_ime_queue_key(&queue, 0, 225, true, true));
    tap(&queue, 0, 4);
    assert(android_ime_queue_key(&queue, 0, 225, false, true));
    process(&queue, 0);
    android_ime_queue_cancel(&queue, emit, NULL);
    assert(!queue.pressed[4] && !queue.pressed[225] && queue.count == 0);
    size_t cancelled_count = count;
    process(&queue, 10000);
    assert(count == cancelled_count);
    tap(&queue, 10000, 5);
    process(&queue, 10000);
    assert(queue.pressed[5]); // No stale cursor after focus loss.

    reset(&queue);
    for (size_t i = 0; i < ANDROID_IME_QUEUE_CAPACITY; i++) {
        assert(android_ime_queue_key(&queue, 0, 4, (i % 2) == 0, false));
    }
    assert(!android_ime_queue_key(&queue, 0, 4, true, false));
    process(&queue, 0);
    android_ime_queue_cancel(&queue, emit, NULL);
    assert(queue.count == 0 && !queue.pressed[4]);

    reset(&queue);
    uint32_t start = UINT32_MAX - 20;
    tap(&queue, start, 4);
    process(&queue, start);
    assert(count == 1 && queue.pressed[4]);
    process(&queue, start + 49);
    assert(count == 1);
    process(&queue, start + 50);
    assert(count == 2 && !queue.pressed[4]);

    reset(&queue);
    for (unsigned i = 0; i < 600; i++) {
        uint32_t at = i * 100;
        tap(&queue, at, 4);
        process(&queue, at);
        process(&queue, at + 50);
    }
    assert(count == 1200 && queue.count == 0); // Ring-buffer wraparound.
    puts("PASS: IME hold timing, repeated keys, Shift, Enter/Space/Delete, cancellation, overflow and tick/ring wraparound");
    return 0;
}
