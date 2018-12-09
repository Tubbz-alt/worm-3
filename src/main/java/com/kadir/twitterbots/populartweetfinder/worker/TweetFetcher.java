package com.kadir.twitterbots.populartweetfinder.worker;

import com.kadir.twitterbots.populartweetfinder.dao.StatusDao;
import com.kadir.twitterbots.populartweetfinder.entity.ApiProcessType;
import com.kadir.twitterbots.populartweetfinder.entity.CustomStatus;
import com.kadir.twitterbots.populartweetfinder.exceptions.IllegalLanguageKeyException;
import com.kadir.twitterbots.populartweetfinder.filter.InteractionCountFilter;
import com.kadir.twitterbots.populartweetfinder.handler.RateLimitHandler;
import com.kadir.twitterbots.populartweetfinder.util.ApplicationConstants;
import com.kadir.twitterbots.populartweetfinder.util.DataUtil;
import com.kadir.twitterbots.populartweetfinder.util.StatusUtil;
import org.apache.log4j.Logger;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author akadir
 * Date: 08/12/2018
 * Time: 15:10
 */
public class TweetFetcher implements Runnable {
    private final Logger logger = Logger.getLogger(this.getClass());

    private String languageKey;
    private Twitter twitter;
    private TweetFilter tweetFilter;
    private Thread fetchThread;
    private StatusDao statusDao;
    private int statusLimitToKeep;

    private Map<Long, CustomStatus> fetchedStatusMap = new HashMap<>();

    public TweetFetcher(Twitter twitter) {
        this.twitter = twitter;
        loadArguments();
        tweetFilter = new TweetFilter(twitter);
        statusDao = new StatusDao();
    }

    public void run() {
        while (true) {
            try {
                if (!fetchThread.isInterrupted()) {
                    fetchTweets();
                } else {
                    saveStatusesToDatabase();
                    break;
                }
            } catch (TwitterException e) {
                logger.error("Error while fetching tweets.", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error(e);
                logger.info("INTERRUPTED");
            }
        }
    }

    public void start() {
        logger.info("Starting tweet fetch thread");
        if (fetchThread == null) {
            fetchThread = new Thread(this, this.getClass().getSimpleName());
            fetchThread.start();
            logger.info("tweet fetch thread started");
        }
    }

    public void interrupt() throws InterruptedException {
        fetchThread.interrupt();
        fetchThread.join();
    }

    private void fetchTweets() throws TwitterException, InterruptedException {
        List<Status> statuses;
        Query query = new Query("lang:" + languageKey);
        query.setCount(100);
        query.setResultType(Query.RECENT);
        query.since(ApplicationConstants.DATE_FORMAT.format(new Date()));

        do {
            QueryResult result = twitter.search(query);
            statuses = result.getTweets();
            logger.debug("Fetched " + statuses.size() + " statuses. max id: " + result.getMaxId() + " completed in: " + result.getCompletedIn());

            for (Status status : statuses) {
                checkStatus(status);
            }

            query = result.nextQuery();
            RateLimitHandler.handle(twitter.getId(), result.getRateLimitStatus(), ApiProcessType.SEARCH);
        } while (query != null);
    }

    private void checkStatus(Status status) throws InterruptedException {
        if (status.isRetweet()) {
            status = status.getRetweetedStatus();

            if (tweetFilter.canStatusBeUsed(status)) {
                addStatus(status);
            }
        }

        if (status.getQuotedStatus() != null) {
            status = status.getQuotedStatus();

            if (tweetFilter.canStatusBeUsed(status)) {
                addStatus(status);
            }
        }
    }

    private void addStatus(Status status) throws InterruptedException {
        CustomStatus customStatus;

        if (fetchedStatusMap.containsKey(status.getId())) {
            customStatus = fetchedStatusMap.get(status.getId());
            customStatus.setScore(StatusUtil.calculateInteractionCount(status));
            fetchedStatusMap.put(status.getId(), customStatus);
            logger.info("Update status score from map. " + customStatus.getScore() + " - " + customStatus.getStatusLink());
        } else if (fetchedStatusMap.size() < statusLimitToKeep) {
            customStatus = new CustomStatus(status);
            fetchedStatusMap.put(status.getId(), new CustomStatus(status));
            logger.info("Save status into map. " + customStatus.getScore() + " - " + customStatus.getStatusLink());
        }

        if (fetchedStatusMap.size() > statusLimitToKeep) {
            removeLowestInteractionStatusesFromMap();
        }
    }

    private void removeLowestInteractionStatusesFromMap() throws InterruptedException {
        List<CustomStatus> customStatusList = new ArrayList<>(fetchedStatusMap.values());

        removeDeletedStatuses(customStatusList);

        if (fetchedStatusMap.size() > statusLimitToKeep) {
            customStatusList.sort(Comparator.comparing(CustomStatus::getScore));
            for (int i = statusLimitToKeep; i < customStatusList.size(); i++) {
                CustomStatus customStatus = fetchedStatusMap.remove(customStatusList.get(i).getStatusId());
                if (customStatus != null) {
                    logger.info("status removed from map: " + customStatus.getScore() + " - " + customStatus.getStatusLink());
                }
            }

            for (CustomStatus status : fetchedStatusMap.values()) {
                if (status.getScore() < InteractionCountFilter.getMinInteractionCount()) {
                    InteractionCountFilter.setMinInteractionCount(status.getScore());
                }
            }
        }
    }

    private void removeDeletedStatuses(List<CustomStatus> customStatusList) throws InterruptedException {
        Iterator<CustomStatus> iterator = customStatusList.iterator();

        while (iterator.hasNext()) {
            CustomStatus customStatus = iterator.next();
            try {
                Status s = twitter.showStatus(customStatus.getStatusId());
                customStatus = new CustomStatus(s);
                fetchedStatusMap.put(customStatus.getStatusId(), customStatus);
                RateLimitHandler.handle(twitter.getId(), s.getRateLimitStatus(), ApiProcessType.SHOW_STATUS);
            } catch (TwitterException e) {
                if (e.getErrorCode() == 144) {
                    fetchedStatusMap.remove(customStatus.getStatusId());
                    iterator.remove();
                }
                logger.error(e);
            }
        }
    }

    public void saveStatusesToDatabase() {
        List<CustomStatus> savedStatuses = statusDao.getTodaysStatuses();
        List<CustomStatus> fetchedStatuses = new ArrayList<>(fetchedStatusMap.values());

        if (savedStatuses.isEmpty()) {
            statusDao.saveAll(fetchedStatuses);
            logger.info("Statuses saved into database. Size: " + fetchedStatuses.size());
        } else {
            Set<Long> fetchedStatusIdSet = fetchedStatusMap.keySet();
            for (CustomStatus customStatus : fetchedStatuses) {
                if (customStatus.getId() == null) {
                    statusDao.saveStatus(customStatus);
                    logger.info("Status saved into database. " + customStatus.getScore() + " - " + customStatus.getStatusLink());
                } else {
                    statusDao.updateTodaysStatusScore(customStatus.getStatusId(), customStatus.getScore());
                    logger.info("Status score updated. " + customStatus.getScore() + " - " + customStatus.getStatusLink());
                }
            }

            for (CustomStatus savedStatus : savedStatuses) {
                if (!fetchedStatusIdSet.contains(savedStatus.getStatusId())) {
                    statusDao.removeStatus(savedStatus);
                    logger.info("Status removed from database. " + savedStatus.getScore() + " - " + savedStatus.getStatusLink());
                }
            }
        }
    }

    private void loadArguments() {
        this.languageKey = System.getProperty("languageKey");
        this.statusLimitToKeep = Integer.parseInt(System.getProperty("statusLimitToKeep", "30"));
        if (DataUtil.isNullOrEmpty(languageKey)) {
            throw new IllegalLanguageKeyException(languageKey);
        } else {
            logger.info("Set languageKey:" + languageKey);
        }
    }
}