package org.xroar.android;

import android.graphics.Color;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.libsdl.app.SDLActivity;

import java.io.File;

public final class XRoarActivity extends SDLActivity {
    private static native boolean nativeTapKey(int scancode);
    private static native void nativeTouchJoystick(int x, int y, boolean fire, int port);
    private TouchJoystickView joystick;

    private void tapKey(int scancode) {
        if (!nativeTapKey(scancode)) {
            Toast.makeText(this, "Could not send the key. Try again.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Some phone keyboards send Enter/Delete as key events rather than committed text.
        boolean virtual = event.getDeviceId() == KeyCharacterMap.VIRTUAL_KEYBOARD
                || (event.getFlags() & KeyEvent.FLAG_SOFT_KEYBOARD) != 0;
        int code = event.getKeyCode();
        if (virtual && (code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_DEL)) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                tapKey(code == KeyEvent.KEYCODE_ENTER ? 40 : 42);
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected String[] getArguments() {
        SDLActivity.nativeSetenv("SDL_ORIENTATIONS",
                "Portrait PortraitUpsideDown LandscapeLeft LandscapeRight");
        MachineProfile profile = MachineProfile.OPTIONS[MachineProfile.indexOf(
                getIntent().getStringExtra(LauncherActivity.EXTRA_MACHINE))];
        String mediaPath = getIntent().getStringExtra(LauncherActivity.EXTRA_MEDIA_PATH);
        return profile.arguments(new File(getFilesDir(), "roms"),
                mediaPath == null ? null : new File(mediaPath));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mBrokenLibraries || mSurface == null) return;
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        EmulatorLayout layout = new EmulatorLayout(this);
        layout.setBackgroundColor(Color.BLACK);
        FrameLayout display = new FrameLayout(this);
        ((ViewGroup) mSurface.getParent()).removeView(mSurface);
        display.addView(mSurface, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER));
        layout.addView(display, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0));
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);

        Button keyboard = new Button(this);
        keyboard.setText("Keyboard");
        keyboard.setAllCaps(false);
        keyboard.setTextColor(Color.WHITE);
        keyboard.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.rgb(29, 55, 71)));
        keyboard.setOnClickListener(view -> {
            int[] position = new int[2];
            keyboard.getLocationInWindow(position);
            SDLActivity.showTextInput(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
                    position[0], position[1], keyboard.getWidth(), keyboard.getHeight());
        });
        addControl(controls, keyboard);
        addKeyButton(controls, "Enter", 40);
        addKeyButton(controls, "Space", 44);

        Button library = new Button(this);
        library.setText("Library");
        library.setAllCaps(false);
        library.setTextColor(Color.WHITE);
        library.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.rgb(29, 55, 71)));
        library.setOnClickListener(view -> returnToLibrary());
        addControl(controls, library);
        layout.addView(controls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        joystick = new TouchJoystickView(this, (x, y, fire, port) -> {
            if (!mBrokenLibraries && !mSDLMainFinished)
                nativeTouchJoystick(x, y, fire, port);
        });
        Button port = new Button(this);
        port.setAllCaps(false);
        port.setText("Joystick: Right · tap to switch port");
        port.setTextSize(13);
        port.setTextColor(Color.WHITE);
        port.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.rgb(29, 55, 71)));
        port.setOnClickListener(view -> port.setText("Joystick: "
                + (joystick.switchPort() == 0 ? "Right" : "Left") + " · tap to switch port"));
        layout.addView(port, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));
        layout.addView(joystick, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        mSurface.setOnTouchListener((view, event) -> joystick.handleTouch(view, event));
        mLayout.addView(layout, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        SafeArea.apply(layout, true, true);
        if (Build.VERSION.SDK_INT < 30) {
            // Older Android does not expose IME insets separately. Keep the
            // unresized layout and reserve the obscured bottom using its
            // visible window frame instead (also handles fullscreen IMEs).
            layout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                Rect visible = new Rect();
                layout.getWindowVisibleDisplayFrame(visible);
                int[] position = new int[2];
                layout.getLocationOnScreen(position);
                int bottom = Math.max(0, position[1] + layout.getHeight() - visible.bottom);
                if (layout.getPaddingBottom() != bottom)
                    layout.setPadding(layout.getPaddingLeft(), layout.getPaddingTop(),
                            layout.getPaddingRight(), bottom);
            });
        }
    }

    @Override protected void onPause() {
        if (joystick != null) joystick.cancelFire();
        super.onPause();
    }

    private void returnToLibrary() {
        if (joystick != null) joystick.cancelFire();
        // Reuse the existing library rather than creating another copy. It
        // lives in the main process, so XRoar's exit() during SDL teardown
        // cannot kill it. Both activities remain in the same Android task.
        Intent library = new Intent(this, LauncherActivity.class);
        library.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(library);
        finish();
    }

    @Override public void onWindowFocusChanged(boolean focused) {
        if (!focused && joystick != null) joystick.cancelFire();
        super.onWindowFocusChanged(focused);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void addControl(LinearLayout controls, Button button) {
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setTextSize(12);
        button.setSingleLine(true);
        button.setPadding(dp(4), 0, dp(4), 0);
        controls.addView(button, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
    }

    private void addKeyButton(LinearLayout controls, String label, int scancode) {
        Button key = new Button(this);
        key.setText(label);
        key.setAllCaps(false);
        key.setFocusable(false);
        key.setTextColor(Color.WHITE);
        key.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.rgb(29, 55, 71)));
        key.setOnClickListener(view -> tapKey(scancode));
        addControl(controls, key);
    }
}
