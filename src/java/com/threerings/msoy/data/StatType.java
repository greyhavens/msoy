//
// $Id$

package com.threerings.msoy.data;

import java.util.Map;

import com.samskivert.util.StringUtil;
import com.threerings.util.ActionScript;
import com.threerings.util.MessageBundle;

import com.threerings.stats.data.IntSetStat;
import com.threerings.stats.data.IntStat;
import com.threerings.stats.data.Stat;

import com.threerings.msoy.badge.data.BadgeType;

/**
 * Enumerates the various stats used in Whirled.
 */
@ActionScript(omit=true)
public enum StatType implements Stat.Type
{
    // social stats
    FRIENDS_MADE(new IntStat(), true),
    INVITES_ACCEPTED(new IntStat(), true),
    MINUTES_ACTIVE(new IntStat(), true),
    // TODO: This stat requires some thought.  Punted for now, hopefully short-term
//    CONSEC_DAILY_LOGINS(new MaxValueIntStat(), true),
    WHIRLEDS_VISITED(new IntSetStat(), true),

    // game stats
    TROPHIES_EARNED(new IntStat(), true),
    GAME_SESSIONS(new IntStat(), true),
    //UNIQUE_GAMES_PLAYED(new IntSetStat(), true),
    MP_GAMES_WON(new IntStat(), true),
    //MP_GAME_PARTNERS(new IntSetStat(), true),

    // creation stats
    COINS_EARNED_SELLING(new IntStat(), true),
    SOLID_4_STAR_RATINGS(new IntSetStat(), true),
    AVATARS_CREATED(new IntStat(), true),
    FURNITURE_CREATED(new IntStat(), true),
    BACKDROPS_CREATED(new IntStat(), true),

    // shopping stats
    ITEMS_RATED(new IntStat(), true),
    ITEM_COMMENTS(new IntStat(), true),
    COINS_SPENT(new IntStat(), true),

    UNUSED(new IntStat());

    /** Constants to indicate levels of creatorship */
    public static final int ITEM_UPLOADED = 1;
    public static final int ITEM_LISTED = 2;
    public static final int ITEM_SOLD = 3;

    /** Constant to indicate that lowest value something can be purchased for to qualify the event
     * for the ITEMS_PURCHASED stat. */
    public static final int ITEMS_PURCHASED_MIN_VALUE = 1; // in coins

    /**
     * A main method so that this class can be run on its own for Badge code discovery.
     */
    public static void main (String[] args)
    {
        // dump all of the known badge types and their code
        System.out.println("  Integer   - Stat\n--------------------");
        for (StatType stat : StatType.values()) {
            System.out.println(stat.code() + " - " + stat.toString());
        }
    }

    /** Returns the translation key used by this stat. */
    public String key ()
    {
        return MessageBundle.qualify(
            MsoyCodes.STATS_MSGS, "m.stat_" + StringUtil.toUSLowerCase(name()));
    }

    // from interface Stat.Type
    public Stat newStat ()
    {
        return (Stat)_prototype.clone();
    }

    // from interface Stat.Type
    public int code ()
    {
        return _code;
    }

    // from interface Stat.Type
    public boolean isPersistent ()
    {
        return _persist;
    }

    // most stats are persistent
    StatType (Stat prototype)
    {
        this(prototype, true);
    }

    StatType (Stat prototype, boolean persist)
    {
        _persist = persist;
        _prototype = prototype;

        // configure our prototype and map ourselves into the Stat system
        _code = Stat.initType(this, _prototype);
    }

    protected Stat _prototype;
    protected int _code;
    protected boolean _persist;
}
