//
// $Id$

package com.threerings.msoy.data;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.data.PlayerObject;

/**
 * Defines the interface provided by both {@link MemberObject} and {@link PlayerObject}.
 */
public interface MsoyUserObject
{
    /**
     * Get this object's oid. (Provided when you extend DObject.)
     */
    int getOid ();

    /**
     * Get this user's place oid. (Provided if you extend BodyObject.)
     */
    int getPlaceOid ();

    /**
     * Returns this member's name.
     */
    MemberName getMemberName ();

    /**
     * Returns this member's unique id.
     */
    int getMemberId ();

//    /**
//     * Get the party to which this user currently belongs.
//     */
//    int getPartyId ();
//
//    /**
//     * Set the party occupied by this user.
//     */
//    void setPartyId (int partyId);
}
