package com.kadir.twitterbots.populartweetfinder.scheduler;

import java.util.concurrent.ScheduledFuture;

/**
 * @author akadir
 * Date: 11/12/2018
 * Time: 11:18
 */
public interface ScheduledRunnable extends Runnable {
    void cancel();

    void setScheduledFuture(ScheduledFuture<?> scheduledFuture);
}
