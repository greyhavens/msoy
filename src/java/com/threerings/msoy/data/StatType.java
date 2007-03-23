//
// $Id$

package com.threerings.msoy.data;

import com.samskivert.util.StringUtil;

import com.threerings.stats.data.IntStat;
import com.threerings.stats.data.Stat;
import com.threerings.util.MessageBundle;

/**
 * Enumerates the various stats used in Whirled.
 */
public enum StatType implements Stat.Type
{
    // general statistics
    CUMULATIVE_FLOW(new IntStat());

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
    public boolean isPersistent () {
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
