//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

/**
 * Represents a game that has pending tables.
 * Note: The server-side class is a DSet.Entry, but this is not presently implemented out here.
 */
public class TablesWaiting
    implements Streamable
{
    /** The gameId. */
    public var gameId :int;

    /** The name of the game is love, baby. */
    public var name :String;

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        gameId = ins.readInt();
        name = ins.readField(String) as String;
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        throw new Error();
    }
}
}
