//
// $Id$

package com.threerings.msoy.badge.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.data.all.DeploymentConfig;

public abstract class Badge
    implements DSet.Entry, IsSerializable
{
    /** The unique code representing the type of this badge. */
    public int badgeCode;

    /**
     * NB - the design for this has changed; we aren't planning on allowing suppressed Badges
     *
     * A badge becomes suppressed when the user decides they aren't interested in pursuing it.
     * Suppressed badges cannot be acquired, and will not be shown on a user's Passport page.
     * Badges that depend on suppressed Badges to be unlocked will also, therefore, never be
     * shown or acquired.
     */
    //public boolean isSuppressed;

    public Badge ()
    {
        // for deserialization
    }

    /**
     * Creates a new badge, and automatically fills in the badge imageUrl from the BadgeType Enum.
     */
    public Badge (int badgeCode)
    {
        this.badgeCode = badgeCode;
    }

    /**
     * Returns the public access image URL for this badge.
     */
    public String imageUrl ()
    {
        return DeploymentConfig.staticMediaURL + BADGE_IMAGE_DIR + Integer.toHexString(badgeCode) +
            BADGE_IMAGE_TYPE;
    }

    // from interface DSet.Entry
    public Comparable<Integer> getKey ()
    {
        return new Integer(badgeCode);
    }

    protected static final String BADGE_IMAGE_DIR = "badge/";
    protected static final String BADGE_IMAGE_TYPE = ".png";
}
