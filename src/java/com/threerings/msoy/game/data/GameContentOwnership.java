//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet;

import com.whirled.data.GameData;

/**
 * Contains information on an item owned by a player for a game.
 */
public class GameContentOwnership extends SimpleStreamableObject
    implements DSet.Entry, Comparable
{
    /** The game to which this content pertains. */
    public int gameId;

    /** The type of this content; see {@link GameData}. */
    public byte type;

    /** The identifier for this content. */
    public String ident;

    // from DSet.Entry
    public Comparable getKey ()
    {
        return this;
    }

    // from Comparable
    public int compareTo (Object other)
    {
        GameContentOwnership oo = (GameContentOwnership)other;
        int rv = (oo.gameId - gameId);
        if (rv != 0) {
            return rv;
        }
        rv = (oo.type - type);
        if (rv != 0) {
            return rv;
        }
        return oo.ident.compareTo(ident);
    }

    // @Override // from Object
    public boolean equals (Object other)
    {
        return (compareTo(other) == 0);
    }
}
