package org.xroar.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Android recording/export UI, shared with the library for access after a session ends. */
final class CassetteFiles {
    interface TapeAccess {
        boolean request(String path);
        int status();
    }
    interface Selection { void select(File file); }
    private static final int EXPORT = 7314;
    private static final String RECORDING = "cassette-recording", EXPORT_PATH = "cassette-export";
    private final Activity activity;
    private final SavedTapeStore store;
    private final TapeAccess tape;
    private final Selection selection;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService copier = Executors.newSingleThreadExecutor();
    private File recording;
    private String exportPath;
    private boolean pending, exporting, closed;
    private Runnable poll;

    CassetteFiles(Activity activity, TapeAccess tape, Selection selection, Bundle restored) {
        this.activity = activity;
        this.tape = tape;
        this.selection = selection;
        store = new SavedTapeStore(activity.getFilesDir());
        if (restored != null) {
            exportPath = restored.getString(EXPORT_PATH);
            String path = restored.getString(RECORDING);
            if (tape != null && tape.status() == 2 && path != null) {
                try { recording = store.requireFile(path); }
                catch (IOException error) { message(error.getMessage()); }
            }
        }
    }

    boolean busy() { return pending || exporting || (tape != null && tape.status() == 1); }
    private boolean recording() { return recording != null || (tape != null && tape.status() == 2); }

    void show() {
        if (busy()) { message("Please wait for the cassette operation to finish."); return; }
        if (tape == null) { showSaved(); return; }
        String[] actions = {recording() ? "Finish recording" : "Record new cassette",
                "Saved cassettes / Export"};
        new AlertDialog.Builder(activity).setTitle("Cassette files")
                .setItems(actions, (dialog, which) -> {
                    if (which == 1) showSaved();
                    else if (recording()) confirmFinish(null);
                    else newRecording();
                }).setNegativeButton("Cancel", null).show();
    }

    private void newRecording() {
        EditText name = new EditText(activity);
        name.setSingleLine(true);
        name.setHint("My program");
        new AlertDialog.Builder(activity).setTitle("New cassette recording")
                .setMessage("Choose a file label. Then type CSAVE \"NAME\" in BASIC. "
                        + "Wait for BASIC's prompt to return before finishing the recording. "
                        + "This records emulator cassette output, not the phone microphone.")
                .setView(name).setNegativeButton("Cancel", null)
                .setPositiveButton("Start recording", (dialog, which) -> {
                    try {
                        File file = store.create(name.getText().toString());
                        if (!tape.request(file.getAbsolutePath())) {
                            if (file.length() == 0) file.delete(); // Only our unused reservation.
                            message("Emulator not ready or busy. Try again.");
                            return;
                        }
                        recording = file;
                        waitForTape(true, null);
                    } catch (IOException error) { message(error.getMessage()); }
                }).show();
    }

    private void confirmFinish(Runnable next) {
        new AlertDialog.Builder(activity).setTitle("Finish cassette recording?")
                .setMessage("Wait until CSAVE has completed and BASIC's prompt has returned. "
                        + "Finishing early leaves an incomplete cassette. "
                        + "The local file is kept even if a write error is reported.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Finish recording", (dialog, which) -> finish(next)).show();
    }

    private void finish(Runnable next) {
        if (!tape.request(null)) { message("Could not finish recording. Try again."); return; }
        waitForTape(false, next);
    }

    private void waitForTape(boolean starting, Runnable next) {
        pending = true;
        long began = SystemClock.uptimeMillis();
        poll = new Runnable() {
            private boolean warned;
            @Override public void run() {
                if (closed) return;
                int status = tape.status();
                if (status == 1) {
                    if (!warned && SystemClock.uptimeMillis() - began > 15000) {
                        warned = true;
                        message("Still waiting for the emulator. Return to its screen to continue.");
                    }
                    handler.postDelayed(this, warned ? 500 : 100);
                    return;
                }
                pending = false;
                if (starting && status == 2) {
                    message("Recording ready. Run CSAVE \"NAME\", then Files > Finish recording.");
                    return;
                }
                File finished = recording;
                recording = null;
                if (status != 3 || starting) {
                    message("Cassette operation failed. Any local file is kept for recovery; "
                            + "do not assume it contains a complete program.");
                    return;
                }
                if (finished == null || !finished.isFile() || finished.length() == 0) {
                    message("Recording closed, but no cassette data was saved.");
                    if (next != null) next.run();
                    return;
                }
                message("Cassette closed and kept in Saved cassettes. Export it to keep a backup.");
                if (next != null) next.run();
            }
        };
        handler.post(poll);
    }

    void prepareToLeave(Runnable next) {
        if (busy()) { message("Wait for the cassette operation to finish before leaving."); return; }
        if (recording()) confirmFinish(next);
        else next.run();
    }

    private void showSaved() {
        File[] files = store.list();
        if (files.length == 0) {
            new AlertDialog.Builder(activity).setTitle("Saved cassettes")
                    .setMessage("No saved cassettes yet. In the emulator, use Files > "
                            + "Record new cassette, run CSAVE in BASIC, then finish recording.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++)
            names[i] = files[i].getName() + "\n" + files[i].length() + " bytes"
                    + (files[i].equals(recording) ? " • recording" : "");
        new AlertDialog.Builder(activity).setTitle("Saved cassettes")
                .setItems(names, (dialog, which) -> fileActions(files[which]))
                .setNegativeButton("Cancel", null).show();
    }

    private void fileActions(File file) {
        if (busy()) { message("Please wait for the current operation."); return; }
        if (file.equals(recording) || (recording() && recording == null)) {
            message("Finish recording before exporting or loading this cassette."); return;
        }
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity).setTitle(file.getName())
                .setMessage("Export a copy to an Android folder or document provider. "
                        + "The original stays in the app. Files left by an interrupted recording "
                        + "may be incomplete; test by loading them before relying on them.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Export copy", (ignored, which) -> export(file));
        if (selection != null) dialog.setNeutralButton("Select to load", (ignored, which) -> {
            if (file.length() == 0) { message("This cassette is empty."); return; }
            selection.select(file);
        });
        dialog.show();
    }

    private void export(File file) {
        if (file.length() == 0) { message("The cassette is empty. Nothing was exported."); return; }
        exportPath = file.getAbsolutePath();
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, file.getName());
        try { activity.startActivityForResult(intent, EXPORT); }
        catch (RuntimeException error) {
            exportPath = null;
            message("Could not open Android's file picker. The local cassette is safe.");
        }
    }

    boolean onActivityResult(int request, int result, Intent data) {
        if (request != EXPORT) return false;
        String source = exportPath;
        exportPath = null;
        if (result != Activity.RESULT_OK) {
            message("Export cancelled. The local cassette is unchanged.");
            return true;
        }
        Uri destination = data == null ? null : data.getData();
        if (source == null || destination == null) {
            message("Export destination is missing. The local cassette is unchanged.");
            return true;
        }
        exporting = true;
        copier.execute(() -> {
            String outcome;
            try (OutputStream output = activity.getContentResolver().openOutputStream(destination, "wt")) {
                if (output == null) throw new IOException("The destination could not be opened.");
                store.copyTo(source, output);
                outcome = "Cassette exported. The original remains in Saved cassettes.";
            } catch (IOException | RuntimeException error) {
                outcome = "Export failed: " + error.getMessage()
                        + ". The local original is safe; the destination may contain a partial copy.";
            }
            String report = outcome;
            handler.post(() -> { exporting = false; if (!closed) message(report); });
        });
        return true;
    }

    void saveState(Bundle state) {
        if (recording != null) state.putString(RECORDING, recording.getAbsolutePath());
        if (exportPath != null) state.putString(EXPORT_PATH, exportPath);
    }

    void close() {
        closed = true;
        if (poll != null) handler.removeCallbacks(poll);
        copier.shutdown();
    }

    private void message(String text) { Toast.makeText(activity, text, Toast.LENGTH_LONG).show(); }
}
