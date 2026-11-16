package com.rawhy.mydayroute;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.SizeF;
import android.widget.RemoteViews;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Shared logic for the My Day Route widgets: size-responsive layouts and live countdowns. */
public abstract class RouteWidgetBase extends AppWidgetProvider {
    public static final String PREFS = "mdr_widget";
    public static final String KEY = "data";

    /** One layout for one size range. Sizes are minimums in dp. */
    public static class Variant {
        final int layout;
        final float w;
        final float h;
        final Set<Integer> ids = new HashSet<>();

        Variant(int layout, float w, float h, int... viewIds) {
            this.layout = layout;
            this.w = w;
            this.h = h;
            for (int id : viewIds) ids.add(id);
        }

        boolean has(int id) {
            return ids.contains(id);
        }
    }

    public interface Builder {
        RemoteViews build(Variant v);
    }

    /** Ordered from smallest to largest. */
    protected abstract Variant[] variants();

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

    /** Android 12+: give the launcher every layout and let it pick by size. Older: pick from the current size. */
    public static RemoteViews responsive(Variant[] vs, Builder b, AppWidgetManager mgr, int widgetId) {
        if (Build.VERSION.SDK_INT >= 31) {
            Map<SizeF, RemoteViews> map = new HashMap<>();
            for (Variant v : vs) map.put(new SizeF(v.w, v.h), b.build(v));
            return new RemoteViews(map);
        }
        Variant best = vs[vs.length - 1];
        Bundle o = mgr.getAppWidgetOptions(widgetId);
        if (o != null) {
            int w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
            if (w > 0 && h > 0) {
                best = vs[0];
                for (Variant v : vs) if (w >= v.w && h >= v.h) best = v;
            }
        }
        return b.build(best);
    }

    /** Label from the app (sent in its language), or the English fallback. */
    static String L(JSONObject data, String key, String fallback) {
        JSONObject l = data.optJSONObject("L");
        String s = l == null ? null : l.optString(key, null);
        return s == null || s.length() == 0 ? fallback : s;
    }

    static void text(RemoteViews rv, Variant v, int id, CharSequence s) {
        if (v.has(id)) rv.setTextViewText(id, s);
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

    private static void setCountdown(RemoteViews rv, Variant v, int viewId, long target, long now) {
        if (!v.has(viewId)) return;
        long base = SystemClock.elapsedRealtime() + (target - now);
        rv.setChronometer(viewId, base, null, true);
        if (Build.VERSION.SDK_INT >= 24) rv.setChronometerCountDown(viewId, true);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context ctx, AppWidgetManager mgr, int widgetId, Bundle newOptions) {
        onUpdate(ctx, mgr, new int[] { widgetId });
    }

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        final long now = System.currentTimeMillis();
        JSONObject parsed;
        try {
            parsed = new JSONObject(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "{}"));
        } catch (Exception e) {
            parsed = new JSONObject();
        }
        final JSONObject data = parsed;
        String pending = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(StepWidget.PENDING, "[]");
        final JSONObject prayer = firstAfter(data.optJSONArray("prayers"), now, null);
        final JSONObject step = firstAfter(data.optJSONArray("steps"), now, pending);
        final DateFormat tf = android.text.format.DateFormat.getTimeFormat(ctx);
        final String pkg = ctx.getPackageName();

        Intent open = new Intent(ctx, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        final PendingIntent openPi = PendingIntent.getActivity(ctx, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Builder b = v -> {
            RemoteViews rv = new RemoteViews(pkg, v.layout);
            if (prayer != null) {
                long t = prayer.optLong("t");
                text(rv, v, R.id.w_prayer_name, prayer.optString("n"));
                text(rv, v, R.id.w_prayer_at, tf.format(new Date(t)));
                setCountdown(rv, v, R.id.w_prayer_cd, t, now);
            } else {
                text(rv, v, R.id.w_prayer_name, L(data, "openApp", "Open app"));
                text(rv, v, R.id.w_prayer_at, L(data, "loadPrayers", "to load prayer times"));
            }
            if (v.has(R.id.w_step_name)) {
                if (step != null) {
                    long t = step.optLong("t");
                    text(rv, v, R.id.w_step_name, step.optString("n"));
                    text(rv, v, R.id.w_step_at, tf.format(new Date(t)));
                    setCountdown(rv, v, R.id.w_step_cd, t, now);
                } else {
                    text(rv, v, R.id.w_step_name, L(data, "allDoneToday", "All done today"));
                    text(rv, v, R.id.w_step_at, "");
                    if (v.has(R.id.w_step_cd)) rv.setChronometer(R.id.w_step_cd, SystemClock.elapsedRealtime(), "🎉", false);
                }
            }
            text(rv, v, R.id.w_progress,
                data.optInt("done") + "/" + data.optInt("total") + " " + L(data, "steps", "steps") + " · " + data.optInt("pct") + "%");
            text(rv, v, R.id.w_lbl_prayer, L(data, "nextPrayer", "NEXT PRAYER"));
            text(rv, v, R.id.w_lbl_step, L(data, "nextStep", "NEXT STEP"));
            text(rv, v, R.id.w_lbl_app, L(data, "app", "My Day Route"));
            rv.setOnClickPendingIntent(R.id.w_root, openPi);
            return rv;
        };
        for (int id : ids) mgr.updateAppWidget(id, responsive(variants(), b, mgr, id));

        long nextEvent = Long.MAX_VALUE;
        if (prayer != null) nextEvent = Math.min(nextEvent, prayer.optLong("t"));
        if (step != null) nextEvent = Math.min(nextEvent, step.optLong("t"));
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
