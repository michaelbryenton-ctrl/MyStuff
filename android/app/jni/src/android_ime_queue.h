#ifndef XROAR_ANDROID_IME_QUEUE_H
#define XROAR_ANDROID_IME_QUEUE_H

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#define ANDROID_IME_QUEUE_CAPACITY 512

struct android_ime_key {
    uint32_t at;
    uint8_t scancode;
    bool down;
};

struct android_ime_queue {
    struct android_ime_key keys[ANDROID_IME_QUEUE_CAPACITY];
    bool pressed[256];
    size_t head;
    size_t count;
    uint32_t hold_ticks;
    uint32_t gap_ticks;
    uint32_t next_press;
    uint32_t last_release;
    bool cursor_valid;
};

typedef void (*android_ime_emit)(void *context, uint8_t scancode, bool down);

void android_ime_queue_init(struct android_ime_queue *, uint32_t hold, uint32_t gap);
bool android_ime_queue_key(struct android_ime_queue *, uint32_t now,
        uint8_t scancode, bool down, bool shift);
void android_ime_queue_process(struct android_ime_queue *, uint32_t now,
        android_ime_emit, void *context);
void android_ime_queue_cancel(struct android_ime_queue *, android_ime_emit, void *context);

#endif
