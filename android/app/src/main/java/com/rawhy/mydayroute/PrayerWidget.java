package com.rawhy.mydayroute;

/** Next prayer widget: tiny (name + countdown) or full (with label and time). */
public class PrayerWidget extends RouteWidgetBase {
    private static final Variant[] VARIANTS = {
        new Variant(R.layout.widget_prayer_s, 40f, 40f, R.id.w_root, R.id.w_prayer_name, R.id.w_prayer_cd),
        new Variant(R.layout.widget_prayer, 100f, 90f, R.id.w_root, R.id.w_prayer_name, R.id.w_prayer_cd, R.id.w_prayer_at),
    };

    @Override
    protected Variant[] variants() {
        return VARIANTS;
    }
}
