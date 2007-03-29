//
// $Id$
//
// MidasTouch - a game for Whirled

package {

import flash.display.Sprite;
import flash.display.Graphics;

import flash.events.Event;
import flash.events.TimerEvent;

import flash.utils.Timer;

import mx.core.BitmapAsset;

import com.whirled.WhirledGameControl;

[SWF(width="96", height="96")]
public class MidasTouch extends Sprite
{
    public function MidasTouch ()
    {
        // listen for an unload event
        root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

        _control = new WhirledGameControl(this);

        [Embed(source="./egg.jpg")]
        var c :Class;

        addChild (new c());
        
        _timer = new Timer (30000); // every 30 secs
        _timer.addEventListener (TimerEvent.TIMER, timerHandler);
        _timer.start();

        _total = 0;

        _control.localChat ("Please wait. Flow is awarded every 30 seconds.");
    }

    /**
     * This is called when your game is unloaded.
     */
    protected function handleUnload (event :Event) :void
    {
        _timer.stop();
        _timer.removeEventListener (TimerEvent.TIMER, timerHandler);
    }
    
    protected function timerHandler (event : TimerEvent) :void
    {
        var max :int = _control.getAvailableFlow ();
        var bit :int = int (Math.round (max * 0.1));
        _control.awardFlow (bit);
        _total += bit;
        _control.localChat ("Awarded " + bit + " out of " + max +
                            ". Total this session: " + _total);
    }

    protected var _control :WhirledGameControl;
    protected var _timer :Timer;
    protected var _total :Number;
}
}
