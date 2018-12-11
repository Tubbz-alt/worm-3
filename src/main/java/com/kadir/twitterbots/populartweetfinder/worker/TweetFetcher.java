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

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author akadir
 * Date: 08/12/2018
 * Time: 15:10
 */
public class TweetFetcher implements Runnable {
    private final Logger logger = Logger.getLogger(this.getClass());

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private Map<Long, CustomStatus> fetchedStatusMap = new HashMap<>();

    private String languageKey;
    private Twitter twitter;
    private TweetFilter tweetFilter;
    private Thread fetchThread;
    private StatusDao statusDao;
    private int statusLimitToKeep;


    public TweetFetcher(Twitter twitter, TweetFilter tweetFilter) {
        loadArguments();
        this.twitter = twitter;
        this.tweetFilter = tweetFilter;
        statusDao = new StatusDao();
        addTodaysStatusesIntoMap();
    }

    public void run() {
        while (!fetchThread.isInterrupted()) {
            try {
                fetchTweets();
            } catch (TwitterException e) {
                logger.error("Error while fetching tweets.", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error(e);
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
        query.since(simpleDateFormat.format(new Date()));

        do {
            QueryResult result = twitter.search(query);
            statuses = result.getTweets();
            logger.debug("Fetch " + statuses.size() + " statuses. Completed in: " + result.getCompletedIn());

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

    private void addStatus(Status newFetchedStatus) throws InterruptedException {
        CustomStatus customStatus = fetchedStatusMap.get(newFetchedStatus.getId());
        if (customStatus != null) {
            if (customStatus.getScore() != StatusUtil.calculateInteractionCount(newFetchedStatus)) {
                customStatus.setScore(StatusUtil.calculateInteractionCount(newFetchedStatus));
                logger.info("Update status score in map. " + customStatus.getScore() + " - " + customStatus.getStatusLink());
            }
        } else {
            CustomStatus alreadyMappedStatus = getAnotherStatusOfUserIfExist(newFetchedStatus);
            if (alreadyMappedStatus != null) {
                if (alreadyMappedStatus.getScore() < StatusUtil.calculateInteractionCount(newFetchedStatus)) {
                    replaceUserStatusByStatusScore(alreadyMappedStatus, newFetchedStatus);
                }
            } else {
                customStatus = new CustomStatus(newFetchedStatus);
                fetchedStatusMap.put(newFetchedStatus.getId(), new CustomStatus(newFetchedStatus));
                logger.info("Save status into map. " + customStatus.getScore() + " - " + customStatus.getStatusLink());
            }
        }

        if (fetchedStatusMap.size() > statusLimitToKeep) {
            removeStatusesWithLowestInteractionFromMap();
        }
    }

    private void replaceUserStatusByStatusScore(CustomStatus alreadyMappedStatus, Status status) {
        CustomStatus newFetchedStatus = new CustomStatus(status);
        fetchedStatusMap.remove(alreadyMappedStatus.getStatusId());
        fetchedStatusMap.put(status.getId(), newFetchedStatus);
        logger.info("Replace user status. " + newFetchedStatus.getScore() + " - " + newFetchedStatus.getStatusLink());
    }

    private CustomStatus getAnotherStatusOfUserIfExist(Status status) {
        List<CustomStatus> userStatusList = fetchedStatusMap.values().stream()
                .filter(value -> value.getUserId() == status.getUser().getId())
                .collect(Collectors.toList());

        return !userStatusList.isEmpty() ? userStatusList.get(0) : null;
    }

    private void removeStatusesWithLowestInteractionFromMap() throws InterruptedException {
        List<CustomStatus> customStatusList = new ArrayList<>(fetchedStatusMap.values());

        removeDeletedStatuses(customStatusList);

        if (fetchedStatusMap.size() > statusLimitToKeep) {
            customStatusList.sort(Comparator.comparing(CustomStatus::getScore).reversed());
            for (int i = statusLimitToKeep; i < customStatusList.size(); i++) {
                CustomStatus customStatus = fetchedStatusMap.remove(customStatusList.get(i).getStatusId());
                if (customStatus != null) {
                    logger.info("Remove status from map: " + customStatus.getScore() + " - " + customStatus.getStatusLink());
                }
            }

            setMinInteractionCount();
        }
    }

    private void setMinInteractionCount() {
        int tempMinScore = -1;
        for (CustomStatus status : fetchedStatusMap.values()) {
            if (tempMinScore == -1 || status.getScore() < tempMinScore) {
                tempMinScore = status.getScore();
            }
        }
        InteractionCountFilter.setMinInteractionCount(tempMinScore);
        logger.info("Set minInteractionCount:" + InteractionCountFilter.getMinInteractionCount());
    }

    private void removeDeletedStatuses(List<CustomStatus> customStatusList) throws InterruptedException {
        Iterator<CustomStatus> iterator = customStatusList.iterator();
        LocalDateTime now = LocalDateTime.now();

        while (iterator.hasNext()) {
            CustomStatus customStatus = iterator.next();
            if (ChronoUnit.MINUTES.between(customStatus.getFetchedAt(), now) > ApplicationConstants.CHECK_DELETED_STATUSES_PERIOD) {
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
    }

    private void addTodaysStatusesIntoMap() {
        List<CustomStatus> todaysStatuses = statusDao.getTodaysStatuses();

        for (CustomStatus customStatus : todaysStatuses) {
            fetchedStatusMap.put(customStatus.getStatusId(), customStatus);
            logger.info("Load status from database. " + customStatus.getScore() + " - " + customStatus.getStatusLink());
        }

        if (fetchedStatusMap.size() > 0) {
            setMinInteractionCount();
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

    public Map<Long, CustomStatus> getFetchedStatusMap() {
        return fetchedStatusMap;
    }
}