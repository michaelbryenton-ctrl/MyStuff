#include <jni.h>
#include <SDL3/SDL.h>
#include "android_tape_bridge.h"
#include "tape.h"
#include "xroar.h"

static SDL_AtomicInt tape_event_type, status, write_error;

void android_tape_bridge_init(void) {
    SDL_SetAtomicInt(&tape_event_type, (int)SDL_RegisterEvents(1));
    SDL_SetAtomicInt(&status, ANDROID_TAPE_IDLE);
    SDL_SetAtomicInt(&write_error, 0);
}

int android_tape_get_status(void) { return SDL_GetAtomicInt(&status); }

void android_tape_note_write_error(void) { SDL_SetAtomicInt(&write_error, 1); }

/* UI callers enqueue only. The SDL/emulator thread exclusively owns tape state. */
bool android_tape_request(const char *path) {
    int type = SDL_GetAtomicInt(&tape_event_type);
    int previous = android_tape_get_status();
    if (!type || previous == ANDROID_TAPE_PENDING
            || (path && previous == ANDROID_TAPE_RECORDING)
            || (!path && previous != ANDROID_TAPE_RECORDING)) return false;
    char *copy = path ? SDL_strdup(path) : NULL;
    if (path && !copy) return false;
    if (!SDL_CompareAndSwapAtomicInt(&status, previous, ANDROID_TAPE_PENDING)) {
        SDL_free(copy);
        return false;
    }
    SDL_Event event = {0};
    event.type = (Uint32)type;
    event.user.data1 = copy;
    if (!SDL_PushEvent(&event)) {
        SDL_free(copy);
        SDL_SetAtomicInt(&status, previous);
        return false;
    }
    return true;
}

bool android_tape_handle_event(const SDL_Event *event) {
    if (!event->type || event->type != (Uint32)SDL_GetAtomicInt(&tape_event_type))
        return false;
    char *path = event->user.data1;
    int result = ANDROID_TAPE_ERROR;
    if (xroar.tape_interface) {
        if (path) {
            SDL_SetAtomicInt(&write_error, 0);
            if (tape_open_writing(xroar.tape_interface, path) == 0)
                result = ANDROID_TAPE_RECORDING;
        } else {
            tape_close_writing(xroar.tape_interface); /* Flush before acknowledging. */
            if (!SDL_GetAtomicInt(&write_error)) result = ANDROID_TAPE_FINISHED;
        }
    }
    SDL_free(path);
    SDL_SetAtomicInt(&status, result);
    return true;
}

JNIEXPORT jboolean JNICALL Java_org_xroar_android_XRoarActivity_nativeTapeCommand(
        JNIEnv *env, jclass activity, jstring path) {
    (void)activity;
    const char *value = path ? (*env)->GetStringUTFChars(env, path, NULL) : NULL;
    if (path && !value) return JNI_FALSE;
    bool accepted = android_tape_request(value);
    if (value) (*env)->ReleaseStringUTFChars(env, path, value);
    return accepted ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_org_xroar_android_XRoarActivity_nativeTapeStatus(
        JNIEnv *env, jclass activity) {
    (void)env;
    (void)activity;
    return android_tape_get_status();
}
