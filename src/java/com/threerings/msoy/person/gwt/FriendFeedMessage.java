//
// $Id$

package com.threerings.msoy.person.gwt;

import com.threerings.msoy.data.all.MemberName;

/**
 * Contains data for a friend-originated feed message.
 */
public class FriendFeedMessage extends FeedMessage
{
    /** The name of the friend to whom this message pertains. */
    public MemberName friend;

    /**
     * Constructs a new friend feed message for serialization.
     */
    public FriendFeedMessage ()
    {
    }

    /**
     * Constructs a new friend feed message with the given field values.
     */
    public FriendFeedMessage (FeedMessageType type, MemberName friend, String[] data, long posted)
    {
        super(type, data, posted);
        this.friend = friend;
    }
}
