package com.kadir.twitterbots.worm.filter;

import com.kadir.twitterbots.ratelimithandler.handler.RateLimitHandler;
import com.kadir.twitterbots.ratelimithandler.process.ApiProcessType;
import com.kadir.twitterbots.worm.dao.UserDao;
import com.kadir.twitterbots.worm.entity.IgnoredUser;
import com.kadir.twitterbots.worm.entity.TaskPriority;
import com.kadir.twitterbots.worm.scheduler.BaseScheduledRunnable;
import com.kadir.twitterbots.worm.scheduler.TaskScheduler;
import com.kadir.twitterbots.worm.util.DataUtil;
import com.kadir.twitterbots.worm.util.WormConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.*;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.kadir.twitterbots.worm.util.WormConstants.DEFAULT_DELAY_FOR_SCHEDULED_TASKS;
import static com.kadir.twitterbots.worm.util.WormConstants.DEFAULT_INITIAL_DELAY_FOR_SCHEDULED_TASKS;

/**
 * @author akadir
 * Date: 08/12/2018
 * Time: 18:57
 */
public class UserBasedFilter extends BaseScheduledRunnable implements StatusFilter {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private int minFollowingCount;
    private int maxFollowingCount;
    private int maxFollowersCount;
    private UserDao userDao = new UserDao();
    private Set<Long> ignoredUsersSet = new HashSet<>();
    private Set<Long> yesterdayQuotedUsersSet = new HashSet<>();
    private boolean isCancelled = false;
    private Twitter twitter;

    public UserBasedFilter(Twitter twitter) {
        super(TaskPriority.LOW);
        executorService = Executors.newScheduledThreadPool(1);
        this.twitter = twitter;
        logger.debug("{} created", this.getClass().getSimpleName());
        this.minFollowingCount = Integer.parseInt(System.getProperty("minFollowingCount", "20"));
        logger.debug("Set minFollowingCount:{}", minFollowingCount);
        this.maxFollowingCount = Integer.parseInt(System.getProperty("maxFollowingCount", "2000"));
        logger.debug("Set maxFollowingCount:{}", maxFollowingCount);
        this.maxFollowersCount = Integer.parseInt(System.getProperty("maxFollowersCount", "200000"));
        logger.debug("Set maxFollowersCount:{}", maxFollowersCount);
        loadYesterdayQuotedUsers();
        loadIgnoredUsers();
    }

    @Override
    public void schedule() {
        scheduledFuture = executorService.scheduleWithFixedDelay(this, DEFAULT_INITIAL_DELAY_FOR_SCHEDULED_TASKS, DEFAULT_DELAY_FOR_SCHEDULED_TASKS, TimeUnit.MINUTES);
        logger.info("add scheduler to run with fixed delay. initial delay:{} delay:{}", DEFAULT_INITIAL_DELAY_FOR_SCHEDULED_TASKS, DEFAULT_DELAY_FOR_SCHEDULED_TASKS);
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
        logger.info("run scheduled task: {}", this.getClass().getSimpleName());
        cleanUpIgnoredUsers();
        loadIgnoredUsers();
        addBlockedUsersIntoIgnoredUsers();
        logger.info("finish scheduled task: {} wait {} mins to next run", this.getClass().getSimpleName(), DEFAULT_DELAY_FOR_SCHEDULED_TASKS);
    }

    @Override
    public void cancel() {
        isCancelled = true;
        super.cancel();
    }

    @Override
    public boolean passed(Status status) {
        User user = status.getUser();

        return (!user.isVerified() && isUserFollowingAndFollowerNumbersInRange(user) && !isUserQuotedYesterday(status.getUser()) && !ignoredUsersSet.contains(user.getId()) && !wouldUserBeParodyAccount(user));
    }

    private boolean wouldUserBeParodyAccount(User user) {
        boolean wouldBe = false;
        if (user.getDescription().toLowerCase().contains("parody") || user.getLocation().toLowerCase().contains("parody") ||
                user.getDescription().toLowerCase().contains("parodi") || user.getLocation().toLowerCase().contains("parodi")) {
            if (!ignoredUsersSet.contains(user.getId())) {
                userDao.insertIgnoredUser(user);
                ignoredUsersSet.add(user.getId());
                logger.info("add into ignored users as it seems like parody account: {}", user.getScreenName());
            }
            wouldBe = true;
        }
        return wouldBe;
    }

    private boolean isUserQuotedYesterday(User user) {
        return yesterdayQuotedUsersSet.contains(user.getId());
    }

    private boolean isUserFollowingAndFollowerNumbersInRange(User user) {
        int friendsCount = user.getFriendsCount();
        int followersCount = user.getFollowersCount();
        return (friendsCount > minFollowingCount && friendsCount < maxFollowingCount && maxFollowersCount > followersCount);
    }

    private void loadYesterdayQuotedUsers() {
        yesterdayQuotedUsersSet = userDao.getYesterdaysQuotedUsers();
        logger.info("load yesterday's quoted users from database. size: {}", yesterdayQuotedUsersSet.size());
    }

    public void loadIgnoredUsers() {
        ignoredUsersSet = userDao.getIgnoredUserIds();
        logger.info("load ignored users from database. size: {}", ignoredUsersSet.size());
    }

    private void addBlockedUsersIntoIgnoredUsers() {
        try {
            IDs blockedIds = twitter.getBlocksIDs();
            RateLimitHandler.handle(twitter.getId(), blockedIds.getRateLimitStatus(), ApiProcessType.GET_BLOCKS_IDS);
            long[] blockedIdsArr = blockedIds.getIDs();

            for (long userId : blockedIdsArr) {
                if (!ignoredUsersSet.contains(userId)) {
                    addUserIntoIgnoredUsers(userId);
                }
            }
        } catch (TwitterException e) {
            logger.error(e.getErrorMessage());
        }
    }

    private void addUserIntoIgnoredUsers(Long userId) {
        try {
            User user = twitter.showUser(userId);
            RateLimitHandler.handle(twitter.getId(), user.getRateLimitStatus(), ApiProcessType.SHOW_USER);
            if (!user.isVerified()) {
                userDao.insertIgnoredUser(user);
                ignoredUsersSet.add(userId);
                logger.info("add user into ignored users: {} - {}", user.getId(), user.getScreenName());
            }
        } catch (TwitterException e) {
            logger.error(e.getErrorMessage());
        }

    }

    private void cleanUpIgnoredUsers() {
        Set<IgnoredUser> ignoredUsers = userDao.getIgnoredUsers();
        Iterator<IgnoredUser> iterator = ignoredUsers.iterator();
        while (iterator.hasNext() && !isCancelled) {
            IgnoredUser ignoredUser = iterator.next();
            ignoredUser.setLastCheck(new Date());
            try {
                if (wouldBeRemoved(ignoredUser)) {
                    userDao.deleteIgnoredUser(ignoredUser);
                    logger.info("Ignored user is deleted due to passive period. {} - {}", ignoredUser.getUserId(), ignoredUser.getScreenName());
                } else {
                    User user = twitter.showUser(ignoredUser.getUserId());
                    RateLimitHandler.handle(twitter.getId(), user.getRateLimitStatus(), ApiProcessType.SHOW_USER);

                    deleteOrUpdateIgnoredUser(ignoredUser, true, user.isVerified());
                }
            } catch (TwitterException e) {
                if ((e.getErrorCode() == 50 || e.getErrorCode() == 63)) {
                    deleteOrUpdateIgnoredUser(ignoredUser, false, false);
                } else {
                    logger.error("An error occured while getting user information from twitter", e);
                }
            }
        }
        logger.info("finish clean up ignored users task");
    }

    private void deleteOrUpdateIgnoredUser(IgnoredUser ignoredUser, boolean userExist, boolean isVerified) {
        if (userExist) {
            if (isVerified) {
                userDao.deleteIgnoredUser(ignoredUser);
                logger.info("Ignored user is deleted due to verification. {} - {}", ignoredUser.getUserId(), ignoredUser.getScreenName());
            } else if (!DataUtil.isNullOrEmpty(ignoredUser.getPassiveSince())) {
                userDao.setUserActive(ignoredUser);
                logger.info("Ignored user updated to active. {} - {}", ignoredUser.getUserId(), ignoredUser.getScreenName());
            } else {
                userDao.setLastCheck(ignoredUser);
            }
        } else if (DataUtil.isNullOrEmpty(ignoredUser.getPassiveSince())) {
            ignoredUser.setPassiveSince(new Date());
            userDao.updateIgnoredUserToPassive(ignoredUser);
            logger.info("User not found and updated to passive. userId: {} screen name: {}", ignoredUser.getUserId(), ignoredUser.getScreenName());
        } else {
            userDao.setLastCheck(ignoredUser);
        }
    }

    private boolean wouldBeRemoved(IgnoredUser ignoredUser) {
        int diff = 0;
        if (!DataUtil.isNullOrEmpty(ignoredUser.getPassiveSince())) {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate now = LocalDate.now();
            LocalDate passiveSince = LocalDate.parse(ignoredUser.getPassiveSince(), dateFormatter);

            Period period = Period.between(passiveSince, now);
            diff = period.getYears() * 365 + period.getMonths() * 30 + period.getDays();
        }

        return diff > WormConstants.MAX_PASSIVE_PERIOD;
    }

}
