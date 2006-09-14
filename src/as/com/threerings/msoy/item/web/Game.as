//
// $Id$

package com.threerings.msoy.item.web {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

public class Game extends MediaItem
{
    /** The name of the game. */
    public var name :String;

    /** The minimum number of players. */
    public var minPlayers :int;

    /** The maximum number of players. */
    public var maxPlayers :int;

    /** The desired number of players. */
    public var desiredPlayers :int;

    override public function getType () :String
    {
        return "GAME";
    }

    override public function getDescription () :String
    {
        return name;
    }

    // @Override from Item
    override public function getThumbnailPath () :String
    {
        // The games aren't really standalone, so we can't show
        // the game as the thumbnail...
        return "/media/static/game.png"; // TODO?
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(name);
        out.writeShort(minPlayers);
        out.writeShort(maxPlayers);
        out.writeShort(desiredPlayers);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        name = (ins.readField(String) as String);
        minPlayers = ins.readShort();
        maxPlayers = ins.readShort();
        desiredPlayers = ins.readShort();
    }
}
}
