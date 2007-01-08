//
// $Id$

package com.threerings.msoy.export {

import flash.display.DisplayObject;

import flash.events.Event;
import flash.events.IEventDispatcher;

/**
 * Handles services that are available to all digital items in a scene. This includes dispatching
 * trigger events and maintaining memory.
 */
public class MsoyControl
{
    /**
     * A function that will get called when an event is triggered on this scene object.
     */
    public var eventTriggered :Function;

    /**
     * A function that is called when an entity's memory has changed. It should have the following
     * signature:
     *
     * <pre>public function memoryChanged (key :String, value :Object) :void</pre>
     * 
     * <code>key</code> will be the key that was modified or null if we have just been initialized
     * and we are being provided with our memory for the first time. <code>value</code> will be the
     * value associated with that key if key is non-null, or null.
     */
    public var memoryChanged :Function;

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
        _props = event.msoyProps;

        disp.root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload, false, 0, true);
    }

    /**
     * Triggers an event on this scene object. The event will be properly distributed to the object
     * running in every client in the scene, resulting in a call to {@link #eventTriggered}.
     */
    public function triggerEvent (event :String) :void
    {
        callMsoyCode("triggerEvent_v1", event);
    }

    /**
     * Returns the value associated with the supplied key in this entity's memory.
     *
     * @return a dynamic object that contains entity specific properties.
     */
    public function lookupMemory (key :String) :Object
    {
        return callMsoyCode("getMemory_v1", key);
    }

    /**
     * Requests that this entity's memory be updated with the supplied key/value pair. The supplied
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
    }

    /**
     * Called when an event is triggered on this scene object.
     */
    protected function eventTriggered_v1 (event :String) :void
    {
        if (eventTriggered != null) {
            eventTriggered(event);
        }
    }

    /**
     * Called when a memory entry has changed or when the entity first receives its memory.
     */
    protected function memoryChanged_v1 (key :String, value :Object) :void
    {
        if (memoryChanged != null) {
            memoryChanged(key, value);
        }
    }

    /**
     * Handle any shutdown required.
     */
    protected function handleUnload (evt :Event) :void
    {
        // nothing in base
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
    }

    /** The properties given us by metasoy. */
    protected var _props :Object;
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
