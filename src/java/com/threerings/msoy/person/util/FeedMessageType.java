//
// $Id$

package com.threerings.msoy.person.util;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;

/**
 * Enumerates the available types of feed messages. Codes are also used for the grouping and order
 * of categories when displaying feed on the Me page.
 *
 * TODO: when GWT supports enums, move this into person.data.
 */
public enum FeedMessageType
{
    // global messages
    GLOBAL_ANNOUNCEMENT(1),

    // group messages
    GROUP_ANNOUNCEMENT(200, 2, oneDay()),

    // self messages
    SELF_ROOM_COMMENT(300),
    SELF_ITEM_COMMENT(301),

    // friend messages
    FRIEND_ADDED_FRIEND(100),
    FRIEND_LISTED_ITEM(103, 3, oneDay()),
    FRIEND_WON_TROPHY(102, 3, oneDay()),
    FRIEND_UPDATED_ROOM(101, 1, oneDay()),
    FRIEND_WON_BADGE(105, 3, oneDay()),
    FRIEND_GAINED_LEVEL(104, 1, oneDay()),

    UNUSED(999);

    /**
     * Looks up a {@link FeedMessageType} by its numerical representation and return it.
     */
    public static FeedMessageType getActionByCode (int code)
    {
        return _reverse.get(code);
    }

    /**
     * Some types will be grouped together to a single category for display. Return the code for
     * the category a given type belongs in.
     */
    public static int getCategoryCode (int code)
    {
        if (code == SELF_ITEM_COMMENT._code) {
            return SELF_ROOM_COMMENT._code;
        } else if (code == GROUP_ANNOUNCEMENT._code) {
            return GLOBAL_ANNOUNCEMENT._code;
        } else {
            return code;
        }
    }

    /**
     * Returns the numerical code of this {@link FeedMessageType}.
     */
    public int getCode ()
    {
        return _code;
    }

    /**
     * Returns the number of messages allowed in the period defined by {@link #getThrottlePeriod}
     * or zero if this message type is not throttled.
     */
    public int getThrottleCount ()
    {
        return _throttleCount;
    }

    /**
     * Returns the period over which we throttle messages or 0 if this message type is not
     * throttled.
     */
    public long getThrottlePeriod ()
    {
        return _throttlePeriod;
    }

    FeedMessageType (int code)
    {
        this(code, 0, 0L);
    }

    FeedMessageType (int code, int throttleCount, long throttlePeriod)
    {
        _code = code;
        _throttleCount = throttleCount;
        _throttlePeriod = throttlePeriod;
    }

    // we can't define a constant and reference it in our enum definitions so we have to do this
    // wacky static final method business
    protected static final long oneDay () {
        return 24 * 60 * 60L * 1000L;
    }

    protected int _code;
    protected int _throttleCount;
    protected long _throttlePeriod;

    protected static IntMap<FeedMessageType> _reverse;
    static {
        _reverse = new HashIntMap<FeedMessageType>();
        for (FeedMessageType type : FeedMessageType.values()) {
            _reverse.put(type.getCode(), type);
        }
    }
}
