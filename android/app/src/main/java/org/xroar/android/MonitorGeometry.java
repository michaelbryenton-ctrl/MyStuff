package org.xroar.android;

/** TV artwork coordinates; the transparent CRT opening has a 4:3 ratio. */
final class MonitorGeometry {
    final int left, top, width, height;
    final int screenLeft, screenTop, screenWidth, screenHeight;

    MonitorGeometry(int availableWidth, int availableHeight) {
        float scale = Math.min(availableWidth / 1000f, availableHeight / 770f);
        width = Math.max(1, Math.round(1000 * scale));
        height = Math.max(1, Math.round(770 * scale));
        left = (availableWidth - width) / 2;
        top = (availableHeight - height) / 2;
        screenLeft = left + Math.round(95 * scale);
        screenTop = top + Math.round(127 * scale);
        screenWidth = Math.max(1, Math.round(660 * scale));
        screenHeight = Math.max(1, Math.round(495 * scale));
    }
}
