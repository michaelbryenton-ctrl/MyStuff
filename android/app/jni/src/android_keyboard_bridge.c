#include <jni.h>
#include <SDL3/SDL.h>
#include "android_keyboard_bridge.h"

static SDL_AtomicInt tap_event_type;

void android_keyboard_bridge_init(void) {
    SDL_SetAtomicInt(&tap_event_type, (int)SDL_RegisterEvents(1));
}

bool android_keyboard_tap_event(const SDL_Event *event, SDL_Scancode *scancode) {
    if (event->type != (Uint32)SDL_GetAtomicInt(&tap_event_type)) return false;
    *scancode = (SDL_Scancode)event->user.code;
    return true;
}

/* UI-thread callers only enqueue events. XRoar's SDL thread owns keyboard state. */
JNIEXPORT jboolean JNICALL Java_org_xroar_android_XRoarActivity_nativeTapKey(
        JNIEnv *env, jclass activity, jint scancode) {
    (void)env;
    (void)activity;
    switch (scancode) {
    case SDL_SCANCODE_RETURN:
    case SDL_SCANCODE_SPACE:
    case SDL_SCANCODE_BACKSPACE:
        break;
    default: return JNI_FALSE;
    }
    int type = SDL_GetAtomicInt(&tap_event_type);
    if (type == 0) return JNI_FALSE;
    SDL_Event event = {0};
    // One event represents the whole tap, so a full SDL queue cannot lose only key-up.
    event.type = (Uint32)type;
    event.user.code = scancode;
    return SDL_PushEvent(&event) ? JNI_TRUE : JNI_FALSE;
}
