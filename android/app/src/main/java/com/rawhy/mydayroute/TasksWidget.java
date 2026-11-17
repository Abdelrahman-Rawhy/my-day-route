package com.rawhy.mydayroute;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;

/** Today's tasks (due today or overdue). Tap the box to complete, the + to add, a task to open the Tasks page. */
public class TasksWidget extends AppWidgetProvider {
    static final String ACTION_DONE = "com.rawhy.mydayroute.TASK_DONE";
    private static final int[][] ROWS = {
        { R.id.w_t0, R.id.w_t0_cb, R.id.w_t0_n, R.id.w_t0_d }, { R.id.w_t1, R.id.w_t1_cb, R.id.w_t1_n, R.id.w_t1_d },
        { R.id.w_t2, R.id.w_t2_cb, R.id.w_t2_n, R.id.w_t2_d }, { R.id.w_t3, R.id.w_t3_cb, R.id.w_t3_n, R.id.w_t3_d },
        { R.id.w_t4, R.id.w_t4_cb, R.id.w_t4_n, R.id.w_t4_d }, { R.id.w_t5, R.id.w_t5_cb, R.id.w_t5_n, R.id.w_t5_d },
    };
    private static final RouteWidgetBase.Variant[] VARIANTS = {
        new RouteWidgetBase.Variant(R.layout.widget_tasks, 110f, 50f, 1),
        new RouteWidgetBase.Variant(R.layout.widget_tasks, 110f, 150f, 3),
        new RouteWidgetBase.Variant(R.layout.widget_tasks, 110f, 220f, 4),
        new RouteWidgetBase.Variant(R.layout.widget_tasks, 110f, 290f, 6),
    };

    private RemoteViews build(Context ctx, RouteWidgetBase.Variant v, JSONObject data) {
        int slots = v.ids.iterator().next();
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), WidgetKit.layout(data, R.layout.widget_tasks, R.layout.widget_tasks_rtl));
        WidgetKit.applyStyle(ctx, rv, R.id.w_root, data, false);
        rv.setOnClickPendingIntent(R.id.w_root, WidgetKit.open(ctx, "tasks", 2200));
        rv.setOnClickPendingIntent(R.id.w_t_add, WidgetKit.open(ctx, "add", 2201));
        rv.setTextViewText(R.id.w_t_title, RouteWidgetBase.L(data, "todo", "Today's tasks"));
        JSONArray list = data.optJSONArray("todo");
        int n = list == null ? 0 : list.length();
        rv.setTextViewText(R.id.w_t_count, n > 0 ? WidgetKit.num(data, data.optInt("todoN", n)) : "");
        rv.setViewVisibility(R.id.w_t_empty, n == 0 ? View.VISIBLE : View.GONE);
        rv.setTextViewText(R.id.w_t_empty, RouteWidgetBase.L(data, "noTasks", "No tasks today 🎉"));
        for (int i = 0; i < ROWS.length; i++) {
            if (i >= slots || n == 0) { rv.setViewVisibility(ROWS[i][0], View.GONE); continue; }
            JSONObject t = i < n ? list.optJSONObject(i) : null;
            if (t == null) { rv.setViewVisibility(ROWS[i][0], View.INVISIBLE); continue; }
            rv.setViewVisibility(ROWS[i][0], View.VISIBLE);
            rv.setTextViewText(ROWS[i][2], t.optString("n"));
            rv.setTextViewText(ROWS[i][3], t.optString("d"));
            rv.setTextColor(ROWS[i][3], t.optInt("ov") == 1 ? 0xFFF87171 : 0x99FFFFFF);
            int pri = t.optInt("pri");
            rv.setInt(ROWS[i][1], "setBackgroundResource", pri >= 3 ? R.drawable.widget_cb_red : pri >= 2 ? R.drawable.widget_cb_hi : R.drawable.widget_cb);
            rv.setOnClickPendingIntent(ROWS[i][1], WidgetKit.broadcast(ctx, TasksWidget.class, ACTION_DONE, t.optString("id"), 2210 + i));
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
        if (!ACTION_DONE.equals(intent.getAction())) { super.onReceive(ctx, intent); return; }
        String id = intent.getStringExtra("id");
        if (id == null) return;
        JSONObject data = WidgetKit.data(ctx);
        JSONArray list = data.optJSONArray("todo");
        if (list != null) {
            JSONArray keep = new JSONArray();
            for (int i = 0; i < list.length(); i++) {
                JSONObject t = list.optJSONObject(i);
                if (t != null && !id.equals(t.optString("id"))) keep.put(t);
            }
            try {
                data.put("todo", keep);
                data.put("todoN", Math.max(0, data.optInt("todoN", list.length()) - 1));
            } catch (Exception ignored) {
            }
            WidgetKit.saveData(ctx, data);
        }
        WidgetKit.addPending(ctx, "task", id);
        if (data.optBoolean("sound", true)) {
            try {
                MediaPlayer mp = MediaPlayer.create(ctx, R.raw.tick_check);
                if (mp != null) { mp.setOnCompletionListener(MediaPlayer::release); mp.start(); }
            } catch (Exception ignored) {
            }
        }
        WidgetKit.refresh(ctx, TasksWidget.class);
    }
}
