package org.xroar.android;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.libsdl.app.SDLActivity;

import java.io.File;

public final class XRoarActivity extends SDLActivity {
    private static native boolean nativeTapKey(int scancode);

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
        MachineProfile profile = MachineProfile.OPTIONS[MachineProfile.indexOf(
                getIntent().getStringExtra(LauncherActivity.EXTRA_MACHINE))];
        String mediaPath = getIntent().getStringExtra(LauncherActivity.EXTRA_MEDIA_PATH);
        return profile.arguments(new File(getFilesDir(), "roms"),
                mediaPath == null ? null : new File(mediaPath));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        controls.addView(keyboard);
        addKeyButton(controls, "Enter", 40);
        addKeyButton(controls, "Space", 44);

        Button library = new Button(this);
        library.setText("Library");
        library.setAllCaps(false);
        library.setTextColor(Color.WHITE);
        library.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.rgb(29, 55, 71)));
        library.setOnClickListener(view -> finish());
        controls.addView(library);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        int margin = Math.round(12 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, margin, margin, margin);
        addContentView(controls, params);
        SafeArea.apply(controls, false, true);
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
        controls.addView(key);
    }
}
