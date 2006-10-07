package {

import flash.display.Bitmap;
import flash.display.Sprite;
import flash.display.Graphics;

import flash.events.Event;;
import flash.events.TextEvent;;

/**
 * An extremely simple pet that moves randomly around the room.
 */
[SWF(width="83", height="47")]
public class Blimp extends Sprite
{
    public function Blimp ()
    {
        addEventListener(Event.ENTER_FRAME, enterFrame);
        _loc = [0, 0, 0];
        for (var ii :int = 0; ii < 3; ii += 2) {
            _loc[ii] = Math.random();
        }

        addChild(Bitmap(new ROOMBA()));
        scaleX = .25;
        scaleY = .25;
    }

    protected function enterFrame (event :Event) :void
    {
        for (var ii :int = 0; ii < 3; ii += 2) {

            var newVal :Number = Number(_loc[ii]) + Number(_incs[ii]);
            if (newVal < 0 || newVal > 1) {
            _incs[ii] = ((Number(_incs[ii]) < 0) ? 1 : -1) *
                (Math.random() / 100);
//                _incs[ii] = -1 * Number(_incs[ii]);
                newVal += Number(_incs[ii]);
            }
            _loc[ii] = newVal;
        }

        // here's how we communicate back to metasoy, this event requests
        // to have our location changed
        if (this.root.loaderInfo != null) {
            // we dispatch a text event called "msoyLoc" with the
            // text set to our x/y/z coords, separated by commas.
            // (All msoy coordinates are between 0 and 1)
            this.root.loaderInfo.sharedEvents.dispatchEvent(
                new TextEvent("msoyLoc", true, false, _loc.join()));
        }
    }

    protected var _loc :Array;
    protected var _incs :Array = [.01, 0, .01];

    [Embed(source="roomba.png")]
    protected static const ROOMBA :Class;
}
}
