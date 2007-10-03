//
// $Id: AVRGamePanel.as 5986 2007-10-02 13:46:12Z zell $

package com.threerings.msoy.game.client {

import flash.display.Loader;

import mx.containers.Canvas;

import com.threerings.flash.MediaContainer;
import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.ControlBackend;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.game.client.AVRGameControlBackend;
import com.threerings.msoy.game.client.AVRGameController;
import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.game.data.AVRGameObject;

public class AVRGamePanel extends Canvas
{
    public static const log :Log = Log.getLog(AVRGamePanel);

    public function AVRGamePanel (
        mctx :WorldContext, gctx :GameContext, ctrl :AVRGameController)
    {
        super();

        _mctx = mctx;
        _gctx = gctx;
        _ctrl = ctrl;
    }

    override protected function createChildren () :void
    {
        _mediaHolder = new MediaContainer();
        this.rawChildren.addChildAt(_mediaHolder, 0);

        // TODO: A nice wee X
        var quit :CommandButton = new CommandButton(MsoyController.LEAVE_AVR_GAME);
        quit.label = Msgs.GAME.get("b.leave_world_game");
        this.addChildAt(quit, 0);
        this.height = 100;
    }

    public function init (gameObj :AVRGameObject) :void
    {
        _gameObj = gameObj;
        _mediaHolder.setMedia(gameObj.gameMedia.getMediaPath());
        _backend = new AVRGameControlBackend(_mctx, _gctx, _gameObj, _ctrl);
        _backend.init(Loader(_mediaHolder.getMedia()));
    }

    protected var _mctx :WorldContext;
    protected var _gctx :GameContext;
    protected var _ctrl :AVRGameController;
    protected var _mediaHolder :MediaContainer;
    protected var _gameObj :AVRGameObject;
    protected var _backend :ControlBackend;
}
}
