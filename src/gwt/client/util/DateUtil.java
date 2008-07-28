//
// $Id$

package client.util;

import java.util.Date;

/**
 * A bunch of Date related utility methods that encapsulate the fact that JavaScript relies on
 * deprecated methods from the Java version of Date.
 */
public class DateUtil
{
    @SuppressWarnings("deprecation")
    public static Date toDate (int[] datevec)
    {
        return new Date(datevec[0] - 1900, datevec[1], datevec[2]);
    }

    @SuppressWarnings("deprecation")
    public static Date newDate (String dateStr)
    {
        return new Date(dateStr);
    }

    @SuppressWarnings("deprecation")
    public static int getDayOfMonth (Date date)
    {
        return date.getDate();
    }

    @SuppressWarnings("deprecation")
    public static int getMonth (Date date)
    {
        return date.getMonth();
    }

    @SuppressWarnings("deprecation")
    public static int getYear (Date date)
    {
        return date.getYear();
    }

    @SuppressWarnings("deprecation")
    public static void zeroTime (Date date)
    {
        date.setHours(0);
        date.setMinutes(0);
        date.setSeconds(0);
    }
}
