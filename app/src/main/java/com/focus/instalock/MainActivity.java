package com.focus.instalock;

import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final long MIN = 60_000L, HOUR = 60 * MIN, DAY = 24 * HOUR;
    private static final long MAX = 30 * DAY;
    private static final long CUSTOM = -1;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private LinearLayout unlockedView, lockedView;
    private FrameLayout bottomBar;
    private View statusDot;
    private TextView statusText, setupCount, cta, startedValue, endsValue;
    private TextView pillA11y, pillAdmin, pillBattery, pillAutostart;
    private TimerFace face;

    private final List<TextView> chips = new ArrayList<>();
    private final List<Long> chipValues = new ArrayList<>();
    private TextView customChip;
    private long selected = HOUR;
    private long customValue = 0;
    private boolean autostartVisited;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            refresh();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.styleWindow(this);
        autostartVisited = getPreferences(MODE_PRIVATE).getBoolean("autostart", false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(28), dp(24), dp(24));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        content.addView(header());
        content.addView(Ui.space(this, 44));

        unlockedView = vertical();
        buildUnlocked(unlockedView);
        content.addView(unlockedView);

        lockedView = vertical();
        buildLocked(lockedView);
        content.addView(lockedView);

        bottomBar = new FrameLayout(this);
        bottomBar.setPadding(dp(24), dp(12), dp(24), dp(24));
        cta = Ui.text(this, "", 16, Ui.ON_ACCENT, Ui.medium());
        cta.setGravity(Gravity.CENTER);
        cta.setOnClickListener(v -> onCtaClick());
        bottomBar.addView(cta, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));
        root.addView(bottomBar);

        setContentView(root);

        content.setAlpha(0f);
        content.setTranslationY(dp(12));
        content.animate().alpha(1f).translationY(0).setDuration(420).start();
    }

    @Override protected void onResume() { super.onResume(); handler.post(tick); }
    @Override protected void onPause() { super.onPause(); handler.removeCallbacks(tick); }

    // ------------------------------------------------------------------ layout

    private View header() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView mark = Ui.text(this, "instalock", 17, Ui.TEXT, Ui.medium());
        mark.setLetterSpacing(-0.01f);
        row.addView(mark, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout status = new LinearLayout(this);
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setPadding(dp(12), dp(7), dp(12), dp(7));
        status.setBackground(Ui.outlined(Color.TRANSPARENT, Ui.LINE, dp(1), dp(20)));
        statusDot = new View(this);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(6), dp(6));
        dlp.rightMargin = dp(8);
        status.addView(statusDot, dlp);
        statusText = Ui.text(this, "", 12, Ui.MUTED, Ui.medium());
        status.addView(statusText);
        row.addView(status);
        return row;
    }

    private void buildUnlocked(LinearLayout v) {
        TextView h1 = Ui.text(this, "Take a break\nfrom Instagram.", 38, Ui.TEXT, Ui.light());
        h1.setLineSpacing(0, 1.08f);
        h1.setLetterSpacing(-0.02f);
        v.addView(h1);
        v.addView(Ui.space(this, 14));
        TextView sub = Ui.text(this, "Pick a length. Once it starts, it can\u2019t be stopped \u2014 not even by you.", 15, Ui.MUTED, Ui.regular());
        sub.setLineSpacing(0, 1.3f);
        v.addView(sub);

        v.addView(Ui.space(this, 40));
        v.addView(Ui.label(this, "Duration"));
        v.addView(Ui.space(this, 14));
        v.addView(chipRow(new String[]{"30 min", "1 hour", "3 hours"}, new long[]{30 * MIN, HOUR, 3 * HOUR}));
        v.addView(Ui.space(this, 8));
        v.addView(chipRow(new String[]{"1 day", "3 days", "7 days"}, new long[]{DAY, 3 * DAY, 7 * DAY}));
        v.addView(Ui.space(this, 8));
        customChip = chip("Custom", CUSTOM);
        v.addView(customChip, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        v.addView(Ui.space(this, 40));
        LinearLayout labelRow = new LinearLayout(this);
        labelRow.addView(Ui.label(this, "Setup"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        setupCount = Ui.label(this, "");
        labelRow.addView(setupCount);
        v.addView(labelRow);
        v.addView(Ui.space(this, 14));

        LinearLayout list = vertical();
        list.setBackground(Ui.rounded(Ui.SURFACE, dp(18)));
        pillA11y = setupRow(list, "01", "Accessibility", "Detects Instagram. Required.",
                x -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        list.addView(Ui.divider(this, 60));
        pillAdmin = setupRow(list, "02", "Device admin", "Makes uninstalling harder.", x -> requestAdmin());
        list.addView(Ui.divider(this, 60));
        pillBattery = setupRow(list, "03", "Battery", "Keeps it running in the background.", x -> requestBattery());
        list.addView(Ui.divider(this, 60));
        pillAutostart = setupRow(list, "04", "Autostart", "Xiaomi, Oppo, Vivo, Realme, Huawei.", x -> openAutostart());
        v.addView(list);

        if (Build.VERSION.SDK_INT >= 33) {
            v.addView(Ui.space(this, 14));
            TextView help = Ui.text(this, "Accessibility switch greyed out?  Open App info \u2192 \u22EE \u2192 Allow restricted settings", 13, Ui.MUTED, Ui.regular());
            help.setLineSpacing(0, 1.3f);
            help.setPadding(dp(4), 0, dp(4), 0);
            help.setOnClickListener(x -> openAppInfo());
            v.addView(help);
        }
        v.addView(Ui.space(this, 8));
    }

    private void buildLocked(LinearLayout v) {
        v.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView h = Ui.text(this, "Instagram is on pause.", 26, Ui.TEXT, Ui.light());
        h.setGravity(Gravity.CENTER);
        v.addView(h, Ui.matchWrap());
        v.addView(Ui.space(this, 36));

        face = new TimerFace(this, 270);
        v.addView(face);
        v.addView(Ui.space(this, 40));

        LinearLayout stats = new LinearLayout(this);
        stats.setBackground(Ui.rounded(Ui.SURFACE, dp(18)));
        stats.setPadding(dp(20), dp(18), dp(20), dp(18));
        LinearLayout a = vertical(), b = vertical();
        a.addView(Ui.label(this, "Started"));
        startedValue = Ui.text(this, "", 16, Ui.TEXT, Ui.regular());
        startedValue.setPadding(0, dp(8), 0, 0);
        a.addView(startedValue);
        b.addView(Ui.label(this, "Ends"));
        endsValue = Ui.text(this, "", 16, Ui.TEXT, Ui.regular());
        endsValue.setPadding(0, dp(8), 0, 0);
        b.addView(endsValue);
        View sep = new View(this);
        sep.setBackgroundColor(Ui.LINE);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT);
        sl.leftMargin = dp(16);
        sl.rightMargin = dp(16);
        stats.addView(a, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        stats.addView(sep, sl);
        stats.addView(b, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        v.addView(stats, Ui.matchWrap());

        v.addView(Ui.space(this, 18));
        TextView note = Ui.text(this, "Settings, uninstall and the clock are locked until then.", 13, Ui.MUTED, Ui.regular());
        note.setGravity(Gravity.CENTER);
        v.addView(note, Ui.matchWrap());
    }

    private LinearLayout chipRow(String[] labels, long[] values) {
        LinearLayout row = new LinearLayout(this);
        for (int i = 0; i < labels.length; i++) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(52), 1f);
            if (i > 0) lp.leftMargin = dp(8);
            row.addView(chip(labels[i], values[i]), lp);
        }
        return row;
    }

    private TextView chip(String label, long value) {
        TextView c = Ui.text(this, label, 15, Ui.TEXT, Ui.medium());
        c.setGravity(Gravity.CENTER);
        c.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            if (value == CUSTOM) openCustomPicker();
            else { selected = value; refresh(); }
        });
        chips.add(c);
        chipValues.add(value);
        return c;
    }

    private TextView setupRow(LinearLayout list, String idx, String title, String sub, View.OnClickListener l) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), dp(16), dp(16), dp(16));
        row.setBackground(Ui.pressable(new ColorDrawable(Color.TRANSPARENT), dp(18)));
        row.setOnClickListener(l);

        TextView n = Ui.text(this, idx, 13, Ui.FAINT, Ui.medium());
        n.setFontFeatureSettings("tnum");
        row.addView(n, new LinearLayout.LayoutParams(dp(42), ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout texts = vertical();
        texts.addView(Ui.text(this, title, 16, Ui.TEXT, Ui.regular()));
        TextView s = Ui.text(this, sub, 13, Ui.MUTED, Ui.regular());
        s.setPadding(0, dp(5), 0, 0);
        texts.addView(s);
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView pill = Ui.text(this, "", 12, Ui.TEXT, Ui.medium());
        pill.setPadding(dp(12), dp(7), dp(12), dp(7));
        row.addView(pill);
        list.addView(row);
        return pill;
    }

    // ------------------------------------------------------------------ state

    private void refresh() {
        boolean locked = LockState.isLocked(this);
        unlockedView.setVisibility(locked ? View.GONE : View.VISIBLE);
        lockedView.setVisibility(locked ? View.VISIBLE : View.GONE);
        bottomBar.setVisibility(locked ? View.GONE : View.VISIBLE);

        boolean a11y = isAccessibilityOn(), admin = isAdminOn(), battery = isBatteryIgnored();
        int done = (a11y ? 1 : 0) + (admin ? 1 : 0) + (battery ? 1 : 0) + (autostartVisited ? 1 : 0);

        if (locked) {
            setStatus(Ui.ACCENT, "Active");
            face.update(this);
            SimpleDateFormat fmt = new SimpleDateFormat("EEE, h:mm a", Locale.getDefault());
            startedValue.setText(fmt.format(new Date(LockState.startTime(this))));
            endsValue.setText(fmt.format(new Date(LockState.until(this))));
            return;
        }

        setStatus(a11y ? Ui.OK : Ui.FAINT, a11y ? "Ready" : "Setup needed");
        setupCount.setText(done + " of 4");
        setPill(pillA11y, a11y ? 1 : 0);
        setPill(pillAdmin, admin ? 1 : 0);
        setPill(pillBattery, battery ? 1 : 0);
        setPill(pillAutostart, autostartVisited ? 1 : 2);

        for (int i = 0; i < chips.size(); i++) {
            TextView c = chips.get(i);
            long val = chipValues.get(i);
            boolean on = val == CUSTOM ? selected == customValue && !isPreset(selected) : val == selected;
            c.setBackground(Ui.pressable(Ui.rounded(on ? Ui.TEXT : Ui.SURFACE, dp(14)), dp(14)));
            c.setTextColor(on ? Ui.BG : Ui.TEXT);
        }
        customChip.setText(customValue > 0 && !isPreset(customValue)
                ? "Custom \u00B7 " + LockState.describe(customValue) : "Custom");

        if (a11y) {
            cta.setText("Block for " + LockState.describe(selected));
            cta.setTextColor(Ui.ON_ACCENT);
            cta.setBackground(Ui.pressable(Ui.rounded(Ui.ACCENT, dp(16)), dp(16)));
        } else {
            cta.setText("Turn on Accessibility to start");
            cta.setTextColor(Ui.MUTED);
            cta.setBackground(Ui.pressable(Ui.rounded(Ui.SURFACE_2, dp(16)), dp(16)));
        }
    }

    private boolean isPreset(long v) {
        for (long p : chipValues) if (p == v) return true;
        return false;
    }

    private void setStatus(int color, String text) {
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(color);
        statusDot.setBackground(dot);
        statusText.setText(text);
    }

    /** 0 = action needed, 1 = done, 2 = optional/unknown. */
    private void setPill(TextView p, int state) {
        if (state == 1) {
            p.setText("On");
            p.setTextColor(Ui.OK);
            p.setBackground(Ui.rounded(0x1AA8D58A, dp(20)));
        } else if (state == 0) {
            p.setText("Turn on");
            p.setTextColor(Ui.ACCENT);
            p.setBackground(Ui.outlined(Color.TRANSPARENT, 0x66FF5A36, dp(1), dp(20)));
        } else {
            p.setText("Open");
            p.setTextColor(Ui.MUTED);
            p.setBackground(Ui.outlined(Color.TRANSPARENT, Ui.LINE, dp(1), dp(20)));
        }
    }

    private void onCtaClick() {
        if (!isAccessibilityOn()) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            return;
        }
        openConsent(selected);
    }

    // ------------------------------------------------------------------ sheets

    private Dialog sheet(LinearLayout body) {
        LinearLayout wrap = vertical();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Ui.SURFACE);
        float r = dp(28);
        bg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        wrap.setBackground(bg);
        wrap.setPadding(dp(24), dp(12), dp(24), dp(28));

        View handle = new View(this);
        handle.setBackground(Ui.rounded(Ui.FAINT, dp(2)));
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(dp(36), dp(4));
        hl.gravity = Gravity.CENTER_HORIZONTAL;
        hl.bottomMargin = dp(24);
        wrap.addView(handle, hl);
        wrap.addView(body, Ui.matchWrap());

        Dialog d = new Dialog(this, android.R.style.Theme_Material_Dialog);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(wrap);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.BOTTOM);
            w.setDimAmount(0.7f);
            w.setWindowAnimations(android.R.style.Animation_InputMethod);
            w.setNavigationBarColor(Ui.SURFACE);
        }
        return d;
    }

    private void openConsent(long duration) {
        String len = LockState.describe(duration);
        LinearLayout body = vertical();

        body.addView(Ui.label(this, "Confirm"));
        body.addView(Ui.space(this, 10));
        TextView title = Ui.text(this, "Block Instagram\nfor " + len + "?", 28, Ui.TEXT, Ui.light());
        title.setLineSpacing(0, 1.08f);
        body.addView(title);
        body.addView(Ui.space(this, 22));

        String[] points = {
                "The app and instagram.com won\u2019t open.",
                "The timer can\u2019t be stopped or shortened.",
                "InstaLock\u2019s settings and uninstall are locked.",
                "Changing the clock won\u2019t end it early."
        };
        for (String p : points) body.addView(point(p));

        if (!isAdminOn() || !isBatteryIgnored()) {
            body.addView(Ui.space(this, 6));
            TextView warn = Ui.text(this, "Some recommended setup is off, so the lock will be easier to get around.", 13, 0xFFE0A37A, Ui.regular());
            warn.setLineSpacing(0, 1.3f);
            warn.setPadding(dp(14), dp(12), dp(14), dp(12));
            warn.setBackground(Ui.rounded(0x14FF5A36, dp(12)));
            body.addView(warn, Ui.matchWrap());
        }

        body.addView(Ui.space(this, 22));

        HoldButton hold = new HoldButton(this);
        hold.setLabels("Hold to start", "Tick the box above first");
        hold.setEnabled(false);

        LinearLayout agree = new LinearLayout(this);
        agree.setGravity(Gravity.CENTER_VERTICAL);
        agree.setPadding(0, dp(6), 0, dp(6));
        TextView box = Ui.text(this, "", 14, Ui.ON_ACCENT, Ui.medium());
        box.setGravity(Gravity.CENTER);
        TextView agreeText = Ui.text(this, "I understand this can\u2019t be undone.", 15, Ui.TEXT, Ui.regular());
        agree.addView(box, new LinearLayout.LayoutParams(dp(22), dp(22)));
        LinearLayout.LayoutParams atl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        atl.leftMargin = dp(14);
        agree.addView(agreeText, atl);
        final boolean[] checked = {false};
        Runnable paintBox = () -> {
            box.setText(checked[0] ? "\u2713" : "");
            box.setBackground(checked[0] ? Ui.rounded(Ui.ACCENT, dp(6))
                    : Ui.outlined(Color.TRANSPARENT, Ui.FAINT, dp(1.5f), dp(6)));
        };
        paintBox.run();
        agree.setOnClickListener(v -> {
            checked[0] = !checked[0];
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            paintBox.run();
            hold.setEnabled(checked[0]);
        });
        body.addView(agree, Ui.matchWrap());
        body.addView(Ui.space(this, 18));
        body.addView(hold, Ui.matchWrap());

        TextView cancel = Ui.text(this, "Not now", 15, Ui.MUTED, Ui.medium());
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(0, dp(18), 0, dp(4));
        body.addView(cancel, Ui.matchWrap());

        Dialog d = sheet(body);
        cancel.setOnClickListener(v -> d.dismiss());
        hold.setOnComplete(() -> {
            LockState.start(this, duration);
            d.dismiss();
            refresh();
        });
        d.show();
    }

    private View point(String s) {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, 0, 0, dp(14));
        View dash = new View(this);
        dash.setBackgroundColor(Ui.ACCENT);
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(dp(10), dp(1.5f));
        dl.topMargin = dp(10);
        dl.rightMargin = dp(14);
        row.addView(dash, dl);
        TextView t = Ui.text(this, s, 15, Ui.MUTED, Ui.regular());
        t.setLineSpacing(0, 1.3f);
        row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private void openCustomPicker() {
        LinearLayout body = vertical();
        body.addView(Ui.label(this, "Custom length"));
        body.addView(Ui.space(this, 10));
        body.addView(Ui.text(this, "How long?", 28, Ui.TEXT, Ui.light()));
        body.addView(Ui.space(this, 16));

        long base = customValue > 0 ? customValue : selected;
        NumberPicker days = picker(0, 30, (int) (base / DAY));
        NumberPicker hours = picker(0, 23, (int) ((base % DAY) / HOUR));
        NumberPicker mins = picker(0, 59, (int) ((base % HOUR) / MIN));

        LinearLayout row = new LinearLayout(this);
        row.addView(pickerColumn(days, "days"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(pickerColumn(hours, "hours"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(pickerColumn(mins, "minutes"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        body.addView(row, Ui.matchWrap());
        body.addView(Ui.space(this, 22));

        TextView done = Ui.text(this, "Use this length", 16, Ui.BG, Ui.medium());
        done.setGravity(Gravity.CENTER);
        done.setBackground(Ui.pressable(Ui.rounded(Ui.TEXT, dp(16)), dp(16)));
        body.addView(done, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        Dialog d = sheet(body);
        done.setOnClickListener(v -> {
            long ms = days.getValue() * DAY + hours.getValue() * HOUR + mins.getValue() * MIN;
            if (ms < MIN) { toast("Pick at least 1 minute"); return; }
            if (ms > MAX) { toast("The longest block is 30 days"); return; }
            customValue = ms;
            selected = ms;
            d.dismiss();
            refresh();
        });
        d.show();
    }

    private NumberPicker picker(int min, int max, int value) {
        NumberPicker p = new NumberPicker(this);
        p.setMinValue(min);
        p.setMaxValue(max);
        p.setValue(Math.max(min, Math.min(max, value)));
        p.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        if (Build.VERSION.SDK_INT >= 29) {
            p.setTextColor(Ui.TEXT);
            p.setSelectionDividerHeight(dp(1));
        }
        return p;
    }

    private View pickerColumn(NumberPicker p, String label) {
        LinearLayout col = vertical();
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.addView(p);
        TextView l = Ui.label(this, label);
        l.setPadding(0, dp(6), 0, 0);
        col.addView(l);
        return col;
    }

    // ------------------------------------------------------------------ permissions

    private boolean isAccessibilityOn() {
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        ComponentName cn = new ComponentName(this, BlockerService.class);
        return enabled.contains(cn.flattenToString()) || enabled.contains(cn.flattenToShortString());
    }

    private boolean isAdminOn() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        return dpm != null && dpm.isAdminActive(new ComponentName(this, AdminReceiver.class));
    }

    private boolean isBatteryIgnored() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private void requestAdmin() {
        if (isAdminOn()) { toast("Already on"); return; }
        startActivity(new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(this, AdminReceiver.class))
                .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Prevents InstaLock from being uninstalled easily while a block is running."));
    }

    private void requestBattery() {
        if (isBatteryIgnored()) { toast("Already on"); return; }
        try {
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        }
    }

    private void openAppInfo() {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName())));
    }

    private void openAutostart() {
        autostartVisited = true;
        getPreferences(MODE_PRIVATE).edit().putBoolean("autostart", true).apply();
        ComponentName[] targets = {
                new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
                new ComponentName("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity"),
                new ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
                new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
                new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
                new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"),
                new ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.MainActivity"),
                new ComponentName("com.transsion.phonemaster", "com.cyin.himgr.autostart.AutoStartActivity"),
        };
        for (ComponentName cn : targets) {
            try {
                startActivity(new Intent().setComponent(cn).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                toast("Find InstaLock and turn Autostart on");
                return;
            } catch (Exception ignored) {
                // Not this manufacturer.
            }
        }
        toast("No Autostart page on this phone \u2014 set Battery to Unrestricted instead");
        openAppInfo();
    }

    // ------------------------------------------------------------------ helpers

    private int dp(float v) { return Ui.dp(this, v); }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
