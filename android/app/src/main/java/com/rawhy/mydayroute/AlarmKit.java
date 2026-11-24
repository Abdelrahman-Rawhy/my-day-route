package com.rawhy.mydayroute;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * v155 real alarms (nap / wake). Alarms are set with AlarmManager.setAlarmClock (exact, shown in the status bar,
 * survives Doze), stored in SharedPreferences so they come back after a reboot or an app update.
 * When one fires: a notification on an ALARM-usage channel with FLAG_INSISTENT (sound + vibration repeat until
 * Stop / Snooze / 10 minutes) and a full-screen intent to AlarmActivity (shows over the lock screen).
 * Alarm JSON: {id, at (ms), title, body, link, stop, snooze, snz (snooze minutes)}.
 */
public class AlarmKit {
    static final String PREF = "yawmi_alarms", KEY = "list", CH = "yawmi_alarm_v1";
    static final String A_FIRE = "com.rawhy.mydayroute.ALARM_FIRE", A_STOP = "com.rawhy.mydayroute.ALARM_STOP", A_SNOOZE = "com.rawhy.mydayroute.ALARM_SNOOZE";
    static final long RING_MS = 10 * 60 * 1000L;

    static SharedPreferences sp(Context c) { return c.getSharedPreferences(PREF, Context.MODE_PRIVATE); }

    static JSONArray load(Context c) {
        try { return new JSONArray(sp(c).getString(KEY, "[]")); } catch (Exception e) { return new JSONArray(); }
    }

    static void store(Context c, JSONArray a) { sp(c).edit().putString(KEY, a.toString()).apply(); }

    static boolean canExact(Context c) {
        if (Build.VERSION.SDK_INT < 31) return true;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        try { return am != null && am.canScheduleExactAlarms(); } catch (Exception e) { return false; }
    }

    static boolean canFullScreen(Context c) {
        if (Build.VERSION.SDK_INT < 34) return true;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        try { return nm != null && nm.canUseFullScreenIntent(); } catch (Exception e) { return true; }
    }

    static int flags() { return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE; }

    static Intent fireIntent(Context c, JSONObject a) {
        Intent i = new Intent(c, Receiver.class);
        i.setAction(A_FIRE);
        i.putExtra("json", a.toString());
        return i;
    }

    static PendingIntent firePi(Context c, int id, JSONObject a) {
        return PendingIntent.getBroadcast(c, 7000 + id, a == null ? new Intent(c, Receiver.class).setAction(A_FIRE) : fireIntent(c, a), flags());
    }

    static void arm(Context c, JSONObject a) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        int id = a.optInt("id");
        long at = a.optLong("at");
        PendingIntent pi = firePi(c, id, a);
        try {
            if (canExact(c)) {
                Intent open = new Intent(c, MainActivity.class);
                open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent show = PendingIntent.getActivity(c, 7500 + id, open, flags());
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, show), pi);
            } else if (Build.VERSION.SDK_INT >= 23) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (Exception e) {
            try { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi); } catch (Exception ignored) {}
        }
    }

    static void disarm(Context c, int id) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        try { am.cancel(firePi(c, id, null)); } catch (Exception ignored) {}
    }

    /** Replace every stored alarm with the given list (past ones are dropped). Returns how many are armed. */
    static int replaceAll(Context c, JSONArray in) {
        JSONArray old = load(c), keep = new JSONArray();
        long now = System.currentTimeMillis();
        for (int i = 0; i < old.length(); i++) {
            JSONObject o = old.optJSONObject(i);
            if (o.optInt("id") == 99 && o.optLong("at") > now) { keep.put(o); continue; } // test alarm stays
            disarm(c, o.optInt("id"));
        }
        for (int i = 0; i < in.length(); i++) {
            JSONObject a = in.optJSONObject(i);
            if (a == null || a.optInt("id") == 99 || a.optLong("at") <= now + 5000) continue;
            arm(c, a);
            keep.put(a);
        }
        store(c, keep);
        return keep.length();
    }

    static void put(Context c, JSONObject a) {
        JSONArray l = load(c), out = new JSONArray();
        int id = a.optInt("id");
        for (int i = 0; i < l.length(); i++) if (l.optJSONObject(i).optInt("id") != id) out.put(l.optJSONObject(i));
        out.put(a);
        store(c, out);
        arm(c, a);
    }

    static void forget(Context c, int id) {
        JSONArray l = load(c), out = new JSONArray();
        for (int i = 0; i < l.length(); i++) if (l.optJSONObject(i).optInt("id") != id) out.put(l.optJSONObject(i));
        store(c, out);
    }

    /** After boot / update / clock change: re-arm future alarms; an alarm missed by under 10 minutes rings now. */
    static void restore(Context c) {
        JSONArray l = load(c), keep = new JSONArray();
        long now = System.currentTimeMillis();
        for (int i = 0; i < l.length(); i++) {
            JSONObject a = l.optJSONObject(i);
            if (a == null) continue;
            long at = a.optLong("at");
            if (at > now) { arm(c, a); keep.put(a); }
            else if (now - at < RING_MS) ring(c, a);
        }
        store(c, keep);
    }

    static void channel(Context c, boolean ar) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null || nm.getNotificationChannel(CH) != null) return;
        NotificationChannel ch = new NotificationChannel(CH, ar ? "المنبّه" : "Alarm", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription(ar ? "منبّه القيلولة والصحيان" : "Nap and wake-up alarm");
        Uri snd = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (snd == null) snd = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ch.setSound(snd, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0, 800, 600, 800, 600});
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);
    }

    static boolean isAr(Context c) {
        try { return c.getResources().getConfiguration().locale.getLanguage().startsWith("ar"); } catch (Exception e) { return true; }
    }

    static Intent actIntent(Context c, JSONObject a) {
        Intent i = new Intent(c, AlarmActivity.class);
        i.putExtra("json", a.toString());
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        return i;
    }

    static void ring(Context c, JSONObject a) {
        int id = a.optInt("id");
        forget(c, id);
        boolean ar = isAr(c);
        channel(c, ar);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        PendingIntent full = PendingIntent.getActivity(c, 7600 + id, actIntent(c, a), flags());
        Intent stop = new Intent(c, Receiver.class).setAction(A_STOP).putExtra("json", a.toString());
        Intent snz = new Intent(c, Receiver.class).setAction(A_SNOOZE).putExtra("json", a.toString());
        String sStop = a.optString("stop", ar ? "إيقاف" : "Stop"), sSnz = a.optString("snooze", ar ? "غفوة" : "Snooze");
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CH) : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_stat_route)
            .setContentTitle(a.optString("title", "⏰"))
            .setContentText(a.optString("body", ""))
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setShowWhen(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(full)
            .setFullScreenIntent(full, true)
            .addAction(0, sSnz, PendingIntent.getBroadcast(c, 7700 + id, snz, flags()))
            .addAction(0, sStop, PendingIntent.getBroadcast(c, 7800 + id, stop, flags()));
        if (Build.VERSION.SDK_INT >= 26) b.setTimeoutAfter(RING_MS);
        else {
            b.setPriority(Notification.PRIORITY_MAX);
            b.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), android.media.AudioManager.STREAM_ALARM);
            b.setVibrate(new long[]{0, 800, 600, 800, 600});
        }
        Notification n = b.build();
        n.flags |= Notification.FLAG_INSISTENT | Notification.FLAG_NO_CLEAR;
        try { nm.notify(91000 + id, n); } catch (Exception ignored) {}
        // Screen already on and unlocked: open the alarm screen directly too (full-screen intents only show as a banner then).
        try { c.startActivity(actIntent(c, a)); } catch (Exception ignored) {}
    }

    static void silence(Context c, int id) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) try { nm.cancel(91000 + id); } catch (Exception ignored) {}
        AlarmActivity.closeIfShowing(id);
    }

    static void snooze(Context c, JSONObject a) {
        int id = a.optInt("id");
        silence(c, id);
        try {
            int m = Math.max(1, a.optInt("snz", 5));
            JSONObject b = new JSONObject(a.toString());
            b.put("at", System.currentTimeMillis() + m * 60000L);
            put(c, b);
        } catch (Exception ignored) {}
    }

    static void stop(Context c, JSONObject a) {
        int id = a.optInt("id");
        silence(c, id);
        disarm(c, id);
        forget(c, id);
        sp(c).edit().putString("last", a.optString("link", "")).putLong("lastAt", System.currentTimeMillis()).apply();
    }

    static JSONObject parse(Intent i) {
        try { return new JSONObject(i.getStringExtra("json")); } catch (Exception e) { return new JSONObject(); }
    }

    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent i) {
            String act = i.getAction();
            JSONObject a = parse(i);
            if (A_FIRE.equals(act)) ring(c, a);
            else if (A_STOP.equals(act)) stop(c, a);
            else if (A_SNOOZE.equals(act)) snooze(c, a);
        }
    }

    public static class Boot extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent i) { restore(c); }
    }
}
