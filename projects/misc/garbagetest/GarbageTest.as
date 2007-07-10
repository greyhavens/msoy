package {

import flash.display.Sprite;

import flash.events.Event;
import flash.events.TimerEvent;
import flash.events.MouseEvent;

import flash.text.TextField;

import flash.system.System;

import flash.utils.getTimer;
import flash.utils.Timer;

[SWF(width="500", height="500")]
public class GarbageTest extends Sprite
{
    public function GarbageTest ()
    {
        addEventListener(Event.ENTER_FRAME, handleFrame);

        _tf = new TextField();
        _tf.background = true;
        _tf.multiline = true;
        _tf.width = 500;
        _tf.height = 500;
        addChild(_tf);

        _removeListener = (Math.random() >= .5);
        _event = TimerEvent.TIMER; //(Math.random() >= .5) ? Event.ENTER_FRAME : MouseEvent.CLICK;
        _tf.appendText("Will " + (_removeListener ? "" : "NOT ") +
            "remove '" + _event + "' listener...");
    }

    protected function handleFrame (... ignored) :void
    {
        _frameCount++;
        switch (_frameCount) {
        case 30:
            _tf.appendText("\n\nBefore creation: " + memUsage());
            _other = new Other();
            _other.addEventListener(_event, theBlackHole);
            _other.start();
            break;

        case 31:
            _tf.appendText("\nCreated big sprite: " + memUsage());
            break;

        case 60:
            if (_removeListener) {
                _other.removeEventListener(_event, theBlackHole);
            }
            _other = null;
            _tf.appendText("\nNulled sprite: " + memUsage());
            break;

        case 90:
            genCrap(new Array());
            _tf.appendText("\nGenerated more objects and immediately tossed: " +
                memUsage());
            break;

        case 120:
            _tf.appendText("\nFinally... " + memUsage());
            break;

        case 150:
            _tf.appendText("\n\nReload to try again.");
            break;
        }
    }

    protected function memUsage () :String
    {
        return String(int(System.totalMemory / (1024 * 1024))) + "MB";
    }

    public function theBlackHole (... ignored) :void
    {
        trace("What are you doing in here? " + getTimer());
    }

    public static function genCrap (crap :Array) :void
    {
        for (var ii :int = 0; ii < 10000; ii++) {
            var s :String = "";
            for (var jj :int = 100; jj >= 0; jj--) {
                s += String.fromCharCode(int(Math.random() * 26) + 65);
            }
            crap.push(s);
        }
    }

    protected var _removeListener :Boolean;

    protected var _other :Other;

    protected var _frameCount :int = 0;

    protected var _tf :TextField;

    protected var _event :String;
}
}

import flash.display.Sprite;

import flash.events.Event;
import flash.events.EventDispatcher;

import flash.utils.Timer;

class Other extends Timer
{
    public function Other ()
    {
        super(20000, 1);
        GarbageTest.genCrap(_crap);
    }

    protected var _crap :Array = [];
}
