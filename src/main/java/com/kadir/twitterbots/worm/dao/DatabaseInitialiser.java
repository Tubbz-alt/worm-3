package com.kadir.twitterbots.worm.dao;

import com.kadir.twitterbots.worm.exceptions.DatabaseInitialisationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author akadir
 * Date: 09/12/2018
 * Time: 13:17
 */
public class DatabaseInitialiser {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitialiser.class);

    private DatabaseInitialiser() {
    }

    public static void initializeDatabase() {
        try {
            logger.info("Initialising databases.");
            createTablesIfNotExist();
            logger.info("Database initialisation successful.");
        } catch (SQLException e) {
            logger.error(e.getMessage());
            throw new DatabaseInitialisationException(e);
        }
    }

    private static void createTablesIfNotExist() throws SQLException {
        Connection connection = DatabaseConnector.getConnection();
        try (Statement statement = connection.createStatement()) {
            createPopularTweetsTableIfNotExist(statement);
            createIgnoredUsersTableIfNotExist(statement);
            createIgnoredKeywordsTableIfNotExist(statement);
        }
    }

    private static void createPopularTweetsTableIfNotExist(Statement statement) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS popular_tweets(" +
                "id SERIAL PRIMARY KEY," +
                "status_id BIGSERIAL NOT NULL," +
                "user_id BIGSERIAL NOT NULL," +
                "score INTEGER NOT NULL," +
                "found_date varchar," +
                "status_creation_date varchar," +
                "is_quoted BOOLEAN DEFAULT false," +
                "quoted_date varchar DEFAULT ''," +
                "status_link varchar DEFAULT ''," +
                "status_text varchar DEFAULT '');" +
                "CREATE INDEX IF NOT EXISTS popular_tweets_idx ON popular_tweets (found_date);" +
                "CREATE INDEX IF NOT EXISTS popular_tweets_idx2 ON popular_tweets (quoted_date);" +
                "CREATE INDEX IF NOT EXISTS popular_tweets_idx3 ON popular_tweets (score);" +
                "CREATE INDEX IF NOT EXISTS popular_tweets_idx4 ON popular_tweets (user_id);" +
                "CREATE INDEX IF NOT EXISTS popular_tweets_idx5 ON popular_tweets (status_id, found_date);";


        statement.execute(sql);
    }

    private static void createIgnoredUsersTableIfNotExist(Statement statement) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS ignored_users(" +
                "user_id BIGSERIAL UNIQUE NOT NULL," +
                "screen_name varchar NOT NULL," +
                "created_date DATE DEFAULT CURRENT_DATE," +
                "passive_since varchar DEFAULT ''," +
                "last_check varchar DEFAULT '');";
        statement.execute(sql);
    }

    private static void createIgnoredKeywordsTableIfNotExist(Statement statement) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS ignored_keywords(" +
                "id SERIAL PRIMARY KEY," +
                "word varchar UNIQUE NOT NULL," +
                "type INTEGER NOT NULL," +
                "created_date DATE DEFAULT CURRENT_DATE);" +
                "CREATE INDEX IF NOT EXISTS ignored_keywords_idx ON ignored_keywords(type);";
        statement.execute(sql);
    }
}
