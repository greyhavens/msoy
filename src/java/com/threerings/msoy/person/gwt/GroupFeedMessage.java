//
// $Id$

package com.threerings.msoy.person.gwt;

import com.threerings.msoy.data.all.GroupName;

/**
 * Contains data for a group-originated feed message.
 */
public class GroupFeedMessage extends FeedMessage
{
    /** The name of the group to which this message pertains. */
    public GroupName group;

    /**
     * Constructs a new group feed message for serialization.
     */
    public GroupFeedMessage ()
    {
    }

    /**
     * Constructs a new group feed message with the given field values.
     */
    public GroupFeedMessage (FeedMessageType type, GroupName group, String[] data, long posted)
    {
        super(type, data, posted);
        this.group = group;
    }
}
