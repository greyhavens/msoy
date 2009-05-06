//
// $Id$

package com.threerings.msoy.data.all;

/**
 * Indicates basic friendship information regarding two members.
 */
public enum Friendship
{
    /** We are not friends with the person in question. */
    NOT_FRIENDS,

    /** We have sent an invitation to become friends with this guy. */
    INVITED,

    /** We have been invited by this person to become friends. */
    INVITEE,

    /** We are friends. */
    FRIENDS;
}
