package com.kadir.twitterbots.populartweetfinder.dao;

import com.kadir.twitterbots.populartweetfinder.exceptions.DatabaseInitialisationException;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author akadir
 * Date: 09/12/2018
 * Time: 13:17
 */
public class DatabaseInitialiser {
    private static final Logger logger = Logger.getLogger(DatabaseInitialiser.class);

    private static Connection connection;

    public static void initializeDatabase() {
        try {
            logger.info("Initialising databases.");
            createTablesIfNotExist();
            logger.info("Database initialisation successful.");
        } catch (SQLException e) {
            logger.error(e);
            throw new DatabaseInitialisationException(e);
        }
    }

    private static void createTablesIfNotExist() throws SQLException {
        connection = DatabaseConnector.getConnection();
        try (Statement statement = connection.createStatement()) {
            createPopularTweetsTableIfNotExist(statement);
            createIgnoredUsersTableIfNotExist(statement);
            createSinceIdTableIfNotExist(statement);
        }
    }

    private static void createPopularTweetsTableIfNotExist(Statement statement) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS PopularTweets(" +
                "id INTEGER PRIMARY KEY," +
                "statusId INTEGER NOT NULL," +
                "userId INTEGER NOT NULL," +
                "score INTEGER NOT NULL," +
                "foundDate TEXT," +
                "statusCreationDate TEXT," +
                "isQuoted INTEGER DEFAULT 0," +
                "quotedDate TEXT DEFAULT ''," +
                "statusLink TEXT DEFAULT ''," +
                "statusText TEXT DEFAULT '');" +
                "CREATE INDEX PopularTweets_IX ON PopularTweets(foundDate);" +
                "CREATE INDEX PopularTweets_IX1 ON PopularTweets(quotedDate);" +
                "CREATE INDEX PopularTweets_IX2 ON PopularTweets(score);" +
                "CREATE INDEX PopularTweets_IX3 ON PopularTweets(userId);" +
                "CREATE INDEX PopularTweets_IX4 ON PopularTweets(statusId, foundDate);";


        statement.execute(sql);
    }

    private static void createIgnoredUsersTableIfNotExist(Statement statement) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS IgnoredUsers(" +
                "userId INTEGER UNIQUE NOT NULL," +
                "screenName TEXT NOT NULL," +
                "createdDate DATE DEFAULT (datetime('now','localtime'))," +
                "passiveSince TEXT DEFAULT '');";
        statement.execute(sql);
    }

    private static void createSinceIdTableIfNotExist(Statement statement) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS SinceIdTable(" +
                "sinceId INTEGER NOT NULL," +
                "createdDate DATE DEFAULT (datetime('now','localtime')));";
        statement.execute(sql);
    }
}
