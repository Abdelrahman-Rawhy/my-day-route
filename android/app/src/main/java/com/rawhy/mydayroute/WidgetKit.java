package com.rawhy.mydayroute;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import java.util.Calendar;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Helpers shared by every widget: stored data, the user's widget look, links into the app and actions from the home screen. */
public final class WidgetKit {
    private WidgetKit() {}

    static final int[] PAD_DP = { 8, 12, 16, 22 };

    static JSONObject data(Context ctx) {
        try {
            return new JSONObject(ctx.getSharedPreferences(RouteWidgetBase.PREFS, Context.MODE_PRIVATE).getString(RouteWidgetBase.KEY, "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    static void saveData(Context ctx, JSONObject d) {
        ctx.getSharedPreferences(RouteWidgetBase.PREFS, Context.MODE_PRIVATE).edit().putString(RouteWidgetBase.KEY, d.toString()).apply();
    }

    static int dp(Context ctx, int v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }

    /** Background (glass or black, corner and opacity levels) and inner margins chosen in the app's settings. */
    static void applyStyle(Context ctx, RemoteViews rv, int rootId, JSONObject data, boolean small) {
        JSONObject w = data.optJSONObject("wg");
        String s = w == null ? "glass" : w.optString("s", "glass");
        if (!"black".equals(s)) s = "glass";
        int r = clamp(w == null ? 3 : w.optInt("r", 3), 0, 4);
        int o = clamp(w == null ? 2 : w.optInt("o", 2), 0, 5);
        int p = clamp(w == null ? 2 : w.optInt("p", 2), 0, 3);
        int res = ctx.getResources().getIdentifier("wbg_" + s + "_" + r + "_" + o, "drawable", ctx.getPackageName());
        if (res != 0) rv.setInt(rootId, "setBackgroundResource", res);
        int px = dp(ctx, small ? Math.max(5, PAD_DP[p] / 2) : PAD_DP[p]);
        rv.setViewPadding(rootId, px, px, px, px);
    }

    static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Opens the app on a page: com.rawhy.mydayroute://open/<where> (tasks, add, prayers, habits, cds, today). */
    static PendingIntent open(Context ctx, String where, int req) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("com.rawhy.mydayroute://open/" + where));
        i.setClass(ctx, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(ctx, req, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static PendingIntent broadcast(Context ctx, Class<?> cls, String action, String id, int req) {
        Intent i = new Intent(ctx, cls);
        i.setAction(action);
        i.putExtra("id", id);
        return PendingIntent.getBroadcast(ctx, req, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static String todayKey() {
        Calendar c = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    static long todayMidnight() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Queue an action for the app to apply next time it opens (same list as step checks). */
    static void addPending(Context ctx, String kind, String id) {
        try {
            String raw = ctx.getSharedPreferences(RouteWidgetBase.PREFS, Context.MODE_PRIVATE).getString(StepWidget.PENDING, "[]");
            JSONArray arr = new JSONArray(raw);
            JSONObject p = new JSONObject();
            p.put("k", kind);
            p.put("id", id);
            p.put("day", todayKey());
            p.put("at", System.currentTimeMillis());
            arr.put(p);
            ctx.getSharedPreferences(RouteWidgetBase.PREFS, Context.MODE_PRIVATE).edit().putString(StepWidget.PENDING, arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    /** Digits in the app's language (Arabic-Indic when the app is in Arabic). */
    static String num(JSONObject data, long n) {
        String s = String.valueOf(n);
        if (!data.optBoolean("rtl")) return s;
        StringBuilder b = new StringBuilder();
        for (char ch : s.toCharArray()) b.append(ch >= '0' && ch <= '9' ? (char) ('٠' + (ch - '0')) : ch);
        return b.toString();
    }

    static int layout(JSONObject data, int ltr, int rtl) {
        return data.optBoolean("rtl") ? rtl : ltr;
    }

    static void refresh(Context ctx, Class<?> cls) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, cls));
        if (ids == null || ids.length == 0) return;
        Intent i = new Intent(ctx, cls);
        i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        ctx.sendBroadcast(i);
    }
}
