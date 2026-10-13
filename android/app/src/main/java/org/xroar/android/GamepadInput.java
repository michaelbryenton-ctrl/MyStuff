package org.xroar.android;

import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

/** Route Android-recognized controllers to the existing native joystick bridge. */
final class GamepadInput implements InputManager.InputDeviceListener {
    interface Output { void update(int x, int y, boolean fire, int port, String deviceName); }
    private final InputManager manager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final GamepadState state = new GamepadState();
    private final Output output;
    private final Runnable finishPulse = this::publish;
    private final Runnable movementTick = this::publish;
    private boolean enabled, active, closed;
    private int deviceId = -1, port;

    GamepadInput(Context context, Output output) {
        this.output = output;
        manager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
        if (manager != null) {
            manager.registerInputDeviceListener(this, handler);
            refreshDevices(-1);
        }
    }

    boolean isEnabled() { return enabled; }
    boolean isPositionHold() { return state.isPositionHold(); }

    void setPositionHold(boolean value) {
        reset();
        state.setPositionHold(value);
        publish();
    }

    void setEnabled(boolean value) {
        reset();
        enabled = value;
        if (enabled) {
            refreshDevices(-1);
            publish();
        }
    }

    void setActive(boolean value) {
        if (active == value) return;
        active = value;
        reset();
        if (active) refreshDevices(-1);
    }

    void setPort(int value) {
        reset(); // Neutralize the old port before selecting the other one.
        port = value == 1 ? 1 : 0;
        publish();
    }

    private static boolean controller(InputDevice device) {
        return device != null && !device.isVirtual()
                && (device.supportsSource(InputDevice.SOURCE_GAMEPAD)
                || device.supportsSource(InputDevice.SOURCE_JOYSTICK));
    }

    private void refreshDevices(int excluded) {
        if (manager == null || closed) return;
        InputDevice current = manager.getInputDevice(deviceId);
        if (deviceId != excluded && controller(current)) {
            publish();
            return;
        }
        deviceId = -1;
        for (int id : manager.getInputDeviceIds()) {
            if (id != excluded && controller(manager.getInputDevice(id))) {
                deviceId = id;
                break;
            }
        }
        handler.removeCallbacks(movementTick);
        state.reset();
        publish();
    }

    boolean motion(MotionEvent event) {
        InputDevice device = event.getDevice();
        if (!event.isFromSource(InputDevice.SOURCE_JOYSTICK) || !controller(device)) return false;
        // Consume controller input even in Touch mode: do not also feed SDL's
        // keyboard/physical-controller paths with the same events.
        if (!enabled || !active || closed) return true;
        if (deviceId == -1) refreshDevices(-1);
        if (device.getId() != deviceId || event.getActionMasked() != MotionEvent.ACTION_MOVE)
            return true;
        state.advance(SystemClock.uptimeMillis());
        state.motion(axis(event, device, MotionEvent.AXIS_X),
                axis(event, device, MotionEvent.AXIS_Y),
                axis(event, device, MotionEvent.AXIS_HAT_X),
                axis(event, device, MotionEvent.AXIS_HAT_Y));
        publish();
        return true;
    }

    private static float axis(MotionEvent event, InputDevice device, int axis) {
        InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());
        if (range == null) range = device.getMotionRange(axis);
        return range == null ? 0 : GamepadState.normalize(
                event.getAxisValue(axis), range.getMin(), range.getMax(), range.getFlat());
    }

    boolean key(KeyEvent event) {
        if (!controller(event.getDevice())) return false;
        int mapped;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT: mapped = GamepadState.LEFT; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: mapped = GamepadState.RIGHT; break;
            case KeyEvent.KEYCODE_DPAD_UP: mapped = GamepadState.UP; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: mapped = GamepadState.DOWN; break;
            case KeyEvent.KEYCODE_BUTTON_A: mapped = GamepadState.FIRE; break;
            case KeyEvent.KEYCODE_BUTTON_B: mapped = GamepadState.RECENTER; break;
            case KeyEvent.KEYCODE_DPAD_CENTER: mapped = GamepadState.CENTER_FIRE; break;
            default: return KeyEvent.isGamepadButton(event.getKeyCode());
        }
        if (!enabled || !active || closed) return true;
        if (deviceId == -1) refreshDevices(-1);
        if (event.getDeviceId() != deviceId) return true;
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            state.key(mapped, true, SystemClock.uptimeMillis());
            if (mapped == GamepadState.FIRE || mapped == GamepadState.CENTER_FIRE) {
                handler.removeCallbacks(finishPulse);
                handler.postDelayed(finishPulse, TouchJoystickState.FIRE_PULSE_MS);
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP)
            state.key(mapped, false, SystemClock.uptimeMillis());
        publish();
        return true;
    }

    private void reset() {
        handler.removeCallbacks(finishPulse);
        handler.removeCallbacks(movementTick);
        state.reset();
        publish();
    }

    private void publish() {
        if (!enabled || closed) return;
        long now = SystemClock.uptimeMillis();
        state.advance(now);
        InputDevice device = manager == null ? null : manager.getInputDevice(deviceId);
        String name = manager == null ? "input unavailable"
                : device == null ? "connect a controller" : device.getName();
        output.update(state.x(), state.y(), state.firing(now), port, name);
        handler.removeCallbacks(movementTick);
        if (active && device != null && state.needsTick())
            handler.postDelayed(movementTick, 16);
    }

    @Override public void onInputDeviceAdded(int id) { refreshDevices(-1); }
    @Override public void onInputDeviceRemoved(int id) {
        if (id == deviceId) {
            reset();
            deviceId = -1;
            refreshDevices(id);
        }
    }
    @Override public void onInputDeviceChanged(int id) {
        if (id == deviceId) reset();
        refreshDevices(-1);
    }

    void close() {
        reset();
        closed = true;
        if (manager != null) manager.unregisterInputDeviceListener(this);
    }
}
