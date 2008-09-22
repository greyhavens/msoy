//
// $Id$

package com.threerings.msoy.data.all;

import java.lang.System;
import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

/**
 * Information about the unique visitor to the site. Initially it's stored in a cookie, until the
 * user registers, in which case it gets added to their account. The tracking number gets used to
 * assign the visitor to test groups, and to assemble a history of their activities up to
 * registration.
 *
 * <p>An ActionScript version of this class exists in VisitorInfo.as
 */
public class VisitorInfo
    implements Streamable, IsSerializable
{
    /** Creates a new, unique visitor info. */
    public VisitorInfo ()
    {
        // take current system time in milliseconds, shift left by eight bits,
        // and fill in the newly emptied bits with a random value. return as a hex string.
        // this gives us a resolution of 256 unique tracking numbers per millisecond.
        //
        // note: this corresponds to a similar function in VisitorInfo.as

        long timestamp = System.currentTimeMillis() * 256;
        int rand = (int) Math.rint(Math.random() * 256);

        this.tracker = Long.toHexString(timestamp + rand);
    }

    /** Creates a new instance with the given tracking id. */
    public VisitorInfo (String tracker)
    {
        this.tracker = tracker;
    }

    /** Player's tracking ID, used to assign them to test groups. */
    public String tracker;

    /**
     * Get the date and time that the tracking ID was assigned, derived from the tracking number.
     */
    public Date getCreationTime ()
    {
        try {
            long timestamp = Long.parseLong(tracker, 16) / 256;
            return new Date(timestamp);
        } catch (NumberFormatException e) {
            return new Date();
        }
    }

    @Override
    public String toString ()
    {
        return "VisitorInfo [" + tracker + "]";
    }
}
