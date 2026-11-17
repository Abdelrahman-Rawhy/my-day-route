package com.rawhy.mydayroute;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import org.json.JSONObject;

/** The «Settings» screen from a widget's long-press menu: look (glass/black, opacity, corners, margins) and, for lists, fixed or scrollable. */
public class WidgetConfigActivity extends Activity {
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private boolean ar = true, listType = false;
    private String kind = "";
    private String style = "glass";
    private int op = 2, rad = 3, pad = 2;
    private boolean scroll = false;
    private LinearLayout previewBox;
    private TextView[] styleBtns = new TextView[2], modeBtns = new TextView[2];
    private TextView opVal, radVal, padVal;
    private static final int[] OP = { 0, 15, 30, 50, 75, 95 }, RD = { 0, 12, 20, 28, 36 };

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private String t(String a, String e) { return ar ? a : e; }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setResult(RESULT_CANCELED);
        Bundle ex = getIntent().getExtras();
        if (ex != null) widgetId = ex.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return; }
        JSONObject data = WidgetKit.data(this);
        ar = data.optBoolean("rtl", true);
        AppWidgetProviderInfo info = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId);
        String cls = info == null ? "" : info.provider.getClassName();
        listType = cls.endsWith("CountdownWidget") || cls.endsWith("TasksWidget");
        kind = cls.substring(cls.lastIndexOf('.') + 1);
        JSONObject look = WidgetKit.look(this, data, widgetId);
        style = look.optString("s", "glass");
        op = look.optInt("o", 2);
        rad = look.optInt("r", 3);
        pad = look.optInt("p", 2);
        scroll = look.optBoolean("scroll", false);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        setContentView(build());
        refresh();
    }

    private GradientDrawable round(int color, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(r));
        return g;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        return v;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(20), dp(18), dp(20), dp(18));
        c.setBackground(round(0xFF1C1C1E, 24));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(14);
        c.setLayoutParams(lp);
        return c;
    }

    private LinearLayout pair(TextView[] holder, String a, String b2, View.OnClickListener ca, View.OnClickListener cb) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.topMargin = dp(12);
        row.setLayoutParams(rp);
        String[] labels = { a, b2 };
        View.OnClickListener[] cl = { ca, cb };
        for (int i = 0; i < 2; i++) {
            TextView v = text(labels[i], 16, Color.WHITE, true);
            v.setGravity(Gravity.CENTER);
            v.setMinHeight(dp(52));
            v.setOnClickListener(cl[i]);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(52), 1f);
            if (i == 0) lp.setMarginEnd(dp(10));
            v.setLayoutParams(lp);
            holder[i] = v;
            row.addView(v);
        }
        return row;
    }

    private LinearLayout slider(String label, int max, int val, TextView[] out, SeekBar.OnSeekBarChangeListener l, String lo, String hi) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.topMargin = dp(16);
        box.setLayoutParams(bp);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        TextView name = text(label, 16, Color.WHITE, true);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = text("", 14, 0xFFAAAAAA, true);
        out[0] = v;
        head.addView(name);
        head.addView(v);
        box.addView(head);
        SeekBar sb = new SeekBar(this);
        sb.setMax(max);
        sb.setProgress(val);
        sb.setOnSeekBarChangeListener(l);
        sb.setPadding(dp(6), dp(14), dp(6), dp(8));
        box.addView(sb);
        LinearLayout ends = new LinearLayout(this);
        TextView a = text(lo, 12, 0xFF888888, false), z = text(hi, 12, 0xFF888888, false);
        a.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ends.addView(a);
        ends.addView(z);
        box.addView(ends);
        return box;
    }

    private SeekBar.OnSeekBarChangeListener onChange(java.util.function.IntConsumer f) {
        return new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) { f.accept(p); refresh(); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        };
    }

    private View build() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.BLACK);
        sv.setLayoutDirection(ar ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(36), dp(18), dp(40));
        sv.addView(root);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView close = text("✕", 24, Color.WHITE, false);
        close.setPadding(dp(8), dp(8), dp(16), dp(8));
        close.setOnClickListener(v -> finish());
        TextView title = text(t("إعدادات الويدجت", "Widget settings"), 22, Color.WHITE, true);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView ok = text("✓", 26, 0xFF22D3EE, true);
        ok.setPadding(dp(16), dp(8), dp(8), dp(8));
        ok.setOnClickListener(v -> save());
        bar.addView(close);
        bar.addView(title);
        bar.addView(ok);
        root.addView(bar);

        // preview on a wallpaper-like background
        FrameLayout wall = new FrameLayout(this);
        GradientDrawable wg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] { 0xFF3B5B8A, 0xFF16233F, 0xFF3A2A5C });
        wg.setCornerRadius(dp(26));
        wall.setBackground(wg);
        wall.setPadding(dp(30), dp(30), dp(30), dp(30));
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wp.topMargin = dp(16);
        wall.setLayoutParams(wp);
        previewBox = new LinearLayout(this);
        previewBox.setOrientation(LinearLayout.VERTICAL);
        // preview rows that look like the widget being edited
        String[][] rows;
        int numColor = 0xFF6B8CFF;
        switch (kind) {
            case "TasksWidget":
                rows = ar ? new String[][] { { "☐", "الدواء", "٩:٠٠ م" }, { "☐", "تسليم المذكرة", "متأخرة" }, { "☐", "اتصل بالموكل", "" } }
                    : new String[][] { { "☐", "Medicine", "9:00 PM" }, { "☐", "Send the memo", "Overdue" }, { "☐", "Call the client", "" } };
                numColor = 0x99FFFFFF;
                break;
            case "FaithWidget":
                rows = ar ? new String[][] { { "📿", "أذكار الصباح", "٧/١٩" }, { "📖", "ورد القرآن", "٢/٤" }, { "📿", "التسبيح", "١٠٠" } }
                    : new String[][] { { "📿", "Morning adhkar", "7/19" }, { "📖", "Quran today", "2/4" }, { "📿", "Tasbih", "100" } };
                numColor = Color.WHITE;
                break;
            case "HabitsWidget":
                rows = ar ? new String[][] { { "💧", "اشرب مية", "٣ / ١٠" }, { "🌙", "النوم بدري", "٠ / ١" } }
                    : new String[][] { { "💧", "Drink water", "3 / 10" }, { "🌙", "Early to bed", "0 / 1" } };
                numColor = 0xB3FFFFFF;
                break;
            case "PrayerDayWidget":
            case "PrayerWidget":
            case "RouteWidget":
                rows = ar ? new String[][] { { "🕌", "الصلاة الجاية · العصر", "١:٢٤:١٠" }, { "", "الفجر ٤:٥٢   الظهر ١١:٤١   المغرب ٤:٥٨", "" } }
                    : new String[][] { { "🕌", "Next · Asr", "1:24:10" }, { "", "Fajr 4:52   Dhuhr 11:41   Maghrib 4:58", "" } };
                numColor = 0xFFFBBF24;
                break;
            case "StepWidget":
                rows = ar ? new String[][] { { "✓", "أذكار الصباح", "١ / ١٥" }, { "", "بعدها: قراءة القرآن · ٥:٢٥ ص", "" } }
                    : new String[][] { { "✓", "Morning adhkar", "1/15" }, { "", "Then: Read Quran · 5:25 AM", "" } };
                numColor = 0xB3FFFFFF;
                break;
            default:
                rows = ar ? new String[][] { { "🌙", "رمضان", "٨٣" }, { "🎉", "عيد الفطر", "١١٢" }, { "🎂", "عيد ميلادي", "٢٤٩" } }
                    : new String[][] { { "🌙", "Ramadan", "83" }, { "🎉", "Eid al-Fitr", "112" }, { "🎂", "My birthday", "249" } };
        }
        for (String[] r : rows) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));
            TextView e = text(r[0], 18, Color.WHITE, false);
            TextView n = text(r[1], r[0].isEmpty() ? 13 : 16, r[0].isEmpty() ? 0xB3FFFFFF : Color.WHITE, !r[0].isEmpty());
            n.setPadding(r[0].isEmpty() ? 0 : dp(12), 0, dp(12), 0);
            n.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView d = text(r[2], 20, numColor, true);
            if (!r[0].isEmpty()) row.addView(e);
            row.addView(n);
            row.addView(d);
            previewBox.addView(row);
        }
        wall.addView(previewBox, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(wall);

        LinearLayout look = card();
        look.addView(text(t("الشكل", "Look"), 17, Color.WHITE, true));
        look.addView(pair(styleBtns, t("زجاج شفاف", "Clear glass"), t("أسود", "Black"), v -> { style = "glass"; refresh(); }, v -> { style = "black"; refresh(); }));
        TextView[] h1 = new TextView[1], h2 = new TextView[1], h3 = new TextView[1];
        look.addView(slider(t("درجة التعتيم", "Opacity"), 5, op, h1, onChange(p -> op = p), t("شفاف", "Clear"), t("مُصمت", "Solid")));
        look.addView(slider(t("استدارة الحواف", "Corners"), 4, rad, h2, onChange(p -> rad = p), t("حادة", "Square"), t("دائرية", "Round")));
        look.addView(slider(t("الهوامش الداخلية", "Inner margins"), 3, pad, h3, onChange(p -> pad = p), t("ضيقة", "Tight"), t("واسعة", "Roomy")));
        opVal = h1[0];
        radVal = h2[0];
        padVal = h3[0];
        root.addView(look);

        if (listType) {
            LinearLayout mode = card();
            mode.addView(text(t("طريقة العرض", "Display mode"), 17, Color.WHITE, true));
            TextView hint = text(t("قابل للتمرير: كل العناصر وتقدر تسحب بينها. ثابت: اللي يكفي حجم الويدجت بس.", "Scrollable: every item, swipe through them. Fixed: only what fits the widget's size."), 13, 0xFF999999, false);
            hint.setPadding(0, dp(6), 0, 0);
            mode.addView(hint);
            mode.addView(pair(modeBtns, t("ثابت", "Fixed"), t("قابل للتمرير", "Scrollable"), v -> { scroll = false; refresh(); }, v -> { scroll = true; refresh(); }));
            if (android.os.Build.VERSION.SDK_INT < 31) modeBtns[1].setEnabled(false);
            root.addView(mode);
        }

        TextView note = text(t("الإعدادات دي للويدجت دي بس. الشكل العام لكل الويدجت من التطبيق: الإعدادات ← المظهر.", "These settings are for this widget only. The look for all widgets is in the app: Settings → Appearance."), 13, 0xFF888888, false);
        note.setPadding(dp(4), dp(18), dp(4), 0);
        root.addView(note);
        return sv;
    }

    private void refresh() {
        int res = WidgetKit.bgRes(this, style, rad, op);
        if (previewBox != null && res != 0) {
            previewBox.setBackgroundResource(res);
            int p = dp(WidgetKit.PAD_DP[WidgetKit.clamp(pad, 0, 3)]);
            previewBox.setPadding(p, p, p, p);
        }
        String[] sk = { "glass", "black" };
        for (int i = 0; i < 2; i++) if (styleBtns[i] != null) styleBtns[i].setBackground(sel(sk[i].equals(style)));
        if (modeBtns[0] != null) {
            modeBtns[0].setBackground(sel(!scroll));
            modeBtns[1].setBackground(sel(scroll));
        }
        if (opVal != null) opVal.setText(WidgetKit.num(WidgetKit.data(this), OP[WidgetKit.clamp(op, 0, 5)]) + "%");
        if (radVal != null) radVal.setText(WidgetKit.num(WidgetKit.data(this), RD[WidgetKit.clamp(rad, 0, 4)]));
        if (padVal != null) padVal.setText(WidgetKit.num(WidgetKit.data(this), WidgetKit.PAD_DP[WidgetKit.clamp(pad, 0, 3)]));
    }

    private GradientDrawable sel(boolean on) {
        GradientDrawable g = round(on ? 0x387C5CFF : 0xFF2A2A2E, 16);
        g.setStroke(dp(on ? 2 : 1), on ? 0xFF7C5CFF : 0x22FFFFFF);
        return g;
    }

    private void save() {
        try {
            JSONObject c = new JSONObject();
            c.put("s", style);
            c.put("o", op);
            c.put("r", rad);
            c.put("p", pad);
            c.put("scroll", scroll);
            getSharedPreferences(RouteWidgetBase.PREFS, Context.MODE_PRIVATE).edit().putString("wcfg_" + widgetId, c.toString()).apply();
        } catch (Exception ignored) {
        }
        RouteWidgetBase.refreshAll(this);
        Intent r = new Intent();
        r.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        setResult(RESULT_OK, r);
        finish();
    }
}
