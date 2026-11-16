package com.rawhy.mydayroute;

import android.content.Context;
import android.content.SharedPreferences;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Receives the next prayers and steps from the app and refreshes the home screen widgets. */
@CapacitorPlugin(name = "WidgetBridge")
public class WidgetBridge extends Plugin {
    @PluginMethod
    public void setData(PluginCall call) {
        String data = call.getString("data", "{}");
        Context ctx = getContext();
        ctx.getSharedPreferences(RouteWidgetBase.PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(RouteWidgetBase.KEY, data)
            .apply();
        RouteWidgetBase.refreshAll(ctx);
        call.resolve();
    }

    /** Steps checked on the home screen widget while the app was closed. Returns them once, then clears. */
    @PluginMethod
    public void getPending(PluginCall call) {
        SharedPreferences sp = getContext().getSharedPreferences(RouteWidgetBase.PREFS, Context.MODE_PRIVATE);
        String pending = sp.getString(StepWidget.PENDING, "[]");
        sp.edit().putString(StepWidget.PENDING, "[]").apply();
        JSObject ret = new JSObject();
        ret.put("data", pending);
        call.resolve(ret);
    }
}
