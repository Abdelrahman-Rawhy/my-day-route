package com.rawhy.mydayroute;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

/** JS side of the real alarms. sync({alarms}) replaces the whole set; status(); openSettings({which}); test({sec}); last(). */
@CapacitorPlugin(name = "AlarmBridge")
public class AlarmBridge extends Plugin {

    @PluginMethod
    public void sync(PluginCall call) {
        try {
            JSArray in = call.getArray("alarms", new JSArray());
            int n = AlarmKit.replaceAll(getContext(), new JSONArray(in.toString()));
            JSObject r = new JSObject();
            r.put("armed", n);
            call.resolve(r);
        } catch (Exception e) {
            call.reject("sync failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void status(PluginCall call) {
        JSObject r = new JSObject();
        r.put("exact", AlarmKit.canExact(getContext()));
        r.put("full", AlarmKit.canFullScreen(getContext()));
        boolean notif = true;
        try {
            android.app.NotificationManager nm = (android.app.NotificationManager) getContext().getSystemService(android.content.Context.NOTIFICATION_SERVICE);
            if (nm != null && Build.VERSION.SDK_INT >= 24) notif = nm.areNotificationsEnabled();
        } catch (Exception ignored) {}
        r.put("notif", notif);
        r.put("count", AlarmKit.load(getContext()).length());
        call.resolve(r);
    }

    @PluginMethod
    public void openSettings(PluginCall call) {
        String w = call.getString("which", "exact");
        String pkg = getContext().getPackageName();
        Intent i = null;
        if ("exact".equals(w) && Build.VERSION.SDK_INT >= 31) i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + pkg));
        else if ("full".equals(w) && Build.VERSION.SDK_INT >= 34) i = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:" + pkg));
        else if (Build.VERSION.SDK_INT >= 26) i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg);
        if (i == null) i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { getContext().startActivity(i); call.resolve(); } catch (Exception e) {
            try {
                Intent d = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(d);
                call.resolve();
            } catch (Exception e2) { call.reject("no settings screen"); }
        }
    }

    /** A test alarm in N seconds (id 99), separate from the synced set. */
    @PluginMethod
    public void test(PluginCall call) {
        try {
            JSONObject a = new JSONObject();
            a.put("id", 99);
            a.put("at", System.currentTimeMillis() + Math.max(5, call.getInt("sec", 10)) * 1000L);
            a.put("title", call.getString("title", "⏰"));
            a.put("body", call.getString("body", ""));
            a.put("stop", call.getString("stop", "Stop"));
            a.put("snooze", call.getString("snooze", "Snooze"));
            a.put("snz", 5);
            a.put("link", "");
            AlarmKit.put(getContext(), a);
            call.resolve();
        } catch (Exception e) { call.reject("test failed"); }
    }

    /** The link of the alarm stopped last and when, so the app can react (e.g. ask «صحيت؟»). */
    @PluginMethod
    public void last(PluginCall call) {
        JSObject r = new JSObject();
        r.put("link", AlarmKit.sp(getContext()).getString("last", ""));
        r.put("at", AlarmKit.sp(getContext()).getLong("lastAt", 0));
        call.resolve(r);
    }
}
