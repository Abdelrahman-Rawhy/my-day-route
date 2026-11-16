package com.rawhy.mydayroute;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.RemoteViews;
import java.text.DateFormat;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONObject;

/** "Current step" widget: shows the step you should do now, with a check button that celebrates and moves on. */
public class StepWidget extends AppWidgetProvider {
    private static final RouteWidgetBase.Variant[] VARIANTS = {
        new RouteWidgetBase.Variant(R.layout.widget_step_s, 40f, 40f,
            R.id.w_flip, R.id.w_step_body, R.id.w_s_name, R.id.w_s_check, R.id.w_c_title),
        new RouteWidgetBase.Variant(R.layout.widget_step, 200f, 90f,
            R.id.w_flip, R.id.w_step_body, R.id.w_s_name, R.id.w_s_check, R.id.w_c_title,
            R.id.w_s_label, R.id.w_s_progress, R.id.w_s_pts, R.id.w_s_next, R.id.w_c_name, R.id.w_c_next),
    };
    public static final String PENDING = "pending";
    public static final String ACTION_CHECK = "com.rawhy.mydayroute.STEP_CHECK";
    public static final String EXTRA_ID = "stepId";

    private static JSONObject readData(Context ctx) {
        try {
            return new JSONObject(ctx.getSharedPreferences(RouteWidgetBase.PREFS, Context.MODE_PRIVATE)
                .getString(RouteWidgetBase.KEY, "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private static JSONArray readPending(Context ctx) {
        try {
            return new JSONArray(ctx.getSharedPreferences(RouteWidgetBase.PREFS, Context.MODE_PRIVATE)
                .getString(PENDING, "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static boolean isPending(JSONArray pending, String id) {
        for (int i = 0; i < pending.length(); i++) {
            JSONObject o = pending.optJSONObject(i);
            if (o != null && id.equals(o.optString("id"))) return true;
        }
        return false;
    }

    private static String todayKey() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        return String.format(java.util.Locale.US, "%04d-%02d-%02d",
            c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH));
    }

    /** Steps still to do today, in route order, skipping ones checked on the widget. */
    private static JSONObject[] remaining(Context ctx, JSONObject data) {
        JSONArray list = data.optJSONArray("today");
        if (list == null || !todayKey().equals(data.optString("day"))) return new JSONObject[0];
        JSONArray pending = readPending(ctx);
        java.util.ArrayList<JSONObject> out = new java.util.ArrayList<>();
        for (int i = 0; i < list.length(); i++) {
            JSONObject o = list.optJSONObject(i);
            if (o != null && !isPending(pending, o.optString("id"))) out.add(o);
        }
        return out.toArray(new JSONObject[0]);
    }

    private RemoteViews build(Context ctx, int widgetId, boolean celebrate, String doneName, int donePts) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        return RouteWidgetBase.responsive(VARIANTS, v -> buildVariant(ctx, v, widgetId, celebrate, doneName, donePts), mgr, widgetId);
    }

    private RemoteViews buildVariant(Context ctx, RouteWidgetBase.Variant var, int widgetId, boolean celebrate, String doneName, int donePts) {
        JSONObject data = readData(ctx);
        JSONObject[] left = remaining(ctx, data);
        int total = data.optInt("total");
        int done = Math.max(0, total - left.length);
        DateFormat tf = android.text.format.DateFormat.getTimeFormat(ctx);
        RemoteViews v = new RemoteViews(ctx.getPackageName(), var.layout);

        Intent open = new Intent(ctx, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        v.setOnClickPendingIntent(R.id.w_step_body, PendingIntent.getActivity(ctx, 1, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        RouteWidgetBase.text(v, var, R.id.w_s_progress, done + "/" + total);
        if (left.length > 0) {
            JSONObject cur = left[0];
            long t = cur.optLong("t");
            boolean now = t <= System.currentTimeMillis();
            RouteWidgetBase.text(v, var, R.id.w_s_label, now ? "NOW" : "NEXT · " + tf.format(new Date(t)));
            RouteWidgetBase.text(v, var, R.id.w_s_name, cur.optString("n"));
            RouteWidgetBase.text(v, var, R.id.w_s_pts, "+" + cur.optInt("pts") + " points");
            RouteWidgetBase.text(v, var, R.id.w_s_next, left.length > 1
                ? "Then: " + left[1].optString("n") + " · " + tf.format(new Date(left[1].optLong("t")))
                : "Last step of the day");
            Intent check = new Intent(ctx, StepWidget.class);
            check.setAction(ACTION_CHECK);
            check.putExtra(EXTRA_ID, cur.optString("id"));
            check.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            v.setOnClickPendingIntent(R.id.w_s_check, PendingIntent.getBroadcast(ctx, 1000 + widgetId, check,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        } else {
            RouteWidgetBase.text(v, var, R.id.w_s_label, total > 0 ? "ALL DONE" : "MY DAY ROUTE");
            RouteWidgetBase.text(v, var, R.id.w_s_name, total > 0 ? "Route complete!" : "Open the app");
            RouteWidgetBase.text(v, var, R.id.w_s_pts, total > 0 ? "MashaAllah, rest well" : "to load today's steps");
            RouteWidgetBase.text(v, var, R.id.w_s_next, "");
        }
        if (celebrate) {
            RouteWidgetBase.text(v, var, R.id.w_c_title, "Done! +" + donePts);
            RouteWidgetBase.text(v, var, R.id.w_c_name, doneName);
            RouteWidgetBase.text(v, var, R.id.w_c_next, left.length > 0 ? "Next: " + left[0].optString("n") : "Route complete! 🏆");
        }
        v.setDisplayedChild(R.id.w_flip, celebrate ? 1 : 0);
        return v;
    }

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) mgr.updateAppWidget(id, build(ctx, id, false, "", 0));
    }

    @Override
    public void onAppWidgetOptionsChanged(Context ctx, AppWidgetManager mgr, int widgetId, Bundle newOptions) {
        mgr.updateAppWidget(widgetId, build(ctx, widgetId, false, "", 0));
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!ACTION_CHECK.equals(intent.getAction())) {
            super.onReceive(ctx, intent);
            return;
        }
        String stepId = intent.getStringExtra(EXTRA_ID);
        if (stepId == null) return;
        JSONObject data = readData(ctx);
        String name = "";
        int pts = 0;
        JSONArray list = data.optJSONArray("today");
        if (list != null) {
            for (int i = 0; i < list.length(); i++) {
                JSONObject o = list.optJSONObject(i);
                if (o != null && stepId.equals(o.optString("id"))) {
                    name = o.optString("n");
                    pts = o.optInt("pts");
                }
            }
        }
        JSONArray pending = readPending(ctx);
        if (!isPending(pending, stepId)) {
            try {
                JSONObject p = new JSONObject();
                p.put("id", stepId);
                p.put("day", data.optString("day"));
                p.put("at", System.currentTimeMillis());
                pending.put(p);
            } catch (Exception ignored) {
            }
            SharedPreferences.Editor ed = ctx.getSharedPreferences(RouteWidgetBase.PREFS, Context.MODE_PRIVATE).edit();
            ed.putString(PENDING, pending.toString()).apply();
        }
        if (data.optBoolean("sound", true)) {
            try {
                MediaPlayer mp = MediaPlayer.create(ctx, R.raw.tick_check);
                if (mp != null) {
                    mp.setOnCompletionListener(MediaPlayer::release);
                    mp.start();
                }
            } catch (Exception ignored) {
            }
        }
        final AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        final int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, StepWidget.class));
        final Context app = ctx.getApplicationContext();
        for (int id : ids) mgr.updateAppWidget(id, build(app, id, true, name, pts));
        final PendingResult result = goAsync();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            for (int id : ids) mgr.updateAppWidget(id, build(app, id, false, "", 0));
            RouteWidgetBase.refreshAll(app);
            result.finish();
        }, 2200);
    }
}
