//
// $Id$

package com.threerings.msoy.badge.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.SimpleStreamableObject;

public abstract class Badge extends SimpleStreamableObject
    implements IsSerializable
{
    /** The unique code representing the type of this badge. */
    public int badgeCode;

    /**
     * A badge becomes suppressed when the user decides they aren't interested in pursuing it.
     * Suppressed badges cannot be acquired, and will not be shown on a user's Passport page.
     * Badges that depend on suppressed Badges to be unlocked will also, therefore, never be
     * shown or acquired.
     */
    public boolean isSuppressed;

    /** Returns the URL where the badge's image is stored */
    //public String getImageUrl (); TODO

    /** Returns this Badge's Type */
    public BadgeType getType ()
    {
        return BadgeType.getType(badgeCode);
    }
}
