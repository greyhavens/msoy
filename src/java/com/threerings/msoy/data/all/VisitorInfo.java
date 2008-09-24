//
// $Id$

package com.threerings.msoy.data.all;

import java.lang.System;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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
    /** Creates a new, not authoritative visitor info. */
    public VisitorInfo ()
    {
        // take current system time in milliseconds, shift left by eight bits,
        // and fill in the newly emptied bits with a random value. return as a hex string.
        // this gives us a resolution of 256 unique tracking numbers per millisecond.
        //
        // note: this corresponds to a similar function in VisitorInfo.as

        long timestamp = System.currentTimeMillis() * 256;
        int rand = (int) Math.rint(Math.random() * 256);

        this.id = Long.toHexString(timestamp + rand);
        this.isAuthoritative = false;
    }

    /** Creates a new instance with the given tracking id. */
    public VisitorInfo (String id, boolean isAuthoritative)
    {
        this.id = id;
        this.isAuthoritative = isAuthoritative;
    }

    /** Player's tracking ID, used to assign them to test groups. */
    public String id;

    /** Did this visitor info come from the server during a login? */
    public boolean isAuthoritative;

    /**
     * Get the date and time that the tracking ID was assigned, derived from the tracking number.
     */
    public Date getCreationTime ()
    {
        try {
            long timestamp = Long.parseLong(id, 16) / 256;
            return new Date(timestamp);
        } catch (NumberFormatException e) {
            return new Date();
        }
    }

    /**
     * Flattens this instance into a string that can be passed between JavaScript apps.
     */
    public List<String> flatten ()
    {
        List<String> data = new ArrayList<String>();
        data.add(id);
        data.add(Boolean.toString(isAuthoritative));
        return data;
    }

    /**
     * Creates and initializes an instance from supplied {@link #flatten}ed string.
     */
    public static VisitorInfo unflatten (Iterator<String> data)
    {
        if (data == null) {
            return null;
        }

        String id = data.next();
        Boolean isAuthoritative = Boolean.parseBoolean(data.next());
        return new VisitorInfo(id, isAuthoritative);
    }

    @Override
    public String toString ()
    {
        String auth = isAuthoritative ? " (server)" : " (client)";
        return "VisitorInfo [" + id + auth + "]";
    }

    /**
     * Conversion helper, to be removed at some convenient time in the future.
     * Some of the old tracking IDs are recorded as simple timestamps,
     * others as 8-bit-shifted timestamps. This one normalizes both forms into the latter.
     */
    public static String normalizeVisitorId (String visitorId)
    {
        try {
            long threshold = 1000L * 60 * 60 * 24 * 365 * 256; // the first 8-bit-shifted year
            long timestamp = Long.parseLong(visitorId, 16);
            if (timestamp > threshold) {
                return visitorId;
            } else {
                return Long.toHexString(timestamp * 256);
            }
        } catch (NumberFormatException ex) {
            // the visitor ID was invalid! let's invent a new one
            return new VisitorInfo().id;
        }
    }

    /**
     * Conversion helper, to be removed at some convenient time in the future.
     * Converts a date into a reasonable visitor ID.
     */
    public static String timestampToVisitorId (Date date)
    {
        long extended = date.getTime() * 256;
        int rand = (int) Math.rint(Math.random() * 256);
        return Long.toHexString(extended + rand);
    }

}
