package com.kadir.twitterbots.populartweetfinder;

import com.kadir.twitterbots.populartweetfinder.authentication.BotAuthentication;
import com.kadir.twitterbots.populartweetfinder.dao.DatabaseInitialiser;
import com.kadir.twitterbots.populartweetfinder.exceptions.PropertyNotLoadedException;
import com.kadir.twitterbots.populartweetfinder.util.ApplicationConstants;
import com.kadir.twitterbots.populartweetfinder.worker.DatabaseWorker;
import com.kadir.twitterbots.populartweetfinder.worker.TweetFetcher;
import org.apache.log4j.Logger;
import twitter4j.Twitter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author akadir
 * Date: 08/12/2018
 * Time: 13:27
 */
public class PopularTweetFinder implements Runnable {
    private final Logger logger = Logger.getLogger(this.getClass());

    private Thread popularTweetFinderThread;
    private TweetFetcher tweetFetcher;
    private DatabaseWorker databaseWorker;
    private volatile ScheduledFuture<?> databaseWorkerScheduler;

    public static void main(String[] args) {
        PopularTweetFinder popularTweetFinder = new PopularTweetFinder();
        popularTweetFinder.start();
    }

    public PopularTweetFinder() {
        setVmArgumentsFromPropertyFile();
        String consumerKey = System.getProperty("finderConsumerKey");
        String consumerSecret = System.getProperty("finderConsumerSecret");
        String accessToken = System.getProperty("finderAccessToken");
        String accessTokenSecret = System.getProperty("finderAccessTokenSecret");

        BotAuthentication botAuthentication = new BotAuthentication(consumerKey, consumerSecret, accessToken, accessTokenSecret);

        Twitter twitter = botAuthentication.authenticate();
        DatabaseInitialiser.initializeDatabase();

        tweetFetcher = new TweetFetcher(twitter);
        databaseWorker = new DatabaseWorker(tweetFetcher);
    }

    public void start() {
        tweetFetcher.start();

        logger.info("Starting popularTweetFinderThread");
        if (popularTweetFinderThread == null) {
            popularTweetFinderThread = new Thread(this, this.getClass().getSimpleName());
            popularTweetFinderThread.start();
            logger.info("popularTweetFinderThread started");
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        try {
            databaseWorkerScheduler = scheduler.scheduleWithFixedDelay(databaseWorker, 1, 1, TimeUnit.MINUTES);

            logger.info("Scheduled");
        } catch (Exception e) {
            logger.error(e);
        }

        try {
            Thread.sleep(1000L * 60 * 2);
            databaseWorkerScheduler.cancel(false);
        } catch (InterruptedException e) {
            logger.error(e);
            Thread.currentThread().interrupt();
        }
    }

    public void run() {
        while (!popularTweetFinderThread.isInterrupted()) {
            try {
                Thread.sleep(1000L * 60 * 3);
            } catch (InterruptedException e) {
                logger.error(e);
                Thread.currentThread().interrupt();
            }
        }
    }

    private void setVmArgumentsFromPropertyFile() {
        Properties properties = new Properties();

        File propertyFile = new File(ApplicationConstants.PROPERTIES_FILE_NAME);

        try (InputStream input = new FileInputStream(propertyFile)) {
            properties.load(input);
            Enumeration propertyKeys = properties.keys();

            while (propertyKeys.hasMoreElements()) {
                String key = (String) propertyKeys.nextElement();
                String value = properties.getProperty(key);
                System.setProperty(key, value);
                logger.info("Set system argument " + key + ":" + value);
            }

            logger.info("All properties loaded from file: " + ApplicationConstants.PROPERTIES_FILE_NAME);
        } catch (IOException e) {
            logger.error("Error occured while getting properties from file. " + ApplicationConstants.PROPERTIES_FILE_NAME, e);
            throw new PropertyNotLoadedException(ApplicationConstants.PROPERTIES_FILE_NAME);
        }
    }
}
