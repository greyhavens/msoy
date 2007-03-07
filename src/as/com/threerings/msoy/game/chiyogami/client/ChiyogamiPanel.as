package com.threerings.msoy.game.chiyogami.client {

import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.Sprite;

import flash.text.TextField;

import flash.events.Event;
import flash.events.TimerEvent;

import flash.media.Sound;
import flash.media.SoundChannel;

import flash.utils.getTimer; // function import
import flash.utils.Timer;

import mx.containers.Canvas;

import com.threerings.flash.FPSDisplay;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.item.web.MediaDesc;

public class ChiyogamiPanel extends Canvas
    implements PlaceView
{
    public function ChiyogamiPanel (ctx :WorldContext, ctrl :ChiyogamiController)
    {
        trace("ChiyogamiPanel starting up.");
        //addChild(new Background(500, 500));

        _boss = new DancerSprite();

//        var fps :FPSDisplay = new FPSDisplay();
//        fps.y = 0;
//        addChild(fps);
    }

    // from PlaceView
    public function willEnterPlace (plobj :PlaceObject) :void
    {
    }

    // from PlaceView
    public function didLeavePlace (plobj :PlaceObject) :void
    {
    }

    /**
     * Set up a new boss.
     */
    public function setBoss (desc :MediaDesc) :void
    {
        _boss.setDancer(desc);
    }

    /**
     * Start things a-moving. TODO
     */
    public function startup () :void
    {
        //_sound = Sound(new MUSIC());

        _boss.x = (500 - _boss.getContentWidth()) / 2;
        _boss.y = (500 - _boss.getContentHeight()) / 2;
        addChild(_boss);

        var minigame :KeyJam = new KeyJam(_millisPerBeat);
        minigame.y = 500;
        addChild(minigame);

        _bossUpdateTimer = new Timer(4000, 1);
        _bossUpdateTimer.addEventListener(TimerEvent.TIMER, kickOffBattle);
        _bossUpdateTimer.start();
    }

    protected function kickOffBattle (evt :Object = null) :void
    {
        _boss.setWalking(true);

        _channel = _sound.play(0, int.MAX_VALUE);

        // make sure we're kosh
        if (_channel == null) {
            var tf :TextField = new TextField();
            tf.text = "No sound for you. So Sorry!";
            tf.width = tf.textWidth + 5;
            addChild(tf);
        }

        _timeBase = getTimer();
        _phase = 0;
        addEventListener(Event.ENTER_FRAME, handleFrame);
    }

    /**
     * Called when we're remove from the stage.
     */
    protected function shutdown (evt :Object = null) :void
    {
        removeEventListener(Event.ENTER_FRAME, handleFrame);
        if (_channel != null) {
            _channel.stop();
            _channel = null;
        }
        _sound = null;

        // remove all children
        for (var ii :int = numChildren - 1; ii >= 0; ii--) {
            removeChildAt(ii);
        }
    }

    protected function handleFrame (evt :Object = null) :void
    {
        var curTime :Number = getTimer();
        var curDur :Number = curTime - _timeBase;
        while (curDur > _millisPerBeat) {
            curDur -= _millisPerBeat;
            _timeBase += _millisPerBeat;
            _phase = (_phase + 1) % 4;
            if (_phase == 0) {
                _boss.toggleFacing();
            }
        }

        /** Scale the time duration into radians */
        var piece :Number = curDur * 2 * Math.PI / _millisPerBeat;

        var left :Number;
        var right :Number;

        switch (_phase) {
        case 0:
        case 1:
        case 2:
            left = Math.sin(piece);
            break;

        case 3:
            left = 0;
            break;
        }

        switch (_phase) {
        case 0:
        case 2:
        case 3:
            right = Math.sin(piece);
            break;

        case 1:
            right = 0;
            break;
        }

        if (_channel != null) {
            left *= Math.min(1, _channel.leftPeak / .75);
            right *= Math.min(1, _channel.rightPeak / .75);
        }

        var leftDip :Number = left * DANCE_AMPLITUDE;
        var rightDip :Number = right * DANCE_AMPLITUDE;

        var radians :Number = Math.atan2(rightDip - leftDip, _boss.getContentWidth());
        var rot :Number = radians * 180 / Math.PI;
        _boss.y = leftDip;
        _boss.rotation = rot;

//        trace("peaks: " + left + "  " + right);
//        trace("radians: " + radians + "  "+ _media.rotation);

        // maybe do some squishing?
        var avg :Number = (left + right) / 2;
        if (avg > .75) {
            _boss.scaleY = .75 + (1 - avg);
        } else {
            _boss.scaleY = 1;
        }
    }

    protected var _boss :DancerSprite;

    protected var _sound :Sound;
    protected var _channel :SoundChannel;

    protected static const BPM :Number = 115.4; // for fetts-vette
    //protected static const BPM :Number = 73.0; // for BollWeevil
    //protected static const BPM :Number = 140.2; // for tarzan

    protected var _millisPerBeat :Number = 60 * 1000 / BPM;

    protected var _timeBase :Number;

    protected var _bossUpdateTimer :Timer;

    /** 0 - 3 for the 4 types of bounce. */
    protected var _phase :int;

    protected static const DANCE_AMPLITUDE :int = 50;
}
}
