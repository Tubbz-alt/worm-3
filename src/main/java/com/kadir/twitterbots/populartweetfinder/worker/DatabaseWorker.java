package com.kadir.twitterbots.populartweetfinder.worker;

import com.kadir.twitterbots.populartweetfinder.dao.StatusDao;
import com.kadir.twitterbots.populartweetfinder.entity.CustomStatus;
import com.kadir.twitterbots.populartweetfinder.entity.TaskPriority;
import com.kadir.twitterbots.populartweetfinder.scheduler.BaseScheduledRunnable;
import com.kadir.twitterbots.populartweetfinder.scheduler.TaskScheduler;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author akadir
 * Date: 10/12/2018
 * Time: 20:17
 */
public class DatabaseWorker extends BaseScheduledRunnable {
    private final Logger logger = Logger.getLogger(this.getClass());

    private TweetFetcher tweetFetcher;
    private StatusDao statusDao;

    public DatabaseWorker(TweetFetcher tweetFetcher) {
        super(TaskPriority.HIGH);
        executorService = Executors.newScheduledThreadPool(1);
        this.tweetFetcher = tweetFetcher;
        statusDao = new StatusDao();
    }

    @Override
    public void schedule() {
        scheduledFuture = executorService.scheduleWithFixedDelay(this, 2, 30, TimeUnit.MINUTES);
        TaskScheduler.addScheduledTask(this);
    }

    @Override
    public void cancel() {
        saveStatusesToDatabase();
        super.cancel();
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
        try {
            logger.info("Run database worker");
            saveStatusesToDatabase();
        } catch (Exception e) {
            logger.error(e);
        }
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
            HashMap<Long, CustomStatus> savedStatusMap = generateMap(savedStatuses);
            Set<Long> mergedStatusIdSet = new HashSet<>();
            mergedStatusIdSet.addAll(savedStatusMap.keySet());
            mergedStatusIdSet.addAll(fetchedStatusMap.keySet());

            for (Long statusId : mergedStatusIdSet) {
                if (fetchedStatusMap.containsKey(statusId) && savedStatusMap.containsKey(statusId)) {
                    CustomStatus statusToUpdate = savedStatusMap.get(statusId);
                    int newScore = fetchedStatusMap.get(statusId).getScore();
                    statusDao.updateTodaysStatusScore(statusToUpdate.getStatusId(), newScore);
                    logger.debug("Update status score in database. " + statusToUpdate.getId() + " - " + newScore + " - " + statusToUpdate.getStatusLink());
                } else if (fetchedStatusMap.containsKey(statusId)) {
                    CustomStatus statusToInsert = fetchedStatusMap.get(statusId);
                    Long id = statusDao.saveStatus(statusToInsert);
                    statusToInsert.setId(id);
                    logger.info("Status saved into database. " + statusToInsert.getId() + " - " + statusToInsert.getScore() + " - " + statusToInsert.getStatusLink());
                } else if (savedStatusMap.containsKey(statusId)) {
                    CustomStatus statusToRemove = savedStatusMap.get(statusId);
                    statusDao.removeStatus(statusToRemove);
                    logger.info("Status removed from database. " + statusToRemove.getId() + " - " + statusToRemove.getScore() + " - " + statusToRemove.getStatusLink());
                }
            }
        }
        logger.info("refresh database statuses");
    }

    private HashMap<Long, CustomStatus> generateMap(List<CustomStatus> customStatusList) {
        HashMap<Long, CustomStatus> customStatusMap = new HashMap<>();

        for (CustomStatus customStatus : customStatusList) {
            customStatusMap.put(customStatus.getStatusId(), customStatus);
        }

        return customStatusMap;
    }
}
