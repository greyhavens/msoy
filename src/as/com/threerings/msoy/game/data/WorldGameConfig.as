//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.crowd.client.PlaceController;

import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.game.client.AVRGameController;

/**
 * A game config for an in-world game.
 */
public class WorldGameConfig extends MsoyGameConfig
{
    /** The scene id in which the game was started. */
    public var startSceneId :int;

    override public function createController () :PlaceController
    {
        if (controller == null) {
            // this is the EZ-game equivalent for WorldGames...
            return new AVRGameController();

        } else {
            return super.createController();
        }
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        startSceneId = ins.readInt();
    }
}
}
