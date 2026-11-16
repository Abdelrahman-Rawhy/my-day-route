package com.rawhy.mydayroute;

/** Large widget: next prayer + next step countdowns and today's progress. */
public class RouteWidget extends RouteWidgetBase {
    @Override
    protected int layoutId() {
        return R.layout.widget_route;
    }

    @Override
    protected boolean showSteps() {
        return true;
    }
}
