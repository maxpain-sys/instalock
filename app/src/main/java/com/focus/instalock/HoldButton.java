package com.focus.instalock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Press-and-hold button: the fill grows while held and fires when full. */
public class HoldButton extends View {
    private static final long HOLD_MS = 1600;

    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textLight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textDark = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path clip = new Path();
    private final float radius;

    private float progress;
    private ValueAnimator anim;
    private boolean fired;
    private String label = "Hold to start";
    private String disabledLabel = "Tick the box to continue";
    private Runnable onComplete;

    public HoldButton(Context c) {
        super(c);
        radius = Ui.dp(c, 16);
        bg.setColor(Ui.SURFACE_2);
        fill.setColor(Ui.ACCENT);
        float ts = 16 * getResources().getDisplayMetrics().scaledDensity;
        textLight.setTextSize(ts);
        textLight.setTypeface(Ui.medium());
        textLight.setTextAlign(Paint.Align.CENTER);
        textLight.setColor(Ui.TEXT);
        textDark.set(textLight);
        textDark.setColor(Ui.ON_ACCENT);
        setClickable(true);
    }

    public void setOnComplete(Runnable r) { onComplete = r; }

    public void setLabels(String enabled, String disabled) {
        label = enabled;
        disabledLabel = disabled;
        invalidate();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) animateTo(0f, 150);
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), w), Ui.dp(getContext(), 58));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        rect.set(0, 0, w, h);
        clip.reset();
        clip.addRoundRect(rect, radius, radius, Path.Direction.CW);

        bg.setAlpha(isEnabled() ? 255 : 140);
        canvas.drawRoundRect(rect, radius, radius, bg);

        String t = isEnabled() ? label : disabledLabel;
        float ty = h / 2f - (textLight.descent() + textLight.ascent()) / 2f;
        textLight.setColor(isEnabled() ? Ui.TEXT : Ui.FAINT);
        canvas.drawText(t, w / 2f, ty, textLight);

        if (progress > 0f) {
            canvas.save();
            canvas.clipPath(clip);
            canvas.clipRect(0, 0, w * progress, h);
            canvas.drawRect(0, 0, w, h, fill);
            canvas.drawText(t, w / 2f, ty, textDark);
            canvas.restore();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!isEnabled() || fired) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                animateTo(1f, (long) ((1f - progress) * HOLD_MS));
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (progress < 1f) animateTo(0f, (long) (progress * 350));
                return true;
            default:
                return true;
        }
    }

    private void animateTo(float target, long duration) {
        if (anim != null) anim.cancel();
        anim = ValueAnimator.ofFloat(progress, target);
        anim.setDuration(Math.max(duration, 1));
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        if (target >= 1f) {
            anim.addListener(new AnimatorListenerAdapter() {
                private boolean cancelled;
                @Override public void onAnimationCancel(Animator a) { cancelled = true; }
                @Override public void onAnimationEnd(Animator a) {
                    if (!cancelled && !fired) {
                        fired = true;
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        if (onComplete != null) onComplete.run();
                    }
                }
            });
        }
        anim.start();
    }
}
