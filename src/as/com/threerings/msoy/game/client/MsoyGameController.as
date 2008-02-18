//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.util.CrowdContext;

import com.whirled.game.client.WhirledGameController;

public class MsoyGameController extends WhirledGameController
{
    // from WhirledGameController
    override public function backToWhirled (showLobby :Boolean = false) :void
    {
        (_pctx as GameContext).backToWhirled(showLobby);
    }

    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        return new MsoyGamePanel((ctx as GameContext), this);
    }
}
}
