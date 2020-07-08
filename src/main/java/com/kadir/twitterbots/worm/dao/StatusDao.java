package com.kadir.twitterbots.worm.dao;

import com.kadir.twitterbots.worm.entity.CustomStatus;
import com.kadir.twitterbots.worm.util.StatusUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.Status;

import java.sql.*;
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
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyyMMdd");

    public void saveAll(List<CustomStatus> fetchedStatuses) {
        for (CustomStatus customStatus : fetchedStatuses) {
            saveStatus(customStatus);
        }
    }

    public Long saveStatus(CustomStatus status) {
        PreparedStatement preparedStatement = null;
        Long id = null;
        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("INSERT INTO popular_tweets(status_id, user_id, score, found_date, status_creation_date, is_quoted, quoted_date, status_link, status_text) " +
                    "VALUES(?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

            preparedStatement.setLong(1, status.getStatusId());
            preparedStatement.setLong(2, status.getUserId());
            preparedStatement.setInt(3, status.getScore());
            preparedStatement.setString(4, sqlDateFormat.format(new Date()));
            preparedStatement.setString(5, status.getStatusCreationDate());
            preparedStatement.setBoolean(6, false);
            preparedStatement.setString(7, "");
            preparedStatement.setString(8, status.getStatusLink());
            preparedStatement.setString(9, status.getStatusText());

            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Insert status failed, no rows affected.");
            }

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    id = generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Insert status failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            logger.error("Error during status save: ", e);
        } finally {
            closeStatement(preparedStatement);
        }
        return id;
    }

    public void updateStatus(CustomStatus customStatus) {
        PreparedStatement preparedStatement = null;
        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("UPDATE popular_tweets SET score = ? WHERE id = ?");

            preparedStatement.setInt(1, customStatus.getScore());
            preparedStatement.setLong(2, customStatus.getId());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error during status update: ", e);
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
            preparedStatement = conn.prepareStatement("INSERT INTO popular_tweets(status_id, user_id, score, found_date, status_creation_date, is_quoted, quoted_date, status_link, status_text) " +
                    "VALUES(?,?,?,?,?,?,?,?,?,?)");
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
            logger.error("Error during status save: ", e);
        } finally {
            closeStatement(preparedStatement);
        }
    }

    public void removeStatus(CustomStatus savedStatus) {
        PreparedStatement preparedStatement = null;
        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("DELETE FROM popular_tweets WHERE id = ?");
            preparedStatement.setLong(1, savedStatus.getId());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error during status remove: ", e);
        } finally {
            closeStatement(preparedStatement);
        }

    }


    public void updateTodaysStatusScore(Long statusId, int score) {
        PreparedStatement preparedStatement = null;
        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("UPDATE popular_tweets SET score = ? WHERE status_id = ? AND found_date = ?");

            preparedStatement.setInt(1, score);
            preparedStatement.setLong(2, statusId);
            preparedStatement.setString(3, sqlDateFormat.format(new Date()));

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            logger.error("Error during today's status score update: ", e);
        } finally {
            closeStatement(preparedStatement);
        }
    }

    public void changeUserStatus(Long oldStatusId, Status newStatus) {
        PreparedStatement preparedStatement = null;
        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("UPDATE popular_tweets SET status_id = ?, score = ?, status_creation_date = ?, status_link = ?, status_text = ? " +
                    "WHERE status_id = ? AND found_date = ?");
            preparedStatement.setLong(1, newStatus.getId());
            preparedStatement.setInt(2, StatusUtil.calculateInteractionCount(newStatus));
            preparedStatement.setString(3, sqlDateFormat.format(newStatus.getCreatedAt()));
            preparedStatement.setString(4, StatusUtil.getStatusLink(newStatus));
            preparedStatement.setString(5, newStatus.getText());
            preparedStatement.setLong(6, oldStatusId);
            preparedStatement.setString(7, sqlDateFormat.format(new Date()));

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error during user status change: ", e);
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
                preparedStatement = conn.prepareStatement("DELETE FROM popular_tweets WHERE status_id = ? AND found_date = ?");
                preparedStatement.setLong(1, statusId);
                preparedStatement.setString(2, sqlDateFormat.format(new Date()));

                preparedStatement.executeUpdate();

                logger.info("Status was deleted. {}", customStatus.getStatusInformation());
            } catch (SQLException e) {
                logger.error("Error during today's status delete: ", e);
            } finally {
                closeStatement(preparedStatement);
            }
        } else {
            logger.info("Status not found for today with status id: {}", statusId);
        }

    }

    public CustomStatus getStatusFromTodayByStatusId(Long statusId) {
        CustomStatus customStatus = null;
        PreparedStatement preparedStatement = null;
        try {
            Connection connection = DatabaseConnector.getConnection();
            preparedStatement = connection.prepareStatement("SELECT * FROM popular_tweets WHERE found_date = ? AND status_id = ?");

            preparedStatement.setString(1, sqlDateFormat.format(new Date()));
            preparedStatement.setLong(2, statusId);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                customStatus = new CustomStatus(resultSet);
            }

        } catch (SQLException e) {
            logger.error("Error during today's status get: ", e);
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
            preparedStatement = connection.prepareStatement("SELECT * FROM popular_tweets WHERE found_date = ? AND status_id = ?");

            preparedStatement.setString(1, sqlDateFormat.format(yesterday));
            preparedStatement.setLong(2, statusId);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                customStatus = new CustomStatus(resultSet);
            }

        } catch (SQLException e) {
            logger.error("Error during yesterday's status get: ", e);
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
            preparedStatement = connection.prepareStatement("SELECT * FROM popular_tweets WHERE found_date = ? ORDER BY score DESC");

            preparedStatement.setString(1, sqlDateFormat.format(new Date()));

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                CustomStatus s = new CustomStatus(resultSet);
                todayStatuses.add(s);
            }

        } catch (SQLException e) {
            logger.error("Error during today's status get: ", e);
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
            preparedStatement = connection.prepareStatement("SELECT * FROM popular_tweets WHERE status_id = ? AND is_quoted = ?");

            preparedStatement.setLong(1, statusId);
            preparedStatement.setBoolean(2, true);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                isQuoted = true;
            }

        } catch (SQLException e) {
            logger.error("Error during status quoted check: ", e);
        } finally {
            closeStatement(preparedStatement);
        }

        return isQuoted;
    }

    public void setStatusQuoted(Long statusId) {
        PreparedStatement preparedStatement = null;

        try {
            Connection connection = DatabaseConnector.getConnection();
            preparedStatement = connection.prepareStatement("UPDATE popular_tweets SET is_quoted = ?, quoted_date = ? WHERE id = ?");

            preparedStatement.setBoolean(1, true);
            preparedStatement.setString(2, sqlDateFormat.format(new Date()));
            preparedStatement.setLong(3, statusId);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error during status quoted set: ", e);
        } finally {
            closeStatement(preparedStatement);
        }
    }

    public int getTodaysPopularTweetsCount() {
        int count = 0;
        PreparedStatement preparedStatement = null;

        try {
            Connection connection = DatabaseConnector.getConnection();
            preparedStatement = connection.prepareStatement("SELECT COUNT(*) FROM popular_tweets WHERE found_date = ?");

            preparedStatement.setString(1, sqlDateFormat.format(new Date()));

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                count = resultSet.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error during today's popular status count get: ", e);
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
                logger.error("Error during statement close: ", e);
            }
        }
    }
}
