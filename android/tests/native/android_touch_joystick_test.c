#include "top-config.h"
#include <assert.h>
#include <jni.h>
#include <SDL3/SDL.h>
#include "joystick.h"

/* Sequential unit substitutes, not a replacement for a real NDK build.
 * Exercise the production adapter and delegates without an Android runtime. */
int SDL_GetAtomicInt(SDL_AtomicInt *value) { return value->value; }
int SDL_SetAtomicInt(SDL_AtomicInt *value, int next) {
    int previous = value->value;
    value->value = next;
    return previous;
}
extern struct joystick_submodule android_touch_joystick;
extern void Java_org_xroar_android_XRoarActivity_nativeTouchJoystick(
        JNIEnv *, jclass, jint, jint, jboolean, jint);

int main(void) {
    struct joystick_control *rx = android_touch_joystick.configure_axis("0", 0);
    struct joystick_control *ry = android_touch_joystick.configure_axis("0", 1);
    struct joystick_control *rb = android_touch_joystick.configure_button("0", 0);
    struct joystick_control *lx = android_touch_joystick.configure_axis("1", 0);
    struct joystick_control *lb = android_touch_joystick.configure_button("1", 0);
    assert(rx && ry && rb && lx && lb);
    assert(!android_touch_joystick.configure_axis("invalid", 0));
    assert(!android_touch_joystick.configure_axis("0", 2));
    assert(!android_touch_joystick.configure_button("0", 1));
    Java_org_xroar_android_XRoarActivity_nativeTouchJoystick(NULL, NULL, -1, 99999, JNI_TRUE, 0);
    assert(DELEGATE_CALL(rx->read) == 0 && DELEGATE_CALL(ry->read) == 65535);
    assert(DELEGATE_CALL(rb->read) == 1);
    assert(DELEGATE_CALL(lx->read) == 32768 && DELEGATE_CALL(lb->read) == 0);
    Java_org_xroar_android_XRoarActivity_nativeTouchJoystick(NULL, NULL, 12345, 23456, JNI_TRUE, 1);
    assert(DELEGATE_CALL(lx->read) == 12345 && DELEGATE_CALL(lb->read) == 1);
    assert(DELEGATE_CALL(rx->read) == 32768 && DELEGATE_CALL(rb->read) == 0);
    Java_org_xroar_android_XRoarActivity_nativeTouchJoystick(NULL, NULL, 12345, 23456, JNI_FALSE, 1);
    assert(DELEGATE_CALL(lx->read) == 12345 && DELEGATE_CALL(lb->read) == 0);
    DELEGATE_CALL(rx->free); DELEGATE_CALL(ry->free); DELEGATE_CALL(rb->free);
    DELEGATE_CALL(lx->free); DELEGATE_CALL(lb->free);
    puts("Native touch joystick range, port selection and release checks passed");
    return 0;
}
