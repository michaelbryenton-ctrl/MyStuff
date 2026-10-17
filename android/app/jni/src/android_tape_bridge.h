#ifndef XROAR_ANDROID_TAPE_BRIDGE_H
#define XROAR_ANDROID_TAPE_BRIDGE_H

#include <SDL3/SDL.h>

enum android_tape_status {
    ANDROID_TAPE_IDLE = 0, ANDROID_TAPE_PENDING = 1,
    ANDROID_TAPE_RECORDING = 2, ANDROID_TAPE_FINISHED = 3,
    ANDROID_TAPE_ERROR = -1
};
void android_tape_bridge_init(void);
bool android_tape_request(const char *path); /* NULL finishes the current recording. */
int android_tape_get_status(void);
bool android_tape_handle_event(const SDL_Event *event);
void android_tape_note_write_error(void);

#endif
