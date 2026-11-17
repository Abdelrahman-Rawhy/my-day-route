package com.rawhy.mydayroute;

import android.app.job.JobParameters;
import android.app.job.JobService;

/** Runs SelfUpdate.runCheck() in the background every few hours. */
public class UpdateJob extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        new Thread(() -> {
            SelfUpdate.runCheck(getApplicationContext());
            jobFinished(params, false);
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
