//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.Graphics;
import flash.display.Sprite;

import flash.events.Event;

import flash.utils.getTimer;

import com.threerings.flash.ColorUtil;

import com.threerings.msoy.game.data.PerfRecord;

public class PerfFeedbacker extends Sprite
{
    public static const WIDTH :int = 20;
    public static const HEIGHT :int = 100;

    public function PerfFeedbacker ()
    {
        _feedback = new Sprite();
        addChild(_feedback);

        reset();

        // create a border that will always draw over our own graphics
        var g :Graphics = this.graphics;
        g.lineStyle(2, 0x000000);
        g.beginFill(0x333399);
        g.drawRect(0, 0, WIDTH, HEIGHT);
        g.endFill();

        addEventListener(Event.ADDED_TO_STAGE, handleAdded);
        addEventListener(Event.REMOVED_FROM_STAGE, handleRemoved);
    }

    public function reset () :void
    {
        _perf = new PerfRecord();
        updatePerf();
    }

    /**
     * Record performance.
     */
    public function recordPerformance (score :Number, style :Number) :void
    {
        _perf.recordPerformance(getTimer(), score, style);
    }

    protected function handleAdded (... ignored) :void
    {
        addEventListener(Event.ENTER_FRAME, updatePerf);
    }

    protected function handleRemoved (... ignored) :void
    {
        removeEventListener(Event.ENTER_FRAME, updatePerf);
    }

    protected function updatePerf (... ignored) :void
    {
        var now :Number = getTimer();

        var score :Number = _perf.calculateScore(now);
        var style :Number = _perf.calculateStyle(now);

        var color :uint = ColorUtil.blend(0x00FF00, 0xFF0000, style);
        var h :Number = HEIGHT * score;

        var g :Graphics = _feedback.graphics;
        g.clear();
        g.beginFill(color);
        g.drawRect(0, HEIGHT - h, WIDTH, h);
        g.endFill();
    }

    protected var _perf :PerfRecord;

    protected var _feedback :Sprite;
}
}
