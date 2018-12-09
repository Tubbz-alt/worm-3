package com.kadir.twitterbots.populartweetfinder.filter;

import twitter4j.Status;

/**
 * @author akadir
 * Date: 08/12/2018
 * Time: 16:00
 */
public interface StatusFilter {
    boolean passed(Status status);
}
