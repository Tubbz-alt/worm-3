package com.kadir.twitterbots.populartweetfinder.filter;

import com.kadir.twitterbots.populartweetfinder.util.StatusUtil;
import twitter4j.Logger;
import twitter4j.Status;

/**
 * @author akadir
 * Date: 08/12/2018
 * Time: 17:14
 */
public class InteractionCountFilter implements StatusFilter {
    private final Logger logger = Logger.getLogger(this.getClass());

    private static int minInteractionCount = 1000;

    public InteractionCountFilter() {
        logger.info(this.getClass().getSimpleName() + " created");
    }

    @Override
    public boolean passed(Status status) {
        return StatusUtil.calculateInteractionCount(status) > minInteractionCount;
    }

    public static void setMinInteractionCount(int interactionCount) {
        minInteractionCount = interactionCount;
    }

    public static int getMinInteractionCount() {
        return minInteractionCount;
    }
}
