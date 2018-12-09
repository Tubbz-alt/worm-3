package com.kadir.twitterbots.populartweetfinder.dao;

import com.kadir.twitterbots.populartweetfinder.entity.CustomStatus;
import com.kadir.twitterbots.populartweetfinder.util.StatusUtil;
import org.apache.log4j.Logger;
import twitter4j.Status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author akadir
 * Date: 09/12/2018
 * Time: 13:30
 */
public class StatusDao {
    private final Logger logger = Logger.getLogger(this.getClass());
    private SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyyMMdd");

    public void saveAll(List<CustomStatus> fetchedStatuses) {
        for (CustomStatus customStatus : fetchedStatuses) {
            saveStatus(customStatus);
        }
    }

    public void saveStatus(CustomStatus status) {
        PreparedStatement preparedStatement = null;

        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("INSERT INTO PopularTweets(statusId, userId, score, foundDate, statusCreationDate, isQuoted, quotedDate, statusLink, statusText) " +
                    "VALUES(?,?,?,?,?,?,?,?,?)");
            preparedStatement.setLong(1, status.getStatusId());
            preparedStatement.setLong(2, status.getUserId());
            preparedStatement.setInt(3, status.getScore());
            preparedStatement.setString(4, sqlDateFormat.format(new Date()));
            preparedStatement.setString(5, status.getStatusCreationDate());
            preparedStatement.setBoolean(6, false);
            preparedStatement.setString(7, "");
            preparedStatement.setString(8, status.getStatusLink());
            preparedStatement.setString(9, status.getStatusText());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error(e);
        } finally {
            closeStatement(preparedStatement);
        }
    }

    public void updateStatus(CustomStatus customStatus) {
        PreparedStatement preparedStatement = null;
        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("UPDATE PopularTweets SET score = ? WHERE id = ?");

            preparedStatement.setInt(1, customStatus.getScore());
            preparedStatement.setLong(2, customStatus.getId());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error(e.getMessage());
        } finally {
            closeStatement(preparedStatement);
        }

    }


    public void saveStatus(Status status) {
        int statusScore = StatusUtil.calculateInteractionCount(status);
        String statusLink = StatusUtil.getStatusLink(status);

        PreparedStatement preparedStatement = null;

        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("INSERT INTO PopularTweets(statusId, userId, score, foundDate, statusCreationDate, isQuoted, quotedDate, statusLink, statusText) " +
                    "VALUES(?,?,?,?,?,?,?,?,?)");
            preparedStatement.setLong(1, status.getId());
            preparedStatement.setLong(2, status.getUser().getId());
            preparedStatement.setInt(3, statusScore);
            preparedStatement.setString(4, sqlDateFormat.format(new Date()));
            preparedStatement.setString(5, sqlDateFormat.format(status.getCreatedAt()));
            preparedStatement.setBoolean(6, false);
            preparedStatement.setString(7, "");
            preparedStatement.setString(8, statusLink);
            preparedStatement.setString(9, status.getText());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error(e);
        } finally {
            closeStatement(preparedStatement);
        }
    }

    public void removeStatus(CustomStatus savedStatus) {
        PreparedStatement preparedStatement = null;
        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("DELETE FROM PopularTweets WHERE id = ?");
            preparedStatement.setLong(1, savedStatus.getId());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error(e.getMessage());
        } finally {
            closeStatement(preparedStatement);
        }

    }


    public void updateTodaysStatusScore(Long statusId, int score) {
        PreparedStatement preparedStatement = null;
        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("UPDATE PopularTweets SET score = ? WHERE statusId = ? AND foundDate = ?");

            preparedStatement.setInt(1, score);
            preparedStatement.setLong(2, statusId);
            preparedStatement.setString(3, sqlDateFormat.format(new Date()));

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            logger.error(e.getMessage());
        } finally {
            closeStatement(preparedStatement);
        }
    }

    public void changeUserStatus(Long oldStatusId, Status newStatus) {
        PreparedStatement preparedStatement = null;
        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("UPDATE PopularTweets SET statusId = ?, score = ?, statusCreationDate = ?, statusLink = ?, statusText = ? " +
                    "WHERE statusId = ? AND foundDate = ?");
            preparedStatement.setLong(1, newStatus.getId());
            preparedStatement.setInt(2, StatusUtil.calculateInteractionCount(newStatus));
            preparedStatement.setString(3, sqlDateFormat.format(newStatus.getCreatedAt()));
            preparedStatement.setString(4, StatusUtil.getStatusLink(newStatus));
            preparedStatement.setString(5, newStatus.getText());
            preparedStatement.setLong(6, oldStatusId);
            preparedStatement.setString(7, sqlDateFormat.format(new Date()));

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error(e.getMessage());
        } finally {
            closeStatement(preparedStatement);
        }
    }

    public void deleteTodaysStatusByStatusId(Long statusId) {
        CustomStatus customStatus = getStatusFromTodayByStatusId(statusId);

        if (customStatus != null) {
            PreparedStatement preparedStatement = null;
            try {
                Connection conn = DatabaseConnector.getConnection();
                preparedStatement = conn.prepareStatement("DELETE FROM PopularTweets WHERE statusId = ? AND foundDate = ?");
                preparedStatement.setLong(1, statusId);
                preparedStatement.setString(2, sqlDateFormat.format(new Date()));

                preparedStatement.executeUpdate();

                logger.info("Status was deleted. " + customStatus.getStatusInformation());
            } catch (SQLException e) {
                logger.error(e.getMessage());
            } finally {
                closeStatement(preparedStatement);
            }
        } else {
            logger.info("Status not found for today with status id: " + statusId);
        }

    }

    public CustomStatus getStatusFromTodayByStatusId(Long statusId) {
        CustomStatus customStatus = null;
        PreparedStatement preparedStatement = null;
        try {
            Connection connection = DatabaseConnector.getConnection();
            preparedStatement = connection.prepareStatement("SELECT * FROM PopularTweets WHERE foundDate = ? AND statusId = ?");

            preparedStatement.setString(1, sqlDateFormat.format(new Date()));
            preparedStatement.setLong(2, statusId);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                customStatus = new CustomStatus(resultSet);
            }

        } catch (SQLException e) {
            logger.error(e);
        } finally {
            closeStatement(preparedStatement);
        }
        return customStatus;
    }

    public CustomStatus getStatusFromYesterdayByStatusId(Long statusId) {
        CustomStatus customStatus = null;

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        Date yesterday = calendar.getTime();

        PreparedStatement preparedStatement = null;

        try {
            Connection connection = DatabaseConnector.getConnection();
            preparedStatement = connection.prepareStatement("SELECT * FROM PopularTweets WHERE foundDate = ? AND statusId = ?");

            preparedStatement.setString(1, sqlDateFormat.format(yesterday));
            preparedStatement.setLong(2, statusId);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                customStatus = new CustomStatus(resultSet);
            }

        } catch (SQLException e) {
            logger.error(e);
        } finally {
            closeStatement(preparedStatement);
        }

        return customStatus;
    }

    public List<CustomStatus> getTodaysStatuses() {
        ArrayList<CustomStatus> todayStatuses = new ArrayList<>();

        PreparedStatement preparedStatement = null;

        try {
            Connection connection = DatabaseConnector.getConnection();
            preparedStatement = connection.prepareStatement("SELECT * FROM PopularTweets WHERE foundDate = ? ORDER BY score DESC");

            preparedStatement.setString(1, sqlDateFormat.format(new Date()));

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                CustomStatus s = new CustomStatus(resultSet);
                todayStatuses.add(s);
            }

        } catch (SQLException e) {
            logger.error(e);
        } finally {
            closeStatement(preparedStatement);
        }

        return todayStatuses;
    }

    public boolean isStatusQuotedBefore(Long statusId) {
        boolean isQuoted = false;

        PreparedStatement preparedStatement = null;

        try {
            Connection connection = DatabaseConnector.getConnection();
            preparedStatement = connection.prepareStatement("SELECT * FROM PopularTweets WHERE statusId = ? AND isQuoted = ?");

            preparedStatement.setLong(1, statusId);
            preparedStatement.setBoolean(2, true);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                isQuoted = true;
            }

        } catch (SQLException e) {
            logger.error(e);
        } finally {
            closeStatement(preparedStatement);
        }

        return isQuoted;
    }

    public void setStatusQuoted(Long statusId) {
        PreparedStatement preparedStatement = null;

        try {
            Connection connection = DatabaseConnector.getConnection();
            preparedStatement = connection.prepareStatement("UPDATE PopularTweets SET isQuoted = ?, quotedDate = ? WHERE statusId = ?");

            preparedStatement.setBoolean(1, true);
            preparedStatement.setString(2, sqlDateFormat.format(new Date()));
            preparedStatement.setLong(3, statusId);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error(e.getMessage());
        } finally {
            closeStatement(preparedStatement);
        }
    }

    public int getTodaysPopularTweetsCount() {
        int count = 0;
        PreparedStatement preparedStatement = null;

        try {
            Connection connection = DatabaseConnector.getConnection();
            preparedStatement = connection.prepareStatement("SELECT COUNT(*) FROM PopularTweets WHERE foundDate = ?");

            preparedStatement.setString(1, sqlDateFormat.format(new Date()));

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                count = resultSet.getInt(1);
            }
        } catch (SQLException e) {
            logger.error(e);
        } finally {
            closeStatement(preparedStatement);
        }

        return count;
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
