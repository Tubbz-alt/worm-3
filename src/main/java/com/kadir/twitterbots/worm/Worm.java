package com.kadir.twitterbots.worm;

import com.kadir.twitterbots.worm.dao.DatabaseInitialiser;
import com.kadir.twitterbots.worm.exceptions.PropertyNotLoadedException;
import com.kadir.twitterbots.worm.util.ApplicationConstants;
import com.kadir.twitterbots.worm.worker.TweetFetcher;
import com.kadir.twitterbots.worm.worker.TweetQuoter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

/**
 * @author akadir
 * Date: 08/12/2018
 * Time: 13:27
 */
public class Worm {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private TweetFetcher tweetFetcher;
    private TweetQuoter tweetQuoter;

    public static void main(String[] args) {
        Worm worm = new Worm();
        worm.start();
    }

    private Worm() {
        setVmArgumentsFromPropertyFile();
        DatabaseInitialiser.initializeDatabase();

        tweetFetcher = new TweetFetcher();
        tweetQuoter = new TweetQuoter();
    }

    private void start() {
        tweetFetcher.schedule();
        tweetQuoter.schedule();
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
                logger.info("Set system argument {}:{}", key, value);
            }

            logger.info("All properties loaded from file: {}", ApplicationConstants.PROPERTIES_FILE_NAME);
        } catch (IOException e) {
            logger.error("error occurred while getting properties from file. " + ApplicationConstants.PROPERTIES_FILE_NAME, e);
            throw new PropertyNotLoadedException(ApplicationConstants.PROPERTIES_FILE_NAME);
        }
    }
}
