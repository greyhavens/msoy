//
// $Id$

package com.threerings.msoy.server.persist;

/**
 * Provides "new and hot" configuration information to repositories that care.
 */
public class HotnessConfig
{
    /** The number of seconds that causes an equivalent drop-off of 1 star in new & hot sorting. */
    public static final int DROPOFF_SECONDS = 14 * 24 * 60 * 60;
}
