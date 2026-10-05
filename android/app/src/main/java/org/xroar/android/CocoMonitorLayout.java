package org.xroar.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;

/** Decorative receiver cabinet with a real SDL surface in its CRT opening. */
final class CocoMonitorLayout extends FrameLayout {
    private final Drawable cabinet;
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private MonitorGeometry geometry;

    CocoMonitorLayout(Context context) {
        super(context);
        cabinet = context.getDrawable(R.drawable.coco_monitor);
        setBackgroundColor(Color.BLACK);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(width, height);
        geometry = new MonitorGeometry(width, height);
        View screen = getChildAt(0);
        if (screen != null) screen.measure(
                MeasureSpec.makeMeasureSpec(geometry.screenWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(geometry.screenHeight, MeasureSpec.EXACTLY));
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (geometry == null) return;
        View screen = getChildAt(0);
        if (screen != null) screen.layout(geometry.screenLeft, geometry.screenTop,
                geometry.screenLeft + geometry.screenWidth,
                geometry.screenTop + geometry.screenHeight);
    }

    @Override protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (geometry == null) return;
        // Draw above SurfaceView so the rounded mask covers only the CRT
        // corners, never the picture itself. No scanlines or tint are applied.
        cabinet.setBounds(geometry.left, geometry.top,
                geometry.left + geometry.width, geometry.top + geometry.height);
        cabinet.draw(canvas);
        canvas.save();
        canvas.translate(geometry.left, geometry.top);
        canvas.scale(geometry.width / 1000f, geometry.height / 770f);
        label.setTextAlign(Paint.Align.CENTER);
        label.setColor(Color.rgb(211, 75, 55));
        label.setTextSize(28);
        canvas.drawText("03", 853, 151, label);
        label.setColor(Color.rgb(31, 37, 39));
        label.setTextSize(18);
        canvas.drawText("TRS-80", 879, 187, label);
        label.setColor(Color.rgb(200, 207, 199));
        label.setTextSize(13);
        canvas.drawText("COLOR VIDEO", 880, 213, label);
        canvas.restore();
    }
}
