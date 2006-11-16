//
// $Id$

package com.threerings.msoy.web.data;

/**
 * This object holds the extra data associated with a single friend invitation message.
 */
public class FriendInviteObject extends MailBodyObject
{
    /**
     * An empty constructor for deserialization.
     */
    public FriendInviteObject ()
    {
    }

    // @Override
    public int getType ()
    {
        return MailBodyObject.TYPE_GROUP_INVITE;
    }
}
