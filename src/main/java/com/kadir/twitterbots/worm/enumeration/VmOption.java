package com.kadir.twitterbots.worm.enumeration;

/**
 * @author akadir
 * Date: 8.07.2020
 * Time: 22:04
 */
public enum VmOption {
    DB_URL("dbUrl"), DB_USER("dbUser"), DB_PASSWORD("dbPassword");

    private final String key;

    VmOption(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
