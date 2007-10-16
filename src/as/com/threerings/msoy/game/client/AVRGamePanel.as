//
// $Id: AVRGamePanel.as 5986 2007-10-02 13:46:12Z zell $

package com.threerings.msoy.game.client {

import flash.events.Event;
import flash.display.Loader;
import flash.display.LoaderInfo;

import mx.containers.Canvas;
import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Text;

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

    public function init (gameObj :AVRGameObject) :void
    {
        _gameObj = gameObj;

        // create the backend
        _backend = new AVRGameControlBackend(_gctx, _gameObj, _ctrl);

        // create the container for the user media
        _mediaHolder = new MediaContainer(gameObj.gameMedia.getMediaPath());
        var loader :Loader = Loader(_mediaHolder.getMedia());

        // hook the backend up with the media
        _backend.init(loader);

        // set ourselves up properly once the media is loaded
        loader.contentLoaderInfo.addEventListener(Event.COMPLETE, mediaComplete);
    }

    public function tutorialEvent (eventName :String) :void
    {
        if (_backend) {
            _backend.tutorialEvent(eventName);
        }
    }

    protected function mediaComplete (event :Event) :void
    {
        var info :LoaderInfo = (event.target as LoaderInfo);
        info.removeEventListener(Event.COMPLETE, mediaComplete);

        _ctrl.gameIsReady();

        this.height = info.height + 2;

        var quit :CommandButton = new CommandButton(MsoyController.LEAVE_AVR_GAME);
        quit.label = Msgs.GAME.get("b.leave_world_game");
        quit.x = quit.y = 0;
        this.addChild(quit);

        this.rawChildren.addChildAt(_mediaHolder, 0);

        _mctx.getTopPanel().setBottomPanel(this);
    }

    protected var _mctx :WorldContext;
    protected var _gctx :GameContext;
    protected var _ctrl :AVRGameController;
    protected var _mediaHolder :MediaContainer;
    protected var _gameObj :AVRGameObject;
    protected var _backend :AVRGameControlBackend;
}
}
