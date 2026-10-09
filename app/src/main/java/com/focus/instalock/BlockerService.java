package com.focus.instalock;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class BlockerService extends AccessibilityService {

    private static final Set<String> INSTAGRAM = new HashSet<>(Arrays.asList(
            "com.instagram.android",
            "com.instagram.lite"
    ));

    private static final String[] PROTECTED_PKG_HINTS = {
            "settings", "packageinstaller", "permissioncontroller", "securitycenter",
            "safecenter", "accessibility", "systemmanager", "devicecare", "lool",
            "iqoo.secure", "phonemanager", "com.android.vending"
    };

    private static final String[] URL_VIEW_HINTS = {"url", "location_bar", "address", "omnibox", "search_box"};

    private static final int MAX_DEPTH = 30;

    private long lastScan;
    private long lastBlock;
    private String appLabel;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        appLabel = getString(R.string.app_name).toLowerCase(Locale.ROOT);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!LockState.isLocked(this)) return;
        CharSequence pkgCs = event.getPackageName();
        if (pkgCs == null) return;
        String pkg = pkgCs.toString();
        if (pkg.equals(getPackageName())) return;

        if (INSTAGRAM.contains(pkg)) {
            block("Instagram can wait.", false);
            return;
        }

        long now = SystemClock.uptimeMillis();
        boolean windowChanged = event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        if (!windowChanged && now - lastScan < 400) return;
        lastScan = now;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        String rootPkg = root.getPackageName() != null ? root.getPackageName().toString() : pkg;

        if (rootPkg.equals(getPackageName())) return;
        if (INSTAGRAM.contains(rootPkg)) {
            block("Instagram can wait.", false);
        } else if (isProtectedSystemApp(rootPkg)) {
            if (appLabel == null) appLabel = getString(R.string.app_name).toLowerCase(Locale.ROOT);
            if (containsText(root, appLabel, 0)) {
                block("These settings are locked.", true);
            }
        } else if (hasInstagramUrl(root, 0)) {
            block("Not in the browser either.", true);
        }
    }

    private static boolean isProtectedSystemApp(String pkg) {
        String p = pkg.toLowerCase(Locale.ROOT);
        for (String hint : PROTECTED_PKG_HINTS) if (p.contains(hint)) return true;
        return false;
    }

    private static boolean containsText(AccessibilityNodeInfo node, String needle, int depth) {
        if (node == null || depth > MAX_DEPTH) return false;
        CharSequence t = node.getText();
        CharSequence d = node.getContentDescription();
        if ((t != null && t.toString().toLowerCase(Locale.ROOT).contains(needle))
                || (d != null && d.toString().toLowerCase(Locale.ROOT).contains(needle))) {
            return true;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            if (containsText(node.getChild(i), needle, depth + 1)) return true;
        }
        return false;
    }

    private static boolean hasInstagramUrl(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > MAX_DEPTH) return false;
        String id = node.getViewIdResourceName();
        CharSequence t = node.getText();
        if (id != null && t != null) {
            String idL = id.toLowerCase(Locale.ROOT);
            for (String hint : URL_VIEW_HINTS) {
                if (idL.contains(hint) && t.toString().toLowerCase(Locale.ROOT).contains("instagram.com")) {
                    return true;
                }
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            if (hasInstagramUrl(node.getChild(i), depth + 1)) return true;
        }
        return false;
    }

    private void block(String message, boolean pressBack) {
        long now = SystemClock.uptimeMillis();
        if (now - lastBlock < 800) return;
        lastBlock = now;

        if (pressBack) performGlobalAction(GLOBAL_ACTION_BACK);
        performGlobalAction(GLOBAL_ACTION_HOME);

        Intent i = new Intent(this, BlockedActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(BlockedActivity.EXTRA_MESSAGE, message);
        try {
            startActivity(i);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onInterrupt() {}
}
