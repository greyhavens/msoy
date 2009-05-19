//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;
import java.util.Calendar;

/**
 * Repository related utiltiy methods.
 */
public class RepositoryUtil
{
    /**
     * Returns a timetstamp that cuts off the specified number of days in the past at midnight
     * rather than at the current time minus that many milliseconds. Use this when using cutoffs
     * for timestamps in queries to ensure that they can be cached.
     */
    public static Timestamp getCutoff (int days)
    {
        Calendar cutoff = Calendar.getInstance();
        cutoff.setTimeInMillis(System.currentTimeMillis());
        cutoff.add(Calendar.DATE, -days);
        cutoff.set(Calendar.HOUR_OF_DAY, 0);
        cutoff.set(Calendar.MINUTE, 0);
        cutoff.set(Calendar.SECOND, 0);
        cutoff.set(Calendar.MILLISECOND, 0);
        return new Timestamp(cutoff.getTimeInMillis());
    }
}
