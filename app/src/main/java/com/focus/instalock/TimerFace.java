package com.focus.instalock;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Ring + remaining time in the middle. Shared by the main and blocked screens. */
public class TimerFace extends FrameLayout {
    private final RingView ring;
    private final TextView time;
    private final TextView caption;

    public TimerFace(Context c, int sizeDp) {
        super(c);
        int size = Ui.dp(c, sizeDp);
        setLayoutParams(new LinearLayout.LayoutParams(size, size));

        ring = new RingView(c);
        addView(ring, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        LinearLayout center = new LinearLayout(c);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER_HORIZONTAL);

        time = Ui.text(c, "", sizeDp > 240 ? 46 : 40, Ui.TEXT, Ui.light());
        time.setFontFeatureSettings("tnum");
        time.setLetterSpacing(-0.02f);
        time.setGravity(Gravity.CENTER);

        caption = Ui.label(c, "remaining");
        caption.setGravity(Gravity.CENTER);
        caption.setPadding(0, Ui.dp(c, 10), 0, 0);

        center.addView(time);
        center.addView(caption);
        addView(center, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
    }

    public void update(Context c) {
        long rem = LockState.remaining(c);
        ring.setProgress(LockState.progress(c));
        long s = rem / 1000;
        long d = s / 86400, h = (s % 86400) / 3600, m = (s % 3600) / 60, sec = s % 60;
        if (d > 0) {
            time.setText(String.format(java.util.Locale.US, "%dd %02dh", d, h));
            caption.setText(String.format(java.util.Locale.US, "%02d MIN REMAINING", m));
        } else {
            time.setText(String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m, sec));
            caption.setText("REMAINING");
        }
    }
}
