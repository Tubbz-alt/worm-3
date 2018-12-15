package com.kadir.twitterbots.populartweetfinder.authentication;

import com.kadir.twitterbots.populartweetfinder.exceptions.IllegalApiTokenException;
import com.kadir.twitterbots.populartweetfinder.exceptions.TwitterAuthenticationException;
import com.kadir.twitterbots.populartweetfinder.util.DataUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author akadir
 * Date: 08/12/2018
 * Time: 13:35
 */
public class BotAuthenticator {
    private static final Logger logger = LoggerFactory.getLogger(BotAuthenticator.class);

    private BotAuthenticator() {
    }

    public static Twitter authenticate(String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret) {
        Twitter twitter;
        try {
            validateTokens(consumerKey, consumerSecret, accessToken, accessTokenSecret);
            ConfigurationBuilder cb = buildConfigurationBuilder(consumerKey, consumerSecret, accessToken, accessTokenSecret);

            twitter = new TwitterFactory(cb.build()).getInstance();
            User authUser = twitter.showUser(twitter.verifyCredentials().getId());
            logger.info("Bot authenticated successfully. Account name: {} - {} - {}", authUser.getName(), authUser.getScreenName(), authUser.getId());
        } catch (IOException | TwitterException e) {
            logger.error("An error occured while authenticating twitter account.", e);
            throw new TwitterAuthenticationException(e);
        }

        return twitter;
    }

    private static ConfigurationBuilder buildConfigurationBuilder(String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret) {
        return new ConfigurationBuilder()
                .setOAuthConsumerKey(consumerKey)
                .setOAuthConsumerSecret(consumerSecret)
                .setOAuthAccessToken(accessToken)
                .setOAuthAccessTokenSecret(accessTokenSecret);
    }

    private static void validateTokens(String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret) throws TwitterException, IOException {
        if (DataUtil.isNullOrEmpty(consumerKey) || DataUtil.isNullOrEmpty(consumerSecret)) {
            logger.error("consumerKey or consumerSecret cannot be null or empty.");
            throw new IllegalApiTokenException("consumerKey or consumerSecret cannot be null or empty.");
        } else if (DataUtil.isNullOrEmpty(accessToken) || DataUtil.isNullOrEmpty(accessTokenSecret)) {
            getAccessTokenAndSecretFromTwitter(consumerKey, consumerSecret, accessToken, accessTokenSecret);
        }
    }

    private static void getAccessTokenAndSecretFromTwitter(String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret) throws TwitterException, IOException {
        Twitter twitter = TwitterFactory.getSingleton();
        twitter.setOAuthConsumer(consumerKey, consumerSecret);

        RequestToken requestToken = twitter.getOAuthRequestToken();
        AccessToken authAccessToken = null;

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (null == authAccessToken) {
            logger.info("Open the following URL and grant access to your account:");
            logger.info(requestToken.getAuthorizationURL());
            logger.info("Enter the PIN (if available) or just hit enter.[PIN]:");
            String pin;
            pin = br.readLine();
            if (pin.length() > 0) {
                authAccessToken = twitter.getOAuthAccessToken(requestToken, pin);
                accessToken = authAccessToken.getToken();
                accessTokenSecret = authAccessToken.getTokenSecret();
                logger.info("accessToken: {}", accessToken);
                logger.info("accessTokenSecret: {}", accessTokenSecret);
            } else {
                authAccessToken = twitter.getOAuthAccessToken();
            }
        }
    }
}
