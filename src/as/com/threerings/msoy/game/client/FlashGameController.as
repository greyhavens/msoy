package com.threerings.msoy.game.client {

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.ezgame.client.EZGameController;

/**
 * A controller for flash games.
 */
public class FlashGameController extends EZGameController
{
    public function FlashGameController ()
    {
    }

    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        return new FlashGamePanel(ctx, this);
    }
}
}
