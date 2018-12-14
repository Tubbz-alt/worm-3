package com.kadir.twitterbots.populartweetfinder.dao;

import com.kadir.twitterbots.populartweetfinder.entity.IgnoredUser;
import com.kadir.twitterbots.populartweetfinder.util.DataUtil;
import org.apache.log4j.Logger;
import twitter4j.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
    private final Logger logger = Logger.getLogger(this.getClass());
    private SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyyMMdd");

    public Long getAnotherTweetOfUserForToday(Long userId) {
        Long anotherStatusId = null;
        PreparedStatement preparedStatement = null;

        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("SELECT statusId FROM PopularTweets WHERE userId = ? AND foundDate = ?");
            preparedStatement.setLong(1, userId);
            preparedStatement.setString(2, sqlDateFormat.format(new Date()));

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                anotherStatusId = rs.getLong("statusId");
            }

        } catch (SQLException e) {
            logger.error(e.getMessage());
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
            preparedStatement = connection.prepareStatement("SELECT DISTINCT(userId) FROM PopularTweets WHERE foundDate = ? AND  isQuoted = ?");

            preparedStatement.setString(1, sqlDateFormat.format(yesterday));
            preparedStatement.setBoolean(2, true);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                userIds.add(resultSet.getLong(1));
            }

        } catch (SQLException e) {
            logger.error(e);
        } finally {
            closeStatement(preparedStatement);
        }

        return userIds;
    }

    public void insertIgnoredUser(User user) {
        PreparedStatement preparedStatement = null;
        try {
            Connection connection = DatabaseConnector.getConnection();
            preparedStatement = connection.prepareStatement("INSERT INTO IgnoredUsers(UserId, ScreenName) VALUES(?,?)");

            preparedStatement.setLong(1, user.getId());
            preparedStatement.setString(2, user.getScreenName());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error(e);
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
            preparedStatement = connection.prepareStatement("SELECT userId FROM IgnoredUsers WHERE passiveSince='' or (passiveSince != '' and lastCheck=?)");
            preparedStatement.setString(1, sqlDateFormat.format(new Date()));

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                ignoredUsersIds.add(resultSet.getLong(1));
            }
        } catch (SQLException e) {
            logger.error(e);
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
            preparedStatement = connection.prepareStatement("SELECT * FROM IgnoredUsers WHERE lastCheck='' or lastCheck < ?");
            preparedStatement.setString(1, DataUtil.getYesterday());

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                ignoredUsers.add(new IgnoredUser(resultSet));
            }
        } catch (SQLException e) {
            logger.error(e);
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
            preparedStatement = conn.prepareStatement("UPDATE IgnoredUsers SET passiveSince = ?, lastCheck = ? WHERE userId = ?");

            preparedStatement.setString(1, ignoredUser.getPassiveSince());
            preparedStatement.setString(2, ignoredUser.getLastCheck());
            preparedStatement.setLong(3, ignoredUser.getUserId());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error(e.getMessage());
        } finally {
            closeStatement(preparedStatement);
        }
    }

    public void setUserActive(IgnoredUser ignoredUser) {
        PreparedStatement preparedStatement = null;
        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("UPDATE IgnoredUsers SET passiveSince = '', lastCheck = ? WHERE userId = ?");

            preparedStatement.setString(1, ignoredUser.getLastCheck());
            preparedStatement.setLong(2, ignoredUser.getUserId());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error(e.getMessage());
        } finally {
            closeStatement(preparedStatement);
        }

    }

    public void setLastCheck(IgnoredUser ignoredUser) {
        PreparedStatement preparedStatement = null;
        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("UPDATE IgnoredUsers SET lastCheck = ? WHERE userId = ?");

            preparedStatement.setString(1, ignoredUser.getLastCheck());
            preparedStatement.setLong(2, ignoredUser.getUserId());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error(e.getMessage());
        } finally {
            closeStatement(preparedStatement);
        }

    }

    public void deleteIgnoredUser(IgnoredUser ignoredUser) {
        PreparedStatement preparedStatement = null;
        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("DELETE FROM IgnoredUsers WHERE userId = ?");
            preparedStatement.setLong(1, ignoredUser.getUserId());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error(e.getMessage());
        } finally {
            closeStatement(preparedStatement);
        }
    }

    private void closeResultSet(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
        }
    }

    private void closeStatement(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
        }
    }

}
