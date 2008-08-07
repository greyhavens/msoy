//
// $Id$

package com.threerings.msoy.badge.data;

import java.util.zip.CRC32;

import com.samskivert.util.HashIntMap;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.StatType;
import com.threerings.stats.Log;

/** Defines the various badge types. */
public enum BadgeType
{
    // social badges
    FRIEND_1(Category.SOCIAL, 1000, "Friends", 1) {
        protected int getAcquiredUnits (MemberObject user) {
            return user.stats.getIntStat(StatType.FRIENDS_MADE);
        }
    },

    FRIEND_2(Category.SOCIAL, 2000, "Friends", 5) {
        protected int getAcquiredUnits (MemberObject user) {
            return user.stats.getIntStat(StatType.FRIENDS_MADE);
        }
    },

    WHIRLEDS_1(Category.SOCIAL, 1000, "Whirleds", 1) {
        protected int getAcquiredUnits (MemberObject user) {
            return user.stats.getIntStat(StatType.WHIRLEDS_CREATED);
        }
    },

    INVITES_1(Category.SOCIAL, 1000, "Invites", 1) {
        protected int getAcquiredUnits (MemberObject user) {
            return user.stats.getIntStat(StatType.INVITES_ACCEPTED);
        }
    },

    PLAYTIME_1(Category.SOCIAL, 1000, "ActiveHours", 24) {
        protected int getAcquiredUnits (MemberObject user) {
            return (int)Math.floor(user.stats.getIntStat(StatType.MINUTES_ACTIVE) / 60);
        }
    },

    // game badges
    GAMER_1(Category.GAME, 1000, "Games", 5) {
        protected int getAcquiredUnits (MemberObject user) {
            return user.stats.getSetStatSize(StatType.UNIQUE_GAMES_PLAYED);
        }
    },

    MULTIPLAYER_1(Category.GAME, 2000, "MultiplayerWins", 1) {
        protected int getAcquiredUnits (MemberObject user) {
            return user.stats.getIntStat(StatType.MP_GAMES_WON);
        }
    },

    TROPHY_1(Category.GAME, 1000, "Trophies", 1) {
        protected int getAcquiredUnits (MemberObject user) {
            return user.stats.getIntStat(StatType.TROPHIES_EARNED);
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
     * Returns the progress that the specified user has made on this badge.
     */
    public BadgeProgress getProgress (MemberObject user)
    {
        return new BadgeProgress(_unitName, _requiredUnits, getAcquiredUnits(user));
    }

    /**
     * Indicates whether the specified user has completed the requirements
     * for this badge type.
     */
    public boolean hasEarned (MemberObject user)
    {
        return getProgress(user).isComplete();
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

    /**
     * Overridden by badge types to indicate how many units of the stat that this badge tracks
     * (games played, friends made, etc) the user has acquired.
     */
    protected int getAcquiredUnits (MemberObject user)
    {
        return 0;
    }

    BadgeType (Category category, int coinValue, String unitName, int requiredUnits)
    {
        _category = category;
        _coinValue = coinValue;
        _unitName = unitName;
        _requiredUnits = requiredUnits;
    }

    /**
     * Create the hash<->BadgeType mapping for each BadgeType.
     * This is done in a static block because it's an error for an enum
     * to access its static members in its constructor.
     */
    static
    {
        _crc = new CRC32();

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
    protected String _unitName;
    protected int _requiredUnits;

    /** The table mapping stat codes to enumerated types. */
    protected static HashIntMap<BadgeType> _codeToType;

    protected static CRC32 _crc;
};
