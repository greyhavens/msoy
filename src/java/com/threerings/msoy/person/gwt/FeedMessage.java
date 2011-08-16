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
    public static final int COMMENT_POSTED = 0;
    public static final int COMMENT_REPLIED = 1;
    public static final int COMMENT_FOLLOWED_UP = 2;

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

    public int getCommentVerb ()
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
        if (idx >= 0 && idx < data.length) {
            String param = data[idx];
            if (param.equals("true")) {
                // Old messages used another format
                return COMMENT_REPLIED;
            }
            return Integer.parseInt(param);
        }
        return COMMENT_POSTED;
    }
}
