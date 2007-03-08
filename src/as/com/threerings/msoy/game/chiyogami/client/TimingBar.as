package com.threerings.msoy.game.chiyogami.client {

import flash.display.DisplayObject;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.TimerEvent;

import flash.utils.getTimer; // function import
import flash.utils.Timer;

public class TimingBar extends Sprite
{
    public function TimingBar (width :int, height :int, msPerBeat :Number)
    {
        _width = width;
        _pixelsPerMs = _width / msPerBeat;

//        // draw the bar
//        with (graphics) {
//            // the background
//            beginFill(0);
//            drawRect(0, 0, width, height);
//            endFill();
//
//            // the border
//            lineStyle(2, 0xFFFFFF);
//            drawRect(0, 0, width, height);
//
//            // the red zones!
//            lineStyle(0, 0, 0);
//
//            var target :Number = TARGET_AREA;
//            for (var ww :int = 8; ww >= 1; ww--) {
//                beginFill((0xFF / ww) << 16); // redness
//                var extent :Number = ww * 3;
//                drawRect(target - extent/2, 0, extent, height);
//                endFill();
//            }
//        }

        addNewNeedle();
        addEventListener(Event.ENTER_FRAME, repositionNeedle);
    }

    protected function addNewNeedle () :void
    {
        // create the needle
        _needle = (new NEEDLE() as DisplayObject);
        addChild(_needle);
        
        // set up a starting needle position
        _needle.x = 0;
        _needle.y = 5;
        _origStamp = getTimer();
    }

    /**
     * Check the needle's closeness to the target area.
     * Calling this method has the side-effect of creating a visual
     * representation of where the needle stopped.
     * The closeness is returned as a value from 0 - 1.
     */
    public function checkNeedle () :Number
    {
        repositionNeedle(); // one last update

        if (_oldNeedle != null) {
            removeChild(_oldNeedle);
        }
        _oldNeedle = _needle;
        _oldNeedle.alpha = .55;
        _needle = null;
        addNewNeedle();

        var target :Number = TARGET_AREA;
        return 1 - (Math.abs(target - _oldNeedle.x) / target);
    }

    /**
     * Repositon the needle, given the current timestmap.
     * This should always be done before querying the needle accuracy.
     */
    protected function repositionNeedle (event :Object = null) :void
    {
        var curStamp :Number = getTimer();
        // always compare to the original for max accuracy
        var elapsed :Number = curStamp - _origStamp;

        _needle.x = (_pixelsPerMs * elapsed) % _width;
    }

    protected var _width :Number;

    protected var _pixelsPerMs :Number;

    protected var _needle :DisplayObject;

    protected var _oldNeedle :DisplayObject;

    protected var _origStamp :Number;

    protected static const TARGET_AREA :Number = 167;

    [Embed(source="keyjam.swf#needle")]
    protected static const NEEDLE :Class;
}
}
