package com.kadir.twitterbots.populartweetfinder.entity;

import com.kadir.twitterbots.populartweetfinder.util.StatusUtil;
import twitter4j.Status;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author akadir
 * Date: 23/12/2017
 * Time: 14:49
 */
public class CustomStatus {
    private final SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyyMMdd");

    private Long id;
    private long statusId;
    private long userId;
    private int score;
    private String foundDate;
    private String statusCreationDate;
    private boolean isQuoted;
    private String quotedDate;
    private String statusLink;
    private String statusText;

    public CustomStatus(ResultSet resultSet) throws SQLException {
        getFromResultSet(resultSet);
    }

    public CustomStatus(Status status) {
        getFromStatus(status);
    }

    private void getFromResultSet(ResultSet resultSet) throws SQLException {
        id = resultSet.getLong("id");
        statusId = resultSet.getLong("statusId");
        userId = resultSet.getLong("userId");
        score = resultSet.getInt("score");
        foundDate = resultSet.getString("foundDate");
        statusCreationDate = resultSet.getString("statusCreationDate");
        isQuoted = resultSet.getBoolean("isQuoted");
        quotedDate = resultSet.getString("quotedDate");
        statusLink = resultSet.getString("statusLink");
        statusText = resultSet.getString("statusText");
    }

    private void getFromStatus(Status status) {
        id = null;
        statusId = status.getId();
        userId = status.getUser().getId();
        score = StatusUtil.calculateInteractionCount(status);
        foundDate = sqlDateFormat.format(new Date());
        statusCreationDate = sqlDateFormat.format(status.getCreatedAt());
        isQuoted = false;
        quotedDate = "";
        statusLink = StatusUtil.getStatusLink(status);
        statusText = status.getText();
    }

    public String getStatusInformation() {
        return "Score: " + score + " | Created at: " + statusCreationDate + " | Link: " + statusLink + " | Text:" + statusText;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getStatusId() {
        return statusId;
    }

    public void setStatusId(long statusId) {
        this.statusId = statusId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getFoundDate() {
        return foundDate;
    }

    public void setFoundDate(String foundDate) {
        this.foundDate = foundDate;
    }

    public String getStatusCreationDate() {
        return statusCreationDate;
    }

    public void setStatusCreationDate(String statusCreationDate) {
        this.statusCreationDate = statusCreationDate;
    }

    public boolean isQuoted() {
        return isQuoted;
    }

    public void setQuoted(boolean quoted) {
        isQuoted = quoted;
    }

    public String getQuotedDate() {
        return quotedDate;
    }

    public void setQuotedDate(String quotedDate) {
        this.quotedDate = quotedDate;
    }

    public String getStatusLink() {
        return statusLink;
    }

    public void setStatusLink(String statusLink) {
        this.statusLink = statusLink;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }
}
