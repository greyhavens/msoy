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

    /** The type of party game, or NOT_PARTY_GAME. */
    public var partyGameType :int;

    /** The minimum number of players. */
    public var minPlayers :int;

    /** The maximum number of players. */
    public var maxPlayers :int;

    /** Is this game unwatchable? Applicable only for non-party games. */
    public var unwatchable :Boolean;

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
        return StringUtil.trim(config) == "avrg";
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

        out.writeByte(partyGameType);
        out.writeShort(minPlayers);
        out.writeShort(maxPlayers);
        out.writeBoolean(unwatchable);
        out.writeField(config);
        out.writeObject(gameMedia);
        out.writeObject(tableMedia);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        partyGameType = ins.readByte();
        minPlayers = ins.readShort();
        maxPlayers = ins.readShort();
        unwatchable = ins.readBoolean();
        config = (ins.readField(String) as String);
        gameMedia = (ins.readObject() as MediaDesc);
        tableMedia = (ins.readObject() as MediaDesc);
    }
}
}
