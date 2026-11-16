package com.rawhy.mydayroute;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.widget.RemoteViews;
import java.text.DateFormat;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONObject;

/** Shared logic for the My Day Route widgets: live countdowns with Chronometer, rolled forward by an alarm. */
public abstract class RouteWidgetBase extends AppWidgetProvider {
    public static final String PREFS = "mdr_widget";
    public static final String KEY = "data";

    protected abstract int layoutId();

    protected abstract boolean showSteps();

    public static void refreshAll(Context ctx) {
        Class<?>[] classes = { RouteWidget.class, PrayerWidget.class, StepWidget.class };
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        for (Class<?> c : classes) {
            int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, c));
            if (ids != null && ids.length > 0) {
                Intent i = new Intent(ctx, c);
                i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                ctx.sendBroadcast(i);
            }
        }
    }

    private static JSONObject firstAfter(JSONArray arr, long now) {
        return firstAfter(arr, now, null);
    }

    private static JSONObject firstAfter(JSONArray arr, long now, String skipIds) {
        if (arr == null) return null;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null || o.optLong("t") <= now) continue;
            String id = o.optString("id");
            if (skipIds != null && id.length() > 0 && skipIds.contains("\"" + id + "\"")) continue;
            return o;
        }
        return null;
    }

    private static void setCountdown(RemoteViews v, int viewId, long target, long now) {
        long base = SystemClock.elapsedRealtime() + (target - now);
        v.setChronometer(viewId, base, null, true);
        if (Build.VERSION.SDK_INT >= 24) {
            v.setChronometerCountDown(viewId, true);
        }
    }

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        long now = System.currentTimeMillis();
        JSONObject data;
        try {
            data = new JSONObject(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "{}"));
        } catch (Exception e) {
            data = new JSONObject();
        }
        JSONObject prayer = firstAfter(data.optJSONArray("prayers"), now);
        String pending = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(StepWidget.PENDING, "[]");
        JSONObject step = firstAfter(data.optJSONArray("steps"), now, pending);
        DateFormat tf = android.text.format.DateFormat.getTimeFormat(ctx);
        long nextEvent = Long.MAX_VALUE;

        Intent open = new Intent(ctx, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(ctx, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        for (int id : ids) {
            RemoteViews v = new RemoteViews(ctx.getPackageName(), layoutId());
            if (prayer != null) {
                long t = prayer.optLong("t");
                v.setTextViewText(R.id.w_prayer_name, prayer.optString("n"));
                v.setTextViewText(R.id.w_prayer_at, tf.format(new Date(t)));
                setCountdown(v, R.id.w_prayer_cd, t, now);
                nextEvent = Math.min(nextEvent, t);
            } else {
                v.setTextViewText(R.id.w_prayer_name, "Open the app");
                v.setTextViewText(R.id.w_prayer_at, "to load prayer times");
            }
            if (showSteps()) {
                if (step != null) {
                    long t = step.optLong("t");
                    v.setTextViewText(R.id.w_step_name, step.optString("n"));
                    v.setTextViewText(R.id.w_step_at, tf.format(new Date(t)));
                    setCountdown(v, R.id.w_step_cd, t, now);
                    nextEvent = Math.min(nextEvent, t);
                } else {
                    v.setTextViewText(R.id.w_step_name, "All done today");
                    v.setTextViewText(R.id.w_step_at, "");
                    v.setChronometer(R.id.w_step_cd, SystemClock.elapsedRealtime(), "🎉", false);
                }
                v.setTextViewText(R.id.w_progress,
                    data.optInt("done") + "/" + data.optInt("total") + " steps · " + data.optInt("pct") + "%");
            }
            v.setOnClickPendingIntent(R.id.w_root, openPi);
            mgr.updateAppWidget(id, v);
        }
        scheduleNext(ctx, ids, nextEvent == Long.MAX_VALUE ? now + 30 * 60 * 1000L : nextEvent + 1500L);
    }

    private void scheduleNext(Context ctx, int[] ids, long at) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(ctx, getClass());
        i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, getClass().getSimpleName().hashCode() & 0xffff, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (SecurityException e) {
            am.set(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }
}
