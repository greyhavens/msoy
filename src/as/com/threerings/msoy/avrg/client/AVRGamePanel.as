//
// $Id$

package com.threerings.msoy.avrg.client {

import flash.events.Event;
import flash.events.ProgressEvent;

import flash.display.Loader;
import flash.display.LoaderInfo;

import flash.utils.setInterval;
import flash.utils.clearInterval;
import flash.utils.getTimer;

import mx.core.UIComponent;
import mx.events.ResizeEvent;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.flash.MediaContainer;
import com.threerings.util.Log;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.PlaceLayer;

import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.avrg.data.AVRGameConfig;
import com.threerings.msoy.avrg.data.AVRGameObject;

public class AVRGamePanel extends UIComponent
    implements PlaceView, PlaceLayer
{
    public static const log :Log = Log.getLog(AVRGamePanel);

    public function AVRGamePanel (ctx :GameContext, ctrl :AVRGameController)
    {
        super();

        _ctrl = ctrl;
        _gctx = ctx;
    }

    // from PlaceView
    public function willEnterPlace (plobj :PlaceObject) :void
    {
        _gameObj = (plobj as AVRGameObject);

        log.info("Entering AVRG [plobj=" + plobj + "]");

        getControlBar().setInAVRGame(true);
    }

    // from PlaceView
    public function didLeavePlace (plobj :PlaceObject) :void
    {
        log.info("Leaving AVRG [plobj=" + plobj + "]");

        getControlBar().setInAVRGame(false);

        // null gameObj for mediaComplete to find if it should run after us
        _gameObj = null;
    }

    // called by our controller when it's created the backend and we should load our media
    public function backendIsReady () :void
    {
        var cfg :AVRGameConfig = (_ctrl.getPlaceConfig() as AVRGameConfig);

        // create the container for the user media
        _mediaHolder = new MediaContainer(
            cfg.getGameDefinition().getMediaPath(cfg.getGameId()));
        var loader :Loader = Loader(_mediaHolder.getMedia());

        // hook the backend up with the media: no context needed here
        _ctrl.backend.init(null, loader);

        // set ourselves up properly once the media is loaded
        loader.contentLoaderInfo.addEventListener(Event.COMPLETE, mediaComplete);

        // do loading feedback on the avrg button
        provideLoadingFeedback(loader.contentLoaderInfo);

        addEventListener(ResizeEvent.RESIZE, handleResize);
    }

    // from PlaceLayer
    public function setPlaceSize (unscaledWidth :Number, unscaledHeight :Number) :void
    {
        // we want to be the full size of the display
        setActualSize(unscaledWidth, unscaledHeight);
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
        var hit :Boolean = (_ctrl.backend != null) && _ctrl.backend.hitTestPoint(x, y, shapeFlag);
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
        if (stage != null && _ctrl.backend != null) {
            _ctrl.backend.panelResized();
        }
    }

    protected function getControlBar () :ControlBar
    {
        const mctx :MsoyContext = _gctx.getMsoyContext();
        return mctx.getTopPanel().getControlBar();
    }

    protected function provideLoadingFeedback (info :LoaderInfo) :void
    {
        const avrgBtn :UIComponent = getControlBar().avrgBtn;
        const PERIOD :Number = 1.5 * 1000;
        const DELAY :Number = 3.0 * 1000;

        var start :Number = getTimer() + DELAY;
        var intervalId :uint = 0;

        // animate the alpha based on time
        function updateAlpha () :void {
            var t :Number = getTimer() - start;
            if (t > 0) {
                var cos :Number = Math.cos(t * 2 * Math.PI / PERIOD);
                avrgBtn.alpha = 0.7 + 0.3 * cos; // 0.4 .. 1.0
            }
        }

        function progress (evt :ProgressEvent) :void {
            // TODO: update text field in menu
        }

        function complete (evt :Event) :void {
            avrgBtn.alpha = 1.0;
            clearInterval(intervalId);
            info.removeEventListener(ProgressEvent.PROGRESS, progress);
            info.removeEventListener(Event.COMPLETE, complete);
        }

        intervalId = setInterval(updateAlpha, 1);
        info.addEventListener(ProgressEvent.PROGRESS, progress);
        info.addEventListener(Event.COMPLETE, complete);
    }

    protected var _gctx :GameContext;
    protected var _ctrl :AVRGameController;
    protected var _mediaHolder :MediaContainer;
    protected var _gameObj :AVRGameObject;
}
}
