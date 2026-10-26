package org.xroar.android;

/** Android-independent sizing rules shared by the normal and expanded display. */
final class DisplaySizing {
    static int displayHeight(boolean expanded, int fullHeight, int usableHeight,
            int controlsHeight) {
        int available = Math.max(1, usableHeight - controlsHeight);
        return expanded ? available : Math.max(1, Math.min(fullHeight / 2, available));
    }

    final int width, height, left, top;

    DisplaySizing(int availableWidth, int availableHeight) {
        int w = Math.max(1, availableWidth);
        int h = Math.max(1, availableHeight);
        // Integer arithmetic avoids growing past the bounds due to rounding.
        if ((long) w * 3 <= (long) h * 4) {
            width = w;
            height = Math.max(1, w * 3 / 4);
        } else {
            height = h;
            width = Math.max(1, h * 4 / 3);
        }
        left = (w - width) / 2;
        top = (h - height) / 2;
    }
}
