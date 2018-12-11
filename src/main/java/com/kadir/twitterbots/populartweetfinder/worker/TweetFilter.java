package com.kadir.twitterbots.populartweetfinder.worker;

import com.kadir.twitterbots.populartweetfinder.filter.ContentBasedFilter;
import com.kadir.twitterbots.populartweetfinder.filter.DateFilter;
import com.kadir.twitterbots.populartweetfinder.filter.InteractionCountFilter;
import com.kadir.twitterbots.populartweetfinder.filter.StatusFilter;
import com.kadir.twitterbots.populartweetfinder.filter.UserBasedFilter;
import com.kadir.twitterbots.populartweetfinder.scheduler.ScheduledRunnable;
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
    private List<StatusFilter> filters;

    public void initForFetch(Twitter twitter) {
        createFetchFilters(twitter);
        scheduleTasksForRunnableFilters();
    }

    public void initForQuote(Twitter twitter) {
        createQuoteFilters(twitter);
    }

    private void createQuoteFilters(Twitter twitter) {
        filters = new ArrayList<>();
        filters.add(new ContentBasedFilter());
        filters.add(new UserBasedFilter(twitter));
    }

    private void createFetchFilters(Twitter twitter) {
        filters = new ArrayList<>();
        filters.add(new ContentBasedFilter());
        filters.add(new DateFilter());
        filters.add(new InteractionCountFilter());
        filters.add(new UserBasedFilter(twitter));
    }

    private void scheduleTasksForRunnableFilters() {
        for (StatusFilter s : filters) {
            if (s instanceof ScheduledRunnable) {
                ((ScheduledRunnable) s).schedule();
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
