//
// $Id$

package com.threerings.msoy.data.all;

import java.lang.System;
import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

/**
 * Stores referral info for the current player: affiliate who sent them to us,
 * entry vector type, creative id (eg. one for each banner graphic), and a unique
 * tracking number used to assign them to test groups.
 */
public class ReferralInfo
    implements Streamable, IsSerializable
{
    /** Creates a random tracking number. */
    public static String makeRandomTracker ()
    {
        return Long.toHexString(System.currentTimeMillis());
    }

    /**
     * Instance creator. Returns a new instance, or null if one of the parameters is null.
     */
    public static ReferralInfo makeInstance (
        String affiliate, String vector, String creative, String tracker)
    {
        if (affiliate == null || vector == null || creative == null || tracker == null) {
            return null;
        }

        ReferralInfo ref = new ReferralInfo();
        ref.affiliate = affiliate;
        ref.vector = vector;
        ref.creative = creative;
        ref.tracker = tracker;
        return ref;
    }

    /** Identifies the affiliate who referred this player to us. */
    public String affiliate;

    /** Identifies the entry vector type. */
    public String vector;

    /** Identifies the creative piece / banner / etc. used in this referral. */
    public String creative;

    /** Player's tracking ID, used to assign them to test groups. */
    public String tracker;

    /** Constructor. */
    public ReferralInfo () { }

    /** Constructor. */
    public ReferralInfo (String affiliate, String vector, String creative, String tracker)
    {
        this.affiliate = affiliate;
        this.vector = vector;
        this.creative = creative;
        this.tracker = tracker;
    }

    /**
     * Get the date and time that the tracking ID was assigned, derived from the tracker.
     */
    public Date getCreationTime()
    {
        try {
            long timestamp = Long.parseLong(tracker, 16);
            return new Date(timestamp);
        } catch (NumberFormatException e) {
            return new Date();
        }
    }

    @Override
    public String toString()
    {
        return "Referral [ref=" + affiliate + " / " + vector + " / " +
            creative + " / " + tracker  + "]";
    }
}
