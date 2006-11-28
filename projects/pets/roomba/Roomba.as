package {

import flash.display.Bitmap;
import flash.display.Sprite;
import flash.display.Graphics;

import flash.events.Event;;
import flash.events.TextEvent;;
import flash.events.TimerEvent;;

import flash.utils.Timer;

import com.threerings.msoy.world.client.FurniInterface;

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

        var t :Timer = new Timer(1000);
        t.addEventListener(TimerEvent.TIMER, tick, false, 0, true);
        t.start();

        this.root.loaderInfo.addEventListener(Event.UNLOAD,
            function (event :Event) :void {
//                t.stop();
//                t = null;
            }, false, 0, true);

        addEventListener(Event.ENTER_FRAME, enterFrame, false, 0, true);
        addChild(Bitmap(new ROOMBA()));
        scaleX = .25;
        scaleY = .25;

        _iface = new FurniInterface(this);
    }

    protected function updateLocation () :void
    {
        var loc :Array = _iface.getLocation();
        trace(_name + " got loc : " + loc);

        if (loc == null) {
            // hmm, something's not quite set up yet.
            return;
        }

        for (var ii :int = 0; ii < 3; ii++) {
            var newVal :Number = Number(loc[ii]) + Number(_incs[ii]);
            if (newVal < 0 || newVal > 1) {
                _incs[ii] = ((Number(_incs[ii]) < 0) ? 1 : -1) // flip sign
                    * Math.random() * MOVEMENT_POWERS[ii]; // mult random
                newVal += Number(_incs[ii]);
            }
            loc[ii] = newVal;
        }

        _iface.setLocation(loc);
    }

    protected function enterFrame (event :Event) :void
    {
        updateLocation();
    }

    protected function tick (event :TimerEvent) :void
    {
        trace(_name + " ticking");
    }

    protected var _name :String;

    protected var _iface :FurniInterface;

    protected var _incs :Array = MOVEMENT_POWERS.concat(); // aka copy

    [Embed(source="roomba.png")]
    protected static const ROOMBA :Class;

//    [Embed(source="schade01.wav")]
//    protected static const BUMP :Class;
}
}
