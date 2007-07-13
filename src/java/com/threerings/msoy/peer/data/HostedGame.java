//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet;

/**
 * Represents a game lobby being hosted by this peer.
 */
public class HostedGame extends SimpleStreamableObject
    implements DSet.Entry
{
    /** The unique identifier for the game being hosted. */
    public Integer gameId;

    /** The name of the game. */
    public String name;

    // from DSet.Entry
    public Comparable getKey ()
    {
        return gameId;
    }
}
