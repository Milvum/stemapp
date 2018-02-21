package com.milvum.stemapp.ballotexchange;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;

/**
 * .
 * Scheduler for background jobs that are involved with exchanging a Voting Pass for a Voting Ballot.
 */

public class ExchangeScheduler {
    /**
     * Schedule a job that is part of the Voting Pass - Voting Ballot exchange.
     * @param job - The type of job that needs to be scheduled
     */
    public static void scheduleJob(Context context, MixJob job) {
        JobInfo.Builder builder = makeBuilder(context, job);

        scheduleJobWithBuilder(context, builder);
    }


    /**
     * Schedule a job that is part of the Voting Pass - Voting Ballot exchange.
     * @param job - The type of job that needs to be scheduled
     * @param extras - The parameters that need to be passed to the job
     */
    public static void scheduleJob(Context context, MixJob job, PersistableBundle extras) {
        JobInfo.Builder builder = makeBuilder(context, job);
        builder.setExtras(extras);

        scheduleJobWithBuilder(context, builder);
    }

    private static JobInfo.Builder makeBuilder(Context context, MixJob job) {
        // setup the builder of the job
        ComponentName serviceComponent = new ComponentName(context, job.getJobClass());
        return new JobInfo.Builder(job.getId(), serviceComponent);
    }

    private static void scheduleJobWithBuilder(Context context, JobInfo.Builder builder) {
        // any extra requirements
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

        // schedule the job
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.schedule(builder.build());
    }
}
