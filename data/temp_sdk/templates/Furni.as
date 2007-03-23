//
// $Id$
//
// @project@ - a piece of furni for Whirled

package {

import flash.display.Sprite;

import flash.events.Event;
import flash.events.TimerEvent;

import com.whirled.FurniControl;
import com.whirled.ControlEvent;

/**
 * @project@ is the coolest piece of Furni ever.
 */
[SWF(width="100", height="100")]
public class @project@ extends Sprite
{
    public static const WIDTH :int = 100;
    public static const HEIGHT :int = 100;

    public function @project@ ()
    {
        // listen for an unload event
        root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

        // instantiate and wire up our control
        _control = new FurniControl(this);

        // To listen for trigger events, uncomment this
        // _control.addEventListener(ControlEvent.EVENT_TRIGGERED, eventTriggered);

        // To listen for memory events, uncomment this
        // _control.addEventListener(ControlEvent.MEMORY_CHANGED, memoryChanged);

        // To set up a periodic tick callback, uncomment this
        // _control.addEventListener(TimerEvent.TIMER, handleTick);
        // _control.setTickInterval(1000);
    }

    /**
     * This is called if you register a tick callback.
     */
    protected function handleTick (event :Object = null) :void
    {
        trace("ticked");
    }

    /**
     * This is called when a trigger event is broadcast.
     */
    protected function eventTriggered (event :ControlEvent) :void
    {
        trace("event triggered: " + event.name + ", value: " + event.value);
    }

    /**
     * This is called when your Furni's memory is updated.
     */
    protected function memoryChanged (event :ControlEvent) :void
    {
        trace("memory changed: " + event.name + " -> " + event.value);
    }

    /**
     * This is called when your furni is unloaded.
     */
    protected function handleUnload (event :Event) :void
    {
        // stop any sounds, clean up any resources that need it.  This specifically includes 
        // unregistering listeners to any events - especially Event.ENTER_FRAME
    }

    protected var _control :FurniControl;
}
}
