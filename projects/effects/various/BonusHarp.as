package {

import flash.display.DisplayObject;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.TimerEvent;

import flash.media.Sound;

import flash.utils.Timer;

import com.whirled.FurniControl;

import com.threerings.flash.Siner;

/**
 * A simple effect that can be played on an avatar when they do well.
 */
[SWF(width="100", height="200")]
public class BonusHarp extends Sprite
{
    public function BonusHarp ()
    {
        _ctrl = new FurniControl(this);

        addEventListener(Event.ADDED_TO_STAGE, handleAdded);
        addEventListener(Event.REMOVED_FROM_STAGE, handleRemoved);

        _yay = new YAY() as DisplayObject;
        _yay.x = 10;
        _yay.y = 50;
        addChild(_yay);

        _ctrl.setHotSpot(_yay.x + _yay.width/2, _yay.y + _yay.height/2);
    }

    protected function handleAdded (... ignored) :void
    {
        addEventListener(Event.ENTER_FRAME, handleFrame);

        var sound :Sound = new SOUND() as Sound;
        sound.play();
        _endTimer.delay = sound.length;
        _endTimer.addEventListener(TimerEvent.TIMER, handleFinished);
        _endTimer.start();
        _tilt.reset();
    }

    protected function handleRemoved (... ignored) :void
    {
        removeEventListener(Event.ENTER_FRAME, handleFrame);
    }

    protected function handleFrame (... ignored) :void
    {
        _yay.rotation = _tilt.value;
    }

    protected function handleFinished (... ignored) :void
    {
        // signify that we're done.
        _ctrl.sendMessage("effectFinished");
    }

    protected var _ctrl :FurniControl;

    protected var _yay :DisplayObject;

    protected var _endTimer :Timer = new Timer(1, 1);

    protected var _tilt :Siner = new Siner(15, 2);

//    protected var _xScale :Siner = new Siner(.2, .5);
//
//    protected var _yScale :Siner = new Siner(.2, .6);

    [Embed(source="harp.mp3")]
    protected static const SOUND :Class;

    [Embed(source="yay.png")]
    protected static const YAY :Class;
}
}
