package com.kadir.twitterbots.worm.exceptions;

/**
 * @author akadir
 * Date: 08/12/2018
 * Time: 23:22
 */
public class LanguageIdentifierInitialisingException extends RuntimeException {
    private static final String MESSAGE = "Error occured while initialising language identifiers";

    public LanguageIdentifierInitialisingException() {
        super(MESSAGE);
    }

    public LanguageIdentifierInitialisingException(Exception e) {
        super(MESSAGE, e);
    }


}
