//
// $Id$

package com.threerings.msoy.web.data;

/**
 * This object holds the extra data associated with a single group invitation message.
 */
public class GroupInviteObject extends MailPayload
{
    /** The group to which the member has been invited. */
    public int groupId;
    
    /** Whether or not the recipient has responded to the invite. */
    public boolean responded;

    /**
     * An empty constructor for deserialization.
     */
    public GroupInviteObject ()
    {
    }

    /**
     * Create a new {@link GroupInviteObject} with the supplied configuration.
     */
    public GroupInviteObject (int groupId, boolean responded)
    {
        this.groupId = groupId;
        this.responded = responded;
    }
    
    // @Override
    public int getType ()
    {
        return MailPayload.TYPE_GROUP_INVITE;
    }
}
