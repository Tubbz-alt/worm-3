package com.kadir.twitterbots.populartweetfinder.exceptions;

/**
 * @author akadir
 * Date: 09/12/2018
 * Time: 13:50
 */
public class TwitterAuthenticationException extends RuntimeException {
    private static final String MESSAGE = "An error occured while authenticating twitter account.";

    public TwitterAuthenticationException() {
        super(MESSAGE);
    }

    public TwitterAuthenticationException(Exception e) {
        super(MESSAGE, e);
    }


}
