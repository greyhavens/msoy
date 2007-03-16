//
// $Id$

package com.threerings.msoy.item.web {

import com.threerings.util.StringUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

public class Game extends Item
{
    /** Identifies our lobby table background media. */
    public static const TABLE_MEDIA :String = "table";

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
     * Checks whether this game is an in-world, as opposed to lobbied, game.
     */
    public function isInWorld () :Boolean
    {
        // TODO: parse configuration as XML
        return (StringUtil.trim(config) == "avrg") ||
            (0 == config.indexOf("Chiyogami"));
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

        out.writeField(config);
        out.writeObject(gameMedia);
        out.writeObject(tableMedia);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        config = (ins.readField(String) as String);
        gameMedia = (ins.readObject() as MediaDesc);
        tableMedia = (ins.readObject() as MediaDesc);
    }
}
}
