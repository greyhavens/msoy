//
// $Id$

package com.threerings.msoy.person.gwt;


/**
 * This object holds the extra data associated with a single group invitation message.
 */
public class GroupInvitePayload extends MailPayload
{
    /** The group to which the member has been invited. */
    public int groupId;

    /** Whether or not the recipient has responded to the invite. */
    public boolean responded;

    /**
     * An empty constructor for deserialization.
     */
    public GroupInvitePayload ()
    {
    }

    /**
     * Create a new {@link GroupInvitePayload} with the supplied configuration.
     */
    public GroupInvitePayload (int groupId, boolean responded)
    {
        this.groupId = groupId;
        this.responded = responded;
    }

    @Override
    public int getType ()
    {
        return MailPayload.TYPE_GROUP_INVITE;
    }
}
