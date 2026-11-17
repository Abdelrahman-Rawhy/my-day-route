package com.rawhy.mydayroute;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONObject;

/** «Faith today»: the adhkar for now, today's Quran pages and the tasbih count. Each row opens its page. */
public class FaithWidget extends AppWidgetProvider {
    private static final int[][] ROWS = {
        { R.id.w_f0, R.id.w_f0_e, R.id.w_f0_n, R.id.w_f0_b, R.id.w_f0_s, R.id.w_f0_v },
        { R.id.w_f1, R.id.w_f1_e, R.id.w_f1_n, R.id.w_f1_b, R.id.w_f1_s, R.id.w_f1_v },
        { R.id.w_f2, R.id.w_f2_e, R.id.w_f2_n, R.id.w_f2_b, R.id.w_f2_s, R.id.w_f2_v },
    };
    private static final RouteWidgetBase.Variant[] VARIANTS = {
        new RouteWidgetBase.Variant(R.layout.widget_faith, 200f, 80f, 1),
        new RouteWidgetBase.Variant(R.layout.widget_faith, 200f, 160f, 2),
        new RouteWidgetBase.Variant(R.layout.widget_faith, 200f, 230f, 3),
    };
    private static final String[] LINKS = { "adhkar", "mushaf", "tasbih" };

    private RemoteViews build(Context ctx, RouteWidgetBase.Variant v, JSONObject data, int wid) {
        int rows = v.ids.iterator().next();
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), WidgetKit.layout(data, R.layout.widget_faith, R.layout.widget_faith_rtl));
        WidgetKit.applyStyle(ctx, rv, R.id.w_root, data, false, wid);
        rv.setOnClickPendingIntent(R.id.w_root, WidgetKit.open(ctx, "adhkar", 2500));
        JSONObject f = data.optJSONObject("faith"), l = data.optJSONObject("faithL");
        if (l == null) l = new JSONObject();
        boolean fresh = f != null && WidgetKit.todayKey().equals(data.optString("day"));
        JSONObject adk = fresh ? f.optJSONObject("adk") : null, kh = fresh ? f.optJSONObject("kh") : null;
        int tz = fresh ? f.optInt("tz") : 0;
        for (int i = 0; i < ROWS.length; i++) {
            int[] r = ROWS[i];
            if (i >= rows) { rv.setViewVisibility(r[0], View.GONE); continue; }
            rv.setViewVisibility(r[0], View.VISIBLE);
            rv.setOnClickPendingIntent(r[0], WidgetKit.open(ctx, LINKS[i], 2510 + i));
            if (i == 0) {
                int d = adk == null ? 0 : adk.optInt("d"), t = adk == null ? 0 : adk.optInt("t");
                boolean ok = adk != null && t > 0 && d >= t;
                rv.setTextViewText(r[1], ok ? "✅" : "📿");
                rv.setTextViewText(r[2], adk == null ? l.optString("adk", "Adhkar") : adk.optString("n"));
                rv.setProgressBar(r[3], 1000, t > 0 ? d * 1000 / t : 0, false);
                rv.setTextViewText(r[4], ok ? l.optString("done", "Done ✓") : l.optString("adk", "Adhkar"));
                rv.setTextViewText(r[5], WidgetKit.num(data, d) + "/" + WidgetKit.num(data, t));
                rv.setTextColor(r[5], ok ? 0xFF34D399 : 0xFFFFFFFF);
            } else if (i == 1) {
                int qp = kh == null ? 0 : kh.optInt("qp"), tg = kh == null ? 1 : Math.max(1, kh.optInt("tg")), pg = kh == null ? 0 : kh.optInt("pg");
                boolean ok = qp >= tg;
                rv.setTextViewText(r[1], ok ? "✅" : "📖");
                rv.setTextViewText(r[2], l.optString("kh", "Quran today"));
                rv.setProgressBar(r[3], 1000, Math.min(1000, qp * 1000 / tg), false);
                rv.setTextViewText(r[4], l.optString("khatma", "Khatma") + " " + WidgetKit.num(data, pg * 100 / 604) + "%");
                rv.setTextViewText(r[5], WidgetKit.num(data, qp) + "/" + WidgetKit.num(data, tg));
                rv.setTextColor(r[5], ok ? 0xFF34D399 : 0xFFFFFFFF);
            } else {
                rv.setTextViewText(r[1], "📿");
                rv.setTextViewText(r[2], l.optString("tz", "Tasbih"));
                rv.setProgressBar(r[3], 1000, Math.min(1000, tz * 10), false);
                rv.setTextViewText(r[4], l.optString("today", "today"));
                rv.setTextViewText(r[5], WidgetKit.num(data, tz));
                rv.setTextColor(r[5], 0xFFFBBF24);
            }
        }
        return rv;
    }

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        JSONObject data = WidgetKit.data(ctx);
        for (int id : ids) { final int wid = id; mgr.updateAppWidget(id, RouteWidgetBase.responsive(VARIANTS, v -> build(ctx, v, data, wid), mgr, id)); }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context ctx, AppWidgetManager mgr, int id, Bundle o) {
        onUpdate(ctx, mgr, new int[] { id });
    }
}
