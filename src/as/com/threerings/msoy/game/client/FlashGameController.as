//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.ezgame.client.EZGameController;

import com.threerings.msoy.client.MsoyContext;

public class FlashGameController extends EZGameController
{
    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        return new FlashGamePanel((ctx as MsoyContext), this);
    }
}
}
