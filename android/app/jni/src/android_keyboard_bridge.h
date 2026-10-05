#ifndef XROAR_ANDROID_KEYBOARD_BRIDGE_H
#define XROAR_ANDROID_KEYBOARD_BRIDGE_H

#include <SDL3/SDL.h>

void android_keyboard_bridge_init(void);
bool android_keyboard_tap_event(const SDL_Event *, SDL_Scancode *);

#endif
