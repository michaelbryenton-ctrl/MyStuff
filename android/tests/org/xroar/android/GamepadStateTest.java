package org.xroar.android;

public final class GamepadStateTest {
    private static int checks;
    private static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
        checks++;
    }
    public static void main(String[] args) {
        GamepadState state = new GamepadState();
        check(state.x() == 32768 && state.y() == 32768, "Starts centered");
        check(GamepadState.normalize(.1f, -1, 1, 0) == 0, "Default dead zone");
        check(GamepadState.normalize(.2f, -1, 1, .3f) == 0, "Reported dead zone");
        check(GamepadState.normalize(Float.NaN, -1, 1, 0) == 0, "Invalid input is neutral");
        check(GamepadState.normalize(1, 1, 1, 0) == 0, "Invalid range is neutral");
        check(GamepadState.normalize(127, 0, 255, 0) == 0, "Unsigned hardware range centers");
        check(GamepadState.normalize(0, 0, 255, 0) == -1, "Unsigned range negative endpoint");
        check(GamepadState.normalize(255, 0, 255, 0) == 1, "Unsigned range positive endpoint");
        state.motion(-1, 1, 0, 0);
        check(state.x() == 0 && state.y() == 65535, "Stick reaches full analog range");
        state.motion(0, 0, 0, 0);
        check(state.x() == 32768 && state.y() == 32768, "Physical stick spring-centers");
        state.motion(1, 1, -1, 0);
        check(state.x() == 0 && state.y() == 32768, "Hat D-pad overrides stick vector");
        state.key(GamepadState.UP, true, 0);
        check(state.x() == 32768 && state.y() == 0, "Key D-pad takes priority");
        state.key(GamepadState.RIGHT, true, 0);
        check(state.x() == 65535 && state.y() == 0, "Key D-pad diagonals");
        state.key(GamepadState.LEFT, true, 0);
        check(state.x() == 32768, "Opposite directions cancel");
        state.key(GamepadState.LEFT, false, 0);
        state.key(GamepadState.RIGHT, false, 0);
        state.key(GamepadState.UP, false, 0);
        check(state.x() == 0 && state.y() == 32768, "Key release restores active hat");
        state.motion(0, 0, 0, 0);
        check(state.x() == 32768 && state.y() == 32768, "Hat release centers");
        state.key(GamepadState.FIRE, true, 100);
        check(state.firing(1000), "Held A remains pressed");
        state.key(GamepadState.FIRE, false, 1000);
        check(!state.firing(1000), "Held A release stops fire");
        state.key(GamepadState.FIRE, true, 2000);
        state.key(GamepadState.FIRE, false, 2001);
        check(state.firing(2010), "Short A tap remains observable by the CPU");
        check(!state.firing(2120), "Short fire pulse expires");
        state.key(GamepadState.FIRE, true, 2200);
        state.key(GamepadState.CENTER_FIRE, true, 2201);
        state.key(GamepadState.FIRE, false, 2400);
        check(state.firing(2400), "Releasing A does not release a held alternate fire button");
        state.key(GamepadState.CENTER_FIRE, false, 2401);
        check(!state.firing(2401), "Both fire button releases stop firing");
        state.key(GamepadState.FIRE, true, 3000);
        state.key(GamepadState.DOWN, true, 3000);
        state.motion(1, -1, 1, 1);
        state.reset();
        check(state.x() == 32768 && state.y() == 32768 && !state.firing(3001),
                "Disconnect/pause/source or port changes immediately clear all state");
        TouchJoystickState touch = new TouchJoystickState();
        touch.move(90, 10, 100, 100);
        int oldX = touch.x, oldY = touch.y;
        touch.press(4000);
        touch.cancel();
        state.motion(-1, 1, 0, 0);
        state.reset();
        check(touch.x == oldX && touch.y == oldY && !touch.firing(4001),
                "Touch's latched position survives independently of gamepad reset");
        System.out.println("PASS: " + checks + " gamepad axis, D-pad, fire and reset checks");
    }
}
