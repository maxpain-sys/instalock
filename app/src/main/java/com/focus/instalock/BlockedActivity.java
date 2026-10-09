package com.focus.instalock;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Shown whenever something blocked is opened. */
public class BlockedActivity extends Activity {
    public static final String EXTRA_MESSAGE = "msg";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView title, ends;
    private TimerFace face;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (LockState.isLocked(BlockedActivity.this)) {
                face.update(BlockedActivity.this);
                handler.postDelayed(this, 1000);
            } else {
                title.setText("You made it.");
                ends.setText("The block is over. Instagram is unlocked.");
                face.update(BlockedActivity.this);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.styleWindow(this);
        int dp24 = Ui.dp(this, 24);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Ui.BG);
        root.setPadding(dp24, Ui.dp(this, 72), dp24, dp24);

        root.addView(Ui.label(this, "instalock"));
        root.addView(Ui.space(this, 18));

        title = Ui.text(this, "", 30, Ui.TEXT, Ui.light());
        title.setGravity(Gravity.CENTER);
        title.setLetterSpacing(-0.02f);
        root.addView(title, Ui.matchWrap());

        root.addView(Ui.space(this, 44));
        face = new TimerFace(this, 250);
        root.addView(face);
        root.addView(Ui.space(this, 36));

        ends = Ui.text(this, "", 15, Ui.MUTED, Ui.regular());
        ends.setGravity(Gravity.CENTER);
        ends.setLineSpacing(0, 1.3f);
        root.addView(ends, Ui.matchWrap());

        LinearLayout spacer = new LinearLayout(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));

        TextView home = Ui.text(this, "Back to home screen", 16, Ui.TEXT, Ui.medium());
        home.setGravity(Gravity.CENTER);
        home.setBackground(Ui.pressable(Ui.outlined(Color.TRANSPARENT, Ui.LINE, Ui.dp(this, 1), Ui.dp(this, 16)), Ui.dp(this, 16)));
        home.setOnClickListener(v -> goHome());
        root.addView(home, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 56)));

        setContentView(root);
        applyMessage(getIntent());

        root.setAlpha(0f);
        root.animate().alpha(1f).setDuration(260).start();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        applyMessage(intent);
    }

    private void applyMessage(Intent intent) {
        String msg = intent != null ? intent.getStringExtra(EXTRA_MESSAGE) : null;
        title.setText(msg != null ? msg : "Instagram can wait.");
        String end = new SimpleDateFormat("EEEE, h:mm a", Locale.getDefault()).format(new Date(LockState.until(this)));
        ends.setText("You set this block yourself.\nIt ends " + end + ".");
    }

    @Override protected void onResume() { super.onResume(); handler.post(tick); }
    @Override protected void onPause() { super.onPause(); handler.removeCallbacks(tick); }

    @Override
    public void onBackPressed() {
        goHome();
    }

    private void goHome() {
        startActivity(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }
}
