package com.rawhy.mydayroute;

import android.content.Context;
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
}
