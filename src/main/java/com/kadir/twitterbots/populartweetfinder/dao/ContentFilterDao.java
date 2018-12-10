package com.kadir.twitterbots.populartweetfinder.dao;

import com.kadir.twitterbots.populartweetfinder.entity.IgnoredWordType;
import com.kadir.twitterbots.populartweetfinder.util.DataUtil;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * @author akadir
 * Date: 11/12/2018
 * Time: 00:24
 */
public class ContentFilterDao {
    private final Logger logger = Logger.getLogger(this.getClass());

    public Set<String> getIgnoredWords() {
        Set<String> ignoredWords = new HashSet<>();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("SELECT word FROM IgnoredWords WHERE type = ?");
            preparedStatement.setInt(1, IgnoredWordType.WORD.getType());
            resultSet = preparedStatement.executeQuery();
            preparedStatement.setString(1, DataUtil.getYesterday());

            while (resultSet.next()) {
                ignoredWords.add(resultSet.getString(1).toLowerCase());
            }
        } catch (SQLException e) {
            logger.error(e);
        } finally {
            closeResultSet(resultSet);
            closeStatement(preparedStatement);
        }
        return ignoredWords;
    }

    public Set<String> getIgnoredUsernames() {
        Set<String> ignoredUsernames = new HashSet<>();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            Connection conn = DatabaseConnector.getConnection();
            preparedStatement = conn.prepareStatement("SELECT word FROM IgnoredWords WHERE type = ?");
            preparedStatement.setInt(1, IgnoredWordType.USERNAME.getType());
            resultSet = preparedStatement.executeQuery();
            preparedStatement.setString(1, DataUtil.getYesterday());

            while (resultSet.next()) {
                ignoredUsernames.add(resultSet.getString(1).toLowerCase());
            }
        } catch (SQLException e) {
            logger.error(e);
        } finally {
            closeResultSet(resultSet);
            closeStatement(preparedStatement);
        }
        return ignoredUsernames;
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
