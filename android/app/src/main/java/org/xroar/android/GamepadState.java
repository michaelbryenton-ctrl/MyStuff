package org.xroar.android;

/** Android-independent direct/position-hold mapping and minimum fire pulse. */
final class GamepadState {
    static final int LEFT = 0, RIGHT = 1, UP = 2, DOWN = 3, FIRE = 4, CENTER_FIRE = 5,
            RECENTER = 6;
    static final float STICK_SPEED = 65535f, DPAD_SPEED = 8192f; // Analog units/second.
    private float stickX, stickY, hatX, hatY;
    private float heldX = 32768, heldY = 32768;
    private boolean positionHold, centerHeld, clockStarted;
    private long lastUpdate;
    private final boolean[] directions = new boolean[4];
    private final TouchJoystickState button = new TouchJoystickState();
    private boolean primaryFire, centerFire;

    static float normalize(float value, float min, float max, float flat) {
        if (Float.isNaN(value) || Float.isInfinite(value)
                || Float.isNaN(min) || Float.isNaN(max) || Float.isInfinite(min)
                || Float.isInfinite(max) || max <= min) return 0;
        float center = (min + max) / 2f;
        float span = value < center ? center - min : max - center;
        if (span <= 0) return 0;
        float amount = Math.max(-1f, Math.min(1f, (value - center) / span));
        float dead = Float.isNaN(flat) ? .15f
                : Math.max(.15f, Math.min(.95f, Math.abs(flat) / span));
        if (Math.abs(amount) <= dead) return 0;
        return Math.signum(amount) * (Math.abs(amount) - dead) / (1f - dead);
    }

    void motion(float x, float y, float horizontalHat, float verticalHat) {
        stickX = x;
        stickY = y;
        hatX = horizontalHat;
        hatY = verticalHat;
    }

    void key(int key, boolean pressed, long now) {
        advance(now);
        if (key == FIRE || key == CENTER_FIRE) {
            boolean wasHeld = primaryFire || centerFire;
            if (key == FIRE) primaryFire = pressed;
            else centerFire = pressed;
            boolean held = primaryFire || centerFire;
            if (held && !wasHeld) button.press(now);
            else if (!held && wasHeld) button.release();
        } else if (key == RECENTER && positionHold) {
            centerHeld = pressed;
            if (pressed) heldX = heldY = 32768;
        } else if (key >= LEFT && key <= DOWN) directions[key] = pressed;
    }

    int x() { return positionHold ? Math.round(heldX) : axis(true); }
    int y() { return positionHold ? Math.round(heldY) : axis(false); }
    boolean firing(long now) { return button.firing(now); }

    boolean isPositionHold() { return positionHold; }

    void setPositionHold(boolean enabled) {
        reset();
        positionHold = enabled;
    }

    private boolean hasDpad() {
        return directions[LEFT] || directions[RIGHT] || directions[UP] || directions[DOWN]
                || hatX != 0 || hatY != 0;
    }

    boolean needsTick() {
        return positionHold && !centerHeld && (vector(true) != 0 || vector(false) != 0);
    }

    void advance(long now) {
        // Cap delayed frames: resuming after a UI stall must not jump to an edge.
        long elapsed = clockStarted ? Math.max(0, Math.min(100, now - lastUpdate)) : 0;
        lastUpdate = now;
        clockStarted = true;
        if (!positionHold || centerHeld) return;
        float distance = (hasDpad() ? DPAD_SPEED : STICK_SPEED) * elapsed / 1000f;
        heldX = Math.max(0, Math.min(65535, heldX + vector(true) * distance));
        heldY = Math.max(0, Math.min(65535, heldY + vector(false) * distance));
    }

    private float vector(boolean horizontal) {
        float value;
        if (directions[LEFT] || directions[RIGHT] || directions[UP] || directions[DOWN]) {
            value = horizontal
                    ? (directions[RIGHT] ? 1 : 0) - (directions[LEFT] ? 1 : 0)
                    : (directions[DOWN] ? 1 : 0) - (directions[UP] ? 1 : 0);
        } else if (hatX != 0 || hatY != 0) value = horizontal ? hatX : hatY;
        else value = horizontal ? stickX : stickY;
        return value;
    }

    private int axis(boolean horizontal) {
        return TouchJoystickState.axis((vector(horizontal) + 1f) / 2f, 1f);
    }

    void reset() {
        stickX = stickY = hatX = hatY = 0;
        heldX = heldY = 32768;
        centerHeld = clockStarted = false;
        java.util.Arrays.fill(directions, false);
        primaryFire = centerFire = false;
        button.cancel();
    }
}
