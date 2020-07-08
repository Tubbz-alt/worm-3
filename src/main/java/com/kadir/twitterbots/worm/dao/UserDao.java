package com.kadir.twitterbots.worm.dao;

import com.kadir.twitterbots.worm.entity.IgnoredUser;
import com.kadir.twitterbots.worm.util.DataUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.User;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author akadir
 * Date: 09/12/2018
 * Time: 13:30
 */
public class UserDao {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyyMMdd");

    public Long getAnotherTweetOfUserForToday(Long userId) {
        Long anotherStatusId = null;
        PreparedStatement preparedStatement = null;

        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("SELECT statusId FROM popular_tweets WHERE user_id = ? AND found_date = ?");
            preparedStatement.setLong(1, userId);
            preparedStatement.setString(2, sqlDateFormat.format(new Date()));

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                anotherStatusId = rs.getLong("statusId");
            }

        } catch (SQLException e) {
            logger.error("Error during another tweet of user today get: ", e);
        } finally {
            closeStatement(preparedStatement);
        }

        return anotherStatusId;
    }

    public Set<Long> getYesterdaysQuotedUsers() {
        Set<Long> userIds = new HashSet<>();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        Date yesterday = calendar.getTime();

        PreparedStatement preparedStatement = null;

        try {
            Connection connection = DatabaseConnector.getConnection();
            preparedStatement = connection.prepareStatement("SELECT DISTINCT(user_id) FROM popular_tweets WHERE found_date = ? AND  is_quoted = ?");

            preparedStatement.setString(1, sqlDateFormat.format(yesterday));
            preparedStatement.setBoolean(2, true);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                userIds.add(resultSet.getLong(1));
            }

        } catch (SQLException e) {
            logger.error("Error during yesterday's quoted users get: ", e);
        } finally {
            closeStatement(preparedStatement);
        }

        return userIds;
    }

    public void insertIgnoredUser(User user) {
        PreparedStatement preparedStatement = null;
        try {
            Connection connection = DatabaseConnector.getConnection();
            preparedStatement = connection.prepareStatement("INSERT INTO ignored_users(user_id, screen_name) VALUES(?,?)");

            preparedStatement.setLong(1, user.getId());
            preparedStatement.setString(2, user.getScreenName());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error during ignored user insert: ", e);
        } finally {
            closeStatement(preparedStatement);
        }
    }

    public Set<Long> getIgnoredUserIds() {
        Set<Long> ignoredUsersIds = new HashSet<>();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            Connection connection = DatabaseConnector.getConnection();
            preparedStatement = connection.prepareStatement("SELECT user_id FROM ignored_users WHERE passive_since='' or (passive_since != '' and last_check=?)");
            preparedStatement.setString(1, sqlDateFormat.format(new Date()));

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                ignoredUsersIds.add(resultSet.getLong(1));
            }
        } catch (SQLException e) {
            logger.error("Error during ignored user id get: ", e);
        } finally {
            closeResultSet(resultSet);
            closeStatement(preparedStatement);
        }
        return ignoredUsersIds;
    }

    public Set<IgnoredUser> getIgnoredUsers() {
        Set<IgnoredUser> ignoredUsers = new HashSet<>();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            Connection connection = DatabaseConnector.getConnection();
            preparedStatement = connection.prepareStatement("SELECT * FROM ignored_users WHERE last_check='' or last_check < ?");
            preparedStatement.setString(1, DataUtil.getYesterday());

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                ignoredUsers.add(new IgnoredUser(resultSet));
            }
        } catch (SQLException e) {
            logger.error("Error during ignored users get: ", e);
        } finally {
            closeResultSet(resultSet);
            closeStatement(preparedStatement);
        }
        return ignoredUsers;
    }

    public void updateIgnoredUserToPassive(IgnoredUser ignoredUser) {
        PreparedStatement preparedStatement = null;
        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("UPDATE ignored_users SET passive_since = ?, last_check = ? WHERE user_id = ?");

            preparedStatement.setString(1, ignoredUser.getPassiveSince());
            preparedStatement.setString(2, ignoredUser.getLastCheck());
            preparedStatement.setLong(3, ignoredUser.getUserId());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error during ignored user update to passive: ", e);
        } finally {
            closeStatement(preparedStatement);
        }
    }

    public void setUserActive(IgnoredUser ignoredUser) {
        PreparedStatement preparedStatement = null;
        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("UPDATE ignored_users SET passive_since = '', last_check = ? WHERE user_id = ?");

            preparedStatement.setString(1, ignoredUser.getLastCheck());
            preparedStatement.setLong(2, ignoredUser.getUserId());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error during user active set: ", e);
        } finally {
            closeStatement(preparedStatement);
        }

    }

    public void setLastCheck(IgnoredUser ignoredUser) {
        PreparedStatement preparedStatement = null;
        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("UPDATE ignored_users SET last_check = ? WHERE user_id = ?");

            preparedStatement.setString(1, ignoredUser.getLastCheck());
            preparedStatement.setLong(2, ignoredUser.getUserId());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error during last check set: ", e);
        } finally {
            closeStatement(preparedStatement);
        }

    }

    public void deleteIgnoredUser(IgnoredUser ignoredUser) {
        PreparedStatement preparedStatement = null;
        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("DELETE FROM ignored_users WHERE user_id = ?");
            preparedStatement.setLong(1, ignoredUser.getUserId());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error during ignored user delete: ", e);
        } finally {
            closeStatement(preparedStatement);
        }
    }

    private void closeResultSet(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                logger.error("Error during result set close: ", e);
            }
        }
    }

    private void closeStatement(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                logger.error("Error during statement close: ", e);
            }
        }
    }

}
