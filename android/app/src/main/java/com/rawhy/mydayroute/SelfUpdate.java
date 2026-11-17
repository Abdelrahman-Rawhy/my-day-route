package com.rawhy.mydayroute;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.os.Build;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

/**
 * Background self-update, like a store: checks update/version.json, downloads the new APK and installs it with
 * PackageInstaller. On Android 12+ updates after the first one install with no prompt (this app becomes the
 * installer of record). When Android still needs a tap, the user gets a notification (or the screen, if open).
 */
public final class SelfUpdate {
    private SelfUpdate() {}

    static final String PREFS = "self_update";
    static final String APK_URL = "https://github.com/Abdelrahman-Rawhy/my-day-route/releases/latest/download/MyDayRoute.apk";
    static final String VERSION_URL = "https://api.github.com/repos/Abdelrahman-Rawhy/my-day-route/contents/update/version.json?ref=main";
    static final int JOB_ID = 7301;
    static volatile boolean foreground = false;

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean autoOn(Context c) {
        return prefs(c).getBoolean("auto", true);
    }

    static long installedCode(Context c) {
        try {
            PackageInfo p = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return Build.VERSION.SDK_INT >= 28 ? p.getLongVersionCode() : p.versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    static File apkFile(Context c) {
        File dir = new File(c.getCacheDir(), "apk");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "Yawmi.apk");
    }

    /** Every ~6 hours with a network connection; survives reboots. */
    static void schedule(Context c) {
        JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (js == null) return;
        if (!autoOn(c)) {
            js.cancel(JOB_ID);
            return;
        }
        for (JobInfo j : js.getAllPendingJobs()) if (j.getId() == JOB_ID) return;
        JobInfo.Builder b = new JobInfo.Builder(JOB_ID, new ComponentName(c, UpdateJob.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(6 * 60 * 60 * 1000L)
            .setPersisted(true);
        if (Build.VERSION.SDK_INT >= 26) b.setRequiresBatteryNotLow(true);
        try {
            js.schedule(b.build());
        } catch (Exception ignored) {
        }
    }

    /** The build number that brought native changes (version.json "apk"), or 0. */
    static int latestApk() throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(VERSION_URL + "&t=" + System.currentTimeMillis()).openConnection();
        con.setRequestProperty("Accept", "application/vnd.github.raw");
        con.setConnectTimeout(15000);
        con.setReadTimeout(20000);
        try (InputStream in = con.getInputStream()) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return new JSONObject(bo.toString("UTF-8")).optInt("apk", 0);
        } finally {
            con.disconnect();
        }
    }

    interface Progress {
        void on(int pct, long got);
    }

    /** Downloads the APK (following GitHub's redirect by hand). */
    static void download(Context c, String url, Progress p) throws Exception {
        HttpURLConnection con = null;
        try {
            String u = url;
            for (int hop = 0; hop < 6; hop++) {
                con = (HttpURLConnection) new URL(u).openConnection();
                con.setInstanceFollowRedirects(false);
                con.setConnectTimeout(20000);
                con.setReadTimeout(60000);
                int st = con.getResponseCode();
                if (st >= 300 && st < 400 && con.getHeaderField("Location") != null) {
                    u = new URL(new URL(u), con.getHeaderField("Location")).toString();
                    con.disconnect();
                    continue;
                }
                if (st != 200) throw new Exception("HTTP " + st);
                break;
            }
            long total = con.getContentLengthLong(), got = 0;
            File out = apkFile(c), tmp = new File(out.getPath() + ".part");
            int last = -1;
            try (InputStream in = con.getInputStream(); FileOutputStream fo = new FileOutputStream(tmp)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    fo.write(buf, 0, n);
                    got += n;
                    int pct = total > 0 ? (int) (got * 100 / total) : -1;
                    if (p != null && pct != last) {
                        last = pct;
                        p.on(pct, got);
                    }
                }
            }
            if (got < 1000000 || (total > 0 && got != total)) throw new Exception("incomplete download");
            if (out.exists()) out.delete();
            if (!tmp.renameTo(out)) throw new Exception("could not save");
        } finally {
            if (con != null) con.disconnect();
        }
    }

    /** Hands the downloaded APK to Android's installer as an update of this app. */
    static void install(Context c) throws Exception {
        File f = apkFile(c);
        if (!f.exists()) throw new Exception("no file");
        PackageInstaller pi = c.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams sp = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        sp.setAppPackageName(c.getPackageName());
        sp.setSize(f.length());
        if (Build.VERSION.SDK_INT >= 31) sp.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
        int id = pi.createSession(sp);
        try (PackageInstaller.Session s = pi.openSession(id)) {
            try (InputStream in = new FileInputStream(f); OutputStream out = s.openWrite("base.apk", 0, f.length())) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                s.fsync(out);
            }
            Intent done = new Intent(c, InstallResultReceiver.class);
            int fl = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
            s.commit(PendingIntent.getBroadcast(c, id, done, fl).getIntentSender());
        }
        prefs(c).edit().putLong("lastInstallAt", System.currentTimeMillis()).apply();
    }

    /** Background check: download and install when version.json asks for a newer build. Max one try per build per day. */
    static void runCheck(Context c) {
        try {
            if (!autoOn(c)) return;
            int want = latestApk();
            long have = installedCode(c);
            if (want <= 0 || have >= want) return;
            String key = want + ":" + new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
            if (key.equals(prefs(c).getString("tried", ""))) return;
            prefs(c).edit().putString("tried", key).apply();
            download(c, APK_URL, null);
            install(c);
        } catch (Exception ignored) {
        }
    }

    static boolean arabic(Context c) {
        return WidgetKit.data(c).optBoolean("rtl", true);
    }

    /** Android wants a tap: show its screen if the app is open, else a notification. */
    static void needsTap(Context c, Intent confirm) {
        confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (foreground) {
            try {
                c.startActivity(confirm);
                return;
            } catch (Exception ignored) {
            }
        }
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        boolean ar = arabic(c);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel("app_updates", ar ? "تحديثات التطبيق" : "App updates", NotificationManager.IMPORTANCE_DEFAULT));
        }
        PendingIntent pi = PendingIntent.getActivity(c, 7302, confirm, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification.Builder nb = Build.VERSION.SDK_INT >= 26 ? new android.app.Notification.Builder(c, "app_updates") : new android.app.Notification.Builder(c);
        nb.setSmallIcon(R.drawable.ic_stat_route)
            .setContentTitle(ar ? "تحديث يومي جاهز" : "Yawmi update ready")
            .setContentText(ar ? "اضغط عشان تكمّل التحديث" : "Tap to finish the update")
            .setContentIntent(pi)
            .setAutoCancel(true);
        try {
            nm.notify(7302, nb.build());
        } catch (Exception ignored) {
        }
    }
}
