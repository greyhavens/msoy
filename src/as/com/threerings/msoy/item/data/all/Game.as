//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.util.StringUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

public class Game extends Item
{
    /** Identifies our lobby table background media. */
    public static const TABLE_MEDIA :String = "table";

    /** We reserve gameId = 1 for the tutorial. */
    public static const TUTORIAL_GAME_ID :int = 1;

    /** XML game configuration. */
    public var config :String;

    /** The game media. */
    public var gameMedia :MediaDesc;

    /** A unique identifier assigned to this game and preserved across new versions of the game
     * item so that ratings and lobbies and content packs all reference the same "game". */
    public var gameId :int;

    override public function getType () :int
    {
        return GAME;
    }

    /**
     * Checks whether this game is an in-world, as opposed to lobbied, game.
     */
    public function isInWorld () :Boolean
    {
        // TODO: this will change
        return (0 <= config.indexOf("<toggle ident=\"avrg\" start=\"true\"/>")) ||
            (0 <= config.indexOf("<toggle ident=\"chiyogami\" start=\"true\"/>"));
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        config = (ins.readField(String) as String);
        gameMedia = (ins.readObject() as MediaDesc);
        gameId = ins.readInt();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(config);
        out.writeObject(gameMedia);
        out.writeInt(gameId);
    }
}
}
