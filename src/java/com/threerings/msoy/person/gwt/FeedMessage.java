//
// $Id$

package com.threerings.msoy.person.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.web.gwt.Activity;

/**
 * Contains information on a feed message.
 */
public class FeedMessage
    implements IsSerializable, Activity
{
    /** The type of feed message. */
    public FeedMessageType type;

    /** The arguments to this feed message. */
    public String[] data;

    /** The time at which this message was posted. */
    public long posted;

    /**
     * Constructs a new feed message for serialization.
     */
    public FeedMessage ()
    {
    }

    /**
     * Constructs a new feed message with the given field values.
     */
    public FeedMessage (FeedMessageType type, String[] data, long posted)
    {
        this.type = type;
        this.data = data;
        this.posted = posted;
    }

    @Override
    public long startedAt ()
    {
        return posted;
    }

    public boolean isCommentReply ()
    {
        int idx = -1;
        switch (type) {
            case SELF_ROOM_COMMENT:
            case SELF_GAME_COMMENT:
                idx = 3;
                break;
            case SELF_ITEM_COMMENT:
                idx = 4;
                break;
            case SELF_PROFILE_COMMENT:
                idx = 2;
                break;
        }
        return (idx >= 0 && idx < data.length && Boolean.parseBoolean(data[idx]));
    }
}
