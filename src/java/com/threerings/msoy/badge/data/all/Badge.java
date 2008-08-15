//
// $Id$

package com.threerings.msoy.badge.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.presents.dobj.DSet;

public abstract class Badge
    implements DSet.Entry, IsSerializable
{
    /** The unique code representing the type of this badge. */
    public int badgeCode;

    /** The level pertinent to this Badge object */
    public int level;

    /** The units required to attain this level.  This will typically be something like "7" or
     * "10k", and is used in a translation message on the client. May be null if not applicable. */
    public String levelUnits;

    public Badge ()
    {
        // for deserialization
    }

    /**
     * Creates a new badge, and automatically fills in the badge imageUrl from the BadgeType Enum.
     */
    public Badge (int badgeCode, int level, String levelUnits)
    {
        this.badgeCode = badgeCode;
        this.level = level;
        this.levelUnits = levelUnits;
    }

    /**
     * Returns the public access image URL for this badge.
     */
    abstract public String imageUrl ();

    @Override // from Object
    public boolean equals (Object o)
    {
        if (o instanceof Badge) {
            Badge other = (Badge)o;
            // Badge equality ignores the current level - Sets should only contain one Badge at
            // at given level at once.
            return other.badgeCode == this.badgeCode;
        }
        return false;
    }

    @Override // from Object
    public int hashCode ()
    {
        // Badges need to hash the same, regardless of level.
        return badgeCode;
    }

    // from interface DSet.Entry
    public Comparable<Integer> getKey ()
    {
        return hashCode();
    }

    protected static final String BADGE_IMAGE_DIR = "badge/";
    protected static final String BADGE_IMAGE_TYPE = ".png";
}
