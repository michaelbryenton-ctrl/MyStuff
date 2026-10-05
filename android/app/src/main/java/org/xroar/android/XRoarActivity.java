package org.xroar.android;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.libsdl.app.SDLActivity;

import java.io.File;
import java.util.ArrayList;

public final class XRoarActivity extends SDLActivity {
    @Override
    protected String[] getArguments() {
        ArrayList<String> arguments = new ArrayList<>();
        arguments.add("-rompath");
        arguments.add(new File(getFilesDir(), "roms").getAbsolutePath());

        String mediaPath = getIntent().getStringExtra(LauncherActivity.EXTRA_MEDIA_PATH);
        if (mediaPath != null && new File(mediaPath).isFile()) {
            arguments.add("-run");
            arguments.add(mediaPath);
        }
        return arguments.toArray(new String[0]);
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
        keyboard.setOnClickListener(view -> SDLActivity.showTextInput(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
                0, 0, 1, 1));
        controls.addView(keyboard);

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
                Gravity.TOP | Gravity.END);
        int margin = Math.round(12 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, margin, margin, margin);
        addContentView(controls, params);
    }
}
