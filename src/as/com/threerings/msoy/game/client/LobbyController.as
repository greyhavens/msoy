//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;

import com.threerings.crowd.util.CrowdContext;

import com.threerings.msoy.client.MsoyContext;

public class LobbyController extends PlaceController
{
    override protected function createPlaceView (ctx :CrowdContetx) :PlaceView
    {
        return new LobbyPanel(ctx as MsoyContext);
    }
}
}
