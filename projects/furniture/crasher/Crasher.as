package {

import flash.display.Sprite;

import flash.events.Event;
import flash.events.TimerEvent;

import flash.system.Capabilities;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

import flash.utils.Timer;

[SWF(width="100", height="100")]
public class Crasher extends Sprite
{
    public const SECONDS :int = 30;
    public const MAC_ONLY :Boolean = false;

    public function Crasher ()
    {
        if (MAC_ONLY &&
                (-1 == Capabilities.os.search(new RegExp("^mac", "i")))) {
            // it's not a Mac, don't crash it
            return;
        }

        // otherwise, let's crash some shit!
        _text = new TextField();
        _text.textColor = 0;
        _text.backgroundColor = 0xFFFFFF;
        _text.background = true;
        _text.width = 400;
        _text.autoSize = TextFieldAutoSize.LEFT;
        _text.text = "You have " + SECONDS + " seconds";
        addChild(_text);

        _timer = new Timer(SECONDS * 1000, 1);
        _timer.addEventListener(TimerEvent.TIMER, handleTimer);
        _timer.start();

        this.root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);
    }

    protected function handleTimer (event :TimerEvent) :void
    {
        if (!_readyToCrash) {
            _readyToCrash = true;
            _text.text = "I crashed you!";
            _timer.reset();
            _timer.delay = 500;
            _timer.start();

        } else {
            crash();
        }
    }

    protected function handleUnload (event :Event) :void
    {
        _timer.stop();
        trace("Saved!");
    }

    protected function crash () :void
    {
        while (true) {
        }
    }

    protected var _text :TextField;

    protected var _readyToCrash :Boolean = false;

    protected var _timer :Timer;
}
}
