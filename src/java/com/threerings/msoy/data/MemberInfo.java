//
// $Id$

package com.threerings.msoy.data;

import com.threerings.msoy.web.data.MemberName;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;

/**
 * Extends {@link ActorInfo} with member-specific information.
 */
public class MemberInfo extends ActorInfo
{
    /** Suitable for unserialization. */
    public MemberInfo ()
    {
    }

    /**
     * Creates a member info for the supplied user.
     */
    public MemberInfo (MemberObject user)
    {
        super(user, (user.avatar != null) ? user.avatar.getIdent() :
              new ItemIdent(Item.OCCUPANT, user.getOid()));
    }

    /**
     * Get the member id for this user, or 0 if they're a guest.
     */
    public int getMemberId ()
    {
        return ((MemberName) username).getMemberId();
    }

    /**
     * Return true if we represent a guest user.
     */
    public boolean isGuest ()
    {
        return (MemberName.GUEST_ID == getMemberId());
    }
}
