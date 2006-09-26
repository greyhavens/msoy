//
// $Id$

package com.threerings.msoy.item.web {

import flash.utils.ByteArray;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

public class Game extends Item
{
    /** The name of the game. */
    public var name :String;

    /** The minimum number of players. */
    public var minPlayers :int;

    /** The maximum number of players. */
    public var maxPlayers :int;

    /** The desired number of players. */
    public var desiredPlayers :int;

    /** A hash code identifying the game media. */
    public var gameMediaHash :ByteArray;

    /** The MIME type of the {@link #gameMediaHash} media. */
    public var gameMimeType :int;

    /**
     * Returns a media descriptor for the actual game media.
     */
    public function getGameMedia () :MediaDesc
    {
        return new MediaDesc(gameMediaHash, gameMimeType);
    }

    override public function getType () :String
    {
        return "GAME";
    }

    override public function getDescription () :String
    {
        return name;
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(name);
        out.writeShort(minPlayers);
        out.writeShort(maxPlayers);
        out.writeShort(desiredPlayers);
        out.writeField(gameMediaHash);
        out.writeByte(gameMimeType);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        name = (ins.readField(String) as String);
        minPlayers = ins.readShort();
        maxPlayers = ins.readShort();
        desiredPlayers = ins.readShort();
        gameMediaHash = (ins.readField(ByteArray) as ByteArray);
        gameMimeType = ins.readByte();
    }
}
}
