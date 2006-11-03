package com.threerings.msoy.world.client {

import flash.display.DisplayObject;

import flash.events.Event;
import flash.events.EventDispatcher;
import flash.events.TextEvent;

/**
 * This file should be included by furniture, so that it can communicate
 * with the metasoy world.
 */
public class MsoyInterface
{
    /**
     * Create a furni interface. The display object is your piece
     * of furni.
     */
    public function MsoyInterface (disp :DisplayObject)
    {
        if (Object(this).constructor == MsoyInterface) {
            throw new Error("Use one of the subclasses, as appropriate: " +
                "FurniInterface, AvatarInterface...");
        }
        _dispatcher = disp.root.loaderInfo.sharedEvents;
        _dispatcher.addEventListener("msoyResult", handleResult);

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
     * Submit a query to metasoy, and immediately return the response.
     */
    protected function query (text :String) :String
    {
        dispatch("msoyQuery", text);
        var s :String = _lastResult;
        _lastResult = null;
        return s;
    }

    /**
     * Convenience method to dispatch a command to metasoy.
     */
    protected function dispatch (name :String, text :String) :void
    {
        if (_dispatcher != null) {
            _dispatcher.dispatchEvent(new TextEvent(name, true, false, text));
        }
        // if the _dispatcher is null, we just silently don't send
        // (the user can use isShutdown() to test if we're shut down)
    }

    /**
     * Listens for result events from metasoy, store the result
     * in _lastResult.
     */
    protected function handleResult (event :TextEvent) :void
    {
        // we simply take the result and place it in _lastResult
        _lastResult = event.text;
    }

    /**
     * Handle unloading.
     */
    protected function handleUnload (event :Event) :void
    {
        _dispatcher.removeEventListener("msoyResult", handleResult);
        _dispatcher = null;
    }

    /** The event dispatcher used to communicate with metasoy. */
    protected var _dispatcher :EventDispatcher;

    /** The last result value received from metasoy. */
    protected var _lastResult :String;
}
}
