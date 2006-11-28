package com.threerings.msoy.world.client {

import flash.display.DisplayObject;

import flash.events.Event;
import flash.events.EventDispatcher;

import mx.events.DynamicEvent;

/**
 * The base class for FurniInterface, AvatarInterface...
 */
public class MsoyInterface
{
    /**
     */
    public function MsoyInterface (disp :DisplayObject)
    {
        if (Object(this).constructor == MsoyInterface) {
            throw new Error("Use one of the subclasses, as appropriate: " +
                "FurniInterface, AvatarInterface...");
        }

        _dispatcher = disp.root.loaderInfo.sharedEvents;
        _dispatcher.addEventListener("msoyMessage", handleMessage);

        disp.root.loaderInfo.addEventListener(
            Event.UNLOAD, handleUnload, false, 0, true);
    }

    /**
     * Have we been shut down?
     */
    public function isShutdown () :Boolean
    {
        return (_dispatcher == null);
    }

    /**
     * Handle a query (or notification message) from metasoy.
     * Override this method to add custom behaviors.
     */
    protected function handleQuery (name :String, val :Object) :Object
    {
        // by default, we do nothing.
        return null;
    }

    /**
     * Convenience method to send a command to metasoy.
     */
    protected function dispatch (name :String, val :Object = null) :Object
    {
        if (_dispatcher == null) {
            // if the _dispatcher is null, we just silently don't send
            // (the user can use isShutdown() to test if we're shut down)
            return null;
        }

        var de :DynamicEvent = new DynamicEvent("msoyQuery", true, false);
        de.msoyName = name;
        de.msoyValue = val;
        _dispatcher.dispatchEvent(de);
        return de.msoyResponse;
    }

    /**
     * Handle messages received from metasoy.
     */
    protected function handleMessage (evt :Object) :void
    {
        evt.msoyResponse = handleQuery(String(evt.msoyName), evt.msoyValue);
    }

    /**
     * Handle unloading.
     */
    protected function handleUnload (event :Event) :void
    {
        _dispatcher.removeEventListener("msoyMessage", handleMessage);
        _dispatcher = null;
    }

    /** The event dispatcher used to communicate with metasoy. */
    protected var _dispatcher :EventDispatcher;
}
}
