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
import android.view.MotionEvent;
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
    private static native boolean nativeTapeCommand(String path);
    private static native int nativeTapeStatus();
    private TouchJoystickView joystick;
    private GamepadInput gamepad;
    private boolean resumed;
    private CassetteFiles cassetteFiles;

    private void tapKey(int scancode) {
        if (!nativeTapKey(scancode)) {
            Toast.makeText(this, "Could not send the key. Try again.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (gamepad != null && gamepad.key(event)) return true;
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

    @Override public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (gamepad != null && gamepad.motion(event)) return true;
        return super.dispatchGenericMotionEvent(event);
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
        cassetteFiles = new CassetteFiles(this, new CassetteFiles.TapeAccess() {
            @Override public boolean request(String path) {
                return !mSDLMainFinished && nativeTapeCommand(path);
            }
            @Override public int status() {
                return mSDLMainFinished ? -1 : nativeTapeStatus();
            }
        }, null, savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        EmulatorLayout layout = new EmulatorLayout(this);
        layout.setBackgroundColor(Color.BLACK);
        String machine = getIntent().getStringExtra(LauncherActivity.EXTRA_MACHINE);
        FrameLayout display = machine != null && machine.startsWith("coco")
                ? new CocoMonitorLayout(this) : new FrameLayout(this);
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
        addKeyButton(controls, "\"", 52);

        Button library = new Button(this);
        library.setText("Library");
        library.setAllCaps(false);
        library.setTextColor(Color.WHITE);
        library.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.rgb(29, 55, 71)));
        library.setOnClickListener(view -> cassetteFiles.prepareToLeave(this::returnToLibrary));
        addControl(controls, library);
        layout.addView(controls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        joystick = new TouchJoystickView(this, (x, y, fire, port) -> {
            if (!mBrokenLibraries && !mSDLMainFinished && (gamepad == null || !gamepad.isEnabled()))
                nativeTouchJoystick(x, y, fire, port);
        });
        gamepad = new GamepadInput(this, (x, y, fire, selectedPort, deviceName) -> {
            if (!mBrokenLibraries && !mSDLMainFinished)
                nativeTouchJoystick(x, y, fire, selectedPort);
            joystick.showGamepad(deviceName, x, y, fire);
        });
        Button port = new Button(this);
        port.setAllCaps(false);
        port.setText("Port: Right");
        port.setTextSize(13);
        port.setTextColor(Color.WHITE);
        port.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.rgb(29, 55, 71)));
        port.setOnClickListener(view -> {
            int selectedPort = joystick.switchPort();
            gamepad.setPort(selectedPort);
            port.setText("Port: " + (selectedPort == 0 ? "Right" : "Left"));
        });
        port.setMinWidth(0);
        port.setMinimumWidth(0);
        LinearLayout sessionControls = new LinearLayout(this);
        sessionControls.setOrientation(LinearLayout.HORIZONTAL);
        sessionControls.addView(port, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        Button source = new Button(this);
        Button mode = new Button(this);
        mode.setText("Gamepad: Direct");
        mode.setAllCaps(false);
        mode.setTextSize(13);
        mode.setTextColor(Color.WHITE);
        mode.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.rgb(29, 55, 71)));
        mode.setVisibility(android.view.View.GONE);
        mode.setOnClickListener(view -> {
            gamepad.setPositionHold(!gamepad.isPositionHold());
            mode.setText(gamepad.isPositionHold() ? "Gamepad: CoCo position-hold"
                    : "Gamepad: Direct");
            joystick.setPositionHold(gamepad.isPositionHold());
        });
        source.setText("Input: Touch");
        source.setAllCaps(false);
        source.setTextColor(Color.WHITE);
        source.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.rgb(29, 55, 71)));
        source.setOnClickListener(view -> {
            joystick.cancelFire();
            gamepad.setEnabled(!gamepad.isEnabled());
            joystick.setGamepadMode(gamepad.isEnabled());
            source.setText(gamepad.isEnabled() ? "Input: Gamepad" : "Input: Touch");
            mode.setVisibility(gamepad.isEnabled() ? android.view.View.VISIBLE
                    : android.view.View.GONE);
        });
        addControl(sessionControls, source);
        Button exit = new Button(this);
        exit.setText("Exit app");
        exit.setAllCaps(false);
        exit.setTextColor(Color.WHITE);
        exit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.rgb(29, 55, 71)));
        Button files = new Button(this);
        files.setText("Files");
        files.setAllCaps(false);
        files.setTextColor(Color.WHITE);
        files.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.rgb(29, 55, 71)));
        files.setOnClickListener(view -> cassetteFiles.show());
        addControl(sessionControls, files);
        exit.setOnClickListener(view -> AppExit.confirmWhenReady(this, finishExit ->
                cassetteFiles.prepareToLeave(() -> {
                    releaseInput();
                    finishExit.run();
                })));
        addControl(sessionControls, exit);
        layout.addView(sessionControls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));
        layout.addView(mode, new LinearLayout.LayoutParams(
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
        resumed = false;
        releaseInput();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        if (gamepad != null) gamepad.setActive(hasWindowFocus());
    }

    @Override protected void onDestroy() {
        if (cassetteFiles != null) cassetteFiles.close();
        if (gamepad != null) gamepad.close();
        super.onDestroy();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        if (cassetteFiles != null) cassetteFiles.saveState(state);
        super.onSaveInstanceState(state);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        if (cassetteFiles != null && cassetteFiles.onActivityResult(request, result, data)) return;
        super.onActivityResult(request, result, data);
    }

    @Override public void onBackPressed() {
        if (cassetteFiles != null) cassetteFiles.prepareToLeave(() -> super.onBackPressed());
        else super.onBackPressed();
    }

    private void releaseInput() {
        if (gamepad != null) gamepad.setActive(false);
        if (joystick != null) joystick.cancelFire();
    }

    private void returnToLibrary() {
        releaseInput();
        // Reuse the existing library rather than creating another copy. It
        // lives in the main process, so XRoar's exit() during SDL teardown
        // cannot kill it. Both activities remain in the same Android task.
        Intent library = new Intent(this, LauncherActivity.class);
        library.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(library);
        finish();
    }

    @Override public void onWindowFocusChanged(boolean focused) {
        if (!focused) releaseInput();
        else if (gamepad != null) gamepad.setActive(resumed);
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
