package com.kadir.twitterbots.populartweetfinder.scheduler;

import com.kadir.twitterbots.populartweetfinder.entity.TaskPriority;
import org.apache.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author akadir
 * Date: 11/12/2018
 * Time: 23:43
 */
public abstract class BaseScheduledRunnable implements ScheduledRunnable {
    private final Logger logger = Logger.getLogger(this.getClass());

    protected TaskPriority priority;
    protected ScheduledExecutorService executorService;
    protected volatile ScheduledFuture<?> scheduledFuture;

    public BaseScheduledRunnable(TaskPriority priority) {
        executorService = Executors.newScheduledThreadPool(1);
        this.priority = priority;
    }

    @Override
    public void cancel() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(10 * 1000L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error(e);
            Thread.currentThread().interrupt();
        }
        logger.info("shutdown scheduled task: " + this.getClass().getSimpleName());
    }

    @Override
    public TaskPriority getPriority() {
        return priority;
    }
}
