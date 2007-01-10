package com.threerings.msoy.export {

import flash.display.DisplayObject;

import com.threerings.util.Name;

import com.threerings.ezgame.EZGameControl;

/**
 * This file should be included by world games so that they can communicate with the
 * metasoy world.
 */
public class WorldGameControl extends EZGameControl
{
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
    public function WorldGameControl (disp :DisplayObject)
    {
        super(disp);
    }
    
    /**
     * Returns an array containing identifiers for each player in the game.
     */
    public function getPlayerOccupantIds () :Array /* of int */
    {
        return (callEZCode("getPlayerOccupantIds_v1") as Array);
    }
    
    /**
     * Returns the player's own occupant identifier.
     */
    public function getMyOccupantId () :int
    {
        return int(callEZCode("getMyOccupantId_v1"));
    }
    
    /**
     * Returns an array containing identifiers for each occupant of the current room.
     */
    public function getRoomOccupantIds () :Array /* of int */
    {
        return (callEZCode("getRoomOccupantIds_v1") as Array);
    }
    
    /**
     * Returns the specified occupant's current location in the scene.
     *
     * @return an array containing [ x, y, z ]. x, y, and z are Numbers between 0 and 1 or null if
     * the location is unknown.
     */
    public function getOccupantLocation (occupantId :int) :Array
    {
        return (callEZCode("getOccupantLocation_v1") as Array);
    }
    
    /**
     * Returns the name of the specified occupant.
     */
    public function getOccupantName (occupantId :int) :String
    {
        return (callEZCode("getOccupantName_v1") as String);
    }
    
    // from EZGameControl
    override protected function populateProperties (o :Object) :void
    {
        super.populateProperties(o);
        o["enteredRoom_v1"] = enteredRoom_v1;
        o["leftRoom_v1"] = leftRoom_v1;
        o["occupantEntered_v1"] = occupantEntered_v1;
        o["occupantLeft_v1"] = occupantLeft_v1;
        o["occupantMoved_v1"] = occupantMoved_v1;
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
