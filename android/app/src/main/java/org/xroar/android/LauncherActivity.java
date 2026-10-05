package org.xroar.android;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LauncherActivity extends Activity {
    private static final int PICK_ROMS = 1;
    private static final int PICK_MEDIA = 2;
    private static final String LAST_MEDIA = "last-media";
    private static final String LAST_MACHINE = "last-machine";
    static final String EXTRA_MEDIA_PATH = "org.xroar.android.MEDIA_PATH";
    static final String EXTRA_MACHINE = "org.xroar.android.MACHINE";

    private final ExecutorService importer = Executors.newSingleThreadExecutor();
    private TextView romSummary;
    private TextView mediaSummary;
    private TextView status;
    private Button startButton;
    private Spinner machinePicker;
    private String selectedMediaPath;
    private boolean importing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        selectedMediaPath = getPreferences(MODE_PRIVATE).getString(LAST_MEDIA, null);
        if (selectedMediaPath != null && !new File(selectedMediaPath).isFile()) {
            selectedMediaPath = null;
        }
        buildScreen();
        refreshSummaries();
    }

    @Override
    protected void onDestroy() {
        importer.shutdown();
        super.onDestroy();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(9, 19, 31));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(28), dp(22), dp(28));
        scroll.addView(content);

        TextView title = text("XRoar", 34, Color.rgb(243, 247, 250));
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        content.addView(title);

        TextView subtitle = text("Dragon and Tandy computer emulator", 16,
                Color.rgb(180, 193, 204));
        LinearLayout.LayoutParams subtitleParams = margins(subtitle, 0, 4, 0, 22);
        content.addView(subtitle, subtitleParams);

        LinearLayout romPanel = panel();
        content.addView(romPanel, margins(romPanel, 0, 0, 0, 16));
        addHeading(romPanel, "1. Add machine ROMs");
        romPanel.addView(text("Computer to emulate", 14, Color.rgb(180, 193, 204)));
        machinePicker = new Spinner(this);
        String[] machineLabels = new String[MachineProfile.OPTIONS.length];
        for (int i = 0; i < machineLabels.length; i++) {
            machineLabels[i] = MachineProfile.OPTIONS[i].label;
        }
        ArrayAdapter<String> machines = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, machineLabels);
        machines.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        machinePicker.setAdapter(machines);
        machinePicker.setSelection(MachineProfile.indexOf(
                getPreferences(MODE_PRIVATE).getString(LAST_MACHINE, "cocous")));
        romPanel.addView(machinePicker);
        machinePicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                getPreferences(MODE_PRIVATE).edit()
                        .putString(LAST_MACHINE, MachineProfile.OPTIONS[position].id).apply();
                if (status != null) refreshSummaries();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        romSummary = text("", 14, Color.rgb(180, 193, 204));
        romPanel.addView(romSummary, margins(romSummary, 0, 8, 0, 12));
        Button importRoms = button("Import ROM files");
        romPanel.addView(importRoms);
        importRoms.setOnClickListener(view -> pickFiles(PICK_ROMS, true));

        LinearLayout mediaPanel = panel();
        content.addView(mediaPanel, margins(mediaPanel, 0, 0, 0, 16));
        addHeading(mediaPanel, "2. Choose something to load");
        mediaSummary = text("", 14, Color.rgb(180, 193, 204));
        mediaPanel.addView(mediaSummary, margins(mediaSummary, 0, 8, 0, 12));
        Button chooseMedia = button("Choose cartridge, tape, disk, or program");
        mediaPanel.addView(chooseMedia);
        chooseMedia.setOnClickListener(view -> pickFiles(PICK_MEDIA, false));

        TextView help = text(
                "ROM images are not included. Import firmware you are entitled to use. "
                        + "A Bluetooth, USB, or on-screen keyboard can be used after launch.",
                14, Color.rgb(180, 193, 204));
        content.addView(help, margins(help, 2, 2, 2, 18));

        startButton = button("Start XRoar");
        content.addView(startButton, margins(startButton, 0, 0, 0, 12));
        startButton.setOnClickListener(view -> startEmulator());

        status = text("", 14, Color.rgb(121, 229, 210));
        content.addView(status);

        setContentView(scroll);
        SafeArea.apply(scroll, true, true);
    }

    private void pickFiles(int requestCode, boolean allowMultiple) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        List<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                uris.add(data.getClipData().getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        if (uris.isEmpty()) {
            return;
        }

        importDocuments(requestCode, uris);
    }

    private void importDocuments(int requestCode, List<Uri> uris) {
        final boolean roms = requestCode == PICK_ROMS;
        final File targetDirectory = new File(getFilesDir(), roms ? "roms" : "media");
        importing = true;
        startButton.setEnabled(false);
        status.setText(roms ? "Importing firmware…" : "Copying media into app storage…");

        importer.execute(() -> {
            int copied = 0;
            int failed = 0;
            String firstError = null;
            String mediaPath = null;
            for (Uri uri : uris) {
                try {
                    if (!targetDirectory.exists() && !targetDirectory.mkdirs()) {
                        throw new IOException("Could not create " + targetDirectory);
                    }
                    String name = safeFileName(displayName(uri));
                    if (roms && MachineProfile.isCartridge(new File(name))) {
                        throw new IOException(name + " is a game cartridge. Use "
                                + "\"Choose cartridge, tape, disk, or program\" instead.");
                    }
                    File destination = uniqueFile(targetDirectory, name);
                    copyDocument(uri, destination);
                    copied++;
                    if (!roms) {
                        mediaPath = destination.getAbsolutePath();
                    }
                } catch (IOException | SecurityException exception) {
                    failed++;
                    if (firstError == null) {
                        firstError = exception.getMessage() == null
                                ? "The selected file could not be read." : exception.getMessage();
                    }
                }
            }

            final int copiedCount = copied;
            final int failedCount = failed;
            final String importError = firstError;
            final String importedMedia = mediaPath;
            runOnUiThread(() -> {
                importing = false;
                if (importedMedia != null) {
                    selectedMediaPath = importedMedia;
                    getPreferences(MODE_PRIVATE).edit().putString(LAST_MEDIA, importedMedia).apply();
                }
                refreshSummaries();
                if (copiedCount > 0) {
                    status.setText("Imported " + copiedCount + (roms ? " firmware file(s)." : " media file."));
                } else {
                    status.setText("No files were imported. Check that the selected files are accessible.");
                }
                if (failedCount > 0) {
                    status.append("\n" + importError);
                    Toast.makeText(this, failedCount + " file(s) could not be copied.",
                            Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private String displayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) {
                    String name = cursor.getString(column);
                    if (name != null && !name.trim().isEmpty()) {
                        return name;
                    }
                }
            }
        }
        String fallback = uri.getLastPathSegment();
        return fallback == null ? "imported-file" : fallback;
    }

    private String safeFileName(String name) {
        // XRoar's firmware lists include spaces, parentheses and apostrophes.
        // Keep those names intact; remove only path separators and control characters.
        String safe = name.replace('\\', '_').replace('/', '_').replaceAll("\\p{Cntrl}", "_");
        if (safe.length() > 120) {
            safe = safe.substring(safe.length() - 120);
        }
        return safe.isEmpty() || safe.equals(".") || safe.equals("..") ? "imported-file" : safe;
    }

    private File uniqueFile(File directory, String name) {
        File candidate = new File(directory, name);
        if (!candidate.exists()) {
            return candidate;
        }
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        String extension = dot > 0 ? name.substring(dot) : "";
        int suffix = 2;
        do {
            candidate = new File(directory, stem + "_" + suffix++ + extension);
        } while (candidate.exists());
        return candidate;
    }

    private void copyDocument(Uri uri, File destination) throws IOException {
        File stagingDirectory = new File(destination.getParentFile(), ".staging");
        if (!stagingDirectory.exists() && !stagingDirectory.mkdirs()) {
            throw new IOException("Could not create import staging directory.");
        }
        File temporaryFile = File.createTempFile("xroar-import-", ".part", stagingDirectory);
        try {
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                if (input == null) {
                    throw new IOException("The selected document could not be opened.");
                }
                try (FileOutputStream output = new FileOutputStream(temporaryFile)) {
                    byte[] buffer = new byte[16 * 1024];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                    }
                }
            }
            if (!temporaryFile.renameTo(destination)) {
                throw new IOException("Could not finish importing " + destination.getName());
            }
        } catch (IOException | SecurityException exception) {
            temporaryFile.delete();
            throw exception;
        }
    }

    private void startEmulator() {
        File romDirectory = new File(getFilesDir(), "roms");
        MachineProfile profile = selectedMachine();
        String error = profile.validationError(romDirectory);
        if (error != null) {
            status.setText(error);
            return;
        }
        Intent intent = new Intent(this, XRoarActivity.class);
        intent.putExtra(EXTRA_MACHINE, profile.id);
        if (selectedMediaPath != null
                && (!new File(selectedMediaPath).isFile() || new File(selectedMediaPath).length() == 0)) {
            status.setText("The selected program is missing or empty. Choose it again.");
            return;
        }
        if (selectedMediaPath != null && new File(selectedMediaPath).isFile()) {
            intent.putExtra(EXTRA_MEDIA_PATH, selectedMediaPath);
        }
        startActivity(intent);
    }

    private void refreshSummaries() {
        File directory = new File(getFilesDir(), "roms");
        String error = selectedMachine().validationError(directory);
        romSummary.setText(error == null
                ? "Firmware ready for " + selectedMachine().label + "."
                : error);
        mediaSummary.setText(selectedMediaPath != null && new File(selectedMediaPath).isFile()
                ? "Selected: " + new File(selectedMediaPath).getName()
                : "Optional. Choose a .ccc cartridge, tape, disk, or program to start automatically.");
        startButton.setEnabled(error == null && !importing);
    }

    private MachineProfile selectedMachine() {
        int position = Math.max(0, machinePicker.getSelectedItemPosition());
        return MachineProfile.OPTIONS[position];
    }

    private LinearLayout panel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(20, 37, 54));
        background.setCornerRadius(dp(14));
        panel.setBackground(background);
        return panel;
    }

    private void addHeading(LinearLayout parent, String value) {
        TextView heading = text(value, 18, Color.rgb(243, 247, 250));
        heading.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        parent.addView(heading);
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(Color.rgb(9, 19, 31));
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.rgb(121, 229, 210)));
        button.setMinHeight(dp(48));
        return button;
    }

    private LinearLayout.LayoutParams margins(View view, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
