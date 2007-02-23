package {

import flash.display.DisplayObject;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.TimerEvent;

import flash.utils.ByteArray;
import flash.utils.Timer;

import com.threerings.util.EmbeddedSwfLoader;

/** The background excitement. */
public class Background extends Sprite
{
    public function Background ()
    {
        // construct an array of the registered backgrounds
        for (var ii :int = 0; true; ii++) {
            try {
                _backgrounds.push(this["BKG_" + ii]);
            } catch (re :ReferenceError) {
                break;
            }
        }

        // kick things off
        chooseNewBackground();
    }

    protected function chooseNewBackground (evt :TimerEvent = null) :void
    {
        _timer.removeEventListener(TimerEvent.TIMER, chooseNewBackground);

        var index :int = 0;
        if (_backgrounds.length > 1) {
            do {
                index = int(Math.random() * _backgrounds.length);
            } while (index == _lastIndex);
        }

        var clazz :Class = Class(_backgrounds[index]);
        trace("clazz: " + clazz);
        var bytes :ByteArray = ByteArray(new clazz());

        _loader = new EmbeddedSwfLoader();
        _loader.addEventListener(Event.COMPLETE, newBackgroundReady);
        _loader.load(bytes);
        _lastIndex = index;
    }

    protected function newBackgroundReady (evt :Event) :void
    {
        _newBackground = _loader.getContent();
        _loader = null;
        addChild(_newBackground);

        if (_background == null) {
            // if there's no old one, jump right to using the new one.
            swapOutOld();
            return;
        }

        _newBackground.alpha = 0;

        // use the timer to fade in the new background
        _timer.reset();
        _timer.delay = 200;
        _timer.repeatCount = 0;
        _timer.addEventListener(TimerEvent.TIMER, fadeInNew);
        _timer.start();
    }

    protected function fadeInNew (evt :TimerEvent) :void
    {
        _newBackground.alpha = Math.min(1, _newBackground.alpha + FADE_INCREMENT);
        if (_newBackground.alpha == 1) {
            _timer.removeEventListener(TimerEvent.TIMER, fadeInNew);

            // let's now fade out the old!
            _timer.addEventListener(TimerEvent.TIMER, fadeOutOld);
        }

        evt.updateAfterEvent(); // flash blows
    }

    protected function fadeOutOld (evt :TimerEvent) :void
    {
        _background.alpha = Math.max(0, _background.alpha - FADE_INCREMENT);
        if (_background.alpha == 0) {
            // once it's all faded out, remove it and swap it
            removeChild(_background);
            swapOutOld();
        }

        evt.updateAfterEvent(); // flash blows
    }

    protected function swapOutOld () :void
    {
        // swap in the new background as the main one
        _background = _newBackground;
        _newBackground = null;
        // ok, we're ready to just show the one we got for a while
        _timer.removeEventListener(TimerEvent.TIMER, fadeOutOld);

        _timer.reset();
        _timer.delay = 5000;
        _timer.repeatCount = 1;
        _timer.addEventListener(TimerEvent.TIMER, chooseNewBackground);
        _timer.start();
    }

    protected var _backgrounds :Array = [];

    protected var _lastIndex :int = -1;

    protected var _loader :EmbeddedSwfLoader;

    protected var _newBackground :DisplayObject;

    protected var _background :DisplayObject;

    protected var _timer :Timer = new Timer(200);

    protected static const FADE_INCREMENT :Number = 1/25;

    [Embed(source="DiscoMoire.swf", mimeType="application/octet-stream")]
    protected var BKG_0 :Class;

    [Embed(source="FX_arrow.swf", mimeType="application/octet-stream")]
    protected var BKG_1 :Class;
}
}
