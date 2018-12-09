package com.kadir.twitterbots.populartweetfinder.util;

import twitter4j.Status;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author akadir
 * Date: 09/12/2018
 * Time: 01:40
 */
public class StatusUtil {

    private StatusUtil() {
    }

    public static String getStatusLink(Status status) {
        return "https://twitter.com/" + status.getUser().getScreenName() + "/status/" + status.getId();
    }

    public static int calculateInteractionCount(Status status) {
        return (status.getFavoriteCount() + status.getRetweetCount());
    }

    public static int calculateWeightedInteractionCount(Status status) {
        int score = status.getFavoriteCount() + status.getRetweetCount();
        SimpleDateFormat dateFormat = ApplicationConstants.DATE_FORMAT;
        Date today = new Date();
        if (dateFormat.format(status.getCreatedAt()).equals(dateFormat.format(today))) {
            return score;
        } else {
            Calendar statusCreation = Calendar.getInstance();
            Calendar currentTime = Calendar.getInstance();
            statusCreation.setTime(status.getCreatedAt());
            currentTime.setTime(today);

            double currentHour = currentTime.get(Calendar.HOUR_OF_DAY);

            double totalHourPassed = (24 * (currentTime.get(Calendar.DAY_OF_YEAR) - statusCreation.get(Calendar.DAY_OF_YEAR))) - statusCreation.get(Calendar.HOUR_OF_DAY) + currentHour;

            return (int) (score * (currentHour / totalHourPassed));
        }
    }

}
