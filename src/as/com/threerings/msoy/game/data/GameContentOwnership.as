//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.util.Comparable;
import com.threerings.util.Equalable;

import com.threerings.presents.dobj.DSet_Entry;

import com.whirled.game.data.GameData;

/**
 * Contains information on an item owned by a player for a game.
 */
public class GameContentOwnership extends SimpleStreamableObject
    implements DSet_Entry, Comparable, Equalable
{
    /** The game to which this content pertains. */
    public var gameId :int;

    /** The type of this content; see {@link GameData}. */
    public var type :int;

    /** The identifier for this content. */
    public var ident :String;

    /**
     * Creates an ownership record for the specified game, type and ident.
     */
    public function GameContentOwnership (gameId :int = 0, type :int = 0, ident :String = null)
    {
        this.gameId = gameId;
        this.type = type;
        this.ident = ident;
    }

    // from Comparable
    public function compareTo (other :Object) :int
    {
        var oo :GameContentOwnership = (other as GameContentOwnership);
        var rv :int = (oo.gameId - gameId);
        if (rv != 0) {
            return rv;
        }
        rv = (oo.type - type);
        if (rv != 0) {
            return rv;
        }
        return oo.ident.localeCompare(ident);
    }

    // from Equalable
    public function equals (other :Object) :Boolean
    {
        return (compareTo(other) == 0);
    }

    // from DSet_Entry
    public function getKey () :Object
    {
        return this;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        gameId = ins.readInt();
        type = ins.readByte();
        ident = (ins.readField(String) as String);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(gameId);
        out.writeByte(type);
        out.writeField(ident);
    }
}
}
