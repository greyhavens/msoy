//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.util.StringUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Extends Item with game info.
 */
public class Game extends Item
{
    /** Identifies our lobby table background media. */
    public static const TABLE_MEDIA :String = "table";

    /** We reserve a very unlikely gameId for the tutorial. */
    public static const TUTORIAL_GAME_ID :int = int.MAX_VALUE;

    /** This game's genre. */
    public var genre :int;

    /** XML game configuration. */
    public var config :String;

    /** The game media. */
    public var gameMedia :MediaDesc;

    /** A unique identifier assigned to this game and preserved across new versions of the game
     * item so that ratings and lobbies and content packs all reference the same "game". */
    public var gameId :int;

    /** The game screenshot media. */
    public var shotMedia :MediaDesc;

    /** The server code media. Games may provide server code (in the form of a compiled action 
     *  script library) to be run in a bureau whenever the game launches. */
    public var serverMedia :MediaDesc;

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
        return 0 <= config.indexOf("<avrg/>");
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        genre = ins.readByte();
        config = (ins.readField(String) as String);
        gameMedia = (ins.readObject() as MediaDesc);
        gameId = ins.readInt();
        shotMedia = (ins.readObject() as MediaDesc);
        serverMedia = (ins.readObject() as MediaDesc);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeByte(genre);
        out.writeField(config);
        out.writeObject(gameMedia);
        out.writeInt(gameId);
        out.writeObject(shotMedia);
        out.writeObject(serverMedia);
    }
}
}
