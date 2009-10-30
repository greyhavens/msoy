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

    /** TEMP: hack to allow us to only run the agent for multiplayer games. */
    public var isAgentMPOnly :Boolean;

    /** Indicates that this AVRG should not auto-send player into a room. */
    public var roomless :Boolean;

    /** We need this here to be able to communicate with the whirled code that will launch the
     *  agent on the server. */
    public var serverMedia :String;

    /** The id of the bureau that this game's server code will run in, if any. */
    public var bureauId :String;

    /** The maximum client width to allow. */
    public var maxWidth :int;

    /** The maximum client height to allow. */
    public var maxHeight :int;

    public function MsoyGameDefinition ()
    {
    }

    // from GameDefinition
    override public function getMediaPath (gameId :int) :String
    {
        return digest;
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
        isAgentMPOnly = ins.readBoolean();
        roomless = ins.readBoolean();
        serverMedia = (ins.readField(String) as String);
        bureauId = (ins.readField(String) as String);
        maxWidth = ins.readInt();
        maxHeight = ins.readInt();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeBoolean(lwjgl);
        out.writeBoolean(isAgentMPOnly);
        out.writeBoolean(roomless);
        out.writeField(serverMedia);
        out.writeField(bureauId);
        out.writeInt(maxWidth);
        out.writeInt(maxHeight);
    }
}
}
