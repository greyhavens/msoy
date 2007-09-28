//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.crowd.client.PlaceController;

import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.game.client.AVRGameController;

/**
 * A game config for an in-world game.
 */
public class AVRGameConfig extends MsoyGameConfig
{
    /** The scene id in which the game was started. */
    public var startSceneId :int;

    public function AVRGameConfig ()
    {
        // used for unserialization
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        startSceneId = ins.readInt();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(startSceneId);
    }

    // from EZGameConfig
    override protected function createDefaultController () :PlaceController
    {
//        return new AVRGameController();
        // TODO: AVRGames are not Places
        return null;
    }
}
}
