package org.xroar.android;

import android.content.Context;
import android.widget.LinearLayout;

/** Keep the display in the top half of the full window, even above the IME. */
final class EmulatorLayout extends LinearLayout {
    EmulatorLayout(Context context) {
        super(context);
        setOrientation(VERTICAL);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int fullHeight = MeasureSpec.getSize(heightSpec) - getPaddingTop();
        int usableHeight = fullHeight - getPaddingBottom();
        int controls = Math.round(96 * getResources().getDisplayMetrics().density);
        // On a short window/landscape keyboard, shrink only as much as needed
        // to keep the control rows above the keyboard rather than overlaying.
        getChildAt(0).getLayoutParams().height = Math.max(1,
                Math.min(fullHeight / 2, usableHeight - controls));
        super.onMeasure(widthSpec, heightSpec);
    }
}
