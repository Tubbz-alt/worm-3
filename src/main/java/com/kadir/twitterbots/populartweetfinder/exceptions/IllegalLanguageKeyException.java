package com.kadir.twitterbots.populartweetfinder.exceptions;

/**
 * @author akadir
 * Date: 08/12/2018
 * Time: 18:29
 */
public class IllegalLanguageKeyException extends IllegalArgumentException {
    private static final String MESSAGE = "languageKey cannot be null or empty. Current value:";

    public IllegalLanguageKeyException(String languageKey) {
        super(MESSAGE + languageKey);
    }
}
