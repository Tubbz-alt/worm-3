package com.kadir.twitterbots.populartweetfinder.scheduler;

import com.kadir.twitterbots.populartweetfinder.entity.TaskPriority;

/**
 * @author akadir
 * Date: 11/12/2018
 * Time: 11:18
 */
public interface ScheduledRunnable extends Runnable {

    void schedule();

    void cancel();

    void cancelNow();

    TaskPriority getPriority();
}
