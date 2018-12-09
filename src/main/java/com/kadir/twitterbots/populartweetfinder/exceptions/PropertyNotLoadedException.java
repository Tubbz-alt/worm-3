package com.kadir.twitterbots.populartweetfinder.exceptions;

/**
 * @author akadir
 * Date: 08/12/2018
 * Time: 15:48
 */
public class PropertyNotLoadedException extends RuntimeException {
    private static final String MESSAGE = "Error occured while getting properties from file. ";

    public PropertyNotLoadedException(String fileName) {
        super(MESSAGE + fileName);
    }

    public PropertyNotLoadedException(String fileName, Exception e) {
        super(MESSAGE + fileName, e);
    }
}
