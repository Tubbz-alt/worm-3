package com.kadir.twitterbots.worm.entity;

/**
 * @author akarakoc
 * Date :   11.12.2018
 * Time :   14:43
 */
public enum TaskPriority {
    VERY_HIGH(3), HIGH(2), LOW(1);

    int level;

    TaskPriority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

}
