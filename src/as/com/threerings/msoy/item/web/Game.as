//
// $Id$

package com.threerings.msoy.item.web {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

public class Game extends Item
{
    /** Identifies our lobby table background media. */
    public static const TABLE_MEDIA :String = "table";

    /** The minimum number of players. */
    public var minPlayers :int;

    /** The maximum number of players. */
    public var maxPlayers :int;

    /** The desired number of players. */
    public var desiredPlayers :int;

    /** XML game configuration. */
    public var config :String;

    /** The game media. */
    public var gameMedia :MediaDesc;

    /** The game's table background. */
    public var tableMedia :MediaDesc;

    override public function getType () :int
    {
        return GAME;
    }

    /**
     * Returns a media descriptor for the media to be used
     * as a table background image.
     */
    public function getTableMedia () :MediaDesc
    {
        return (tableMedia != null) ? tableMedia :
            new StaticMediaDesc(MediaDesc.IMAGE_PNG, GAME, TABLE_MEDIA);
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeShort(minPlayers);
        out.writeShort(maxPlayers);
        out.writeShort(desiredPlayers);
        out.writeField(config);
        out.writeObject(gameMedia);
        out.writeObject(tableMedia);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        minPlayers = ins.readShort();
        maxPlayers = ins.readShort();
        desiredPlayers = ins.readShort();
        config = (ins.readField(String) as String);
        gameMedia = (ins.readObject() as MediaDesc);
        tableMedia = (ins.readObject() as MediaDesc);
    }
}
}
