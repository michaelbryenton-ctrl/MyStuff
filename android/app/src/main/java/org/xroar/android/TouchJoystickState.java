package org.xroar.android;

/** Platform-independent absolute stick and minimum fire-pulse timing. */
final class TouchJoystickState {
    static final long FIRE_PULSE_MS = 120;
    int x = 32768;
    int y = 32768;
    private boolean held;
    private long fireUntil;

    static int axis(float coordinate, float extent) {
        if (extent <= 0 || Float.isNaN(coordinate)) return 32768;
        return Math.round(Math.max(0f, Math.min(1f, coordinate / extent)) * 65535f);
    }

    void move(float x, float y, float width, float height) {
        this.x = axis(x, width);
        this.y = axis(y, height);
    }

    void press(long now) {
        held = true;
        fireUntil = now + FIRE_PULSE_MS;
    }

    void release() { held = false; }

    void cancel() {
        held = false;
        fireUntil = 0;
    }

    boolean firing(long now) { return held || now < fireUntil; }
}
