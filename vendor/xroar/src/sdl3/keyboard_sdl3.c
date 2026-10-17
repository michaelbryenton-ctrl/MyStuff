/* SDL3 keyboard module.
 *
 * Copyright 2015-2024 Ciaran Anscomb
 *
 * This file is part of XRoar, a Dragon/Tandy CoCo emulator.
 *
 * XRoar is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * See COPYING.GPL for redistribution conditions.
 */

#include "top-config.h"

#include <SDL3/SDL.h>

#include "hkbd.h"
#include "keyboard.h"
#include "events.h"
#include "xroar.h"

#include "sdl3/common.h"

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

#ifdef __ANDROID__
static void emit_ime_key(void *context, uint8_t scancode, bool down) {
struct ui_sdl3_interface *uisdl3 = context;
if (!xroar.keyboard_interface) return;
if (down) {
unsigned unicode = android_ime_symbol(scancode, (hkbd.state & HK_MASK_SHIFT) != 0);
if (unicode) {
uisdl3->ime_unicode[scancode] = unicode;
keyboard_unicode_press(xroar.keyboard_interface, unicode);
return;
}
} else if (uisdl3->ime_unicode[scancode]) {
keyboard_unicode_release(xroar.keyboard_interface, uisdl3->ime_unicode[scancode]);
uisdl3->ime_unicode[scancode] = 0;
// A translated character can temporarily override Shift; restore held-key state.
if (hkbd.state & HK_MASK_SHIFT) {
KBD_MATRIX_PRESS(xroar.keyboard_interface, DSCAN_SHIFT);
} else {
KBD_MATRIX_RELEASE(xroar.keyboard_interface, DSCAN_SHIFT);
}
return;
}
if (down) {
hk_scan_press(scancode);
} else {
hk_scan_release(scancode);
}
}

void sdl_process_textinput(struct ui_sdl3_interface *uisdl3) {
android_ime_queue_process(&uisdl3->ime_keys, event_current_tick, emit_ime_key, uisdl3);
}

void sdl_cancel_textinput(struct ui_sdl3_interface *uisdl3) {
android_ime_queue_cancel(&uisdl3->ime_keys, emit_ime_key, uisdl3);
}

static bool queue_ime_key(struct ui_sdl3_interface *uisdl3, SDL_KeyboardEvent *ev, bool down) {
// SDL's Android IME generates Unicode key pairs with virtual device id/raw code 0.
// Physical keyboards use Android keycodes and remain on the normal held-key path.
if (ev->which != 0 || ev->raw != 0) return false;
if (ev->scancode > 0 && ev->scancode < 256) {
bool shift = ev->scancode == SDL_SCANCODE_LSHIFT || ev->scancode == SDL_SCANCODE_RSHIFT;
if (!android_ime_queue_key(&uisdl3->ime_keys, event_current_tick, ev->scancode, down, shift)) {
sdl_cancel_textinput(uisdl3);
SDL_ShowSimpleMessageBox(SDL_MESSAGEBOX_WARNING, "Keyboard input",
        "Too much queued input. Wait, then try typing again.", uisdl3->vo_window);
}
}
return true;
}

void sdl_tap_key(struct ui_sdl3_interface *uisdl3, SDL_Scancode scancode) {
// The Quote button requests the US shifted apostrophe, translated to the
// emulated machine's actual quotation-mark chord by emit_ime_key().
bool quote = scancode == SDL_SCANCODE_APOSTROPHE;
if (uisdl3->ime_keys.count > ANDROID_IME_QUEUE_CAPACITY - (quote ? 4 : 2)) {
sdl_cancel_textinput(uisdl3);
}
SDL_KeyboardEvent event = {0};
if (quote) {
event.scancode = SDL_SCANCODE_LSHIFT;
queue_ime_key(uisdl3, &event, true);
}
event.scancode = scancode;
queue_ime_key(uisdl3, &event, true);
queue_ime_key(uisdl3, &event, false);
if (quote) {
event.scancode = SDL_SCANCODE_LSHIFT;
queue_ime_key(uisdl3, &event, false);
}
}
#endif

void sdl_keypress(struct ui_sdl3_interface *uisdl3, SDL_KeyboardEvent *ev) {

#ifdef WINDOWS32
	// In Windows, AltGr generates two events: Left Control then Right Alt.
	// Filter out the Control key here.
	if (ev->scancode == SDL_SCANCODE_LCTRL) {
		SDL_Event ev2;
		if (SDL_PeepEvents(&ev2, 1, SDL_PEEKEVENT, SDL_EVENT_KEY_DOWN, SDL_EVENT_KEY_DOWN) == 1) {
			if (ev2.key.scancode == SDL_SCANCODE_RALT) {
				return;
			}
		}
	}
#endif

#ifdef __ANDROID__
if (queue_ime_key(uisdl3, ev, true)) return;
#endif
if (hkbd.layout == hk_layout_iso) {
		if (ev->scancode == SDL_SCANCODE_BACKSLASH) {
			ev->scancode = SDL_SCANCODE_NONUSHASH;
		}
	}

	if (ev->scancode < 256) {
		hk_scan_press(ev->scancode);
	}

	if (!uisdl3->mouse_hidden) {
		SDL_HideCursor();
		uisdl3->mouse_hidden = 1;
	}

}

void sdl_keyrelease(struct ui_sdl3_interface *uisdl3, SDL_KeyboardEvent *ev) {
	(void)uisdl3;

#ifdef WINDOWS32
	// In Windows, AltGr generates two events: Left Control then Right Alt.
	// Filter out the Control key here.
	if (ev->scancode == SDL_SCANCODE_LCTRL) {
		SDL_Event ev2;
		if (SDL_PeepEvents(&ev2, 1, SDL_PEEKEVENT, SDL_EVENT_KEY_UP, SDL_EVENT_KEY_UP) == 1) {
			if (ev2.key.scancode == SDL_SCANCODE_RALT) {
				return;
			}
		}
	}
#endif

#ifdef __ANDROID__
if (queue_ime_key(uisdl3, ev, false)) return;
#endif
if (hkbd.layout == hk_layout_iso) {
		if (ev->scancode == SDL_SCANCODE_BACKSLASH) {
			ev->scancode = SDL_SCANCODE_NONUSHASH;
		}
	}

	if (ev->scancode < 256) {
		hk_scan_release(ev->scancode);
	}
}

#ifndef __ANDROID__
static SDL_Scancode text_scancode(unsigned char ch, bool *shift) {
	*shift = false;
	if (ch >= 'A' && ch <= 'Z') {
		*shift = true;
		ch = (unsigned char)(ch - 'A' + 'a');
	}
	if (ch >= 'a' && ch <= 'z') {
		return (SDL_Scancode)(SDL_SCANCODE_A + ch - 'a');
	}
	if (ch >= '1' && ch <= '9') {
		return (SDL_Scancode)(SDL_SCANCODE_1 + ch - '1');
	}

	switch (ch) {
	case '0': return SDL_SCANCODE_0;
	case ' ': return SDL_SCANCODE_SPACE;
	case '\n':
	case '\r': return SDL_SCANCODE_RETURN;
	case '\t': return SDL_SCANCODE_TAB;
	case '\b': return SDL_SCANCODE_BACKSPACE;
	case '-': return SDL_SCANCODE_MINUS;
	case '_': *shift = true; return SDL_SCANCODE_MINUS;
	case '=': return SDL_SCANCODE_EQUALS;
	case '+': *shift = true; return SDL_SCANCODE_EQUALS;
	case '[': return SDL_SCANCODE_LEFTBRACKET;
	case '{': *shift = true; return SDL_SCANCODE_LEFTBRACKET;
	case ']': return SDL_SCANCODE_RIGHTBRACKET;
	case '}': *shift = true; return SDL_SCANCODE_RIGHTBRACKET;
	case '\\': return SDL_SCANCODE_BACKSLASH;
	case '|': *shift = true; return SDL_SCANCODE_BACKSLASH;
	case ';': return SDL_SCANCODE_SEMICOLON;
	case ':': *shift = true; return SDL_SCANCODE_SEMICOLON;
	case '\'': return SDL_SCANCODE_APOSTROPHE;
	case '"': *shift = true; return SDL_SCANCODE_APOSTROPHE;
	case '`': return SDL_SCANCODE_GRAVE;
	case '~': *shift = true; return SDL_SCANCODE_GRAVE;
	case ',': return SDL_SCANCODE_COMMA;
	case '<': *shift = true; return SDL_SCANCODE_COMMA;
	case '.': return SDL_SCANCODE_PERIOD;
	case '>': *shift = true; return SDL_SCANCODE_PERIOD;
	case '/': return SDL_SCANCODE_SLASH;
	case '?': *shift = true; return SDL_SCANCODE_SLASH;
	case '!': *shift = true; return SDL_SCANCODE_1;
	case '@': *shift = true; return SDL_SCANCODE_2;
	case '#': *shift = true; return SDL_SCANCODE_3;
	case '$': *shift = true; return SDL_SCANCODE_4;
	case '%': *shift = true; return SDL_SCANCODE_5;
	case '^': *shift = true; return SDL_SCANCODE_6;
	case '&': *shift = true; return SDL_SCANCODE_7;
	case '*': *shift = true; return SDL_SCANCODE_8;
	case '(': *shift = true; return SDL_SCANCODE_9;
	case ')': *shift = true; return SDL_SCANCODE_0;
	default: return SDL_SCANCODE_UNKNOWN;
	}
}

static void send_scancode(struct ui_sdl3_interface *uisdl3, SDL_Scancode scancode, bool down) {
	SDL_KeyboardEvent event = {0};
	event.scancode = scancode;
	if (down) {
		sdl_keypress(uisdl3, &event);
	} else {
		sdl_keyrelease(uisdl3, &event);
	}
}
#endif

void sdl_textinput(struct ui_sdl3_interface *uisdl3, const char *text) {
#ifdef __ANDROID__
// SDLInputConnection already emits the matching virtual scancodes for ASCII.
// Handling committed text a second time would type each character twice.
(void)uisdl3;
(void)text;
#else
	for (const unsigned char *p = (const unsigned char *)text; *p; ++p) {
		bool shift = false;
		SDL_Scancode scancode = text_scancode(*p, &shift);
		if (scancode == SDL_SCANCODE_UNKNOWN) {
			continue;
		}
		if (shift) {
			send_scancode(uisdl3, SDL_SCANCODE_LSHIFT, true);
		}
		send_scancode(uisdl3, scancode, true);
		send_scancode(uisdl3, scancode, false);
		if (shift) {
			send_scancode(uisdl3, SDL_SCANCODE_LSHIFT, false);
		}
	}
#endif
}
