package {

import flash.display.Sprite;
import flash.display.Graphics;

import flash.events.Event;;
import flash.events.TextEvent;;

/**
 * An extremely simple pet that moves randomly around the room.
 */
[SWF(width="40", height="40")]
public class Blimp extends Sprite
{
    public function Blimp ()
    {
        addEventListener(Event.ENTER_FRAME, enterFrame);
        _loc = [];
        for (var ii :int = 0; ii < 3; ii++) {
            _loc[ii] = Math.random();
        }
    }

    protected function enterFrame (event :Event) :void
    {
        var color :uint = ((256 * Math.random()) << 16) |
            ((256 * Math.random()) << 8) | (256 * Math.random());
        graphics.clear();
        graphics.beginFill(color);
        graphics.drawCircle(20, 20, 20);

        for (var ii :int = 0; ii < 3; ii++) {
            _incs[ii] = ((Number(_incs[ii]) < 0) ? -1 : 1) *
                (Math.random() / 100);

            var newVal :Number = Number(_loc[ii]) + Number(_incs[ii]);
            if (newVal < 0 || newVal > 1) {
                _incs[ii] = -1 * Number(_incs[ii]);
                newVal += Number(_incs[ii]);
            }
            _loc[ii] = newVal;
        }

        // here's how we communicate back to metasoy, this event requests
        // to have our location changed
        if (this.root.loaderInfo != null) {
            this.root.loaderInfo.sharedEvents.dispatchEvent(
                new TextEvent("msoyLoc", true, false, _loc.join()));
        }
    }

    protected var _loc :Array;
    protected var _incs :Array = [.01, .01, .01];
}
}
