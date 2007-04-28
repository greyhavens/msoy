package com.threerings.msoy.game.chiyogami.client {

import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.Shape;
import flash.display.Sprite;

import flash.text.TextField;

import flash.events.Event;
import flash.events.TimerEvent;

import flash.media.Sound;
import flash.media.SoundChannel;

import flash.utils.getTimer; // function import
import flash.utils.Timer;

import mx.containers.Canvas;

import mx.core.ScrollPolicy;

import com.threerings.flash.FPSDisplay;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.StaticMediaDesc;

import com.threerings.msoy.game.client.MiniGameContainer;
import com.threerings.msoy.game.chiyogami.data.ChiyogamiObject;

public class ChiyogamiPanel extends Canvas
    implements PlaceView
{
    public function ChiyogamiPanel (ctx :WorldContext, ctrl :ChiyogamiController)
    {
        _ctrl = ctrl;
        
        horizontalScrollPolicy = ScrollPolicy.OFF;

        // TODO: Splash screen
        rawChildren.addChild(new SPLASH() as DisplayObject);
    }

    // from PlaceView
    public function willEnterPlace (plobj :PlaceObject) :void
    {
        _gameObj = (plobj as ChiyogamiObject);

        recheckTagEntry();
    }

    // from PlaceView
    public function didLeavePlace (plobj :PlaceObject) :void
    {
        _gameObj = null;
    }

    /**
     * Start things a-moving. TODO
     */
    public function gameDidStart () :void
    {
        recheckTagEntry();

        // pick a game!
        var game :MediaDesc = MediaDesc(GAMES[
            int(Math.floor(Math.random() * GAMES.length))]);

        _minigame = new MiniGameContainer();
        _minigame.setup(game);

        _minigame.performanceCallback = _ctrl.miniGameReportedPerformance;

        rawChildren.addChild(_minigame);

        var mask :Shape = new Shape();
        with (mask.graphics) {
            beginFill(0xffFFff);
            drawRect(0, 0, 800, 100);
            endFill();
        }

        _minigame.mask = mask;
        rawChildren.addChild(mask);
    }

    public function gameDidEnd () :void
    {
        recheckTagEntry();
        rawChildren.removeChild(_minigame);

        _minigame.performanceCallback = null;
    }

    protected function recheckTagEntry () :void
    {
        if (_gameObj.isInPlay() == (_tagEntry == null)) {
            return;
        }

        if (_tagEntry == null) {
            _tagEntry = new TagEntryPanel();
            _tagEntry.x = 450;
            addChild(_tagEntry);

        } else {
            removeChild(_tagEntry);
            _tagEntry = null;
        }
    }

    /** Our controller. */
    protected var _ctrl :ChiyogamiController;

    protected var _minigame :MiniGameContainer;

    protected var _gameObj :ChiyogamiObject;

    protected var _tagEntry :TagEntryPanel;

    /** The hardcoded games we currently use. */
    protected static const GAMES :Array = [
        new StaticMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH,
            Item.GAME, "chiyogami/Match3") ,
        new StaticMediaDesc(MediaDesc.APPLICATION_SHOCKWAVE_FLASH,
            Item.GAME, "chiyogami/KeyJam")
    ];

    [Embed(source="splash.png")]
    protected static const SPLASH :Class;
}
}
