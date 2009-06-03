//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet;

/**
 * An entry published to the node object whenever there are pending tables
 * for the specified parlor game.
 */
public class TablesWaiting extends SimpleStreamableObject
    implements DSet.Entry
{
    /** The game id and our key. */
    public int gameId;

    /** The name of the game is love, baby. */
    public String name;

    /**
     * Create.
     */
    public TablesWaiting (int gameId, String name)
    {
        this.gameId = gameId;
        this.name = name;
    }

    /** Unserialize. */
    public TablesWaiting ()
    {
    }

    // from DSet.Entry
    public Comparable<?> getKey ()
    {
        return gameId;
    }
}
