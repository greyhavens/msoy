package {

import flash.display.Bitmap;
import flash.display.Sprite;
import flash.display.Graphics;

import flash.events.Event;
import flash.events.TimerEvent;

import flash.utils.Timer;

import com.threerings.msoy.export.PetControl;

/**
 * An extremely simple pet that moves randomly around the room.
 */
[SWF(width="83", height="47")]
public class Roomba extends Sprite
{
    private static const MOVEMENT_POWERS :Array = [ .01, 0, .01 ]; // no Y

    public function Roomba ()
    {
        var name :Array = [];
        for (var ii :int = 0; ii < 6; ii++) {
            name.push(int(Math.random() * 26 + 97)); // lowercase letters
        }
        _name = String.fromCharCode.apply(null, name);

        var t :Timer = new Timer(3000);
        t.addEventListener(TimerEvent.TIMER, tick, false, 0, true);
        t.start();
        this.root.loaderInfo.addEventListener(Event.UNLOAD, function (event :Event) :void {
            t.stop();
            t = null;
        }, false, 0, true);

        addChild(Bitmap(new ROOMBA()));
        scaleX = .25;
        scaleY = .25;

        _ctrl = new PetControl(this);
    }

    protected function tick (event :TimerEvent) :void
    {
        trace(_name + " ticking");

        _ctrl.setLocation(Math.random(), 0, Math.random(), Math.random());
    }

    protected var _name :String;
    protected var _ctrl :PetControl;
    protected var _incs :Array = MOVEMENT_POWERS.concat(); // aka copy

    [Embed(source="roomba.png")]
    protected static const ROOMBA :Class;

//    [Embed(source="schade01.wav")]
//    protected static const BUMP :Class;
}
}
