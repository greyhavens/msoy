//
// $Id$

package com.threerings.msoy.server.persist;

import com.google.inject.Singleton;

/**
 * Provides "new and hot" configuration information to repositories that care.
 */
@Singleton
public class HotnessConfig
{
    public HotnessConfig ()
    {
        setDropoffDays(7);
    }

    /**
     * Returns the current configured dropoff seconds value.
     */
    public int getDropoffSeconds ()
    {
        return _dropoffSeconds;
    }

    /**
     * Configures the dropoff value using days.
     */
    public void setDropoffDays (int days)
    {
        _dropoffSeconds = Math.max(1, days * 24 * 60 * 60); // don't let us div by 0.
    }

    /** The number of seconds that causes an equivalent drop-off of 1 star in new & hot sorting. */
    protected volatile int _dropoffSeconds;
}
