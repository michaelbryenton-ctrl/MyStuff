---
name: Phone interaction requirements
description: User's reasoning for the Android display arrangement and touch joystick behavior.
---

The user requested the emulator screen centered in the top half, with labels and keyboard below, after seeing buttons overlap the picture and the picture too close to the top.

The user described the CoCo joystick as a non-centered analog stick with a single button and suggested using phone touch position and double-tap for the button. Preserve that non-centering behavior rather than replacing it with a spring-centered virtual thumbstick.

**Why:** The phone controls should match the original joystick behavior the user expects, while keeping the emulated picture unobstructed.

**How to apply:** Keep picture and controls in separate layout regions. Preserve absolute analog position after finger lift-off. Explain any proposed change to this input model before replacing it.
