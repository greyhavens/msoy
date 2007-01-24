//
// $Id$

package com.threerings.msoy.export {

import flash.display.DisplayObject;
import flash.utils.Timer;

import flash.events.Event;
import flash.events.IEventDispatcher;
import flash.events.TimerEvent;

/**
 * Handles services that are available to all digital items in a scene. This includes dispatching
 * trigger events and maintaining memory.
 */
public class MsoyControl
{
    /**
     * A function that will get called when an event is triggered on this scene object.
     * Signature:
     * <pre>function (eventName :String, arg :Object) :void</pre>
     */
    public var eventTriggered :Function;

    /**
     * A function that is called when an item's memory has changed. It should have the following
     * signature:
     *
     * <pre>function (key :String, value :Object) :void</pre>
     * 
     * <code>key</code> will be the key that was modified or null if we have just been initialized
     * and we are being provided with our memory for the first time. <code>value</code> will be the
     * value associated with that key if key is non-null, or null.
     */
    public var memoryChanged :Function;

    /**
     * A function that is called periodically (twice a second by default) to allow this item to do
     * its "thinking", make any desired changes to its memory, trigger different animation events
     * and generally execute behavior. This is where all AI code should run, rather than every
     * frame (where only animation related code should run), to ensure that coordination is
     * properly handled when multiple clients are viewing the same item. This is only called after
     * a call to @{link #setTickInterval} is called to indicate that an item wishes to be ticked.
     */
    public var tick :Function;

    /**
     * A callback called if a particular instance of this object has
     * gained control.
     */
    public var gotControl :Function;

    /**
     */
    public function MsoyControl (disp :DisplayObject)
    {
        if (Object(this).constructor == MsoyControl) {
            throw new Error("Use one of the subclasses, as appropriate: " +
                "FurniInterface, AvatarInterface...");
        }

        var event :DynEvent = new DynEvent();
        event.userProps = new Object();
        populateProperties(event.userProps);
        disp.root.loaderInfo.sharedEvents.dispatchEvent(event);
        if ("msoyProps" in event) {
            _props = event.msoyProps;
        }

        disp.root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload, false, 0, true);
    }

    /**
     * Are we connected and running inside the metasoy world, or are we
     * merely being displayed standalone?
     */
    public function isConnected () :Boolean
    {
        return (_props != null);
    }

    /**
     * Triggers an event on this scene object. The event will be properly distributed to the object
     * running in every client in the scene, resulting in a call to {@link #eventTriggered}.
     */
    public function triggerEvent (event :String, arg :Object = null) :void
    {
        callMsoyCode("triggerEvent_v1", event, arg);
    }

    /**
     * Returns the value associated with the supplied key in this item's memory. If no value is
     * mapped in the item's memory, the supplied default value will be returned.
     *
     * @return the value for the specified key from this item's memory or the supplied default.
     */
    public function lookupMemory (key :String, defval :Object) :Object
    {
        var value :Object = callMsoyCode("lookupMemory_v1", key);
        return (value == null) ? defval : value;
    }

    /**
     * Is this instance in control?
     */
    public function hasControl () :Boolean
    {
        return _hasControl;
    }

    /**
     * Request to have this instance of the object control all the instances
     * in the room. The other instances are the same item, running on
     * other browsers.
     */
    public function requestControl () :void
    {
        callMsoyCode("requestControl_v1");
    }

    /**
     * Configures the interval on which this item is "ticked" in milliseconds. The tick interval
     * can be no smaller than 100ms to avoid bogging down the client. By calling this method with a
     * non-zero value, the item indicates that it wants to be ticked and the ticking mechanism will
     * be activated. If this method is not called, ticking will not be done. Calling this method
     * with a 0ms interval will deactivate ticking.
     *
     * Note: Setting the tickInterval implicitely requests control, as
     * only the instance that is in control may tick.
     */
    public function setTickInterval (interval :Number) :void
    {
        _tickInterval = (interval > 100 || interval <= 0) ? interval : 100;

        if (_hasControl) {
            recheckTicker();

        } else if (_tickInterval > 0) {
            requestControl();
        }
    }

    /**
     * Requests that this item's memory be updated with the supplied key/value pair. The supplied
     * value must be a simple object (Integer, Number, String) or an Array of simple objects. The
     * contents of the Pet's memory (keys and values) must not exceed 4096 bytes when AMF3 encoded.
     *
     * @return true if the memory was updated, false if the memory update could not be completed
     * due to size restrictions.
     */
    public function updateMemory (key :String, value :Object) :Boolean
    {
        return callMsoyCode("updateMemory_v1", key, value);
    }

    /**
     * Populate any properties that we provide back to metasoy.
     */
    protected function populateProperties (o :Object) :void
    {
        o["eventTriggered_v1"] = eventTriggered_v1;
        o["memoryChanged_v1"] = memoryChanged_v1;
        o["gotControl_v1"] = gotControl_v1;
    }

    /**
     * Called when an event is triggered on this scene object.
     */
    protected function eventTriggered_v1 (event :String, arg :Object) :void
    {
        if (eventTriggered != null) {
            eventTriggered(event, arg);
        }
    }

    /**
     * Called when one of this item's memory entries has changed.
     */
    protected function memoryChanged_v1 (key :String, value :Object) :void
    {
        if (memoryChanged != null) {
            memoryChanged(key, value);
        }
    }

    /**
     * Called when this client has been assigned control of this object.
     */
    protected function gotControl_v1 () :void
    {
        _hasControl = true;

        // dispatch to user code..
        if (gotControl != null) {
            gotControl();
        }

        // possibly set up a ticker now
        recheckTicker();
    }

    /**
     * Handle any shutdown required.
     */
    protected function handleUnload (evt :Event) :void
    {
        _hasControl = false;
        stopTicker();
    }

    /**
     * Check the status of the ticker, starting or stopping it as necessary.
     */
    protected function recheckTicker () :void
    {
        if (_hasControl && _tickInterval > 0) {
            if (_ticker == null) {
                // we may be creating the timer for the first time
                _ticker = new Timer(_tickInterval);
                _ticker.addEventListener(TimerEvent.TIMER,
                    function (evt :TimerEvent) :void {
                        if (tick != null) {
                            tick();
                        }
                    });

            } else {
                // we may just be committing a new interval
                _ticker.delay = _tickInterval;
            }
            _ticker.start(); // start if not already running

        } else {
            stopTicker();
        }
    }

    /**
     * Stops our AI ticker.
     */
    protected function stopTicker () :void
    {
        if (_ticker != null) {
            _ticker.stop();
            _ticker = null;
        }
    }

    /**
     * Call an exposed function back in msoy land.
     */
    protected function callMsoyCode (name :String, ... args) :*
    {
        if (_props != null) {
            try {
                var func :Function = (_props[name] as Function);
                if (func != null) {
                    return func.apply(null, args);
                }

            } catch (err :Error) {
                trace("Unable to call msoy code: " + err);
            }
        }

        return undefined;
    }

    /** The properties given us by metasoy. */
    protected var _props :Object;

    /** Our desired tick interval (in milliseconds). */
    protected var _tickInterval :Number = 0;

    /** Used to tick this object when this client is running its AI. */
    protected var _ticker :Timer;

    /** Whether this instance has control. */
    protected var _hasControl :Boolean = false;
}
}

import flash.events.Event;

/**
 * A dynamic event we can use to pass info back to metasoy.
 */
dynamic class DynEvent extends Event
{
    public function DynEvent ()
    {
        super("msoyQuery", true, false);
    }

    override public function clone () :Event
    {
        return new DynEvent();
    }
}
