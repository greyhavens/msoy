package com.threerings.msoy.game.client {

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.parlor.game.client.GameController;

import com.threerings.msoy.client.MsoyContext;

/**
 * A controller for flash games.
 */
public class FlashGameController extends GameController
{
    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        return new FlashGameView(ctx as MsoyContext, this);
    }
}
}
