package org.xroar.android;

public final class TouchJoystickStateTest {
    private static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        TouchJoystickState stick = new TouchJoystickState();
        check(stick.x == 32768 && stick.y == 32768, "Starts neutral");
        stick.move(0, 100, 100, 100);
        check(stick.x == 0 && stick.y == 65535, "Full analog range");
        stick.release();
        check(stick.x == 0 && stick.y == 65535, "Lift does not recenter");
        stick.move(-50, 200, 100, 100);
        check(stick.x == 0 && stick.y == 65535, "Out-of-bounds motion clamps");
        stick.move(50, 50, 100, 100);
        check(stick.x == 32768 && stick.y == 32768, "Center position");
        check(TouchJoystickState.axis(0, 0) == 32768, "Unmeasured pad stays neutral");
        check(!stick.firing(1000), "Single movement does not fire");
        stick.press(1000);
        stick.release();
        check(stick.firing(1001) && stick.firing(1119), "Quick fire pulse is readable");
        check(!stick.firing(1120), "Pulse ends without a stuck button");
        stick.press(2000);
        check(stick.firing(4000), "Held second tap stays pressed");
        stick.release();
        check(!stick.firing(4000), "Long hold releases on lift");
        stick.press(5000);
        stick.cancel();
        check(!stick.firing(5001), "Cancel/focus loss clears fire immediately");
        check(stick.x == 32768 && stick.y == 32768, "Cancel preserves position");
        System.out.println("Touch joystick position and fire timing checks passed");
    }
}
