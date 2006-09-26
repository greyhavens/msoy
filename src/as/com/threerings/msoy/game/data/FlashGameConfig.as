//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.parlor.client.TableConfigurator;
import com.threerings.parlor.client.DefaultFlexTableConfigurator;

import com.threerings.parlor.game.client.GameConfigurator;
import com.threerings.parlor.game.client.FlexGameConfigurator;

import com.threerings.ezgame.data.EZGameConfig;

/**
 * A game config for a simple multiplayer flash game.
 */
public class FlashGameConfig extends EZGameConfig
{
    override public function createConfigurator () :GameConfigurator
    {
        return new FlexGameConfigurator();
    }
}
}
