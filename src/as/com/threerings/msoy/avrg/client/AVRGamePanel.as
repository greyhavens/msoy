//
// $Id: AVRGamePanel.as 5986 2007-10-02 13:46:12Z zell $

package com.threerings.msoy.avrg.client {

import flash.events.Event;
import flash.events.MouseEvent;
import flash.display.Loader;
import flash.display.LoaderInfo;

import mx.core.UIComponent;
import mx.events.ResizeEvent;

import com.threerings.flash.MediaContainer;
import com.threerings.util.Log;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.msoy.client.ControlBackend;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.TopPanel;

import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.avrg.data.AVRGameObject;

public class AVRGamePanel extends UIComponent
    implements AttributeChangeListener
{
    public static const log :Log = Log.getLog(AVRGamePanel);

    public function AVRGamePanel (wctx :WorldContext, gctx :GameContext, ctrl :AVRGameController)
    {
        super();

        _wctx = wctx;
        _gctx = gctx;
        _ctrl = ctrl;
    }

    public function init (gameObj :AVRGameObject) :void
    {
        _gameObj = gameObj;

        // create the backend
        _backend = new AVRGameBackend(_wctx, _gctx, _ctrl, _gameObj);

        loadMedia();

        _gameObj.addListener(this);

        _wctx.getWorldController().setAVRGamePanel(this);

        addEventListener(ResizeEvent.RESIZE, handleResize);
    }

    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        if (event.getName() == AVRGameObject.GAME_MEDIA) {
            // if the media changes, brutally reload the AVRG
            loadMedia();
        }
    }

    protected function loadMedia () :void {
        // if this is a reload, toss the old media
        if (_mediaHolder != null && _mediaHolder.parent != null) {
            removeChild(_mediaHolder);
        }

        // create the container for the user media
        _mediaHolder = new MediaContainer(_gameObj.gameMedia.getMediaPath());
        var loader :Loader = Loader(_mediaHolder.getMedia());

        // hook the backend up with the media
        _backend.init(loader);

        // set ourselves up properly once the media is loaded
        loader.contentLoaderInfo.addEventListener(Event.COMPLETE, mediaComplete);
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
        _wctx.getWorldController().setAVRGamePanel(null);
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
            log.warning("AVRG load aborted due to shutdown.");
            return;
        }

        _ctrl.gameIsReady();
        addChild(_mediaHolder);
    }

    protected function handleResize (evt :ResizeEvent) :void
    {
        if (stage != null) {
            // scale the AVRG panel to match the stage height
            var scale :Number = Math.min(1, this.height / stage.stageHeight);
            this.scaleX = this.scaleY = scale;

            _backend.panelResized();
        }
    }

    protected var _wctx :WorldContext;
    protected var _gctx :GameContext;
    protected var _ctrl :AVRGameController;
    protected var _mediaHolder :MediaContainer;
    protected var _gameObj :AVRGameObject;
    protected var _backend :AVRGameBackend;
}
}
