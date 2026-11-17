package com.rawhy.mydayroute;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;

/** Result of a PackageInstaller session started by SelfUpdate. */
public class InstallResultReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirm = Build.VERSION.SDK_INT >= 33
                ? intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class)
                : intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirm != null) SelfUpdate.needsTap(ctx, confirm);
        }
        SelfUpdate.prefs(ctx).edit().putInt("lastStatus", status)
            .putString("lastMsg", String.valueOf(intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))).apply();
    }
}
