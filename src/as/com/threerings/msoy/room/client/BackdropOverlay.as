//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.BitmapData;
import flash.display.Shape;

import flash.geom.Matrix;

import com.threerings.msoy.room.client.RoomElement;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.RoomCodes;
    
/**
 * This shape is used to draw room edges on top of everything during editing.
 */ 
public class BackdropOverlay extends Shape
    implements RoomElement
{   
    // documentation inherited from interface RoomElement
    public function getLayoutType () :int
    {
        return RoomCodes.LAYOUT_NORMAL;
    }
    
    // documentation inherited from interface RoomElement
    public function getRoomLayer () :int
    {
        return RoomCodes.FOREGROUND_LAYER;
    }
    
    // documentation inherited from interface RoomElement
    public function getLocation () :MsoyLocation
    {
        return DEFAULT_LOCATION;
    }
    
    // documentation inherited from interface RoomElement
    public function isImportant () :Boolean
    {
        return false;
    }

    // documentation inherited from interface RoomElement
    public function snapshot (bitmapData :BitmapData, matrix :Matrix) :Boolean
    {
        return true; // we do nothing, innocuously
    }

    // documentation inherited from interface RoomElement
    public function setLocation (newLoc :Object) :void
    {
        // no op - this object is not placed inside the room
    }

    // documentation inherited from interface RoomElement
    public function setScreenLocation (x :Number, y :Number, scale :Number) :void
    {
        // no op - this object cannot be moved, it's always displayed directly on top of the room
    }

    protected static const DEFAULT_LOCATION :MsoyLocation = new MsoyLocation(0, 0, 0, 0);
}
}
