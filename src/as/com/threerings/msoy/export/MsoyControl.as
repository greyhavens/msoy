//
// $Id$

package com.threerings.msoy.export {

import flash.display.DisplayObject;
import flash.utils.Timer;

import flash.events.Event;
import flash.events.EventDispatcher;
import flash.events.TimerEvent;

/**
 * Dispatched when the instance in control sends a trigger event to
 * all instances.
 * 
 * @eventType com.threerings.msoy.export.ControlEvent.EVENT_TRIGGERED
 */
[Event(name="eventTriggered", type="com.threerings.msoy.export.ControlEvent")]

/**
 * Dispatched when the instance in control updates the memory of this
 * digital item.
 *
 * @eventType com.threerings.msoy.export.ControlEvent.MEMORY_CHANGED
 */
[Event(name="memoryChanged", type="com.threerings.msoy.export.ControlEvent")]

/**
 * Dispatched when this instance gains control.
 *
 * @eventType com.threerings.msoy.export.ControlEvent.GOT_CONTROL
 */
[Event(name="gotControl", type="com.threerings.msoy.export.ControlEvent")]

/**
 * Dispatched when this instance has control and a tick interval is
 * registered.
 * 
 * @eventType flash.events.TimerEvent.TIMER
 */
[Event(name="timer", type="flash.events.TimerEvent")]

/**
 * Handles services that are available to all digital items in a scene. This includes dispatching
 * trigger events and maintaining memory.
 */
public class MsoyControl extends EventDispatcher
{
    /**
     */
    public function MsoyControl (disp :DisplayObject)
    {
        if (Object(this).constructor == MsoyControl) {
            throw new Error("Use one of the subclasses, as appropriate: " +
                "FurniInterface, AvatarInterface...");
        }

        var event :ConnectEvent = new ConnectEvent();
        var userProps :Object = new Object();
        populateProperties(userProps);
        event.userProps = userProps;
        disp.root.loaderInfo.sharedEvents.dispatchEvent(event);
        _props = event.hostProps;

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
     * Get the instance id of this instance.
     */
    public function getInstanceId () :int
    {
        return int(callMsoyCode("getInstanceId_v1"));
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
     * Set the layout "hotspot" for your item, specified as pixels relative
     * to (0, 0) the top-left coordinate.
     *
     * If unset, the default hotspot will be based off of the SWF dimensions,
     * with x = width / 2, y = height.
     */
    public function setHotSpot (x :Number, y :Number) :void
    {
        callMsoyCode("setHotSpot_v1", x, y);
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
        dispatch(ControlEvent.EVENT_TRIGGERED, event, arg);
    }

    /**
     * Called when one of this item's memory entries has changed.
     */
    protected function memoryChanged_v1 (key :String, value :Object) :void
    {
        dispatch(ControlEvent.MEMORY_CHANGED, key, value);
    }

    /**
     * Called when this client has been assigned control of this object.
     */
    protected function gotControl_v1 () :void
    {
        _hasControl = true;

        // dispatch to user code..
        dispatch(ControlEvent.GOT_CONTROL);

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
                // re-route it
                _ticker.addEventListener(TimerEvent.TIMER, dispatchEvent);

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
     * Helper method to dispatch a ControlEvent, but only if there
     * is an associated listener.
     */
    protected function dispatch (ctrlEvent :String, key :String = null, value :Object = null) :void
    {
        if (hasEventListener(ctrlEvent)) {
            dispatchEvent(new ControlEvent(ctrlEvent, key, value));
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
                trace("Unable to call host code: " + err);
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
 * A special event we can use to pass info back to metasoy.
 */
class ConnectEvent extends Event
{
    public function ConnectEvent ()
    {
        super("controlConnect", true, false);
    }

    /** Setter: hostProps */
    public function set hostProps (props :Object) :void
    {
        if (_parent != null) {
            _parent.hostProps = props;

        } else {
            _hostProps = props;
        }
    }

    /** Getter: hostProps */
    public function get hostProps () :Object
    {
        return _hostProps;
    }

    /** Setter: userProps */
    public function set userProps (props :Object) :void
    {
        _userProps = props;
    }

    /** Getter: userProps */
    public function get userProps () :Object
    {
        if (_parent != null) {
            return _parent.userProps;
        } else {
            return _userProps;
        }
    }

    override public function clone () :Event
    {
        var clone :ConnectEvent = new ConnectEvent();
        clone._parent = this;
        return clone;
    }

    protected var _parent :ConnectEvent;

    protected var _hostProps :Object;
    protected var _userProps :Object;
}
