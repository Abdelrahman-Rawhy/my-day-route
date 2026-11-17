package com.rawhy.mydayroute;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;

/** Habit cards in a 2-column grid. Tap a card to add +1. */
public class HabitsWidget extends AppWidgetProvider {
    static final String ACTION_ADD = "com.rawhy.mydayroute.HABIT_ADD";
    private static final int[] ROW_IDS = { R.id.w_hr0, R.id.w_hr1, R.id.w_hr2 };
    private static final int[][] CARDS = {
        { R.id.w_h0, R.id.w_h0_e, R.id.w_h0_n, R.id.w_h0_s }, { R.id.w_h1, R.id.w_h1_e, R.id.w_h1_n, R.id.w_h1_s },
        { R.id.w_h2, R.id.w_h2_e, R.id.w_h2_n, R.id.w_h2_s }, { R.id.w_h3, R.id.w_h3_e, R.id.w_h3_n, R.id.w_h3_s },
        { R.id.w_h4, R.id.w_h4_e, R.id.w_h4_n, R.id.w_h4_s }, { R.id.w_h5, R.id.w_h5_e, R.id.w_h5_n, R.id.w_h5_s },
    };
    private static final RouteWidgetBase.Variant[] VARIANTS = {
        new RouteWidgetBase.Variant(R.layout.widget_habits, 250f, 90f, 1),
        new RouteWidgetBase.Variant(R.layout.widget_habits, 250f, 175f, 2),
        new RouteWidgetBase.Variant(R.layout.widget_habits, 250f, 260f, 3),
    };

    private RemoteViews build(Context ctx, RouteWidgetBase.Variant v, JSONObject data) {
        int rows = v.ids.iterator().next();
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), WidgetKit.layout(data, R.layout.widget_habits, R.layout.widget_habits_rtl));
        WidgetKit.applyStyle(ctx, rv, R.id.w_root, data, false);
        rv.setOnClickPendingIntent(R.id.w_root, WidgetKit.open(ctx, "habits", 2300));
        JSONArray list = data.optJSONArray("habits");
        int n = list == null ? 0 : list.length();
        rv.setViewVisibility(R.id.w_h_empty, n == 0 ? View.VISIBLE : View.GONE);
        rv.setTextViewText(R.id.w_h_empty, RouteWidgetBase.L(data, "noHabits", "Add a habit in Goals"));
        for (int r = 0; r < ROW_IDS.length; r++) rv.setViewVisibility(ROW_IDS[r], r < rows && r * 2 < n ? View.VISIBLE : View.GONE);
        for (int i = 0; i < CARDS.length; i++) {
            JSONObject h = i < n ? list.optJSONObject(i) : null;
            if (h == null) { rv.setViewVisibility(CARDS[i][0], View.INVISIBLE); continue; }
            rv.setViewVisibility(CARDS[i][0], View.VISIBLE);
            int c = h.optInt("c"), tg = Math.max(1, h.optInt("tg", 1));
            rv.setTextViewText(CARDS[i][1], c >= tg ? "✅" : h.optString("e", "✅"));
            rv.setTextViewText(CARDS[i][2], h.optString("n"));
            rv.setTextViewText(CARDS[i][3], WidgetKit.num(data, c) + " / " + WidgetKit.num(data, tg) + " " + h.optString("u"));
            rv.setTextColor(CARDS[i][3], c >= tg ? 0xFF34D399 : 0xB3FFFFFF);
            rv.setOnClickPendingIntent(CARDS[i][0], WidgetKit.broadcast(ctx, HabitsWidget.class, ACTION_ADD, h.optString("id"), 2310 + i));
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

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!ACTION_ADD.equals(intent.getAction())) { super.onReceive(ctx, intent); return; }
        String id = intent.getStringExtra("id");
        if (id == null) return;
        JSONObject data = WidgetKit.data(ctx);
        JSONArray list = data.optJSONArray("habits");
        if (list != null) {
            for (int i = 0; i < list.length(); i++) {
                JSONObject h = list.optJSONObject(i);
                if (h != null && id.equals(h.optString("id"))) {
                    try { h.put("c", h.optInt("c") + 1); } catch (Exception ignored) { }
                }
            }
            WidgetKit.saveData(ctx, data);
        }
        WidgetKit.addPending(ctx, "habit", id);
        WidgetKit.refresh(ctx, HabitsWidget.class);
    }
}
