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

    /** True if this summary is for the development version of the game. */
    public var isDevelopment :Boolean;

    /** The name of the game - used as a tooltip */
    public var name :String;

    /** The description of the game - used for sharing */
    public var description :String;

    /** Whether or not this is an AVRGame. */
    public var avrGame :Boolean;

    /** The thumbnail media for the game we're summarizing. */
    public var thumbMedia :MediaDesc;

    public function GameSummary ()
    {
        // only used for unserialization
    }

    /**
     * Returns -gameId if we're the development version of the game and +gameId if we're the
     * published version. This typed id is used in various places where we have to encode whether
     * or not we're a development version of the game in the identifier.
     */
    public function getTypedId () :int
    {
        return isDevelopment ? -gameId : gameId;
    }

    // documentation from Cloneable
    public function clone () :Object
    {
        var data :GameSummary = new GameSummary();
        data.gameId = this.gameId;
        data.isDevelopment = this.isDevelopment;
        data.name = this.name;
        data.description = this.description;
        data.avrGame = this.avrGame;
        data.thumbMedia = this.thumbMedia;
        return data;
    }

    // documentation from Equalable
    public function equals (other :Object) :Boolean
    {
        if (other is GameSummary) {
            var data :GameSummary = other as GameSummary;
            return data.gameId == this.gameId && data.isDevelopment == this.isDevelopment;
        }
        return false;
    }

    // documentation from Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        gameId = ins.readInt();
        isDevelopment = ins.readBoolean();
        name = (ins.readField(String) as String);
        description = (ins.readField(String) as String);
        avrGame = ins.readBoolean();
        thumbMedia = MediaDesc(ins.readObject());
    }

    // documntation from Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(gameId);
        out.writeBoolean(isDevelopment);
        out.writeField(name);
        out.writeField(description);
        out.writeBoolean(avrGame);
        out.writeObject(thumbMedia);
    }
}
}
