//
// $Id$

package com.threerings.msoy.item.data.all {

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

    /** Value of groupId when there is no associated group */
    public static const NO_GROUP :int = 0;

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

    /** The game splash screen media for the loader. */
    public var splashMedia :MediaDesc;

    /** The server code media. Games may provide server code (in the form of a compiled action
     *  script library) to be run in a bureau whenever the game launches. */
    public var serverMedia :MediaDesc;

    /** Optional group associated with this game; values < 0 mean no group */
    public var groupId :int;

    /** The tag used to identify items in this game's shop. */
    public var shopTag :String;

    /**
     * Returns true if the specified game is a developer's in-progress original game rather than
     * one listed in the catalog.
     */
    public static function isDevelopmentVersion (gameId :int) :Boolean
    {
        return (gameId < 0);
    }

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
        gameMedia = MediaDesc(ins.readObject());
        gameId = ins.readInt();
        shotMedia = MediaDesc(ins.readObject());
        splashMedia = MediaDesc(ins.readObject());
        serverMedia = MediaDesc(ins.readObject());
        groupId = (ins.readInt());
        shopTag = (ins.readField(String) as String);
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
        out.writeObject(splashMedia);
        out.writeObject(serverMedia);
        out.writeInt(groupId);
        out.writeField(shopTag);
    }
}
}
