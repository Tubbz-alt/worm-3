package com.kadir.twitterbots.worm.exceptions;

/**
 * @author akadir
 * Date: 09/12/2018
 * Time: 13:46
 */
public class DatabaseInitialisationException extends RuntimeException {
    private static final String MESSAGE = "An error occured while initialising database. ";

    public DatabaseInitialisationException() {
        super(MESSAGE);
    }

    public DatabaseInitialisationException(Exception e) {
        super(MESSAGE, e);
    }

}
