package com.kadir.twitterbots.populartweetfinder.worker;

import com.kadir.twitterbots.populartweetfinder.filter.ContentBasedFilter;
import com.kadir.twitterbots.populartweetfinder.filter.DateFilter;
import com.kadir.twitterbots.populartweetfinder.filter.InteractionCountFilter;
import com.kadir.twitterbots.populartweetfinder.filter.StatusFilter;
import com.kadir.twitterbots.populartweetfinder.filter.UserBasedFilter;
import com.kadir.twitterbots.populartweetfinder.scheduler.ScheduledRunnable;
import com.kadir.twitterbots.populartweetfinder.scheduler.TaskScheduler;
import com.kadir.twitterbots.populartweetfinder.util.ApplicationConstants;
import org.apache.log4j.Logger;
import twitter4j.Status;
import twitter4j.Twitter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author akadir
 * Date: 08/12/2018
 * Time: 15:15
 */
public class TweetFilter {
    private final Logger logger = Logger.getLogger(this.getClass());

    private List<StatusFilter> filters;

    public TweetFilter() {
    }

    public void createFilters(Twitter twitter) {
        filters = new ArrayList<>();
        filters.add(new ContentBasedFilter());
        filters.add(new DateFilter());
        filters.add(new InteractionCountFilter());
        filters.add(new UserBasedFilter(twitter));
    }

    public void scheduleTasksForRunnableFilters() {
        for (StatusFilter s : filters) {
            if (s instanceof ScheduledRunnable) {
                TaskScheduler.scheduleWithFixedDelay((ScheduledRunnable) s, ApplicationConstants.INITIAL_DELAY_FOR_SCHEDULED_TASKS, ApplicationConstants.DELAY_FOR_SCHEDULED_TASKS);
            }
        }
    }

    public boolean canStatusBeUsed(Status status) {
        for (StatusFilter filter : filters) {
            if (!filter.passed(status)) {
                return false;
            }
        }
        return true;
    }

}
