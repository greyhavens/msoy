//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.item.web.MediaDesc;

public class MemberInfo extends OccupantInfo
{
    /** The itemId of this user's avatar, or 0. */
    public var avatarId :int;

    /**
     * Get the member id for this user, or 0 if they're a guest.
     */
    public function getMemberId () :int
    {
        return (username as MemberName).getMemberId();
    }

    /**
     * Return true if we represent a guest user.
     */
    public function isGuest () :Boolean
    {
        return (MemberName.GUEST_ID == getMemberId());
    }

    // documentation inherited
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        avatarId = ins.readInt();
    }
}
}
