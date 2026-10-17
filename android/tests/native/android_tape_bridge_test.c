#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <SDL3/SDL.h>
#include "android_tape_bridge.h"
#include "tape.h"
#include "xroar.h"

struct xroar xroar;
static struct tape_interface tape;
static SDL_Event queued;
static bool push_ok = true, register_ok = true, fail_open, fail_close;
static int opens, closes, allocations;
static char opened_path[512];

int SDL_GetAtomicInt(SDL_AtomicInt *a) { return a->value; }
int SDL_SetAtomicInt(SDL_AtomicInt *a, int v) { int old = a->value; a->value = v; return old; }
bool SDL_CompareAndSwapAtomicInt(SDL_AtomicInt *a, int old, int v) {
    if (a->value != old) return false;
    a->value = v;
    return true;
}
Uint32 SDL_RegisterEvents(int count) { assert(count == 1); return register_ok ? 0x8000 : 0; }
char *SDL_strdup(const char *s) {
    char *copy = malloc(strlen(s) + 1);
    assert(copy);
    strcpy(copy, s);
    allocations++;
    return copy;
}
void SDL_free(void *p) { if (p) allocations--; free(p); }
bool SDL_PushEvent(SDL_Event *event) { if (push_ok) queued = *event; return push_ok; }

int tape_open_writing(struct tape_interface *ti, const char *path) {
    assert(ti == &tape);
    opens++;
    snprintf(opened_path, sizeof(opened_path), "%s", path);
    return fail_open ? -1 : 0;
}
void tape_close_writing(struct tape_interface *ti) {
    assert(ti == &tape);
    closes++;
    if (fail_close) android_tape_note_write_error();
}

int main(void) {
    assert(!android_tape_request("before-init.cas"));
    xroar.tape_interface = &tape;
    android_tape_bridge_init();
    assert(android_tape_get_status() == ANDROID_TAPE_IDLE);
    assert(!android_tape_request(NULL));
    char path[] = "original.cas";
    assert(android_tape_request(path));
    path[0] = 'X';
    assert(android_tape_get_status() == ANDROID_TAPE_PENDING && opens == 0);
    assert(!android_tape_request("duplicate.cas") && !android_tape_request(NULL));
    assert(android_tape_handle_event(&queued));
    assert(opens == 1 && strcmp(opened_path, "original.cas") == 0 && allocations == 0);
    assert(android_tape_get_status() == ANDROID_TAPE_RECORDING);
    assert(!android_tape_request("overwrite.cas"));
    assert(android_tape_request(NULL) && closes == 0);
    assert(android_tape_get_status() == ANDROID_TAPE_PENDING);
    assert(android_tape_handle_event(&queued));
    assert(closes == 1 && android_tape_get_status() == ANDROID_TAPE_FINISHED);
    SDL_Event other = {0};
    other.type = 123;
    assert(!android_tape_handle_event(&other));
    push_ok = false;
    assert(!android_tape_request("queue-full.cas") && allocations == 0);
    assert(android_tape_get_status() == ANDROID_TAPE_FINISHED);
    push_ok = true;
    fail_open = true;
    assert(android_tape_request("cannot-open.cas"));
    assert(android_tape_handle_event(&queued));
    assert(android_tape_get_status() == ANDROID_TAPE_ERROR && allocations == 0);
    fail_open = false;
    assert(android_tape_request("retry.cas"));
    assert(android_tape_handle_event(&queued));
    push_ok = false;
    assert(!android_tape_request(NULL));
    assert(android_tape_get_status() == ANDROID_TAPE_RECORDING);
    push_ok = true;
    fail_close = true;
    assert(android_tape_request(NULL));
    assert(android_tape_handle_event(&queued));
    assert(android_tape_get_status() == ANDROID_TAPE_ERROR);
    fail_close = false;
    assert(android_tape_request("after-error.cas"));
    assert(android_tape_handle_event(&queued));
    assert(android_tape_request(NULL));
    assert(android_tape_handle_event(&queued));
    assert(android_tape_get_status() == ANDROID_TAPE_FINISHED);
    xroar.tape_interface = NULL;
    assert(android_tape_request("no-machine.cas"));
    assert(android_tape_handle_event(&queued));
    assert(android_tape_get_status() == ANDROID_TAPE_ERROR && allocations == 0);
    register_ok = false;
    android_tape_bridge_init();
    assert(!android_tape_request("no-event-slot.cas"));
    puts("PASS: tape-thread queueing, status, flush ordering, retries and failure handling");
}
