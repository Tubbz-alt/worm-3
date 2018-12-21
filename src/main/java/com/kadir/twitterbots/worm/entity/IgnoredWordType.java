package com.kadir.twitterbots.worm.entity;

/**
 * @author akadir
 * Date: 11/12/2018
 * Time: 00:46
 */
public enum IgnoredWordType {
    WORD(1), USERNAME(2);

    private int type;

    IgnoredWordType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
