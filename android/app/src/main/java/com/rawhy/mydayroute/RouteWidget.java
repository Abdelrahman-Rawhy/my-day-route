package com.rawhy.mydayroute;

/** Day route widget: compact two lines, wide two columns, or full with times and progress. */
public class RouteWidget extends RouteWidgetBase {
    private static final Variant[] VARIANTS = {
        new Variant(R.layout.widget_route_s, 40f, 40f,
            R.id.w_root, R.id.w_prayer_name, R.id.w_prayer_cd, R.id.w_step_name, R.id.w_step_cd),
        new Variant(R.layout.widget_route_w, 220f, 40f,
            R.id.w_root, R.id.w_prayer_name, R.id.w_prayer_cd, R.id.w_step_name, R.id.w_step_cd),
        new Variant(R.layout.widget_route, 220f, 100f,
            R.id.w_root, R.id.w_prayer_name, R.id.w_prayer_cd, R.id.w_prayer_at,
            R.id.w_step_name, R.id.w_step_cd, R.id.w_step_at, R.id.w_progress),
    };

    @Override
    protected Variant[] variants() {
        return VARIANTS;
    }
}
