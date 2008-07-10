//
// $Id$

package com.threerings.msoy.badge.data;

import java.util.zip.CRC32;

import com.samskivert.util.HashIntMap;
import com.threerings.msoy.data.MemberObject;
import com.threerings.stats.Log;

/** Defines the various badge types. */
public enum BadgeType
{
    // social badges
    FRIEND_1(Category.SOCIAL, 1000) {
        public boolean getProgress (MemberObject user) {
            return user.stats.getIntStat(StatType.FRIENDS_MADE) >= 1;
        }
    },

    FRIEND_2(Category.SOCIAL, 1000) {
        public boolean getProgress (MemberObject user) {
            return user.stats.getIntStat(StatType.FRIENDS_MADE) >= 5;
        }
    },

    ;

    /**
     * Defines the various badge categories, which can be used to suggest to the user
     * which badges to pursue next, based on their past activity.
     */
    public static enum Category
    {
        SOCIAL, GAME, CREATION, EXPLORATION
    };

    /**
     * Maps a {@link BadgeType}'s code back to a {@link BadgeType} instance.
     */
    public static BadgeType getType (int code)
    {
        return _codeToType.get(code);
    }

    /**
     * Badge types can override this to apply constraints to Badges (e.g., only unlocked when
     * another badge is earned.)
     */
    public boolean isUnlocked (MemberObject user)
    {
        return true;
    }

    /**
     * Overridden by badge types to indicate whether the specified user qualifies
     * for this badge.
     * TODO: eventually, this function will be changed to return a String indicating
     * how much progress has been made. For now, it returns true if the Badge has been earned.
     */
    public boolean getProgress (MemberObject user)
    {
        return false;
    }

    /**
     * Returns the unique code for this badge type, which is a function of its name.
     */
    public final int getCode()
    {
        return _code;
    }

    /**
     * Returns the Category this badge falls under.
     */
    public Category getCategory ()
    {
        return _category;
    }

    /**
     * Returns the number of coins awarded to a player who completes this badge.
     */
    public int getCoinValue ()
    {
        return _coinValue;
    }

    BadgeType (Category category, int coinValue)
    {
        _category = category;
        _coinValue = coinValue;
    }

    /**
     * Create the hash<->BadgeType mapping for each BadgeType.
     * This is done in a static block because it's an error for an enum
     * to access its static members in its constructor.
     */
    static
    {
        for (BadgeType type : BadgeType.values()) {
            type._code = mapCodeForType(type);
        }
    }

    protected static int mapCodeForType (BadgeType type)
    {
        // compute the CRC32 hash
        _crc.reset();
        _crc.update(type.name().getBytes());
        int code = (int) _crc.getValue();

        // store the hash in a map
        if (_codeToType == null) {
            _codeToType = new HashIntMap<BadgeType>();
        }
        if (_codeToType.containsKey(code)) {
            Log.log.warning("Badge type collision! " + type + " and " + _codeToType.get(code) +
                " both map to '" + code + "'.");
        } else {
            _codeToType.put(code, type);
        }

        return code;
    }

    protected Category _category;
    protected int _coinValue;
    protected int _code;

    /** The table mapping stat codes to enumerated types. */
    protected static HashIntMap<BadgeType> _codeToType;

    protected static CRC32 _crc = new CRC32();
};
