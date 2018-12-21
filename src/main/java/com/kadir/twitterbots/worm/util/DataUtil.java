package com.kadir.twitterbots.worm.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author akadir
 * Date: 08/12/2018
 * Time: 14:24
 */
public class DataUtil {

    private DataUtil() {
    }

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static String getYesterday() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);

        return simpleDateFormat.format(cal.getTime());
    }

}
