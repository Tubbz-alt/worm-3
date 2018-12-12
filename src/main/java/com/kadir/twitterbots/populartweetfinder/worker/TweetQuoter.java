package com.kadir.twitterbots.populartweetfinder.worker;

import com.kadir.twitterbots.populartweetfinder.authentication.BotAuthenticator;
import com.kadir.twitterbots.populartweetfinder.dao.StatusDao;
import com.kadir.twitterbots.populartweetfinder.entity.ApiProcessType;
import com.kadir.twitterbots.populartweetfinder.entity.CustomStatus;
import com.kadir.twitterbots.populartweetfinder.entity.TaskPriority;
import com.kadir.twitterbots.populartweetfinder.handler.RateLimitHandler;
import com.kadir.twitterbots.populartweetfinder.scheduler.BaseScheduledRunnable;
import com.kadir.twitterbots.populartweetfinder.scheduler.TaskScheduler;
import org.apache.log4j.Logger;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author akarakoc
 * Date :   11.12.2018
 * Time :   15:50
 */
public class TweetQuoter extends BaseScheduledRunnable {
    private final Logger logger = Logger.getLogger(this.getClass());

    private Twitter twitter;
    private StatusDao statusDao;
    private int quoteLimit;
    private int quoteHour;
    private int quoteMinute;
    private final int QUOTE_RETRY_COUNT = 5;

    public TweetQuoter() {
        super(TaskPriority.VERY_HIGH);
        executorService = Executors.newScheduledThreadPool(1);
        loadArguments();
    }

    public void schedule() {
        Long quoteTime = LocalDateTime.now().until(LocalDate.now().atTime(quoteHour, quoteMinute, 0), ChronoUnit.SECONDS);
        scheduledFuture = executorService.scheduleAtFixedRate(this, quoteTime, 1440, TimeUnit.SECONDS);
        logger.info("schedule " + this.getClass().getSimpleName() + " to run at " + quoteHour + ":" + quoteMinute);
        TaskScheduler.addScheduledTask(this);
    }

    @Override
    public void run() {
        try {
            TaskScheduler.shutdownLowerPriorityTasks(this);
            statusDao = new StatusDao();
            authenticate();
            List<CustomStatus> popularStatuses = loadPopularTweetsFromDatabase();
            quoteTweets(popularStatuses);
        } catch (TwitterException e) {
            logger.error(e);
        }
    }

    private List<CustomStatus> loadPopularTweetsFromDatabase() {
        List<CustomStatus> savedStatuses = statusDao.getTodaysStatuses();

        Iterator<CustomStatus> iterator = savedStatuses.iterator();
        while (iterator.hasNext()) {
            CustomStatus customStatus = iterator.next();
            Status s = showStatus(customStatus.getStatusId());
            if (s == null) {
                iterator.remove();
            }
        }

        savedStatuses.sort(Comparator.comparing(CustomStatus::getScore).reversed());

        logger.info("loaded popular status' from database to quote.");
        return savedStatuses;
    }

    private Status showStatus(Long id) {
        Status status = null;
        try {
            status = twitter.showStatus(id);
            RateLimitHandler.handle(twitter.getId(), status.getRateLimitStatus(), ApiProcessType.SHOW_STATUS);
        } catch (TwitterException e) {
            if (e.getErrorCode() == 144) {
                logger.error("Status could not be found. Status id: " + id);
            } else {
                logger.error("Error while getting status details." + e);
            }
        } catch (InterruptedException e) {
            logger.error(e);
            Thread.currentThread().interrupt();
        }
        return status;
    }

    private void authenticate() throws TwitterException {
        String consumerKey = System.getProperty("quoterConsumerKey");
        String consumerSecret = System.getProperty("quoterConsumerSecret");
        String accessToken = System.getProperty("quoterAccessToken");
        String accessTokenSecret = System.getProperty("quoterAccessTokenSecret");
        twitter = BotAuthenticator.authenticate(consumerKey, consumerSecret, accessToken, accessTokenSecret);
        logger.info("authenticate: " + twitter.getScreenName() + " - " + twitter.getId());
    }

    private void quoteTweets(List<CustomStatus> mostPopularTweets) {
        if (mostPopularTweets.size() > quoteLimit) {
            mostPopularTweets = mostPopularTweets.subList(0, quoteLimit);
        }

        long lastPostedStatusId = -1;

        int retryCount = 0;
        for (int i = mostPopularTweets.size() - 1; i >= 0; i--) {
            try {
                CustomStatus s = mostPopularTweets.get(i);

                String statusToPost = (i + 1) + ". " + s.getStatusLink();

                StatusUpdate statusUpdate = new StatusUpdate(statusToPost);
                statusUpdate.setInReplyToStatusId(lastPostedStatusId);

                Status updatedStatus = twitter.updateStatus(statusUpdate);
                statusDao.setStatusQuoted(s.getId());
                logger.info("new status: " + updatedStatus.getText());
                if (i > 0) {
                    RateLimitHandler.handle(twitter.getId(), updatedStatus.getRateLimitStatus(), ApiProcessType.UPDATE_STATUS);
                    Thread.sleep(50 * 1000L);
                }
                retryCount = 0;
            } catch (TwitterException e) {
                logger.error(e.getMessage());
                retryCount++;
                if (retryCount > QUOTE_RETRY_COUNT) {
                    logger.error("retry count: " + retryCount + ". Program finished.");
                    return;
                }
                logger.info("wait 1 minute before retry. retry count: " + retryCount);
                try {
                    Thread.sleep(60 * 1000L);
                } catch (InterruptedException e1) {
                    logger.error(e1);
                    Thread.currentThread().interrupt();

                }
                i++;
            } catch (InterruptedException e) {
                logger.error(e);
                Thread.currentThread().interrupt();
            }
        }

        TaskScheduler.shutdownAllTasks();
    }

    private void loadArguments() {
        this.quoteLimit = Integer.parseInt(System.getProperty("quoteLimit", "13"));
        logger.debug("set quoteLimit:" + quoteLimit);
        this.quoteHour = Integer.parseInt(System.getProperty("quoteHour", "22"));
        logger.debug("set quoteHour:" + quoteHour);
        this.quoteMinute = Integer.parseInt(System.getProperty("quoteMinute", "0"));
        logger.debug("set quoteMinute:" + quoteMinute);
    }
}
