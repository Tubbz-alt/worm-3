package com.kadir.twitterbots.populartweetfinder.worker;

import com.kadir.twitterbots.populartweetfinder.dao.StatusDao;
import com.kadir.twitterbots.populartweetfinder.entity.CustomStatus;
import com.kadir.twitterbots.populartweetfinder.scheduler.ScheduledRunnable;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

/**
 * @author akadir
 * Date: 10/12/2018
 * Time: 20:17
 */
public class DatabaseWorker implements ScheduledRunnable {
    private final Logger logger = Logger.getLogger(this.getClass());

    private TweetFetcher tweetFetcher;
    private StatusDao statusDao;
    private volatile ScheduledFuture<?> scheduledFuture;

    public DatabaseWorker(TweetFetcher tweetFetcher) {
        this.tweetFetcher = tweetFetcher;
        statusDao = new StatusDao();
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        logger.info("Run database worker");
        saveStatusesToDatabase();
    }

    @Override
    public void cancel() {
        scheduledFuture.cancel(false);
        logger.info("cancel scheduled task: " + this.getClass().getSimpleName());
    }

    @Override
    public void setScheduledFuture(ScheduledFuture<?> scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }

    public void saveStatusesToDatabase() {
        Map<Long, CustomStatus> fetchedStatusMap = tweetFetcher.getFetchedStatusMap();
        List<CustomStatus> savedStatuses = statusDao.getTodaysStatuses();
        List<CustomStatus> fetchedStatuses = new ArrayList<>(fetchedStatusMap.values());

        if (savedStatuses.isEmpty()) {
            for (CustomStatus customStatus : fetchedStatuses) {
                Long id = statusDao.saveStatus(customStatus);
                customStatus.setId(id);
                logger.info("Save status into database. " + customStatus.getId() + " - " + customStatus.getScore() + " - " + customStatus.getStatusLink());
            }
        } else {
            Set<Long> fetchedStatusIdSet = fetchedStatusMap.keySet();
            for (CustomStatus customStatus : fetchedStatuses) {
                if (customStatus.getId() == null) {
                    Long id = statusDao.saveStatus(customStatus);
                    customStatus.setId(id);
                    logger.info("Status saved into database. " + customStatus.getId() + " - " + customStatus.getScore() + " - " + customStatus.getStatusLink());
                } else {
                    statusDao.updateTodaysStatusScore(customStatus.getStatusId(), customStatus.getScore());
                    logger.info("Update status score in database. " + customStatus.getId() + " - " + customStatus.getScore() + " - " + customStatus.getStatusLink());
                }
            }

            for (CustomStatus customStatus : savedStatuses) {
                if (!fetchedStatusIdSet.contains(customStatus.getStatusId())) {
                    statusDao.removeStatus(customStatus);
                    logger.info("Status removed from database. " + customStatus.getId() + " - " + customStatus.getScore() + " - " + customStatus.getStatusLink());
                }
            }
        }
    }
}
