//
// $Id$

package com.threerings.msoy.data;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.web.data.MemberName;

/**
 * Extends basic OccupantInfo with member-specific information.
 */
public class MemberInfo extends OccupantInfo
{
    /** The itemId of this user's avatar, or 0 if none. */
    public int avatarId;

    /** Suitable for unserialization. */
    public MemberInfo ()
    {
    }

    public MemberInfo (MemberObject user)
    {
        super(user);

        if (user.avatar != null) {
            avatarId = user.avatar.itemId;
        }
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
