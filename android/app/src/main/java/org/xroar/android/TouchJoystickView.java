package org.xroar.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/** Absolute touch input shared by the lower pad and the emulator surface. */
final class TouchJoystickView extends View {
    interface Output { void update(int x, int y, boolean fire, int port); }
    private final TouchJoystickState state = new TouchJoystickState();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final GestureDetector gestures;
    private final Output output;
    private int pointer = -1;
    private int port;
    private boolean gamepadMode;
    private boolean positionHold;
    private String gamepadName = "connect a controller";
    private int gamepadX = 32768, gamepadY = 32768;
    private boolean gamepadFire;
    private final Runnable updateFire = this::publish;

    TouchJoystickView(Context context, Output output) {
        super(context);
        this.output = output;
        setBackgroundColor(Color.rgb(12, 21, 28));
        setContentDescription("Absolute analog joystick. Drag to move. Double tap to fire; "
                + "hold the second tap for continuous fire. Position stays after release.");
        gestures = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent event) { return true; }
            @Override public boolean onDoubleTap(MotionEvent event) {
                state.press(SystemClock.uptimeMillis());
                removeCallbacks(updateFire);
                postDelayed(updateFire, TouchJoystickState.FIRE_PULSE_MS);
                publish();
                return true;
            }
        });
        // Reset the shared native source on each new emulator session, including
        // when the previous session used the other joystick port.
        publish();
    }

    int switchPort() {
        cancelFire();
        port = 1 - port;
        publish();
        return port;
    }

    void cancelFire() {
        pointer = -1;
        state.cancel();
        removeCallbacks(updateFire);
        publish();
    }

    void setGamepadMode(boolean enabled) {
        gamepadMode = enabled;
        cancelFire();
        updateDescription();
    }

    void setPositionHold(boolean enabled) {
        positionHold = enabled;
        updateDescription();
        invalidate();
    }

    private void updateDescription() {
        setContentDescription(gamepadMode ? (positionHold
                ? "CoCo position-hold. Left stick adjusts position; release to hold. "
                    + "D-pad fine adjusts; A fires; B centres."
                : "Direct gamepad input. Left stick or D-pad moves; A fires.")
                : "Absolute analog joystick. Drag to move. Double tap to fire; "
                + "hold the second tap for continuous fire. Position stays after release.");
    }

    void showGamepad(String name, int x, int y, boolean fire) {
        gamepadName = name;
        gamepadX = x;
        gamepadY = y;
        gamepadFire = fire;
        invalidate();
    }

    private void publish() {
        output.update(state.x, state.y, state.firing(SystemClock.uptimeMillis()), port);
        invalidate();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        return handleTouch(this, event);
    }

    boolean handleTouch(View source, MotionEvent event) {
        if (gamepadMode) return true;
        gestures.onTouchEvent(event);
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_CANCEL) {
            cancelFire();
            return true;
        }
        if (action == MotionEvent.ACTION_DOWN) pointer = event.getPointerId(0);
        int index = event.findPointerIndex(pointer);
        if (index >= 0) {
            float top = source == this ? dp(52) : 0;
            float margin = source == this ? dp(16) : 0;
            state.move(event.getX(index) - margin, event.getY(index) - top,
                    source.getWidth() - margin * 2, source.getHeight() - top - margin);
        }
        if (action == MotionEvent.ACTION_UP || (action == MotionEvent.ACTION_POINTER_UP
                && event.getPointerId(event.getActionIndex()) == pointer)) {
            pointer = -1;
            state.release();
            // Keep quick double taps pressed long enough for the emulated CPU
            // to observe them; a held second tap remains down until lift-off.
            removeCallbacks(updateFire);
            postDelayed(updateFire, TouchJoystickState.FIRE_PULSE_MS);
        }
        publish();
        return true;
    }

    @Override protected void onDetachedFromWindow() {
        cancelFire();
        super.onDetachedFromWindow();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getHeight() < dp(90)) return;
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(14));
        paint.setColor(Color.rgb(202, 219, 228));
        String heading = gamepadMode ? "Gamepad: " + gamepadName : "Touch or drag — position stays";
        int length = paint.breakText(heading, true, getWidth() - dp(32), null);
        if (length < heading.length()) heading = heading.substring(0, Math.max(0, length - 1)) + "…";
        canvas.drawText(heading, getWidth() / 2f, dp(22), paint);
        paint.setTextSize(dp(12));
        canvas.drawText(gamepadMode ? (positionHold
                ? "Release holds · D-pad fine · A fire · B centre"
                : "Left stick / D-pad moves · A fires · touch disabled")
                : "Double-tap to fire · hold second tap to keep firing",
                getWidth() / 2f, dp(42), paint);
        float left = dp(16), top = dp(52), right = getWidth() - left;
        float bottom = getHeight() - left;
        paint.setColor(Color.rgb(42, 66, 80));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        canvas.drawRect(left, top, right, bottom, paint);
        canvas.drawLine((left + right) / 2, top, (left + right) / 2, bottom, paint);
        canvas.drawLine(left, (top + bottom) / 2, right, (top + bottom) / 2, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor((gamepadMode ? gamepadFire : state.firing(SystemClock.uptimeMillis()))
                ? Color.rgb(255, 154, 89) : Color.rgb(87, 198, 226));
        canvas.drawCircle(left + (gamepadMode ? gamepadX : state.x) / 65535f * (right - left),
                top + (gamepadMode ? gamepadY : state.y) / 65535f * (bottom - top), dp(9), paint);
    }
}
