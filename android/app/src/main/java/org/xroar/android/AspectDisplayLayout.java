package org.xroar.android;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.FrameLayout;

/** Fit the live surface inside a black 4:3 frame without stretching or cropping. */
class AspectDisplayLayout extends FrameLayout {
    private DisplaySizing picture;

    AspectDisplayLayout(Context context) {
        super(context);
        setBackgroundColor(Color.BLACK);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(width, height);
        picture = new DisplaySizing(width, height);
        View surface = getChildAt(0);
        if (surface != null) surface.measure(
                MeasureSpec.makeMeasureSpec(picture.width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(picture.height, MeasureSpec.EXACTLY));
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View surface = getChildAt(0);
        if (surface != null && picture != null) surface.layout(
                picture.left, picture.top, picture.left + picture.width,
                picture.top + picture.height);
    }
}
