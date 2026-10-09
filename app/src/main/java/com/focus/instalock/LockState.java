package com.focus.instalock;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import java.util.Locale;

public final class LockState {
    private static final String PREFS = "lock";
    private static final String KEY_UNTIL = "until";
    private static final String KEY_START = "start";
    private static final String KEY_ANCHOR_WALL = "aWall";
    private static final String KEY_ANCHOR_ELAPSED = "aElapsed";

    private LockState() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static long until(Context c) {
        return prefs(c).getLong(KEY_UNTIL, 0);
    }

    public static boolean isLocked(Context c) {
        return until(c) > System.currentTimeMillis();
    }

    public static long remaining(Context c) {
        return Math.max(0, until(c) - System.currentTimeMillis());
    }

    public static void start(Context c, long durationMs) {
        long now = System.currentTimeMillis();
        prefs(c).edit().putLong(KEY_START, now).putLong(KEY_UNTIL, now + durationMs).commit();
        anchor(c);
    }

    public static long startTime(Context c) {
        return prefs(c).getLong(KEY_START, 0);
    }

    public static float progress(Context c) {
        long start = startTime(c), until = until(c);
        if (until <= start) return 1f;
        float p = (System.currentTimeMillis() - start) / (float) (until - start);
        return Math.max(0f, Math.min(1f, p));
    }

    public static void anchor(Context c) {
        prefs(c).edit()
                .putLong(KEY_ANCHOR_WALL, System.currentTimeMillis())
                .putLong(KEY_ANCHOR_ELAPSED, SystemClock.elapsedRealtime())
                .commit();
    }

    public static void onClockChanged(Context c) {
        SharedPreferences sp = prefs(c);
        long until = sp.getLong(KEY_UNTIL, 0);
        long aWall = sp.getLong(KEY_ANCHOR_WALL, 0);
        long aElapsed = sp.getLong(KEY_ANCHOR_ELAPSED, -1);
        long nowElapsed = SystemClock.elapsedRealtime();

        if (until > 0 && aWall > 0 && aElapsed >= 0 && nowElapsed >= aElapsed) {
            long expectedWall = aWall + (nowElapsed - aElapsed);
            long jump = System.currentTimeMillis() - expectedWall;
            boolean stillLockedInRealTime = until > expectedWall;
            if (stillLockedInRealTime && Math.abs(jump) > 5000) {
                sp.edit()
                        .putLong(KEY_UNTIL, until + jump)
                        .putLong(KEY_START, sp.getLong(KEY_START, 0) + jump)
                        .commit();
            }
        }
        anchor(c);
    }

    public static String format(long ms) {
        long s = ms / 1000;
        long d = s / 86400, h = (s % 86400) / 3600, m = (s % 3600) / 60, sec = s % 60;
        if (d > 0) return String.format(Locale.US, "%dd %02dh %02dm %02ds", d, h, m, sec);
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, sec);
    }

    public static String describe(long ms) {
        long totalMin = ms / 60000;
        long d = totalMin / 1440, h = (totalMin % 1440) / 60, m = totalMin % 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append(d == 1 ? " day " : " days ");
        if (h > 0) sb.append(h).append(h == 1 ? " hour " : " hours ");
        if (m > 0) sb.append(m).append(m == 1 ? " minute" : " minutes");
        return sb.toString().trim();
    }
}
