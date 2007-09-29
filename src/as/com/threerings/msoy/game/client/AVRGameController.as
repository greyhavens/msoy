//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.util.Controller;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.game.data.AVRGameObject;

public class AVRGameController extends Controller
{
    public function AVRGameController (ctx :WorldContext, gameObj :AVRGameObject)
    {
        super();

        _mctx = (ctx as WorldContext);

        setControlledPanel(new AVRGamePanel(ctx, gameObj, this));
    }

    protected var _mctx :WorldContext;
}
}

import flash.display.Loader;

import com.threerings.flash.MediaContainer;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.client.ControlBackend;

import com.threerings.msoy.game.client.AVRGameControlBackend;
import com.threerings.msoy.game.client.AVRGameController;
import com.threerings.msoy.game.data.AVRGameObject;

class AVRGamePanel extends MediaContainer
{
    public function AVRGamePanel (
        ctx :WorldContext, gameObj :AVRGameObject, ctrl :AVRGameController)
    {
//        super(gameObj.gameDef.getMediaPath(1));
        super(null);

        _backend = new AVRGameControlBackend(ctx, gameObj, ctrl);
        _backend.init(Loader(_media));
    }

    protected var _backend :ControlBackend;
}
