//
// $Id: AVRGamePanel.as 5986 2007-10-02 13:46:12Z zell $

package com.threerings.msoy.game.client {

import flash.events.Event;
import flash.events.MouseEvent;
import flash.display.Loader;
import flash.display.LoaderInfo;

import mx.core.UIComponent;
import mx.events.ResizeEvent;

import com.threerings.util.Log;

import com.threerings.flash.MediaContainer;

import com.threerings.msoy.client.ControlBackend;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.game.client.AVRGameBackend;
import com.threerings.msoy.game.client.AVRGameController;
import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.game.data.AVRGameObject;

import com.threerings.msoy.world.client.RoomView;

public class AVRGamePanel extends UIComponent
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
        _backend = new AVRGameBackend(_gctx, _ctrl, _gameObj);

        // create the container for the user media
        _mediaHolder = new MediaContainer(gameObj.gameMedia.getMediaPath());
        var loader :Loader = Loader(_mediaHolder.getMedia());

        // hook the backend up with the media
        _backend.init(loader);

        // set ourselves up properly once the media is loaded
        loader.contentLoaderInfo.addEventListener(Event.COMPLETE, mediaComplete);

        // TODO: We should probably listen for _gameObj.gameMedia updates and
        // TODO: perhaps just brutally reload ourselves when that happens?
    }

    public function getAVRGameBackend () :AVRGameBackend
    {
        return _backend;
    }

    public function tutorialEvent (eventName :String) :void
    {
        if (_backend) {
            _backend.tutorialEvent(eventName);
        }
    }

    public function shutdown () :void
    {
        _mctx.getMsoyController().setAVRGamePanel(null);
        // null gameObj for mediaComplete to find if it should run after us
        _gameObj = null;
    }

    // We want to give the AVRG control over what pixels it considers 'hits' and which
    // it doesn't -- thus we forward the request to the backend, where it is in turn sent
    // on to user code. This itself, however, is not enough to handle mouse clicks well;
    // it seems once a click has been found to target a non-transparent pixel, the event
    // dispatched will travel up to the root of the display hierarchy and then vanish;
    // nothing will trigger on e.g. room entities, which are in a different subtree from
    // the PlaceBox. Thus the second trick, which is, whenever we are hovering over a
    // pixel which the AVRG says is not a hit pixel, we turn off mouse events for the
    // *entire* AVRG sub-tree. This forces the click event to take place in the PlaceView
    // domain, and everything works correctly. Huzzah.
    override public function hitTestPoint (
        x :Number, y :Number, shapeFlag :Boolean = false) :Boolean
    {
        var hit :Boolean = _backend && _backend.hitTestPoint(x, y, shapeFlag);
        this.mouseEnabled = this.mouseChildren = hit;
        return hit;
    }

    protected function mediaComplete (event :Event) :void
    {
        var info :LoaderInfo = (event.target as LoaderInfo);
        info.removeEventListener(Event.COMPLETE, mediaComplete);

        if (_gameObj == null) {
            // we've already been shut down
            return;
        }

        this.addEventListener(ResizeEvent.RESIZE, handleResize);

        _ctrl.gameIsReady();

        this.addChild(_mediaHolder);

        _mctx.getMsoyController().setAVRGamePanel(this);
    }

    protected function handleResize (evt :ResizeEvent) :void
    {
        // an AVRG panel should be designed for 700 x 500
        var scale :Number = Math.min(1, this.height / stage.stageHeight);
        // so we'll brutally scale it accordingly
        this.scaleX = this.scaleY = scale;

        _backend.panelResized();
    }

    protected var _mctx :WorldContext;
    protected var _gctx :GameContext;
    protected var _ctrl :AVRGameController;
    protected var _mediaHolder :MediaContainer;
    protected var _gameObj :AVRGameObject;
    protected var _backend :AVRGameBackend;
}
}
