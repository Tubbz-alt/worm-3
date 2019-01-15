package com.kadir.twitterbots.worm.util;

/**
 * @author akadir
 * Date: 08/12/2018
 * Time: 13:55
 */
public class ApplicationConstants {
    public static final String PROPERTIES_FILE_NAME = "application.properties";
    public static final String RESOURCES_FOLDER_NAME = "resources/";
    public static final int MAX_PASSIVE_PERIOD = 40;
    public static final int CHECK_DELETED_STATUSES_PERIOD = 30;
    public static final int DEFAULT_INITIAL_DELAY_FOR_SCHEDULED_TASKS = 0;
    public static final int DEFAULT_DELAY_FOR_SCHEDULED_TASKS = 15;

    private ApplicationConstants() {
    }
}