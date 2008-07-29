//
// $Id$

package com.threerings.msoy.badge.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.presents.dobj.DSet;

public abstract class Badge
    implements DSet.Entry, IsSerializable
{
    /** The unique code representing the type of this badge. */
    public int badgeCode;
    
    /** The public image URL for this badge. */
    public String imageUrl;

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
    public Badge (BadgeType type)
    {
        this.badgeCode = type.getCode();
        imageUrl = getType().getImageUrl();
    }

    /** Returns this Badge's Type */
    public BadgeType getType ()
    {
        return BadgeType.getType(badgeCode);
    }

    // from interface DSet.Entry
    public Comparable<Integer> getKey ()
    {
        return new Integer(badgeCode);
    }
}
