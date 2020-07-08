package com.kadir.twitterbots.worm.dao;

import com.kadir.twitterbots.worm.enumeration.VmOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author akadir
 * Date: 09/12/2018
 * Time: 13:19
 */
public class DatabaseConnector {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnector.class);
    private static Connection connection;

    private DatabaseConnector() {
    }

    public static Connection getConnection() throws SQLException {
        return openConnection();
    }

    private static Connection openConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = "jdbc:postgresql://" + System.getProperty(VmOption.DB_URL.getKey());

            logger.info("Generated database url: {}", url);

            Properties props = new Properties();
            props.setProperty("user", System.getProperty(VmOption.DB_USER.getKey()));
            props.setProperty("password", System.getProperty(VmOption.DB_PASSWORD.getKey()));

            connection = DriverManager.getConnection(url, props);
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
