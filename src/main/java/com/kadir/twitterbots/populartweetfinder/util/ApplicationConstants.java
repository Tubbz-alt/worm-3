package com.kadir.twitterbots.populartweetfinder.util;

import java.text.SimpleDateFormat;

/**
 * @author akadir
 * Date: 08/12/2018
 * Time: 13:55
 */
public class ApplicationConstants {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static final String PROPERTIES_FILE_NAME = "popularTweetFinder.properties";
    public static final String RESOURCES_FOLDER_NAME = "resources/";
    public static final int MAX_PASSIVE_PERIOD = 40;

    private ApplicationConstants() {
    }
}
