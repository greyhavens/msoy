//
// $Id$

package com.threerings.msoy.data {

import com.threerings.msoy.data.all.MemberName;

/**
 * Extends the default actor occupant info and provides member related functionality.
 */
public class MemberInfo extends ActorInfo
{
    /** Suitable for unserialization. */
    public function MemberInfo ()
    {
    }

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
}
}
