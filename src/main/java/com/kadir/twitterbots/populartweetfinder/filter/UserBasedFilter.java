package com.kadir.twitterbots.populartweetfinder.filter;

import com.kadir.twitterbots.populartweetfinder.dao.UserDao;
import com.kadir.twitterbots.populartweetfinder.entity.ApiProcessType;
import com.kadir.twitterbots.populartweetfinder.entity.IgnoredUser;
import com.kadir.twitterbots.populartweetfinder.entity.TaskPriority;
import com.kadir.twitterbots.populartweetfinder.handler.RateLimitHandler;
import com.kadir.twitterbots.populartweetfinder.scheduler.BaseScheduledRunnable;
import com.kadir.twitterbots.populartweetfinder.scheduler.TaskScheduler;
import com.kadir.twitterbots.populartweetfinder.util.ApplicationConstants;
import com.kadir.twitterbots.populartweetfinder.util.DataUtil;
import org.apache.log4j.Logger;
import twitter4j.IDs;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.kadir.twitterbots.populartweetfinder.util.ApplicationConstants.DEFAULT_DELAY_FOR_SCHEDULED_TASKS;
import static com.kadir.twitterbots.populartweetfinder.util.ApplicationConstants.DEFAULT_INITIAL_DELAY_FOR_SCHEDULED_TASKS;

/**
 * @author akadir
 * Date: 08/12/2018
 * Time: 18:57
 */
public class UserBasedFilter extends BaseScheduledRunnable implements StatusFilter {
    private final Logger logger = Logger.getLogger(this.getClass());

    private int minFollowingCount;
    private int maxFollowingCount;
    private int maxFollowersCount;
    private UserDao userDao = new UserDao();
    private Set<Long> ignoredUsersSet = new HashSet<>();
    private boolean isCancelled = false;
    private Twitter twitter;

    public UserBasedFilter(Twitter twitter) {
        super(TaskPriority.LOW);
        executorService = Executors.newScheduledThreadPool(1);
        this.twitter = twitter;
        logger.debug(this.getClass().getSimpleName() + " created");
        this.minFollowingCount = Integer.parseInt(System.getProperty("minFollowingCount", "20"));
        logger.debug("Set minFollowingCount:" + minFollowingCount);
        this.maxFollowingCount = Integer.parseInt(System.getProperty("maxFollowingCount", "2000"));
        logger.debug("Set maxFollowingCount:" + maxFollowingCount);
        this.maxFollowersCount = Integer.parseInt(System.getProperty("maxFollowersCount", "200000"));
        logger.debug("Set maxFollowersCount:" + maxFollowersCount);
        loadIgnoredUsers();
    }

    @Override
    public void schedule() {
        scheduledFuture = executorService.scheduleWithFixedDelay(this, DEFAULT_INITIAL_DELAY_FOR_SCHEDULED_TASKS, DEFAULT_DELAY_FOR_SCHEDULED_TASKS, TimeUnit.MINUTES);
        logger.info("add scheduler to run with fixed delay. initial delay: " + DEFAULT_INITIAL_DELAY_FOR_SCHEDULED_TASKS + " delay:" + DEFAULT_DELAY_FOR_SCHEDULED_TASKS);
        TaskScheduler.addScheduledTask(this);
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
        logger.info("run scheduled task: " + this.getClass().getSimpleName());
        cleanUpIgnoredUsers();
        loadIgnoredUsers();
        addBlockedUsersIntoIgnoredUsers();
        logger.info("finish scheduled task: " + this.getClass().getSimpleName());
    }

    @Override
    public void cancel() {
        isCancelled = true;
        super.cancel();
    }

    @Override
    public boolean passed(Status status) {
        User user = status.getUser();

        return (!user.isVerified() && isUserFollowingAndFollowerNumbersInRange(user) && !wouldUserBeParodyAccount(user));
    }

    private boolean wouldUserBeParodyAccount(User user) {
        boolean wouldBe = false;
        if (user.getDescription().toLowerCase().contains("parody") || user.getLocation().toLowerCase().contains("parody") ||
                user.getDescription().toLowerCase().contains("parodi") || user.getLocation().toLowerCase().contains("parodi")) {
            if (!ignoredUsersSet.contains(user.getId())) {
                userDao.insertIgnoredUser(user);
                ignoredUsersSet.add(user.getId());
                logger.info("add into ignored users as it seems like parody account: " + user.getScreenName());
            }
            wouldBe = true;
        }
        return wouldBe;
    }

    private boolean isUserFollowingAndFollowerNumbersInRange(User user) {
        int friendsCount = user.getFriendsCount();
        int followersCount = user.getFollowersCount();
        return (friendsCount > minFollowingCount && friendsCount < maxFollowingCount && maxFollowersCount > followersCount);
    }

    public void loadIgnoredUsers() {
        ignoredUsersSet = userDao.getIgnoredUserIds();
        logger.info("load ignored users from database. size: " + ignoredUsersSet.size());
    }

    private void addBlockedUsersIntoIgnoredUsers() {
        try {
            IDs blockedIds = twitter.getBlocksIDs();
            RateLimitHandler.handle(twitter.getId(), blockedIds.getRateLimitStatus(), ApiProcessType.GET_BLOCKS_IDS);
            long[] blockedIdsArr = blockedIds.getIDs();

            for (long userId : blockedIdsArr) {
                if (!ignoredUsersSet.contains(userId)) {
                    try {
                        User user = twitter.showUser(userId);
                        RateLimitHandler.handle(twitter.getId(), user.getRateLimitStatus(), ApiProcessType.SHOW_USER);
                        if (!user.isVerified()) {
                            userDao.insertIgnoredUser(user);
                            ignoredUsersSet.add(userId);
                            logger.info("add user into ignored users: " + user.getId() + " - " + user.getScreenName());
                        }
                    } catch (TwitterException e) {
                        logger.error(e.getErrorMessage());
                    }
                }
            }
        } catch (TwitterException e) {
            logger.error(e.getErrorMessage());
        } catch (InterruptedException e) {
            logger.error(e);
            Thread.currentThread().interrupt();
        }
    }

    private void cleanUpIgnoredUsers() {
        Set<IgnoredUser> ignoredUsers = userDao.getIgnoredUsers();
        Iterator<IgnoredUser> iterator = ignoredUsers.iterator();
        while (iterator.hasNext() && !isCancelled) {
            IgnoredUser ignoredUser = iterator.next();
            try {
                if (wouldBeRemoved(ignoredUser)) {
                    userDao.deleteIgnoredUser(ignoredUser);
                    logger.info("Ignored user is deleted due to passive period. " + ignoredUser.getUserId() + " - " + ignoredUser.getScreenName());
                } else {
                    User user = twitter.showUser(ignoredUser.getUserId());
                    RateLimitHandler.handle(twitter.getId(), user.getRateLimitStatus(), ApiProcessType.SHOW_USER);

                    deleteOrUpdateIgnoredUser(ignoredUser, true, user.isVerified());
                }
            } catch (TwitterException e) {
                if ((e.getErrorCode() == 50 || e.getErrorCode() == 63)) {
                    deleteOrUpdateIgnoredUser(ignoredUser, false, false);
                } else {
                    logger.error(e);
                }
            } catch (InterruptedException e) {
                logger.error(e);
                Thread.currentThread().interrupt();
            }
        }
        logger.info("finish clean up ignored users task");
    }

    private void deleteOrUpdateIgnoredUser(IgnoredUser ignoredUser, boolean userExist, boolean isVerified) {
        if (userExist) {
            if (isVerified) {
                userDao.deleteIgnoredUser(ignoredUser);
                logger.info("Ignored user is deleted due to verification. " + ignoredUser.getUserId() + " - " + ignoredUser.getScreenName());
            } else if (!DataUtil.isNullOrEmpty(ignoredUser.getPassiveSince())) {
                ignoredUser.setLastCheck(new Date());
                userDao.setUserActive(ignoredUser);
                logger.info("Ignored user updated to active. " + ignoredUser.getUserId() + " - " + ignoredUser.getScreenName());
            }
        } else {
            if (DataUtil.isNullOrEmpty(ignoredUser.getPassiveSince())) {
                ignoredUser.setPassiveSince(new Date());
                ignoredUser.setLastCheck(new Date());
                userDao.updateIgnoredUserToPassive(ignoredUser);
                logger.info("User not found and updated to passive. userId: " + ignoredUser.getUserId() + " screen name: " + ignoredUser.getScreenName());
            }
        }
    }

    private boolean wouldBeRemoved(IgnoredUser ignoredUser) {
        int diff = 0;
        if (!DataUtil.isNullOrEmpty(ignoredUser.getPassiveSince())) {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate now = LocalDate.now();
            LocalDate passiveSince = LocalDate.parse(ignoredUser.getPassiveSince(), dateFormatter);

            Period period = Period.between(now, passiveSince);
            diff = period.getDays();
        }

        return diff > ApplicationConstants.MAX_PASSIVE_PERIOD;
    }

}
