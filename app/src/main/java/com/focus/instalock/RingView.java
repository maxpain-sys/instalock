package com.focus.instalock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Watch-face style progress ring: 60 ticks + a thin progress arc. */
public class RingView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickMajor = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private float progress;

    public RingView(Context c) {
        super(c);
        float stroke = Ui.dp(c, 3);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(stroke);
        track.setColor(Ui.SURFACE_2);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(stroke);
        arc.setStrokeCap(Paint.Cap.ROUND);
        arc.setColor(Ui.ACCENT);
        tick.setStrokeWidth(Ui.dp(c, 1));
        tick.setColor(Ui.LINE);
        tickMajor.setStrokeWidth(Ui.dp(c, 1.5f));
        tickMajor.setColor(Ui.FAINT);
    }

    /** 0..1 elapsed fraction. */
    public void setProgress(float p) {
        progress = Math.max(0f, Math.min(1f, p));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float size = Math.min(w, h);
        float cx = w / 2f, cy = h / 2f;
        float pad = arc.getStrokeWidth();
        float r = size / 2f - pad;
        oval.set(cx - r, cy - r, cx + r, cy + r);

        canvas.drawArc(oval, 0, 360, false, track);
        if (progress > 0f) canvas.drawArc(oval, -90, 360f * progress, false, arc);

        // ticks
        float outer = r - Ui.dp(getContext(), 14);
        for (int i = 0; i < 60; i++) {
            boolean major = i % 5 == 0;
            float len = Ui.dp(getContext(), major ? 10 : 5);
            double a = Math.toRadians(i * 6 - 90);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            canvas.drawLine(cx + cos * outer, cy + sin * outer,
                    cx + cos * (outer - len), cy + sin * (outer - len),
                    major ? tickMajor : tick);
        }
    }
}
