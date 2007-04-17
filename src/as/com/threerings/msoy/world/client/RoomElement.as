package com.threerings.msoy.world.client {

import com.threerings.msoy.world.data.MsoyLocation;

/**
 * Interface for all objects that exist in a scene, and have both scene location in room
 * coordinate space, and screen location that needs to be updated appropriately.
 */ 
public interface RoomElement
{
    /**
     * Should this element be z-sorted? If true, it will be laid out based on its z coordinate.
     * If false, it will be displayed behind all other objects in the room.
     */
    function isIncludedInLayout () :Boolean;

    /**
     * Set the logical location of the element. The orientation is not updated.
     * @param newLoc may be an MsoyLocation or an Array.
     */
    function setLocation (newLoc :Object) :void

    /**
     * Get the logical location of this object.
     */
    function getLocation () :MsoyLocation;

    /**
     * Set the screen location of the object, based on its location in the scene.
     */
    function setScreenLocation (x :Number, y :Number, scale :Number) :void;
}
}
