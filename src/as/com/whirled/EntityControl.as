//
// $Id$
//
// Copyright (c) 2007 Three Rings Design, Inc.  Please do not redistribute.

package com.whirled {

import flash.display.DisplayObject;
import flash.utils.Timer;

import flash.events.Event;
import flash.events.EventDispatcher;
import flash.events.TimerEvent;

/**
 * Dispatched when the instance in control sends a trigger action to
 * all instances.
 * 
 * @eventType com.whirled.ControlEvent.ACTION_TRIGGERED
 */
[Event(name="actionTriggered", type="com.whirled.ControlEvent")]

/**
 * Dispatched when any instance sends a message to all instances.
 * 
 * @eventType com.whirled.ControlEvent.MESSAGE_RECEIVED
 */
[Event(name="messageReceived", type="com.whirled.ControlEvent")]

/**
 * Dispatched when the instance in control updates the memory of this
 * digital item.
 *
 * @eventType com.whirled.ControlEvent.MEMORY_CHANGED
 */
[Event(name="memoryChanged", type="com.whirled.ControlEvent")]

/**
 * Dispatched when this instance gains control.
 *
 * @eventType com.whirled.ControlEvent.GOT_CONTROL
 */
[Event(name="gotControl", type="com.whirled.ControlEvent")]

/**
 * Dispatched when this instance has control and a tick interval is
 * registered.
 * 
 * @eventType flash.events.TimerEvent.TIMER
 */
[Event(name="timer", type="flash.events.TimerEvent")]

/**
 * Handles services that are available to all entities in a room.
 * This includes dispatching trigger events and maintaining memory.
 */
public class EntityControl extends WhirledControl
{
    /**
     */
    public function EntityControl (disp :DisplayObject)
    {
        super(disp);
    }

    /**
     * Triggers an action on this scene object. The action will be properly distributed to the
     * object running in every client in the scene, resulting in a ACTION_TRIGGERED event.
     *
     * Note: the name must be a String and may be up to 64 characters.
     * TODO: restriction on size of the argument. It will probably be 1k or something.
     */
    public function triggerAction (name :String, arg :Object = null) :void
    {
        callHostCode("sendMessage_v1", name, arg, true);
    }

    /**
     * Send a message to other instances of this entity, resulting in a MESSAGE_RECEIVED event.
     *
     * Note: the name must be a String and may be up to 64 characters.
     * TODO: restriction on size of the argument. It will probably be 1k or something.
     */
    public function sendMessage (name :String, arg :Object = null) :void
    {
        callHostCode("sendMessage_v1", name, arg, false);
    }

    /**
     * Returns the value associated with the supplied key in this item's memory. If no value is
     * mapped in the item's memory, the supplied default value will be returned.
     *
     * @return the value for the specified key from this item's memory or the supplied default.
     */
    public function lookupMemory (key :String, defval :Object) :Object
    {
        var value :Object = callHostCode("lookupMemory_v1", key);
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
     * Request to have this instance of the object control all the instances in the room. The other
     * instances are the same item, running on other browsers.
     */
    public function requestControl () :void
    {
        callHostCode("requestControl_v1");
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
        return int(callHostCode("getInstanceId_v1"));
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
        return callHostCode("updateMemory_v1", key, value);
    }

    /**
     * Set the layout "hotspot" for your item, specified as pixels relative to (0, 0) the top-left
     * coordinate.
     *
     * If unset, the default hotspot will be based off of the SWF dimensions,
     * with x = width / 2, y = height.
     */
    public function setHotSpot (x :Number, y :Number) :void
    {
        callHostCode("setHotSpot_v1", x, y);
    } 

    /**
     * Populate any properties that we provide back to whirled.
     */
    override protected function populateProperties (o :Object) :void
    {
        o["memoryChanged_v1"] = memoryChanged_v1;
        o["gotControl_v1"] = gotControl_v1;
        o["messageReceived_v1"] = messageReceived_v1;
    }

    /**
     * Called when an action or message is triggered on this scene object.
     */
    protected function messageReceived_v1 (name :String, arg :Object, isAction :Boolean) :void
    {
        dispatch(isAction ? ControlEvent.ACTION_TRIGGERED
                          : ControlEvent.MESSAGE_RECEIVED, name, arg);
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
        if (_hasControl) {
            return; // avoid re-dispatching
        }
        _hasControl = true;

        // dispatch to user code..
        dispatch(ControlEvent.GOT_CONTROL);

        // possibly set up a ticker now
        recheckTicker();
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

    override protected function handleUnload (evt :Event) :void
    {
        super.handleUnload(evt);

        _hasControl = false;
        stopTicker();
    }

    /** Our desired tick interval (in milliseconds). */
    protected var _tickInterval :Number = 0;

    /** Used to tick this object when this client is running its AI. */
    protected var _ticker :Timer;

    /** Whether this instance has control. */
    protected var _hasControl :Boolean = false;
}
}
