//
// $Id$

package com.threerings.msoy.badge.data;

import java.util.Map;
import java.util.zip.CRC32;

import com.samskivert.util.HashIntMap;

import com.threerings.stats.Log;
import com.threerings.stats.data.StatSet;

import com.threerings.msoy.badge.gwt.StampCategory;

import com.threerings.msoy.data.StatType;

/** Defines the various badge types. */
public enum BadgeType
{
    // social badges
    FRIENDLY(StampCategory.SOCIAL, StatType.FRIENDS_MADE, new Level[] {
        new Level(1, 1000),
        new Level(5, 2000),
        new Level(15, 3000),
        new Level(50, 4000),
        new Level(150, 5000),
        new Level(500, 6000)
        }) {
        // TODO
    },

    IRONMAN(StampCategory.SOCIAL, StatType.CONSEC_DAILY_LOGINS, new Level[] {
        new Level(2, 1000),
        new Level(4, 2000),
        new Level(10, 3000),
        new Level(25, 4000),
        new Level(90, 5000),
        new Level(180, 6000)
        }) {
        // TODO
    },

    MAGNET(StampCategory.SOCIAL, StatType.INVITES_ACCEPTED, new Level[] {
        new Level(1, 1000),
        new Level(5, 2000),
        new Level(10, 3000),
        new Level(20, 4000),
        new Level(50, 5000),
        new Level(100, 6000)
        }) {
        // TODO
    },

    FIXTURE(StampCategory.SOCIAL, StatType.MINUTES_ACTIVE, new Level[] {
        new Level(3 * 60, 1000),
        new Level(24 * 60, 2000),
        new Level(48 * 60, 3000),
        new Level(96 * 60, 4000),
        new Level(200 * 60, 5000),
        new Level(500 * 60, 6000)
        }) {
        // TODO
    },

    // TODO: Builder - under discussion

    // game badges
    GAMER(StampCategory.GAME, StatType.GAME_SESSIONS, new Level[] {
        new Level(1, 1000),
        new Level(5, 2000),
        new Level(25, 3000),
        new Level(100, 4000),
        new Level(500, 5000),
        new Level(2000, 6000)
        }) {
        // TODO
    },

    CONTENDER(StampCategory.GAME, StatType.MP_GAMES_WON, new Level[] {
        new Level(1, 1000),
        new Level(5, 2000),
        new Level(10, 3000),
        new Level(25, 4000),
        new Level(50, 5000),
        new Level(100, 6000)
        }) {
        // TODO
    },

    COLLECTOR(StampCategory.GAME, StatType.TROPHIES_EARNED, new Level[] {
        new Level(1, 1000),
        new Level(5, 2000),
        new Level(10, 3000),
        new Level(25, 4000),
        new Level(50, 5000),
        new Level(100, 6000)
        }) {
        // TODO
    },

    // TODO: SOCIAL_GAMER - under discussion

    // creation badges
    // TODO: this whole category is under discussion

    // shopping badges
    OUTSPOKEN(StampCategory.SHOPPING, StatType.WHIRLED_COMMENTS, new Level[] {
        new Level(1, 1000),
        new Level(5, 2000),
        new Level(25, 3000),
        new Level(100, 4000),
        new Level(500, 5000),
        new Level(2000, 6000)
        }) {
        // TODO
    },

    // TODO: Judge - needs an item ratings stat
    // TODO: Shopper - needs an items purchased stat

    ;

    public static class Level
    {
        public int requiredUnits;
        public int coinValue;

        Level (int requiredUnits, int coinValue)
        {
            this.requiredUnits = requiredUnits;
            this.coinValue = coinValue;
        }
    }

    /**
     * A main method so that this class can be run on its own for Badge code discovery.
     */
    public static void main (String[] args)
    {
        // dump all of the known badge types and their code
        System.out.println("  Code   -   Badge\n--------------------");
        for (Map.Entry<Integer, BadgeType> entry : _codeToType.entrySet()) {
            System.out.println(Integer.toHexString(entry.getKey()) + " - " + entry.getValue());
        }
    }

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
    public boolean isUnlocked (EarnedBadgeSet badges)
    {
        return true;
    }

    /**
     * @return the number of levels this badge has.
     */
    public int getNumLevels ()
    {
        return _levels.length;
    }

    /**
     * @return the level data for the specified badge level, or null if the level is out of range.
     */
    public Level getLevel (int level)
    {
        return (level >= 0 && level < _levels.length ? _levels[level] : null);
    }

    /**
     * Returns the progress that the specified user has made on this badge.
     */
    public BadgeProgress getProgress (StatSet stats)
    {
        int highestLevel = -1;
        int requiredUnits = 0;
        int acquiredUnits = getAcquiredUnits(stats);
        if (_levels != null) {
            for (Level level : _levels) {
                if (acquiredUnits >= level.requiredUnits) {
                    highestLevel++;
                } else {
                    requiredUnits = level.requiredUnits;
                    break;
                }
            }
        }

        return new BadgeProgress(highestLevel, requiredUnits, acquiredUnits);
    }

    /**
     * @return the unique code for this badge type, which is a function of its name.
     */
    public final int getCode()
    {
        return _code;
    }

    /**
     * @return the relevant StatType associated with this badge, or null if the badge doesn't have
     * one. The badge system uses this information to update badges when their associated stats
     * are updated.
     */
    public StatType getRelevantStat ()
    {
        return _relevantStat;
    }

    /**
     * @return the Category this badge falls under.
     */
    public StampCategory getCategory ()
    {
        return _category;
    }

    /**
     * Overridden by badge types to indicate how many units of the stat that this badge tracks
     * (games played, friends made, etc) the user has acquired.
     */
    protected int getAcquiredUnits (StatSet stats)
    {
        return 0;
    }

    /** Constructs a new BadgeType. */
    BadgeType (StampCategory category, StatType relevantStat, Level[] levels)
    {
        _category = category;
        _relevantStat = relevantStat;
        _levels = levels;

        // ensure the badge has at least one level
        if (_levels == null || _levels.length == 0) {
            _levels = new Level[] { new Level(0, 0) };
        }
    }

    /** Constructs a new BadgeType with no relevant stat and a single level. */
    BadgeType (StampCategory category, int coinValue)
    {
        this(category, null, new Level[] { new Level(0, coinValue) });
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

    protected int _code;
    protected StampCategory _category;
    protected StatType _relevantStat;
    protected Level[] _levels;

    /** The table mapping stat codes to enumerated types. */
    protected static HashIntMap<BadgeType> _codeToType;

    protected static CRC32 _crc;
};
