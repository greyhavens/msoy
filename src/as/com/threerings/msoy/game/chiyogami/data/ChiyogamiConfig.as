package com.threerings.msoy.game.chiyogami.data {

import com.threerings.crowd.client.PlaceController;

import com.threerings.parlor.game.client.GameConfigurator;
import com.threerings.parlor.game.data.GameConfig;

import com.threerings.msoy.game.chiyogami.client.ChiyogamiController;

public class ChiyogamiConfig extends GameConfig
{
    override public function getGameType () :int
    {
        return PARTY;
    }

    override public function getBundleName () :String
    {
        return "chiyogami";
    }

    override public function createConfigurator () :GameConfigurator
    {
        return null; // ?
    }

    override public function createController () :PlaceController
    {
        return new ChiyogamiController();
    }
}
}
