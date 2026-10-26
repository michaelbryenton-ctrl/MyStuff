package org.xroar.android;

import android.graphics.Insets;
import android.os.Build;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;

/** Keep Android system bars, display cutouts and the keyboard clear of controls. */
final class SafeArea {
    private SafeArea() {}

    interface ExpandedState { boolean isExpanded(); }

    static void apply(View view, boolean includeTop, boolean includeKeyboard) {
        apply(view, includeTop, includeKeyboard, () -> false);
    }

    static void apply(View view, boolean includeTop, boolean includeKeyboard,
            ExpandedState state) {
        final int leftPadding = view.getPaddingLeft();
        final int topPadding = view.getPaddingTop();
        final int rightPadding = view.getPaddingRight();
        final int bottomPadding = view.getPaddingBottom();
        view.setOnApplyWindowInsetsListener((target, windowInsets) -> {
            int left;
            int top;
            int right;
            int bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                int types = WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout();
                Insets safe = state.isExpanded() ? windowInsets.getInsets(types)
                        : windowInsets.getInsetsIgnoringVisibility(types);
                left = safe.left;
                top = safe.top;
                right = safe.right;
                bottom = safe.bottom;
                if (includeKeyboard) {
                    bottom = Math.max(bottom,
                            windowInsets.getInsets(WindowInsets.Type.ime()).bottom);
                }
            } else {
                left = windowInsets.getSystemWindowInsetLeft();
                top = windowInsets.getSystemWindowInsetTop();
                right = windowInsets.getSystemWindowInsetRight();
                bottom = windowInsets.getSystemWindowInsetBottom();
                if (Build.VERSION.SDK_INT >= 28) {
                    DisplayCutout cutout = windowInsets.getDisplayCutout();
                    if (cutout != null) {
                        left = Math.max(left, cutout.getSafeInsetLeft());
                        top = Math.max(top, cutout.getSafeInsetTop());
                        right = Math.max(right, cutout.getSafeInsetRight());
                        bottom = Math.max(bottom, cutout.getSafeInsetBottom());
                    }
                }
            }
            target.setPadding(leftPadding + left, topPadding + (includeTop ? top : 0),
                    rightPadding + right, bottomPadding + bottom);
            return windowInsets;
        });
        view.requestApplyInsets();
    }
}
