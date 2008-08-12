//
// $Id$

package com.threerings.msoy.badge.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.presents.dobj.DSet;

public abstract class Badge
    implements DSet.Entry, IsSerializable, Cloneable
{
    /** The unique code representing the type of this badge. */
    public int badgeCode;

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
    abstract public String imageUrl ();

    // from interface DSet.Entry
    public Comparable<Integer> getKey ()
    {
        return new Integer(badgeCode);
    }

    @Override // from Object
    public Object clone ()
    {
        try {
            return super.clone();
        } catch (Exception e) {
            throw new RuntimeException("Clone failed", e);
        }
    }

    protected static final String BADGE_IMAGE_DIR = "badge/";
    protected static final String BADGE_IMAGE_TYPE = ".png";
}
