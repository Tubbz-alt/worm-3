package com.kadir.twitterbots.populartweetfinder.entity;

/**
 * @author akadir
 * Date: 09/12/2018
 * Time: 23:23
 */
public enum ApiProcessType {
    SEARCH("search"), SHOW_STATUS("showStatus"), SHOW_USER("showUser"), UPDATE_STATUS("updateStatus");

    private String name;

    private ApiProcessType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
