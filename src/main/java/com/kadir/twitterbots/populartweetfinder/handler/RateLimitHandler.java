package com.kadir.twitterbots.populartweetfinder.handler;

import com.kadir.twitterbots.populartweetfinder.entity.ApiProcessType;
import org.apache.log4j.Logger;
import twitter4j.RateLimitStatus;

import java.util.HashMap;

/**
 * @author akadir
 * Date: 09/12/2018
 * Time: 15:47
 */
public class RateLimitHandler {
    private static final Logger logger = Logger.getLogger(RateLimitHandler.class);

    private static HashMap<String, RateLimitStatus> rateLimit = new HashMap<>();

    public static void handle(long id, RateLimitStatus rts, ApiProcessType processType) throws InterruptedException {
        //as default use 180 calls per 15 minute period (+75 seconds)
        double sleep = 15.0 * 65.0 / 180.0;
        if (rts != null) {
            RateLimitStatus oldRTS = rateLimit.get(id + "-" + processType.getName());
            double remaining = rts.getRemaining();
            double resetTime = rts.getSecondsUntilReset();

            if (remaining == 0) {
                sleep = rts.getSecondsUntilReset() + 1.0;
            } else if (oldRTS != null && oldRTS.getRemaining() > rts.getRemaining()) {
                double oldRemaining = oldRTS.getRemaining();
                double oldResetTime = oldRTS.getSecondsUntilReset();

                double dif = oldResetTime - resetTime;
                if (dif < (oldResetTime / oldRemaining)) {
                    sleep = (oldResetTime / oldRemaining) - dif + 0.3;
                }
            }

            rateLimit.put(id + "-" + processType.getName(), rts);
        }

        if (sleep > 0) {
            logger.debug("Sleep " + String.format("%.2f", sleep) + " seconds for the next " + processType.getName() + " process. Thread name: " + Thread.currentThread().getName());
            Thread.currentThread().sleep((long) (sleep * 1000));
        }
    }
}
