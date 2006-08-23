//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.parlor.game.client.GameConfigurator;

import com.threerings.ezgame.data.EZGameConfig;

import com.threerings.msoy.game.client.FlashGameConfigurator;

/**
 * A game config for a simple multiplayer flash game.
 */
public class FlashGameConfig extends EZGameConfig
{
    override public function createConfigurator () :GameConfigurator
    {
        return new FlashGameConfigurator();
    }
}
}
