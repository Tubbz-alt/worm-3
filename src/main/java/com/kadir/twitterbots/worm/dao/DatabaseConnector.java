package com.kadir.twitterbots.worm.dao;

import com.kadir.twitterbots.worm.util.WormConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author akadir
 * Date: 09/12/2018
 * Time: 13:19
 */
public class DatabaseConnector {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnector.class);
    private static final String DATABASE_URL = "jdbc:sqlite:" + WormConstants.RESOURCES_FOLDER_NAME + "popularTweetsDb";
    private static Connection connection;

    private DatabaseConnector() {
    }

    public static Connection getConnection() throws SQLException {
        return openConnection();
    }

    private static Connection openConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DATABASE_URL);
            DatabaseMetaData meta = connection.getMetaData();
            logger.debug("Connected to database: {}", meta.getURL());
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            logger.debug("Closing db connection.");
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            logger.debug("db connection closed.");
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
    }
}
