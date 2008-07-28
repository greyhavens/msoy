//
// $Id$

package com.threerings.msoy.mail.gwt;


/**
 * This object holds the extra data associated with a single friend invitation message.
 */
public class FriendInvitePayload extends MailPayload
{
    /**
     * An empty constructor for deserialization.
     */
    public FriendInvitePayload ()
    {
    }

    @Override
    public int getType ()
    {
        return MailPayload.TYPE_FRIEND_INVITE;
    }
}
