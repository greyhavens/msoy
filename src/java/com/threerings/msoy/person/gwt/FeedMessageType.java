//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.HashMap;

import com.google.common.collect.Maps;
import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Enumerates the available types of feed messages. Codes are also used for the grouping and order
 * of categories when displaying feed on the Me page.
 */
public enum FeedMessageType
    implements IsSerializable
{
    // global messages
    GLOBAL_ANNOUNCEMENT(1, Category.ANNOUNCEMENTS),

    // friend messages
    FRIEND_ADDED_FRIEND(100, Category.FRIENDINGS),
    FRIEND_UPDATED_ROOM(101, Category.ROOMS, 1, oneDay()),
    FRIEND_WON_TROPHY(102, Category.GAMES, 3, oneDay()),
    FRIEND_LISTED_ITEM(103, Category.LISTED_ITEMS, 3, oneDay()),
    FRIEND_GAINED_LEVEL(104, Category.LEVELS, 1, oneDay()),
    FRIEND_WON_BADGE(105, Category.BADGES, 3, oneDay()),
    FRIEND_WON_MEDAL(106, Category.MEDALS, 3, oneDay()),
    FRIEND_SUBSCRIBED(107, Category.LEVELS, 1, oneDay()),
    FRIEND_CREATED_GROUP(108, Category.GROUPS, 3, oneDay()),
    FRIEND_JOINED_GROUP(109, Category.GROUPS, 3, oneDay()),
    FRIEND_PLAYED_GAME(110, Category.GAMES, 3, oneDay()),
    FRIEND_LIKED_MUSIC(111, Category.MUSIC, 3, oneDay()),

    // group messages
    GROUP_ANNOUNCEMENT(200, Category.ANNOUNCEMENTS, 2, oneDay()),
    GROUP_UPDATED_ROOM(201, Category.ROOMS, 1, oneDay()),

    // self messages
    SELF_ROOM_COMMENT(300, Category.COMMENTS),
    SELF_ITEM_COMMENT(301, Category.COMMENTS),
    SELF_FORUM_REPLY(302, Category.FORUMS),
    SELF_GAME_COMMENT(303, Category.COMMENTS),
    SELF_PROFILE_COMMENT(304, Category.COMMENTS),
    SELF_POKE(305, Category.COMMENTS, 1, oneDay()),

    UNUSED(999, null);

    /** Types are grouped together into categories. NOTE: Don't change the ordinals; they are used
     * for translation on the client. */
    public static enum Category {
        ANNOUNCEMENTS, COMMENTS, FRIENDINGS, LISTED_ITEMS, FORUMS, GAMES, ROOMS, BADGES,
        MEDALS, LEVELS, GROUPS, MUSIC;
    }

    /**
     * Translate a stored code back into the FeedMessageType instance.
     */
    public static FeedMessageType fromCode (int code)
    {
        FeedMessageType type = _byCode.get(code);
        if (type == null) {
            throw new IllegalArgumentException("Invalid feed message type code: " + code);
        }
        return type;
    }

    /**
     * Some types will be grouped together to a single category for display. Return the category
     * a given type belongs in, or null if none.
     */
    public Category getCategory ()
    {
        return _category;
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

    FeedMessageType (int code, Category category)
    {
        this(code, category, 0, 0L);
    }

    FeedMessageType (int code, Category category, int throttleCount, long throttlePeriod)
    {
        _code = code;
        _category = category;
        _throttleCount = throttleCount;
        _throttlePeriod = throttlePeriod;
    }

    // we can't define a constant and reference it in our enum definitions so we have to do this
    // wacky static final method business
    protected static final long oneDay () {
        return 24 * 60 * 60L * 1000L;
    }

    protected int _code;
    protected Category _category;
    protected int _throttleCount;
    protected long _throttlePeriod;

    protected static HashMap<Integer, FeedMessageType> _byCode;
    static {
        _byCode = Maps.newHashMap();
        for (FeedMessageType type : FeedMessageType.values()) {
            _byCode.put(type.getCode(), type);
        }
    }
}
