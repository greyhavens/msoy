//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.util.StringUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.msoy.game.data.GameDefinition;

public class Game extends Item
{
    /** Identifies our lobby table background media. */
    public static const TABLE_MEDIA :String = "table";

    /** XML game configuration. */
    public var config :String;

    /** The game media. */
    public var gameMedia :MediaDesc;

    override public function getType () :int
    {
        return GAME;
    }

    /**
     * Checks whether this game is an in-world, as opposed to lobbied, game.
     */
    public function isInWorld () :Boolean
    {
        return (StringUtil.trim(config) == "avrg") ||
            (0 == config.indexOf("Chiyogami"));
    }

    public function getGameDefinition() :GameDefinition 
    {
        return _gameDef;
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(config);
        out.writeObject(gameMedia);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        config = (ins.readField(String) as String);
        gameMedia = (ins.readObject() as MediaDesc);

        _gameDef = new GameDefinition(config);
    }

    /** transient Actionscript-only field for pre-parsed game configuration data */
    protected var _gameDef :GameDefinition;
}
}
