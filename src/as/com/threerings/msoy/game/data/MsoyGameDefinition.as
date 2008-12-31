//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.whirled.game.data.GameDefinition;

/**
 * Customizes the standard {@link GameDefinition} for MSOY which mainly means looking for our game
 * jar files using a different naming scheme.
 */
public class MsoyGameDefinition extends GameDefinition
{
    /** If true, the game requires the LWJGL libraries. */
    public var lwjgl :Boolean;

    /** We need this here to be able to communicate with the whirled code that will launch the
     *  agent on the server. */
    public var serverMedia :String;

    /** The id of the bureau that this game's server code will run in, if any. */
    public var bureauId :String;

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

    /**
     * Configures the path to this game's server media.
     */
    public function setServerMediaPath (mediaPath :String) :void
    {
        serverMedia = mediaPath;
    }

    // from GameDefinition
    public override function getServerMediaPath (gameId :int) :String
    {
        // TODO: what are we supposed to do with gameId?
        return serverMedia;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        lwjgl = ins.readBoolean();
        serverMedia = (ins.readField(String) as String);
        bureauId = (ins.readField(String) as String);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeBoolean(lwjgl);
        out.writeField(serverMedia);
        out.writeField(bureauId);
    }
}
}
