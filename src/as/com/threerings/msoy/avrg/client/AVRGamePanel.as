//
// $Id$

package com.threerings.msoy.avrg.client {

import flash.events.Event;

import flash.display.BitmapData;
import flash.display.LoaderInfo;

import flash.geom.Matrix;

import flash.utils.setInterval;
import flash.utils.clearInterval;
import flash.utils.getTimer;

import mx.core.UIComponent;
import mx.events.ResizeEvent;

import com.threerings.util.Log;
import com.threerings.util.ValueEvent;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.PlaceLayer;
import com.threerings.msoy.client.Snapshottable;
import com.threerings.msoy.ui.DataPackMediaContainer;

import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.avrg.data.AVRGameConfig;
import com.threerings.msoy.avrg.data.AVRGameObject;

public class AVRGamePanel extends UIComponent
    implements PlaceView, PlaceLayer, Snapshottable
{
    public const log :Log = Log.getLog(this);

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

        getMsoyCtx().getUIState().setInAVRGame(true);
    }

    // from PlaceView
    public function didLeavePlace (plobj :PlaceObject) :void
    {
        log.info("Leaving AVRG [plobj=" + plobj + "]");

        getMsoyCtx().getUIState().setInAVRGame(false);

        // null gameObj for mediaComplete to find if it should run after us
        _gameObj = null;
    }

    // called by our controller when it's created the backend and we should load our media
    public function backendIsReady () :void
    {
        var cfg :AVRGameConfig = (_ctrl.getPlaceConfig() as AVRGameConfig);

        // create the container for the user media
        _mediaHolder = new DataPackMediaContainer();
        // set ourselves up properly once the media is loaded
        _mediaHolder.addEventListener(Event.COMPLETE, mediaComplete);
        _mediaHolder.addEventListener(DataPackMediaContainer.LOADING_MEDIA, handleGameMediaLoading);
        _mediaHolder.setMedia(cfg.getGameDefinition().getMediaPath(cfg.getGameId()));

        // do loading feedback on the avrg button
        provideLoadingFeedback();

        addEventListener(ResizeEvent.RESIZE, handleResize);

        // Give the control bar button our thumbnail
        getMsoyCtx().getControlBar().setGameButtonIcon(cfg.thumbnail);
    }

    // from PlaceLayer
    public function setPlaceSize (unscaledWidth :Number, unscaledHeight :Number) :void
    {
        // we want to be the full size of the display
        setActualSize(unscaledWidth, unscaledHeight);
    }

    // from Snapshottable
    public function snapshot (
        bitmapData :BitmapData, matrix :Matrix, childPredicate :Function = null) :Boolean
    {
        if (_mediaHolder.parent != null) {
            var m :Matrix = _mediaHolder.transform.matrix;
            m.concat(matrix);
            return _mediaHolder.snapshot(bitmapData, m);
        }

        return true; // snap nothing, but report no error
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

    protected function handleGameMediaLoading (event :ValueEvent) :void
    {
        // hook the backend up with the media: no context needed here
        _ctrl.backend.init(null, LoaderInfo(event.value));
    }

    protected function mediaComplete (event :Event) :void
    {
        _mediaHolder.removeEventListener(Event.COMPLETE, mediaComplete);

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

    protected function getMsoyCtx() :MsoyContext
    {
        return _gctx.getMsoyContext();
    }

    protected function provideLoadingFeedback () :void
    {
        const gameBtn :UIComponent = getMsoyCtx().getControlBar().gameBtn;
        const PERIOD :Number = 1.5 * 1000;
        const DELAY :Number = 3.0 * 1000;

        var start :Number = getTimer() + DELAY;
        var intervalId :uint = 0;

        // animate the alpha based on time
        function updateAlpha () :void {
            var t :Number = getTimer() - start;
            if (t > 0) {
                var cos :Number = Math.cos(t * 2 * Math.PI / PERIOD);
                gameBtn.alpha = 0.7 + 0.3 * cos; // 0.4 .. 1.0
            }
        }

        function complete (evt :Event) :void {
            gameBtn.alpha = 1.0;
            clearInterval(intervalId);
            _mediaHolder.removeEventListener(Event.COMPLETE, complete);
        }

        intervalId = setInterval(updateAlpha, 1);
        _mediaHolder.addEventListener(Event.COMPLETE, complete);
        // TODO: fix runaway interval if loading never completes
    }

    protected var _gctx :GameContext;
    protected var _ctrl :AVRGameController;
    protected var _mediaHolder :DataPackMediaContainer;
    protected var _gameObj :AVRGameObject;
}
}
