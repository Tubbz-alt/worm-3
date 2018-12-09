package com.kadir.twitterbots.populartweetfinder.worker;

import com.kadir.twitterbots.populartweetfinder.filter.ContentBasedFilter;
import com.kadir.twitterbots.populartweetfinder.filter.DateFilter;
import com.kadir.twitterbots.populartweetfinder.filter.InteractionCountFilter;
import com.kadir.twitterbots.populartweetfinder.filter.StatusFilter;
import com.kadir.twitterbots.populartweetfinder.filter.UserBasedFilter;
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

    private List<StatusFilter> filterList = new ArrayList<>();
    private UserBasedFilter userBasedFilter;

    public TweetFilter(Twitter twitter) {
        userBasedFilter = new UserBasedFilter(twitter);
        addFiltersToSet(twitter);
        userBasedFilter.start();
    }

    private void addFiltersToSet(Twitter twitter) {
        filterList.add(new DateFilter());
        filterList.add(userBasedFilter);
        filterList.add(new InteractionCountFilter());
        filterList.add(new ContentBasedFilter());
    }

    public boolean canStatusBeUsed(Status status) {
        for (StatusFilter filter : filterList) {
            if (!filter.passed(status)) {
                return false;
            }
        }
        return true;
    }
}
