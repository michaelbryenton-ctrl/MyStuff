#ifndef XROAR_ANDROID_IME_SYMBOLS_H
#define XROAR_ANDROID_IME_SYMBOLS_H

#include <stdbool.h>
#include <stdint.h>

/* SDL Android's synthetic ASCII keys use USB/US positions, not the host layout. */
unsigned android_ime_symbol(uint8_t scancode, bool shift);

#endif
