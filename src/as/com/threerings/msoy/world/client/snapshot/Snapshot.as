package com.threerings.msoy.world.client.snapshot {

import com.threerings.util.Log;

import flash.display.BitmapData;
import flash.display.DisplayObject;

import flash.geom.Matrix;
import flash.geom.Rectangle;

import com.threerings.util.Log;

import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.client.LayeredContainer;
import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.client.OccupantSprite;


/**
 * Represents a particular snapshot
 */ 
public class Snapshot 
{    
    public var bitmap :BitmapData;

    public function Snapshot (view :RoomView, width :int, height :int)
    {
        _view = view;
        _width = width;
        _height = height;
        bitmap = new BitmapData(width, height);  
//        Log.getLog(this).info("created snapshot with size: "+width+"x"+height);
    }
    
    /**
     * Update the snapshot.
     */
    public function updateSnapshot (
        includeOccupants :Boolean = true, includeOverlays :Boolean = true) :Boolean
    {
        // first let's fill the bitmap with black or something
        bitmap.fillRect(new Rectangle(0, 0, _width, _height), 0x000000);

        // draw the room, scaling down to the appropriate size
        var newScale :Number = _height / _view.getScrollBounds().height;
        var allSuccess :Boolean = true;

        if (!renderChildren(bitmap, newScale, includeOccupants)) {
            allSuccess = false;
        }

        if (includeOverlays) {
            renderOverlays(bitmap, newScale);
        }

        return allSuccess;
    }
    
    /**
     * Render the overlays 
     */ 
    protected function renderOverlays (bitmap :BitmapData, newScale :Number) :void {        
        var d :DisplayObject = _view;
        
        // search up through the containment hierarchy until you find the LayeredContainer
        // or the end of the hierarchy
        while (!(d is LayeredContainer) && d.parent != null) {
            d = d.parent;
        }
        if (d is LayeredContainer) {
            (d as LayeredContainer).snapshotOverlays(bitmap, newScale);
        }
    }

    /**
     * Render the children
     */
    protected function renderChildren (
        bitmap :BitmapData, newScale :Number, includeOccupants :Boolean) :Boolean 
    {
        var allSuccess:Boolean = true;
        
        for (var ii :int = 0; ii < _view.numChildren; ii++) {
            var child :DisplayObject = _view.getChildAt(ii);

            var matrix :Matrix = child.transform.matrix; // makes a clone...
            matrix.scale(newScale, newScale); // scales the matrix taken from that child object

            if (child is MsoySprite) {
                if (!includeOccupants && (child is OccupantSprite)) {
                    continue; // skip occupants
                }

                var success :Boolean = MsoySprite(child).snapshot(bitmap, matrix);
                allSuccess &&= success;

            } else {
                try {
                    bitmap.draw(child, matrix, null, null, null, true);
                    //trace("== Snapshot: raw sprite");

                } catch (err :SecurityError) {
                    // not a critical error
                    Log.getLog(this).info("Unable to snapshot Room element: " + err);
                    allSuccess = false;
                }
            }            
        }
        return allSuccess;
    }
    
    protected var _view :RoomView;
    protected var _width :int;
    protected var _height :int;
}
}