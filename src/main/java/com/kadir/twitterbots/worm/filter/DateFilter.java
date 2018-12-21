package com.kadir.twitterbots.worm.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.Status;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author akadir
 * Date: 08/12/2018
 * Time: 17:10
 */
public class DateFilter implements StatusFilter {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public DateFilter() {
        logger.info("{} created", this.getClass().getSimpleName());
    }

    @Override
    public boolean passed(Status status) {
        Date today = new Date();
        Date statusDate = status.getCreatedAt();

        return simpleDateFormat.format(statusDate).equals(simpleDateFormat.format(today));
    }
}
