#include "top-config.h"
#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <SDL3/SDL.h>
#include "joystick.h"

/* Android's UI thread publishes values; the emulator reads them at its own
 * rate. Absolute positions persist after lift-off, like a non-centering stick.
 * The inactive port is neutral, not a mirrored second joystick. */
static SDL_AtomicInt axes[2] = {{32768}, {32768}};
static SDL_AtomicInt fire;
static SDL_AtomicInt active_port;

struct touch_control {
    struct joystick_control control;
    unsigned port;
    unsigned axis;
    bool button;
};

static int read_control(void *opaque) {
    struct touch_control *control = opaque;
    if ((unsigned)SDL_GetAtomicInt(&active_port) != control->port)
        return control->button ? 0 : 32768;
    return SDL_GetAtomicInt(control->button ? &fire : &axes[control->axis]);
}

static struct joystick_control *configure(char *spec, unsigned index, bool button) {
    if (index >= (button ? 1u : 2u) || !spec ||
            (strcmp(spec, "0") != 0 && strcmp(spec, "1") != 0))
        return NULL;
    struct touch_control *control = calloc(1, sizeof(*control));
    if (!control) return NULL;
    control->port = (unsigned)(spec[0] - '0');
    control->axis = index;
    control->button = button;
    control->control.read = DELEGATE_AS0(int, read_control, control);
    control->control.free = DELEGATE_AS0(void, free, control);
    return &control->control;
}

static struct joystick_control *configure_axis(char *spec, unsigned axis) {
    return configure(spec, axis, false);
}

static struct joystick_control *configure_button(char *spec, unsigned button) {
    return configure(spec, button, true);
}

struct joystick_submodule android_touch_joystick = {
    .name = "touch",
    .configure_axis = configure_axis,
    .configure_button = configure_button,
};

JNIEXPORT void JNICALL Java_org_xroar_android_XRoarActivity_nativeTouchJoystick(
        JNIEnv *env, jclass activity, jint x, jint y, jboolean pressed, jint port) {
    (void)env;
    (void)activity;
    SDL_SetAtomicInt(&axes[0], x < 0 ? 0 : x > 65535 ? 65535 : x);
    SDL_SetAtomicInt(&axes[1], y < 0 ? 0 : y > 65535 ? 65535 : y);
    SDL_SetAtomicInt(&fire, pressed ? 1 : 0);
    SDL_SetAtomicInt(&active_port, port == 1 ? 1 : 0);
}
