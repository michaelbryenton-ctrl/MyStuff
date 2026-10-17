---
name: Android IME timing
description: Why Android text keyboards can appear to work while cartridge games miss every keystroke.
---

Treat SDL's Android virtual scancodes as the primary ASCII input stream, not as ordinary physical held-key events. Separate virtual press and release in emulated time, and avoid also typing the committed-text event.

**Why:** SDL's Android IME generates both virtual scancodes and committed text for a character. A virtual down/up pair can be consumed entirely before the next emulation step, leaving no pressed key for a cartridge to scan. Receiving keyboard events, or successfully opening the phone keyboard, is not proof that the emulated machine can read them.

**How to apply:** Preserve timing and character order across batched input, keep Shift with its character, handle phone Enter/Delete paths, and release queued keys when focus is lost. Host-side timing checks do not replace verification on an Android device.

Synthetic Android punctuation needs character translation independent of the physical host layout. Keep physical keyboards on their existing path.

**Why:** The user could not enter the double quote required by `CSAVE`. SDL generates US-position virtual ASCII keys, while XRoar's default host fallback can be GB and its emulated keyboard is position-mapped. A phone's shifted apostrophe is not the CoCo's Shift+2 quote chord.

**How to apply:** Translate virtual punctuation through the selected machine's Unicode-to-key mapping while preserving the timed queue and suppressing duplicate committed ASCII. Do not globally change physical keyboard language or translation settings to fix soft-key punctuation. BASIC needs straight quotes; the dedicated quote button also avoids smart-quote keyboard substitutions.
