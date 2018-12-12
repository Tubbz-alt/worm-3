package com.kadir.twitterbots.populartweetfinder.scheduler;

import com.kadir.twitterbots.populartweetfinder.entity.TaskPriority;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author akadir
 * Date: 11/12/2018
 * Time: 11:14
 */
public class TaskScheduler {
    private static final Logger logger = Logger.getLogger(TaskScheduler.class);

    private static List<ScheduledRunnable> scheduledTasks = new ArrayList<>();

    private TaskScheduler() {
    }

    public static void addScheduledTask(ScheduledRunnable scheduledRunnable) {
        scheduledTasks.add(scheduledRunnable);
        logger.debug("add scheduled task into list: " + scheduledRunnable.getClass().getSimpleName());
    }

    public static void shutdownLowerPriorityTasks(ScheduledRunnable scheduledRunnable) {
        TaskPriority basePriority = scheduledRunnable.getPriority();
        Iterator<ScheduledRunnable> iterator = scheduledTasks.iterator();
        while (iterator.hasNext()) {
            ScheduledRunnable task = iterator.next();
            if (task.getPriority().getLevel() < basePriority.getLevel()) {
                task.cancel();
                iterator.remove();
            }
        }
    }

    public static void shutdownAllTasks() {
        Iterator<ScheduledRunnable> iterator = scheduledTasks.iterator();
        while (iterator.hasNext()) {
            ScheduledRunnable task = iterator.next();
            task.cancelNow();
            iterator.remove();
        }
    }
}
