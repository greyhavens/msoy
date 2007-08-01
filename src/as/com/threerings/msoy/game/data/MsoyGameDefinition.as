//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.ezgame.data.GameDefinition;

/**
 * Customizes the standard {@link GameDefinition} for MSOY which mainly means looking for our game
 * jar files using a different naming scheme.
 */
public class MsoyGameDefinition extends GameDefinition
{
    /** If true, the game requires the LWJGL libraries. */
    public var lwjgl :Boolean;

    public function MsoyGameDefinition ()
    {
    }

    // from GameDefinition
    override public function getMediaPath (gameId :int) :String
    {
        return digest;
    }

    /**
     * Configures the path to this game's media.
     */
    public function setMediaPath (path :String) :void
    {
        digest = path;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        lwjgl = ins.readBoolean();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeBoolean(lwjgl);
    }
}
}
