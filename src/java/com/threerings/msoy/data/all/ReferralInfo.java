//
// $Id$

package com.threerings.msoy.data.all;

import java.lang.Math;
import java.lang.System;

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
        // take current system time in milliseconds, shift left by eight bits,
        // and fill in the newly emptied bits with a random value. return as a hex string.
        // this gives us a resolution of 256 unique tracking numbers per millisecond.
        long now = System.currentTimeMillis() * 256;
        Double rand = Math.floor(Math.random() * 256);
        return Long.toHexString(now + rand.longValue());
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

    @Override
    public String toString()
    {
        return "Referral [ref=" + affiliate + " / " + vector + " / " +
            creative + " / " + tracker  + "]";
    }
}
