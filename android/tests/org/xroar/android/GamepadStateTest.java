package org.xroar.android;

public final class GamepadStateTest {
    private static int checks;
    private static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
        checks++;
    }
    private static void near(int actual, int expected, String name) {
        check(Math.abs(actual - expected) <= 1, name + ": " + actual);
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
        check(!state.isPositionHold() && !state.needsTick(), "Direct remains the default");
        state.setPositionHold(true);
        check(state.isPositionHold() && state.x() == 32768 && state.y() == 32768,
                "Position-hold starts neutral");
        state.advance(5000);
        state.motion(1, -.5f, 0, 0);
        check(state.x() == 32768 && state.needsTick(), "Stick sets velocity, not absolute position");
        state.advance(5100);
        near(state.x(), 39322, "Full stick integrates over elapsed time");
        near(state.y(), 29491, "Half stick has half speed");
        state.advance(5200);
        near(state.x(), 45875, "Unchanged held stick continues without new motion events");
        state.motion(0, 0, 0, 0);
        int heldX = state.x(), heldY = state.y();
        state.advance(5300);
        check(!state.needsTick() && state.x() == heldX && state.y() == heldY,
                "Spring-centred physical stick leaves stored position unchanged");
        state.key(GamepadState.RECENTER, true, 5300);
        check(state.x() == 32768 && state.y() == 32768 && !state.firing(5300),
                "B centres both axes without firing");
        state.motion(1, 1, 0, 0);
        state.advance(5400);
        check(!state.needsTick() && state.x() == 32768 && state.y() == 32768,
                "Held B keeps position centred");
        state.key(GamepadState.RECENTER, false, 5400);
        state.advance(5500);
        near(state.x(), 39322, "Releasing B restores stick adjustment");
        state.key(GamepadState.RIGHT, true, 5500);
        state.advance(5600);
        near(state.x(), 40141, "Key D-pad adjusts at fine speed");
        near(state.y(), 39322, "D-pad overrides the stick's other axis");
        state.key(GamepadState.LEFT, true, 5600);
        state.advance(5700);
        near(state.x(), 40141, "Opposite D-pad keys cancel without drifting");
        check(!state.needsTick(), "Opposing keys stop ticking despite active stick");
        state.key(GamepadState.LEFT, false, 5700);
        state.key(GamepadState.RIGHT, false, 5700);
        state.motion(0, 0, -1, -1);
        state.advance(5800);
        near(state.x(), 39322, "Hat D-pad has the same fine speed");
        near(state.y(), 38502, "Hat diagonal fine adjustment");
        state.motion(0, 0, 0, 0);
        state.key(GamepadState.DOWN, true, 5800);
        state.key(GamepadState.DOWN, false, 5810);
        near(state.y(), 38584, "Short D-pad tap integrates before key release");
        check(!state.needsTick(), "D-pad release holds position and stops ticks");
        state.key(GamepadState.FIRE, true, 5900);
        state.key(GamepadState.FIRE, false, 5901);
        check(state.firing(5910) && !state.firing(6020), "Position-hold preserves minimum A pulse");
        state.motion(1, -1, 0, 0);
        for (int i = 1; i <= 20; i++) state.advance(6000 + i * 100);
        check(state.x() == 65535 && state.y() == 0, "Position clamps to analog endpoints");
        state.motion(-1, 1, 0, 0);
        state.advance(8100);
        near(state.x(), 58982, "Reversing from endpoint moves immediately");
        state.advance(18100);
        near(state.x(), 52428, "Delayed UI frame caps elapsed time at 100 ms");
        int beforeBackwardsTime = state.x();
        state.advance(18000);
        check(state.x() == beforeBackwardsTime, "Backwards time cannot reverse movement");
        state.key(GamepadState.FIRE, true, 18000);
        state.reset();
        check(state.isPositionHold() && !state.needsTick() && state.x() == 32768
                        && state.y() == 32768 && !state.firing(18001),
                "Disconnect/pause/source/port reset neutralizes held position but preserves mode");
        state.advance(90000);
        check(state.x() == 32768, "Reset discards elapsed time and velocity");
        state.motion(1, 1, 0, 0);
        state.setPositionHold(false);
        check(!state.isPositionHold() && state.x() == 32768 && !state.needsTick(),
                "Mode change clears input state");
        state.motion(1, -1, 0, 0);
        state.key(GamepadState.RECENTER, true, 90010);
        check(state.x() == 65535 && state.y() == 0 && !state.firing(90010),
                "B does not alter original Direct mapping");
        state.setPositionHold(true);
        state.advance(100000);
        state.motion(.5f, 0, 0, 0);
        for (int i = 1; i <= 10; i++) state.advance(100000 + i * 10);
        near(state.x(), 36045, "Small ticks retain fractional precision");
        GamepadState singleTick = new GamepadState();
        singleTick.setPositionHold(true);
        singleTick.advance(100000);
        singleTick.motion(.5f, 0, 0, 0);
        singleTick.advance(100100);
        check(state.x() == singleTick.x(), "Movement is independent of ticker frequency");
        check(touch.x == oldX && touch.y == oldY,
                "Position-hold never changes the independent touch position");
        System.out.println("PASS: " + checks + " direct/position-hold, timing, D-pad, fire and reset checks");
    }
}
