package com.rawhy.mydayroute;

/** Small widget: next prayer countdown only. */
public class PrayerWidget extends RouteWidgetBase {
    @Override
    protected int layoutId() {
        return R.layout.widget_prayer;
    }

    @Override
    protected boolean showSteps() {
        return false;
    }
}
