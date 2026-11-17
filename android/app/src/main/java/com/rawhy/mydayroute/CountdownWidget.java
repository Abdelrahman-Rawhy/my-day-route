package com.rawhy.mydayroute;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;

/** Days left to your dates, goal deadlines and Islamic occasions. Small: one big number. Wide: a list. */
public class CountdownWidget extends AppWidgetProvider {
    private static final int[][] ROWS = {
        { R.id.w_c0, R.id.w_c0_e, R.id.w_c0_n, R.id.w_c0_d }, { R.id.w_c1, R.id.w_c1_e, R.id.w_c1_n, R.id.w_c1_d },
        { R.id.w_c2, R.id.w_c2_e, R.id.w_c2_n, R.id.w_c2_d }, { R.id.w_c3, R.id.w_c3_e, R.id.w_c3_n, R.id.w_c3_d },
        { R.id.w_c4, R.id.w_c4_e, R.id.w_c4_n, R.id.w_c4_d }, { R.id.w_c5, R.id.w_c5_e, R.id.w_c5_n, R.id.w_c5_d },
    };
    // ids carry the number of rows; 0 = the single big-number layout
    private static final RouteWidgetBase.Variant[] VARIANTS = {
        new RouteWidgetBase.Variant(R.layout.widget_cd_s, 40f, 40f, 0),
        new RouteWidgetBase.Variant(R.layout.widget_cd, 200f, 50f, 2),
        new RouteWidgetBase.Variant(R.layout.widget_cd, 200f, 150f, 3),
        new RouteWidgetBase.Variant(R.layout.widget_cd, 200f, 220f, 4),
        new RouteWidgetBase.Variant(R.layout.widget_cd, 200f, 290f, 6),
    };

    /** Days from today, recomputed on every refresh so it stays right after midnight. */
    private static long days(JSONObject c) {
        long at = c.optLong("at");
        if (at <= 0) return c.optLong("days");
        return Math.max(0, Math.round((at - WidgetKit.todayMidnight()) / 86400000.0));
    }

    private static JSONArray upcoming(JSONObject data) {
        JSONArray src = data.optJSONArray("cds"), out = new JSONArray();
        if (src == null) return out;
        java.util.ArrayList<JSONObject> l = new java.util.ArrayList<>();
        for (int i = 0; i < src.length(); i++) {
            JSONObject c = src.optJSONObject(i);
            if (c != null && (c.optLong("at") <= 0 || c.optLong("at") >= WidgetKit.todayMidnight())) l.add(c);
        }
        java.util.Collections.sort(l, (a, b) -> Long.compare(days(a), days(b)));
        for (JSONObject c : l) out.put(c);
        return out;
    }

    private String dayText(JSONObject data, long d) {
        return d == 0 ? RouteWidgetBase.L(data, "today", "Today") : WidgetKit.num(data, d);
    }

    private RemoteViews build(Context ctx, RouteWidgetBase.Variant v, JSONObject data) {
        int rows = v.ids.iterator().next();
        JSONArray list = upcoming(data);
        int n = list.length();
        if (rows == 0) {
            RemoteViews rv = new RemoteViews(ctx.getPackageName(), WidgetKit.layout(data, R.layout.widget_cd_s, R.layout.widget_cd_s_rtl));
            WidgetKit.applyStyle(ctx, rv, R.id.w_root, data, true);
            rv.setOnClickPendingIntent(R.id.w_root, WidgetKit.open(ctx, "cds", 2400));
            if (n == 0) {
                rv.setTextViewText(R.id.w_cs_e, "📅");
                rv.setTextViewText(R.id.w_cs_n, RouteWidgetBase.L(data, "cds", "Countdown"));
                rv.setTextViewText(R.id.w_cs_d, "–");
                rv.setTextViewText(R.id.w_cs_l, RouteWidgetBase.L(data, "noCds", "Add a countdown in Settings"));
            } else {
                JSONObject c = list.optJSONObject(0);
                long d = days(c);
                rv.setTextViewText(R.id.w_cs_e, c.optString("e"));
                rv.setTextViewText(R.id.w_cs_n, c.optString("n"));
                rv.setTextViewText(R.id.w_cs_d, d == 0 ? "🎉" : WidgetKit.num(data, d));
                rv.setTextViewText(R.id.w_cs_l, d == 0 ? RouteWidgetBase.L(data, "today", "Today") : RouteWidgetBase.L(data, "daysLeft", "days left"));
            }
            return rv;
        }
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), WidgetKit.layout(data, R.layout.widget_cd, R.layout.widget_cd_rtl));
        WidgetKit.applyStyle(ctx, rv, R.id.w_root, data, false);
        rv.setOnClickPendingIntent(R.id.w_root, WidgetKit.open(ctx, "cds", 2401));
        rv.setViewVisibility(R.id.w_c_empty, n == 0 ? View.VISIBLE : View.GONE);
        rv.setTextViewText(R.id.w_c_empty, RouteWidgetBase.L(data, "noCds", "Add a countdown in Settings"));
        for (int i = 0; i < ROWS.length; i++) {
            if (i >= rows || n == 0) { rv.setViewVisibility(ROWS[i][0], View.GONE); continue; }
            JSONObject c = i < n ? list.optJSONObject(i) : null;
            if (c == null) { rv.setViewVisibility(ROWS[i][0], View.INVISIBLE); continue; }
            long d = days(c);
            rv.setViewVisibility(ROWS[i][0], View.VISIBLE);
            rv.setTextViewText(ROWS[i][1], c.optString("e"));
            rv.setTextViewText(ROWS[i][2], c.optString("n"));
            rv.setTextViewText(ROWS[i][3], dayText(data, d));
            rv.setTextColor(ROWS[i][3], d == 0 ? 0xFFFBBF24 : i == 0 ? 0xFFFF7A3D : 0xFF6B8CFF);
        }
        return rv;
    }

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        JSONObject data = WidgetKit.data(ctx);
        for (int id : ids) mgr.updateAppWidget(id, RouteWidgetBase.responsive(VARIANTS, v -> build(ctx, v, data), mgr, id));
    }

    @Override
    public void onAppWidgetOptionsChanged(Context ctx, AppWidgetManager mgr, int id, Bundle o) {
        onUpdate(ctx, mgr, new int[] { id });
    }
}
