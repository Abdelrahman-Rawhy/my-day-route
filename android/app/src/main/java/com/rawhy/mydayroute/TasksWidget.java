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
        new RouteWidgetBase.Variant(R.layout.widget_tasks, 180f, 110f, 1),
        new RouteWidgetBase.Variant(R.layout.widget_tasks, 180f, 160f, 2),
        new RouteWidgetBase.Variant(R.layout.widget_tasks, 180f, 210f, 3),
        new RouteWidgetBase.Variant(R.layout.widget_tasks, 180f, 260f, 4),
        new RouteWidgetBase.Variant(R.layout.widget_tasks, 180f, 310f, 5),
        new RouteWidgetBase.Variant(R.layout.widget_tasks, 180f, 360f, 6),
    };

    private RemoteViews build(Context ctx, RouteWidgetBase.Variant v, JSONObject data, int wid) {
        int slots = v.ids.iterator().next();
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), WidgetKit.layout(data, R.layout.widget_tasks, R.layout.widget_tasks_rtl));
        WidgetKit.applyStyle(ctx, rv, R.id.w_root, data, false, wid);
        rv.setOnClickPendingIntent(R.id.w_root, WidgetKit.open(ctx, "tasks", 2200));
        rv.setOnClickPendingIntent(R.id.w_t_add, WidgetKit.open(ctx, "add", 2201));
        JSONArray list = data.optJSONArray("todo"), next = data.optJSONArray("todoNext");
        int n = list == null ? 0 : list.length(), m = next == null ? 0 : next.length();
        rv.setTextViewText(R.id.w_t_title, RouteWidgetBase.L(data, "todo", "Today's tasks") + (n > 0 ? " · " + WidgetKit.num(data, data.optInt("todoN", n)) : ""));
        rv.setTextViewText(R.id.w_t_count, "");
        rv.setViewVisibility(R.id.w_t_count, View.GONE);
        boolean empty = n == 0 && m == 0;
        rv.setViewVisibility(R.id.w_t_empty, empty ? View.VISIBLE : View.GONE);
        rv.setTextViewText(R.id.w_t_empty, RouteWidgetBase.L(data, "noTasks", "No tasks today 🎉"));
        for (int i = 0; i < ROWS.length; i++) {
            if (i >= slots || empty) { rv.setViewVisibility(ROWS[i][0], View.GONE); continue; }
            // today's tasks first, then the coming ones (dimmed) so the widget is never half empty
            boolean later = i >= n;
            JSONObject t = !later ? list.optJSONObject(i) : (i - n < m ? next.optJSONObject(i - n) : null);
            if (t == null) { rv.setViewVisibility(ROWS[i][0], View.INVISIBLE); continue; }
            rv.setViewVisibility(ROWS[i][0], View.VISIBLE);
            rv.setTextViewText(ROWS[i][2], t.optString("n"));
            rv.setTextColor(ROWS[i][2], later ? 0x99FFFFFF : 0xFFFFFFFF);
            rv.setTextViewText(ROWS[i][3], t.optString("d"));
            rv.setTextColor(ROWS[i][3], t.optInt("ov") == 1 ? 0xFFF87171 : later ? 0x80FFFFFF : 0x99FFFFFF);
            int pri = t.optInt("pri");
            rv.setInt(ROWS[i][1], "setBackgroundResource", pri >= 3 ? R.drawable.widget_cb_red : pri >= 2 ? R.drawable.widget_cb_hi : R.drawable.widget_cb);
            rv.setOnClickPendingIntent(ROWS[i][1], WidgetKit.broadcast(ctx, TasksWidget.class, ACTION_DONE, t.optString("id"), 2210 + i));
        }
        return rv;
    }

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        JSONObject data = WidgetKit.data(ctx);
        for (int id : ids) {
            final int wid = id;
            if (WidgetKit.scrollable(ctx, id)) mgr.updateAppWidget(id, buildList(ctx, data, id));
            else mgr.updateAppWidget(id, RouteWidgetBase.responsive(VARIANTS, v -> build(ctx, v, data, wid), mgr, id));
        }
    }

    /** Scrollable mode (Android 12+): every task in a list you can swipe through. */
    private RemoteViews buildList(Context ctx, JSONObject data, int wid) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), WidgetKit.layout(data, R.layout.widget_tasks_list, R.layout.widget_tasks_list_rtl));
        WidgetKit.applyStyle(ctx, rv, R.id.w_root, data, false, wid);
        rv.setOnClickPendingIntent(R.id.w_root, WidgetKit.open(ctx, "tasks", 2200));
        rv.setOnClickPendingIntent(R.id.w_t_add, WidgetKit.open(ctx, "add", 2201));
        JSONArray list0 = data.optJSONArray("todo"), next = data.optJSONArray("todoNext");
        int n0 = list0 == null ? 0 : list0.length();
        rv.setTextViewText(R.id.w_t_title, RouteWidgetBase.L(data, "todo", "Today's tasks") + (n0 > 0 ? " · " + WidgetKit.num(data, data.optInt("todoN", n0)) : ""));
        rv.setViewVisibility(R.id.w_t_count, View.GONE);
        JSONArray list = new JSONArray();
        for (int i = 0; i < n0; i++) list.put(list0.optJSONObject(i));
        if (next != null) for (int i = 0; i < next.length(); i++) { JSONObject x = next.optJSONObject(i); if (x != null) { try { x.put("later", 1); } catch (Exception ignored) { } list.put(x); } }
        int n = list.length();
        rv.setViewVisibility(R.id.w_t_empty, n == 0 ? View.VISIBLE : View.GONE);
        rv.setViewVisibility(R.id.w_list, n == 0 ? View.GONE : View.VISIBLE);
        rv.setTextViewText(R.id.w_t_empty, RouteWidgetBase.L(data, "noTasks", "No tasks today 🎉"));
        if (android.os.Build.VERSION.SDK_INT >= 31 && n > 0) {
            RemoteViews.RemoteCollectionItems.Builder b = new RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(1);
            for (int i = 0; i < n; i++) {
                JSONObject t = list.optJSONObject(i);
                if (t == null) continue;
                RemoteViews row = new RemoteViews(ctx.getPackageName(), WidgetKit.layout(data, R.layout.widget_tasks_row, R.layout.widget_tasks_row_rtl));
                row.setTextViewText(R.id.w_r_n, t.optString("n"));
                row.setTextColor(R.id.w_r_n, t.optInt("later") == 1 ? 0x99FFFFFF : 0xFFFFFFFF);
                row.setTextViewText(R.id.w_r_d, t.optString("d"));
                row.setTextColor(R.id.w_r_d, t.optInt("ov") == 1 ? 0xFFF87171 : 0x99FFFFFF);
                int pri = t.optInt("pri");
                row.setInt(R.id.w_r_cb, "setBackgroundResource", pri >= 3 ? R.drawable.widget_cb_red : pri >= 2 ? R.drawable.widget_cb_hi : R.drawable.widget_cb);
                row.setOnClickFillInIntent(R.id.w_r_cb, new Intent().putExtra("id", t.optString("id")));
                b.addItem(t.optString("id").hashCode(), row);
            }
            rv.setRemoteAdapter(R.id.w_list, b.build());
            Intent tpl = new Intent(ctx, TasksWidget.class).setAction(ACTION_DONE);
            rv.setPendingIntentTemplate(R.id.w_list, android.app.PendingIntent.getBroadcast(ctx, 2290, tpl,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_MUTABLE));
        }
        return rv;
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
            JSONArray nx = data.optJSONArray("todoNext"), keep2 = new JSONArray();
            if (nx != null) for (int i = 0; i < nx.length(); i++) { JSONObject t = nx.optJSONObject(i); if (t != null && !id.equals(t.optString("id"))) keep2.put(t); }
            try {
                data.put("todoNext", keep2);
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
