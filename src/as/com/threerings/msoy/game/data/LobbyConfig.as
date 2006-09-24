//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.crowd.data.PlaceConfig;

import com.threerings.msoy.item.web.Game;

/**
 * The configuration data for a lobby object.
 */
public class LobbyConfig extends PlaceConfig
{
    /** The game item that is being played in this lobby. */
    public var game :Game;

    override public function createController () :PlaceController
    {
        return new LobbyController();
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        game = (ins.readObject() as Game);
    }
}
}
