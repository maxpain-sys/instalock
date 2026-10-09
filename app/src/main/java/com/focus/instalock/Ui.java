package com.focus.instalock;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int BG = 0xFF0D0D0C;
    static final int SURFACE = 0xFF161615;
    static final int SURFACE_2 = 0xFF201F1D;
    static final int LINE = 0xFF2A2926;
    static final int TEXT = 0xFFEDEBE6;
    static final int MUTED = 0xFF8C8A84;
    static final int FAINT = 0xFF57554F;
    static final int ACCENT = 0xFFFF5A36;
    static final int ON_ACCENT = 0xFF160A05;
    static final int OK = 0xFFA8D58A;

    private Ui() {}

    static Typeface light() { return Typeface.create("sans-serif-light", Typeface.NORMAL); }
    static Typeface regular() { return Typeface.create("sans-serif", Typeface.NORMAL); }
    static Typeface medium() { return Typeface.create("sans-serif-medium", Typeface.NORMAL); }

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static void styleWindow(Activity a) {
        Window w = a.getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        w.getDecorView().setBackgroundColor(BG);
    }

    static GradientDrawable rounded(int fill, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radiusPx);
        return g;
    }

    static GradientDrawable outlined(int fill, int stroke, int strokePx, float radiusPx) {
        GradientDrawable g = rounded(fill, radiusPx);
        g.setStroke(strokePx, stroke);
        return g;
    }

    static Drawable pressable(Drawable content, float radiusPx) {
        return new RippleDrawable(ColorStateList.valueOf(0x1FFFFFFF), content, rounded(Color.WHITE, radiusPx));
    }

    static TextView text(Context c, String s, float sp, int color, Typeface tf) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(tf);
        t.setIncludeFontPadding(false);
        return t;
    }

    static TextView label(Context c, String s) {
        TextView t = text(c, s.toUpperCase(), 11, MUTED, medium());
        t.setLetterSpacing(0.14f);
        return t;
    }

    static View space(Context c, int heightDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, heightDp)));
        return v;
    }

    static View divider(Context c, int insetDp) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.leftMargin = dp(c, insetDp);
        v.setLayoutParams(lp);
        return v;
    }

    static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }
}
