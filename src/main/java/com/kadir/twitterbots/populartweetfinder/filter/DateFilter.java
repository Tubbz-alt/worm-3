package com.kadir.twitterbots.populartweetfinder.filter;

import com.kadir.twitterbots.populartweetfinder.util.ApplicationConstants;
import org.apache.log4j.Logger;
import twitter4j.Status;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author akadir
 * Date: 08/12/2018
 * Time: 17:10
 */
public class DateFilter implements StatusFilter {
    private final Logger logger = Logger.getLogger(this.getClass());

    public DateFilter() {
        logger.info(this.getClass().getSimpleName() + " created");
    }

    @Override
    public boolean passed(Status status) {
        Date today = new Date();
        Date statusDate = status.getCreatedAt();
        SimpleDateFormat simpleDateFormat = ApplicationConstants.DATE_FORMAT;

        return simpleDateFormat.format(statusDate).equals(simpleDateFormat.format(today));
    }
}
