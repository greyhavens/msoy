//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * Contains the runtime data for a Mob (mobile-object, monster) item.
 */
public class Mob extends SubItem
{
    /** The id of the game with which we're associated. */
    public var gameId :int;

    public function Mob ()
    {
    }

    // from Item
    override public function getType () :int
    {
        return MOB;
    }

    // from Item
    override public function getPreviewMedia () :MediaDesc
    {
        return getFurniMedia();
    }

    // from Item
    override protected function getDefaultFurniMedia () :MediaDesc
    {
        return null; // there is no default
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        gameId = ins.readInt();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(gameId);
    }
}
}
