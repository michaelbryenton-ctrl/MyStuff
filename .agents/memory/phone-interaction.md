---
name: Phone interaction requirements
description: User's reasoning for the Android display arrangement and touch joystick behavior.
---

The user requested the emulator screen centered in the top half, with labels and keyboard below, after seeing buttons overlap the picture and the picture too close to the top.

The user described the CoCo joystick as a non-centered analog stick with a single button and suggested using phone touch position and double-tap for the button. Preserve that non-centering behavior rather than replacing it with a spring-centered virtual thumbstick.

**Why:** The phone controls should match the original joystick behavior the user expects, while keeping the emulated picture unobstructed. The user confirmed the touch joystick seemed to work fine, with further testing still pending.

**How to apply:** Keep picture and controls in separate layout regions. Preserve absolute analog position after finger lift-off. Explain any proposed change to this input model before replacing it.

## Physical controller compatibility

Gamepad support is an additional selectable input source, not a replacement for touch. Physical sticks may spring-center normally, but adding or revising gamepad support must not change the touch joystick's latched position behavior.

**Why:** The user approved basic USB gamepad support explicitly while keeping the existing touch controls.

The user confirmed that gamepad input works on their Android device. The controller model was not specified; this confirms their setup, not universal controller compatibility.

**How to apply:** Keep the two input models independent, retain the joystick port selector, and require real Android/controller verification before claiming hardware compatibility.

## Selectable CoCo gamepad behavior

The user approved a selectable CoCo position-hold mode alongside the existing Direct gamepad mode: stick deflection adjusts position at proportional speed, release holds it, D-pad makes fine adjustments, A fires, and B centres.

**Why:** A spring-centred gamepad cannot physically replicate the original non-centering CoCo stick. The user accepted cursor-like relative positioning rather than a heuristic that ignores centre events.

**How to apply:** Keep Direct available and leave touch behavior unchanged. Safety resets, including controller disconnection, must neutralize even a stored gamepad position. Do not describe relative positioning as exact physical CoCo-stick replication.

## CoCo display and cassette expectations

The user requested an optional full-screen game display that keeps the aspect ratio and a small control strip, rather than hiding every control.

**Why:** They want a larger picture while retaining access to controls during play.

**How to apply:** Keep controls outside the picture and preserve a clear return to normal layout. Full-screen display must not stretch/crop the game or change the selected input method.

The user requested a CoCo bezel based on their picture of the TRS-80 13-inch Color Video Receiver, with live emulation inside the TV opening and the keyboard/joystick controls below.

The user expects a selected machine-code cassette to initiate `CLOADM` automatically. Do not require manual console commands after choosing this kind of media. BASIC cassettes still need their own type-appropriate loading command.

**Why:** The user explicitly asked for the receiver-style bezel and cassette auto-loading. Their cassette example was supplied as an archive containing alternate CAS and WAV representations, so importing an archive is part of the expected media-picker flow.

**How to apply:** Preserve the CoCo-specific receiver styling and automatic, header-appropriate cassette loading when revising phone UI or media import. A successful extraction or launch-argument test is not proof that the cassette actually boots on Android.
