package com.threerings.msoy.game.client {

import mx.containers.Canvas;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.world.client.MsoySprite;

import com.threerings.msoy.game.data.FlashGameConfig;

public class FlashGameView extends Canvas
    implements PlaceView
{
    public function FlashGameView (ctx :MsoyContext, ctrl :FlashGameController)
    {
        _ctx = ctx;
        _ctrl = ctrl;

        var cfg :FlashGameConfig = (ctrl.getPlaceConfig() as FlashGameConfig);
        _game = new MsoySprite(cfg.game);
        _game.x = 0;
        _game.y = 0;
        addChild(_game);
    }

    // from PlaceView
    public function willEnterPlace (plobj :PlaceObject) :void
    {
    }

    // from PlaceView
    public function didLeavePlace (plobj :PlaceObject) :void
    {
    }

    protected var _ctx :MsoyContext;
    protected var _ctrl :FlashGameController;

    protected var _game :MsoySprite;
}
}
