package com.kadir.twitterbots.populartweetfinder.scheduler;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author akadir
 * Date: 11/12/2018
 * Time: 11:14
 */
public class TaskScheduler {
    private static final Logger logger = Logger.getLogger(TaskScheduler.class);

    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static List<ScheduledRunnable> scheduledTasks = new ArrayList<>();

    public static void scheduleWithFixedDelay(ScheduledRunnable runnable, int initialDelayForScheduledTasks, int delayForScheduledTasks) {
        ScheduledFuture<?> scheduledFuture = scheduler.scheduleWithFixedDelay(runnable, initialDelayForScheduledTasks, delayForScheduledTasks, TimeUnit.MINUTES);
        runnable.setScheduledFuture(scheduledFuture);
        scheduledTasks.add(runnable);
        logger.info("task scheduled: " + runnable.getClass().getSimpleName() + " - " + initialDelayForScheduledTasks + " - " + delayForScheduledTasks);
    }

    public static void cancelAllScheduledTasks() {
        Iterator<ScheduledRunnable> iterator = scheduledTasks.iterator();
        while (iterator.hasNext()) {
            ScheduledRunnable task = iterator.next();
            task.cancel();
            iterator.remove();
        }
    }
}
