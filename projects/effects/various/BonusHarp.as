package {

import flash.display.DisplayObject;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.TimerEvent;

import flash.media.Sound;

import flash.utils.Timer;

import com.whirled.EffectControl;

import com.threerings.flash.FrameSprite;
import com.threerings.flash.Siner;

/**
 * A simple effect that can be played on an avatar when they do well.
 */
[SWF(width="100", height="200")]
public class BonusHarp extends FrameSprite
{
    public function BonusHarp ()
    {
        _ctrl = new EffectControl(this);

        _yay = new YAY() as DisplayObject;
        _yay.x = 10;
        _yay.y = 50;
        addChild(_yay);

        _ctrl.setHotSpot(_yay.x + _yay.width/2, _yay.y + _yay.height/2);
    }

    override protected function handleAdded (... ignored) :void
    {
        var sound :Sound = new SOUND() as Sound;
        sound.play();
        _endTimer.delay = sound.length;
        _endTimer.addEventListener(TimerEvent.TIMER, handleFinished);
        _endTimer.start();
        _tilt.reset();

        super.handleAdded();
    }

    override protected function handleFrame (... ignored) :void
    {
        _yay.rotation = _tilt.value;
    }

    protected function handleFinished (... ignored) :void
    {
        // signify that we're done.
        _ctrl.effectFinished();
    }

    protected var _ctrl :EffectControl;

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
