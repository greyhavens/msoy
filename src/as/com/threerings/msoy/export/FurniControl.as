package com.threerings.msoy.export {

import flash.display.DisplayObject;

import flash.events.Event;
import flash.events.EventDispatcher;
import flash.events.TextEvent;

/**
 * This file should be included by furniture, so that it can communicate
 * with the metasoy world.
 */
public class FurniControl extends MsoyControl
{
    /**
     * Create a furni interface. The display object is your piece
     * of furni.
     */
    public function FurniControl (disp :DisplayObject)
    {
        super(disp);
    }

    /**
     * Get our current location in the room.
     *
     * @return an array containing [ x, y, z, orient ]. x, y, and z are
     * Numbers between 0 and 1, orient is an int between 0 and 360.
     *
     * @return null if our location is unknown.
     */
    public function getLocation () :Array
    {
        return (callMsoyCode("getLocation_v1") as Array);
    }

    /**
     * Request to update our location.
     */
    public function setLocation (loc :Array) :void
    {
        callMsoyCode("setLocation_v1", loc);
    }
}
}
