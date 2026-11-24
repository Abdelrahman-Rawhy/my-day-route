package com.rawhy.mydayroute;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONObject;

/** Full-screen alarm screen (over the lock screen): big clock, title, Snooze and Stop. Built in code, no layout XML. */
public class AlarmActivity extends Activity {
    static WeakReference<AlarmActivity> cur;
    private JSONObject a = new JSONObject();
    private final Handler h = new Handler(Looper.getMainLooper());
    private TextView clock;

    static void closeIfShowing(int id) {
        AlarmActivity x = cur == null ? null : cur.get();
        if (x != null && x.a.optInt("id") == id) x.runOnUiThread(x::finish);
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private Button btn(String t, int bg, int fg) {
        Button b = new Button(this);
        b.setText(t);
        b.setAllCaps(false);
        b.setTextSize(20);
        b.setTextColor(fg);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable d = new GradientDrawable();
        d.setColor(bg);
        d.setCornerRadius(dp(22));
        b.setBackground(d);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(68));
        lp.topMargin = dp(14);
        b.setLayoutParams(lp);
        return b;
    }

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        a = AlarmKit.parse(getIntent());
        cur = new WeakReference<>(this);
        boolean ar = AlarmKit.isAr(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(40), dp(28), dp(40));
        root.setBackgroundColor(Color.parseColor("#1E1610"));
        root.setLayoutDirection(ar ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);

        clock = new TextView(this);
        clock.setTextSize(72);
        clock.setTextColor(Color.parseColor("#F6EFE2"));
        clock.setTypeface(Typeface.DEFAULT_BOLD);
        clock.setGravity(Gravity.CENTER);
        root.addView(clock);

        TextView t = new TextView(this);
        t.setText(a.optString("title", "⏰"));
        t.setTextSize(26);
        t.setTextColor(Color.WHITE);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(18), 0, dp(8));
        root.addView(t);

        TextView b = new TextView(this);
        b.setText(a.optString("body", ""));
        b.setTextSize(17);
        b.setTextColor(Color.parseColor("#E8D9C2"));
        b.setGravity(Gravity.CENTER);
        b.setLineSpacing(0, 1.25f);
        root.addView(b);

        View gap = new View(this);
        root.addView(gap, new LinearLayout.LayoutParams(1, dp(56)));

        Button snz = btn(a.optString("snooze", ar ? "غفوة 5 دقايق" : "Snooze 5 min"), Color.parseColor("#3A2C1F"), Color.parseColor("#F6EFE2"));
        Button stop = btn(a.optString("stop", ar ? "إيقاف" : "Stop"), Color.parseColor("#C9963F"), Color.parseColor("#1E1610"));
        root.addView(snz);
        root.addView(stop);
        snz.setOnClickListener(v -> { AlarmKit.snooze(this, a); finish(); });
        stop.setOnClickListener(v -> { AlarmKit.stop(this, a); openApp(); finish(); });

        setContentView(root);
        tick();
        h.postDelayed(this::finish, AlarmKit.RING_MS);
    }

    private void tick() {
        clock.setText(new SimpleDateFormat("h:mm", Locale.US).format(new Date()));
        h.postDelayed(this::tick, 15000);
    }

    /** After Stop: open Yawmi on the right screen, but only when the phone is unlocked (no unlock prompt at 5 am). */
    private void openApp() {
        String link = a.optString("link", "");
        if (link.isEmpty()) return;
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (km != null && km.isKeyguardLocked()) return;
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(link), this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception ignored) {}
    }

    @Override
    public void onBackPressed() { /* Back does nothing: choose Stop or Snooze. */ }

    @Override
    protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        if (cur != null && cur.get() == this) cur = null;
        super.onDestroy();
    }
}
