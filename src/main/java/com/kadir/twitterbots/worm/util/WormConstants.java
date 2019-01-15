package com.kadir.twitterbots.worm.util;

/**
 * @author akadir
 * Date: 08/12/2018
 * Time: 13:55
 */
public class WormConstants {
    public static final String PROPERTIES_FILE_NAME = "worm.properties";
    public static final String AUTH_PROPERTIES_FILE_NAME = "auth.properties";
    public static final String FETCH_API_KEYS_PREFIX = "finder-";
    public static final String QUOTE_API_KEYS_PREFIX = "quoter-";
    public static final String RESOURCES_FOLDER_NAME = "resources/";
    public static final int MAX_PASSIVE_PERIOD = 40;
    public static final int CHECK_DELETED_STATUSES_PERIOD = 30;
    public static final int DEFAULT_INITIAL_DELAY_FOR_SCHEDULED_TASKS = 0;
    public static final int DEFAULT_DELAY_FOR_SCHEDULED_TASKS = 15;

    private WormConstants() {
    }
}
