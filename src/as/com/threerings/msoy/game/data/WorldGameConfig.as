//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.crowd.client.PlaceController;

import com.threerings.msoy.item.web.Game;

import com.threerings.msoy.game.client.WorldGameController;

/**
 * A game config for an in-world game.
 */
public class WorldGameConfig extends MsoyGameConfig
{
    /** The game item. */
    public var game :Game;

    override public function createController () :PlaceController
    {
        return new WorldGameController();
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        game = (ins.readObject() as Game);
    }
}
}
