//
// $Id$

package com.threerings.msoy.game.data;

import com.whirled.game.data.TableMatchConfig;

/**
 * Class to encapsulate extended properties to table matches for Whirled.
 */
public class MsoyMatchConfig extends TableMatchConfig
{
    /** The creator configured matchmaking type. */
    public int type;

    /** Whether this game is watchable or not. defaults to watchable */
    public boolean unwatchable = false;

    /** Whether this game should automatically start in single-player mode, if possible. */
    public boolean autoSingle = false;

    @Override // from MatchConfig
    public int getMatchType ()
    {
        return type;
    }
}
