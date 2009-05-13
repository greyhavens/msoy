//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.util.Cloneable;
import com.threerings.util.Equalable;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains metadata about a game for which a player is currently matchmaking.
 */
public class GameSummary extends SimpleStreamableObject
    implements Cloneable
{
    /** The game item id */
    public var gameId :int;

    /** The name of the game - used as a tooltip */
    public var name :String;

    /** Whether or not this is an AVRGame. */
    public var avrGame :Boolean;

    /** The thumbnail media for the game we're summarizing. */
    public var thumbMedia :MediaDesc;

    public function GameSummary ()
    {
        // only used for unserialization
    }

    // documentation from Cloneable
    public function clone () :Object
    {
        var data :GameSummary = new GameSummary();
        data.gameId = this.gameId;
        data.name = this.name;
        data.avrGame = this.avrGame;
        data.thumbMedia = this.thumbMedia;
        return data;
    }

    // documentation from Equalable
    public function equals (other :Object) :Boolean
    {
        if (other is GameSummary) {
            var data :GameSummary = other as GameSummary;
            return data.gameId == this.gameId;
        }
        return false;
    }

    // documentation from Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        gameId = ins.readInt();
        name = (ins.readField(String) as String);
        avrGame = ins.readBoolean();
        thumbMedia = MediaDesc(ins.readObject());
    }

    // documntation from Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(gameId);
        out.writeField(name);
        out.writeBoolean(avrGame);
        out.writeObject(thumbMedia);
    }
}
}
