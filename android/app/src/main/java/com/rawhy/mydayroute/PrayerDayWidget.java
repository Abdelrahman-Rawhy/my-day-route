package com.rawhy.mydayroute;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;

/** Today's prayer times: date + Hijri, countdown to the next prayer, all prayers in a row (next one highlighted), and a day bar. */
public class PrayerDayWidget extends AppWidgetProvider {
    private static final int[][] COLS = {
        { R.id.w_p0, R.id.w_p0_n, R.id.w_p0_t }, { R.id.w_p1, R.id.w_p1_n, R.id.w_p1_t }, { R.id.w_p2, R.id.w_p2_n, R.id.w_p2_t },
        { R.id.w_p3, R.id.w_p3_n, R.id.w_p3_t }, { R.id.w_p4, R.id.w_p4_n, R.id.w_p4_t }, { R.id.w_p5, R.id.w_p5_n, R.id.w_p5_t },
    };

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        JSONObject data = WidgetKit.data(ctx);
        long now = System.currentTimeMillis();
        JSONArray pd = data.optJSONArray("pday");
        boolean fresh = pd != null && WidgetKit.todayKey().equals(data.optString("day"));
        long nextAt = Long.MAX_VALUE;
        for (int id : ids) {
            RemoteViews rv = new RemoteViews(ctx.getPackageName(), WidgetKit.layout(data, R.layout.widget_pday, R.layout.widget_pday_rtl));
            WidgetKit.applyStyle(ctx, rv, R.id.w_root, data, false, id);
            rv.setOnClickPendingIntent(R.id.w_root, WidgetKit.open(ctx, "prayers", 2100));
            rv.setTextViewText(R.id.w_pd_date, fresh ? data.optString("dateG") : RouteWidgetBase.L(data, "openApp", "Open the app"));
            rv.setTextViewText(R.id.w_pd_hijri, fresh ? data.optString("dateH") : RouteWidgetBase.L(data, "loadPrayers", "to load prayer times"));
            int next = -1;
            long first = 0, last = 0, nextT = 0;
            String nextName = "", nextTT = "";
            if (fresh) {
                // prayers only (not sunrise) count for the next-prayer countdown
                for (int i = 0; i < pd.length(); i++) {
                    JSONObject p = pd.optJSONObject(i);
                    if (p == null) continue;
                    long t = p.optLong("t");
                    if (i == 0) first = t;
                    last = t;
                    if (next < 0 && t > now && !"sunrise".equals(p.optString("k"))) { next = i; nextT = t; nextName = p.optString("n"); }
                }
                if (next < 0 && data.optLong("fajrTom") > now) {
                    nextT = data.optLong("fajrTom");
                    nextName = pd.length() > 0 ? pd.optJSONObject(0).optString("n") : "";
                }
            }
            for (int i = 0; i < COLS.length; i++) {
                JSONObject p = fresh && i < pd.length() ? pd.optJSONObject(i) : null;
                if (p == null) { rv.setViewVisibility(COLS[i][0], View.GONE); continue; }
                rv.setViewVisibility(COLS[i][0], View.VISIBLE);
                rv.setTextViewText(COLS[i][1], p.optString("n"));
                rv.setTextViewText(COLS[i][2], shortTime(p.optString("tt")));
                boolean hi = i == next;
                rv.setInt(COLS[i][0], "setBackgroundResource", hi ? R.drawable.widget_pill : 0);
                rv.setTextColor(COLS[i][1], hi ? 0xFFFBBF24 : 0xCCFFFFFF);
                rv.setTextColor(COLS[i][2], hi ? 0xFFFBBF24 : (p.optLong("t") < now ? 0x80FFFFFF : 0xFFFFFFFF));
            }
            if (nextT > 0) {
                rv.setTextViewText(R.id.w_pd_next, RouteWidgetBase.L(data, "nextP", "Next") + " · " + nextName);
                rv.setChronometer(R.id.w_pd_cd, SystemClock.elapsedRealtime() + (nextT - now), null, true);
                if (Build.VERSION.SDK_INT >= 24) rv.setChronometerCountDown(R.id.w_pd_cd, true);
                nextAt = Math.min(nextAt, nextT);
            } else {
                rv.setTextViewText(R.id.w_pd_next, "");
                rv.setChronometer(R.id.w_pd_cd, SystemClock.elapsedRealtime(), "", false);
            }
            int prog = 0;
            if (fresh && last > first) prog = (int) Math.max(0, Math.min(1000, (now - first) * 1000 / (last - first)));
            rv.setProgressBar(R.id.w_pd_bar, 1000, prog, false);
            mgr.updateAppWidget(id, rv);
        }
        schedule(ctx, nextAt == Long.MAX_VALUE ? now + 30 * 60 * 1000L : Math.min(nextAt + 1500L, now + 30 * 60 * 1000L));
    }

    /** "4:52 ص" stays; "4:52 AM" → "4:52" to keep the row tidy. */
    private static String shortTime(String tt) {
        return tt.replace(" AM", "").replace(" PM", "").replace(" am", "").replace(" pm", "");
    }

    @Override
    public void onAppWidgetOptionsChanged(Context ctx, AppWidgetManager mgr, int id, Bundle o) {
        onUpdate(ctx, mgr, new int[] { id });
    }

    private void schedule(Context ctx, long at) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(ctx, PrayerDayWidget.class);
        i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(ctx)
            .getAppWidgetIds(new android.content.ComponentName(ctx, PrayerDayWidget.class)));
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 2101, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        } catch (Exception e) {
            am.set(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }
}
