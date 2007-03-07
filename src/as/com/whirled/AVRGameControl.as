//
// $Id$
//
// Copyright (c) 2007 Three Rings Design, Inc.  Please do not redistribute.

package com.whirled {

import flash.display.DisplayObject;

import com.threerings.util.Name;

// NOTE: This API is going to change, a lot, before everything's said and
// done. Time hasn't been had to give it the love it needs.

/**
 * This file should be included by AVR games so that they can communicate
 * with the whirled.
 * AVRGame means: Alternate Virtual Reality Game, and refers to games
 * played within the whirled environment.
 */
public class AVRGameControl extends WhirledGameControl
{
    /**
     * A function that is called when the game's memory has changed. It should have the following
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
     * A function that will get called when the user enters a room.
     */
    public var enteredRoom :Function;
    
    /**
     * A function that will get called when the user leaves a room.
     */
    public var leftRoom :Function;
    
    /**
     * A function that will get called when an occupant enters the room.
     */
    public var occupantEntered :Function; /* (occupant :int) */
    
    /**
     * A function that will get called when an occupant leaves the room.
     */
    public var occupantLeft :Function; /* (occupant :int) */
    
    /**
     * A function that will get called when an occupant moves to a new location.
     */
    public var occupantMoved :Function; /* (occupant :int) */
        
    /**
     * Create a world game interface. The display object is your world game.
     */
    public function AVRGameControl (disp :DisplayObject)
    {
        super(disp);
    }
    
    /**
     * Returns the value associated with the supplied key in this game's memory. If no value is
     * mapped in the game's memory, the supplied default value will be returned.
     *
     * @return the value for the specified key from this game's memory or the supplied default.
     */
    public function lookupMemory (key :String, defval :Object) :Object
    {
        var value :Object = callEZCode("lookupMemory_v1", key);
        return (value == null) ? defval : value;
    }
    
    /**
     * Requests that this game's memory be updated with the supplied key/value pair. The supplied
     * value must be a simple object (Integer, Number, String) or an Array of simple objects. The
     * contents of the game's memory (keys and values) must not exceed 4096 bytes when AMF3 encoded.
     *
     * @return true if the memory was updated, false if the memory update could not be completed
     * due to size restrictions.
     */
    public function updateMemory (key :String, value :Object) :Boolean
    {
        return callEZCode("updateMemory_v1", key, value);
    }

    /**
     * Returns the specified occupant's current location in the scene.
     *
     * @return an array containing [ x, y, z ]. x, y, and z are Numbers between 0 and 1 or null if
     * the location is unknown.
     */
    public function getOccupantLocation (occupantId :int) :Array
    {
        return (callEZCode("getOccupantLocation_v1", occupantId) as Array);
    }
    
    // from EZGameControl
    override protected function populateProperties (o :Object) :void
    {
        super.populateProperties(o);
        o["memoryChanged_v1"] = memoryChanged_v1;
        o["enteredRoom_v1"] = enteredRoom_v1;
        o["leftRoom_v1"] = leftRoom_v1;
        o["occupantEntered_v1"] = occupantEntered_v1;
        o["occupantLeft_v1"] = occupantLeft_v1;
        o["occupantMoved_v1"] = occupantMoved_v1;
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
     * Called when the user enters a room.
     */
    protected function enteredRoom_v1 () :void
    {
        if (enteredRoom != null) {
            enteredRoom();
        }
    }
    
    /**
     * Called when the user leaves the room.
     */
    protected function leftRoom_v1 () :void
    {
        if (leftRoom != null) {
            leftRoom();
        }
    }
    
    /**
     * Called when an occupant enters the room.
     */    
    protected function occupantEntered_v1 (occupant :int) :void
    {
        if (occupantEntered != null) {
            occupantEntered(occupant);
        }
    }
    
    /**
     * Called when an occupant leaves the room.
     */    
    protected function occupantLeft_v1 (occupant :int) :void
    {
        if (occupantLeft != null) {
            occupantLeft(occupant);
        }
    }
    
    /**
     * Called when an occupant moves to a new location within the room.
     */    
    protected function occupantMoved_v1 (occupant :int) :void
    {
        if (occupantMoved != null) {
            occupantMoved(occupant);
        }
    }
}
}
