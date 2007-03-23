//
// $Id$
//
// @project@ - a pet for Whirled

package {

import flash.display.Sprite;

import flash.events.Event;
import flash.events.TimerEvent;

import com.whirled.PetControl;
import com.whirled.ControlEvent;

/**
 * @project@ is the coolest Pet ever.
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
        _control = new PetControl(this);

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
     * This is called when your Pet's memory is updated.
     */
    protected function memoryChanged (event :ControlEvent) :void
    {
        trace("memory changed: " + event.name + " -> " + event.value);
    }

    /**
     * This is called when your pet is unloaded.
     */
    protected function handleUnload (event :Event) :void
    {
        // stop any sounds, clean up any resources that need it.  This specifically includes 
        // unregistering listeners to any events - especially Event.ENTER_FRAME
    }

    protected var _control :PetControl;
}
}
