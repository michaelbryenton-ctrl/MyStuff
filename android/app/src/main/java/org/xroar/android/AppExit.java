package org.xroar.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;

/** Close the shared task from the library process, not just the SDL activity. */
final class AppExit {
    static final String EXTRA_EXIT = "org.xroar.android.EXIT_APP";

    private AppExit() {}

    static boolean handleRequest(Activity library, Intent intent) {
        if (intent == null || !intent.getBooleanExtra(EXTRA_EXIT, false)) return false;
        intent.removeExtra(EXTRA_EXIT);
        library.finishAndRemoveTask();
        return true;
    }

    static void confirm(Activity activity, Runnable beforeExit) {
        new AlertDialog.Builder(activity)
                .setTitle("Exit XRoar?")
                .setMessage("Any running emulation will stop. Imported ROMs and media will be kept.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Exit app", (dialog, which) -> {
                    beforeExit.run();
                    if (activity instanceof LauncherActivity) {
                        activity.finishAndRemoveTask();
                    } else {
                        Intent library = new Intent(activity, LauncherActivity.class);
                        library.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        library.putExtra(EXTRA_EXIT, true);
                        activity.startActivity(library);
                        activity.finish();
                    }
                })
                .show();
    }
}
