//
// $Id$

package com.threerings.msoy.web.data;

/**
 * This object holds the extra data associated with a single friend invitation message.
 */
public class FriendInviteObject extends MailPayload
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
        return MailPayload.TYPE_FRIEND_INVITE;
    }
}
