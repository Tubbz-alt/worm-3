package com.kadir.twitterbots.worm.entity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author akadir
 * Date: 09/12/2018
 * Time: 14:42
 */
public class IgnoredUser {
    private long userId;
    private String screenName;
    private String createdDate;
    private String passiveSince;
    private String lastCheck;

    public IgnoredUser(ResultSet resultSet) throws SQLException {
        getFromResultSet(resultSet);
    }

    private void getFromResultSet(ResultSet resultSet) throws SQLException {
        userId = resultSet.getLong("user_id");
        screenName = resultSet.getString("screen_name");
        createdDate = resultSet.getString("created_date");
        passiveSince = resultSet.getString("passive_since");
        lastCheck = resultSet.getString("last_check");
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public String getPassiveSince() {
        return passiveSince;
    }

    public void setPassiveSince(Date passiveSince) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        this.passiveSince = simpleDateFormat.format(passiveSince);
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastCheck() {
        return lastCheck;
    }

    public void setLastCheck(Date lastCheck) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        this.lastCheck = simpleDateFormat.format(lastCheck);
    }
}
