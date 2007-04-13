package com.threerings.msoy.world.client {


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
     * Return the Z value of this object, in room coordinates.
     */
    function getZ () :Number;

    /**
     * Set the screen location of the object, based on its location in the scene.
     */
    function setScreenLocation (x :Number, y :Number) :void;
    
    /**
     * Set the screen scale of the object, based on its location in the room.
     */
    function setScreenScale (scale :Number) :void; 
}
}
