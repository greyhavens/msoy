//
// $Id$

package com.threerings.msoy.badge.data;

import com.threerings.stats.data.Stat;
import com.threerings.stats.data.IntStat;

public enum StatType
    implements Stat.Type
{
    // social stats
    FRIENDS_MADE(new IntStat(), true),
    INVITES_ACCEPTED(new IntStat(), true),
    WHIRLED_COMMENTS(new IntStat(), true),
    MINUTES_ACTIVE(new IntStat(), true),
    CONSEC_DAILY_LOGINS(new IntStat(), true),
    //WHIRLEDS_VISITED(new IntSetStat(), true),

    // game stats
    TROPHIES_EARNED(new IntStat(), true),
    GAMES_PLAYED(new IntStat(), true),
    MP_GAMES_HOSTED(new IntStat(), true),
    MP_GAMES_WON(new IntStat(), true),
    // MP_GAME_PARTNERS(new IntSetStat(), true),

    // creation stats
    ITEMS_LISTED(new IntStat(), true),
    ITEMS_SOLD(new IntStat(), true),
    ITEMS_PURCHASED(new IntStat(), true),

    ;

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
    public boolean isPersistent () {
        return _persist;
    }

    StatType (Stat prototype, boolean persist)
    {
        _prototype = prototype;
        _persist = persist;

        // configure our prototype and map ourselves into the Stat system
        _code = Stat.initType(this, _prototype);
    }

    protected Stat _prototype;
    protected boolean _persist;
    protected int _code;
}
