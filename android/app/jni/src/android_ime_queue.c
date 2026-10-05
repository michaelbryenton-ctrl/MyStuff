#include "android_ime_queue.h"

#include <string.h>

/* Compare wrapping emulator tick counters; queued delays must stay below 2^31 ticks. */
static bool reached(uint32_t now, uint32_t at) {
    return (int32_t)(now - at) >= 0;
}

void android_ime_queue_init(struct android_ime_queue *queue, uint32_t hold, uint32_t gap) {
    memset(queue, 0, sizeof(*queue));
    queue->hold_ticks = hold;
    queue->gap_ticks = gap;
}

bool android_ime_queue_key(struct android_ime_queue *queue, uint32_t now,
        uint8_t scancode, bool down, bool shift) {
    if (queue->count == ANDROID_IME_QUEUE_CAPACITY) return false;
    if (!queue->cursor_valid || reached(now, queue->next_press)) {
        queue->next_press = now;
        queue->cursor_valid = true;
    }
    uint32_t at = queue->next_press;
    if (!down) {
        if (shift) {
            // Release Shift alongside its character, before the next character's gap.
            at = reached(now, queue->last_release) ? now : queue->last_release;
        } else {
            at += queue->hold_ticks;
            queue->last_release = at;
            queue->next_press = at + queue->gap_ticks;
        }
    }
    size_t tail = (queue->head + queue->count) % ANDROID_IME_QUEUE_CAPACITY;
    queue->keys[tail] = (struct android_ime_key){at, scancode, down};
    queue->count++;
    return true;
}

void android_ime_queue_process(struct android_ime_queue *queue, uint32_t now,
        android_ime_emit emit, void *context) {
    while (queue->count && reached(now, queue->keys[queue->head].at)) {
        struct android_ime_key key = queue->keys[queue->head];
        queue->head = (queue->head + 1) % ANDROID_IME_QUEUE_CAPACITY;
        queue->count--;
        queue->pressed[key.scancode] = key.down;
        emit(context, key.scancode, key.down);
    }
}

void android_ime_queue_cancel(struct android_ime_queue *queue,
        android_ime_emit emit, void *context) {
    for (unsigned code = 0; code < 256; code++) {
        if (queue->pressed[code]) emit(context, (uint8_t)code, false);
    }
    uint32_t hold = queue->hold_ticks;
    uint32_t gap = queue->gap_ticks;
    android_ime_queue_init(queue, hold, gap);
}
