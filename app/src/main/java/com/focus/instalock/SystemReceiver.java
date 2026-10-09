package com.focus.instalock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SystemReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_TIME_CHANGED.equals(action)) {
            LockState.onClockChanged(context);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            LockState.anchor(context);
        }
    }
}
