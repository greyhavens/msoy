package com.threerings.msoy.export {

import flash.display.DisplayObject;

import flash.events.Event;
import flash.events.IEventDispatcher;

/**
 * The base class for FurniControl, AvatarControl...
 */
public class MsoyControl
{
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

        disp.root.loaderInfo.addEventListener(
            Event.UNLOAD, handleUnload, false, 0, true);
    }

    /**
     * Populate any properties that we provide back to metasoy.
     */
    protected function populateProperties (o :Object) :void
    {
        // nothing in base
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
