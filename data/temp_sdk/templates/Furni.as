//
// $Id$

package {

import flash.display.Sprite;

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
        // instantiate and wire up our control
        _ctrl = new FurniControl(this);

        // To listen for trigger events, uncomment this
        // _ctrl.addEventListener(ControlEvent.EVENT_TRIGGERED, eventTriggered);

        // To listen for memory events, uncomment this
        // _ctrl.addEventListener(ControlEvent.MEMORY_CHANGED, memoryChanged);

        // To set up a periodic tick callback, uncomment this
        // _ctrl.addEventListener(TimerEvent.TIMER, handleTick);
        // _ctrl.setTickInterval(1000);
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

    protected var _ctrl :FurniControl;
}
}
