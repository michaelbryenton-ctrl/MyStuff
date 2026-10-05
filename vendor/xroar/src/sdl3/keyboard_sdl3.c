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
#include "xroar.h"

#include "sdl3/common.h"

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

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

	if (hkbd.layout == hk_layout_iso) {
		if (ev->scancode == SDL_SCANCODE_BACKSLASH) {
			ev->scancode = SDL_SCANCODE_NONUSHASH;
		}
	}

	if (ev->scancode < 256) {
		hk_scan_release(ev->scancode);
	}
}

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

void sdl_textinput(struct ui_sdl3_interface *uisdl3, const char *text) {
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
}
