//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.Event;

import flash.geom.Point;
import flash.geom.Rectangle;

import com.threerings.flash.Vector3;
import com.threerings.msoy.world.data.DecorData;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;


/**
 * This class factors out room layout math that converts between 3D room coordinate space
 * and screen coordinates.
 */
public class RoomLayout {

    /** Constructor. */
    public function RoomLayout (view :AbstractRoomView)
    {
        _parentView = view;
        _metrics = new RoomMetrics();
    }

    /** Updates the room layout object (and its room metrics storage) with fresh data. */
    public function update (data :DecorData) :void
    {
        _metrics.update(data);
    }

    /** Get the room metrics used in the layout. */
    public function get metrics () :RoomMetrics
    {
        return _metrics;
    }

    /**
     * Turn the screen coordinate into a MsoyLocation, with the orient field set to 0.
     *   @param shiftPoint if present, constraints movement to points along a vertical
     *                     line (parallel with the y-axis) passing through that point.
     *
     *   @return a ClickLocation object.
     */
    public function pointToLocation (
        globalX :Number, globalY :Number, shiftPoint :Point = null, yOffset :Number = 0)
        :ClickLocation
    {
        // get click location, in screen coords
        var p :Point = new Point(globalX, globalY);
        p = _parentView.globalToLocal(p);

        var cloc :ClickLocation;
        if (shiftPoint == null) {
            // just return the intersection of the line of sight with the first available wall
            cloc = _metrics.screenToWallProjection(p.x, p.y);
            
        } else {
            // convert shift point to a line passing vertically through the room
            var constraint :Point = _parentView.globalToLocal(shiftPoint);
            var constLocation :ClickLocation =
                _metrics.screenToWallProjection(constraint.x, constraint.y);
            var constraintVector :Vector3 = _metrics.toVector3(constLocation.loc);

            // now find a point on the constraint line pointed to by the mouse
            var yLocation :Vector3 =
                _metrics.screenToYLineProjection(p.x, p.y, constraintVector).clampToUnitBox();

            // we're done - make a fake "floor" location
            cloc = new ClickLocation(ClickLocation.FLOOR, _metrics.toMsoyLocation(yLocation));
        }

        // take any optional offset into account
        cloc.loc.y += yOffset / _metrics.sceneHeight;
        return cloc;

    }
        


    // Perspectivization
    // Disabled for now, until we settle on new room layout logic
    // getPerspInfo comes from FurniSprite, checkPerspective() and updatePerspective()
    
    /**
     * Calculate the info needed to perspectivize a piece of furni.
     */

    /*
    public function getPerspInfo (
        sprite :MsoySprite, contentWidth :int, contentHeight :int,
        loc :MsoyLocation) :PerspInfo
    {
        var hotSpot :Point = sprite.getMediaHotSpot();
        var mediaScaleX :Number = Math.abs(sprite.getMediaScaleX());
        var mediaScaleY :Number = Math.abs(sprite.getMediaScaleY());

        // below, 0 refers to the right side of the source sprite
        // N refers to the left side, and H refers to the location
        // of the hotspot

        // the scale of the object is determined by the z coordinate
        var distH :Number = _metrics.focal + (_metrics.sceneDepth * loc.z);
        var dist0 :Number = (hotSpot.x * mediaScaleX);
        var distN :Number = (contentWidth - hotSpot.x) * mediaScaleX;
        if (loc.x < .5) {
            dist0 *= -1;
        } else {
            distN *= -1;
        }

        var scale0 :Number = _metrics.focal / (distH + dist0);
        var scaleH :Number = _metrics.focal / distH;
        var scaleN :Number = _metrics.focal / (distH + distN);

        var logicalY :Number = loc.y + ((contentHeight * mediaScaleY) / _metrics.sceneHeight);

        var p0 :Point = projectedLocation(scale0, loc.x, logicalY);
        var pH :Point = projectedLocation(scaleH, loc.x, loc.y);
        var pN :Point = projectedLocation(scaleN, loc.x, logicalY);

        var height0 :Number = contentHeight * scale0 * mediaScaleY;
        var heightN :Number = contentHeight * scaleN * mediaScaleY;

        // min/max don't account for the hotspot location
        var minX :Number = Math.min(p0.x, pN.x);
        var minY :Number = Math.min(p0.y, pN.y);
        p0.offset(-minX, -minY);
        pN.offset(-minX, -minY);
        pH.offset(-minX, -minY);

        return new PerspInfo(p0, height0, pN, heightN, pH);
    }
    */


    /**
     * Determine the location of the projected coordinate.
     *
     * @param x the logical x coordinate (0 - 1)
     * @param y the logical y coordinate (0 - 1)
     */
    // TODO: deprecate, fix perspectivization, use the _metrics version
    // of these methods
    /*
    protected function projectedLocation (
        scale :Number, x :Number, y :Number) :Point
    {
        // x position depends on logical x and the scale
        var floorWidth :Number = (_metrics.sceneWidth * scale);
        var floorInset :Number = (_metrics.sceneWidth - floorWidth) / 2;

        var horizonY :Number = _metrics.sceneHeight * (1 - _metrics.sceneHorizon);
        return new Point(floorInset + (x * floorWidth),
                         horizonY + ((_metrics.sceneHeight - horizonY) -
                                     (y * _metrics.sceneHeight)) * scale);
    }
    */
    

    /**
     * Given a position in room space, this function finds its projection in screen space, and
     * updates the DisplayObject's position and scale appropriately. If the display object
     * participates in screen layout (and most of them do, with the notable exception of decor),
     * it will also ask the room view to recalculate the object's z-ordering.
     *
     * @param target object to be updated
     * @param offset optional Point argument that, if not null, will be used to shift
     *        the object left and up by the specified x and y amounts.
     */
    public function updateScreenLocation (target :RoomElement, offset :Point = null) :void
    {
        if (!(target is DisplayObject)) {
            throw new ArgumentError("Invalid target passed to updateScreenLocation: " + target);
        }

        var loc :MsoyLocation = target.getLocation();
        var screen :Point = _metrics.roomToScreen(loc.x, loc.y, loc.z);
        var scale :Number = _metrics.scaleAtDepth(loc.z);
        offset = (offset != null ? offset : NO_OFFSET);
        
        target.setScreenLocation(screen.x - offset.x, screen.y - offset.y, scale);
        adjustZOrder(target as DisplayObject);
    }


    /**
     * Adjust the z order of the specified sprite so that it is drawn according to its logical Z
     * coordinate relative to other sprites.
     */
    protected function adjustZOrder (sprite :DisplayObject) :void
    {
        var dex :int;
        try {
            dex = _parentView.getChildIndex(sprite);
        } catch (er :Error) {
            // this can happen if we're resized during editing as we
            // try to reposition a sprite that's still in our data structures
            // but that has been removed as a child.
            return;
        }

        var newdex :int = dex;
        var ourZ :Number = getZOfChildAt(dex);
        var z :Number;
        while (newdex > 0) {
            z = getZOfChildAt(newdex - 1);
            if (z >= ourZ) {
                break;
            }
            newdex--;
        }

        if (newdex == dex) {
            while (newdex < _parentView.numChildren - 1) {
                z = getZOfChildAt(newdex + 1);
                if (z <= ourZ) {
                    break;
                }
                newdex++;
            }
        }

        if (newdex != dex) {
            _parentView.setChildIndex(sprite, newdex);
        }
    }

    /**
     * Convenience method to get the logical z coordinate of the child at the specified index.
     * Returns the z ordering, or NaN if the object is not part of the scene layout.
     */
    protected function getZOfChildAt (index :int) :Number
    {
        var re :RoomElement = RoomElement(_parentView.getChildAt(index));

        // we multiply the layer constant by 1000 to spread out the z values that
        // normally lie in the 0 -> 1 range.
        return re.getLocation().z + (1000 * re.getRoomLayer());
    }


    private static const MAX_SCALE :Number = 1;
    private static const MAX_COORD :Number = 1;


    /** Room metrics storage. */
    protected var _metrics :RoomMetrics;

    /** AbstractRoomView object that contains this instance. */
    protected var _parentView :AbstractRoomView;

    /** Point (0, 0) expressed as a constant. */
    protected static const NO_OFFSET :Point = new Point (0, 0);
}
}
    
