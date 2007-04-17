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
     * @param shiftPoint (optional) another global coordinate at which shift
     *                   was held down, to offset the y coordinate of the result.
     *
     * @return a ClickLocation object.
     */
    public function pointToLocation (
        globalX :Number, globalY :Number, shiftPoint :Point = null, yOffset :Number = 0)
        :ClickLocation
    {
        var p :Point;
        var shiftOffset :Number = 0;
        if (shiftPoint == null) {
            p = new Point(globalX, globalY);

        } else {
            shiftOffset = shiftPoint.y - globalY;
            p = shiftPoint;
        }

        p = _parentView.globalToLocal(p);
        var x :Number = p.x;
        var y :Number = p.y;

        var floorWidth :Number, floorInset :Number;
        var xx :Number, yy :Number, zz :Number;
        var scale :Number;
        var clickWall :int;

        // do some partitioning depending on where the y lies
        if (y < _metrics.backWallTop) {
            clickWall = ClickLocation.CEILING;
            scale = _metrics.farScale +
                (_metrics.backWallTop - y) / _metrics.backWallTop * (MAX_SCALE - _metrics.farScale);

        } else if (y < _metrics.backWallBottom) {
            clickWall = ClickLocation.BACK_WALL;
            scale = _metrics.farScale;

        } else {
            clickWall = ClickLocation.FLOOR;
            scale = _metrics.farScale +
                (y - _metrics.backWallBottom) / (_metrics.sceneHeight - _metrics.backWallBottom) *
                (MAX_SCALE - _metrics.farScale);
        }

        // see how wide the floor is at that scale
        floorWidth = (_metrics.sceneWidth * scale);
        floorInset = (_metrics.sceneWidth - floorWidth) / 2;

        if (x < floorInset || x - floorInset > floorWidth) {
            if (x < floorInset) {
                clickWall = ClickLocation.LEFT_WALL;
                xx = 0;

            } else {
                clickWall = ClickLocation.RIGHT_WALL;
                xx = MAX_COORD;
            }

            // recalculate floorWidth at the minimum scale
            if (scale != _metrics.farScale) {
                floorWidth = (_metrics.sceneWidth * _metrics.farScale);
                floorInset = (_metrics.sceneWidth - floorWidth) / 2;
            }

            switch (clickWall) {
            case ClickLocation.LEFT_WALL:
                scale = _metrics.farScale + (x / floorInset) * (MAX_SCALE - _metrics.farScale);
                break;

            case ClickLocation.RIGHT_WALL:
                scale = _metrics.farScale +
                    ((_metrics.sceneWidth - x) / floorInset) * (MAX_SCALE - _metrics.farScale);
                break;

            default:
                throw new Error(clickWall);
            }

            // TODO: factor in horizon here
            var wallHeight :Number = (_metrics.sceneHeight * scale);
            var wallInset :Number = (_metrics.sceneHeight - wallHeight) / 2;
            yy = MAX_COORD * (1 - ((y - wallInset) / wallHeight));
            zz = MAX_COORD * ((scale - _metrics.farScale) / (MAX_SCALE - _metrics.farScale));

        } else {
            // normal case: the x coordinate is within the floor width
            // at that scale, so we're definitely not clicking on a side wall
            xx = ((x - floorInset) / floorWidth) * MAX_COORD;

            switch (clickWall) {
            case ClickLocation.CEILING:
            case ClickLocation.FLOOR:
                yy = (clickWall == ClickLocation.CEILING) ? MAX_COORD : 0;
                // if on the floor, we want take into account the yOffset
                if (clickWall == ClickLocation.FLOOR) {
                    yy = (yOffset / _metrics.sceneHeight) +
                        (shiftOffset / (scale * _metrics.sceneHeight * _parentView.scaleY));
                    if (yy < 0 || yy > MAX_COORD) {
                        yy = Math.min(MAX_COORD, Math.max(0, yy));
                    }
                } else {
                    yy = 0;
                }
                zz = MAX_COORD * (1 - ((scale - _metrics.farScale) / _metrics.scaleRange));
                break;

            case ClickLocation.BACK_WALL:
                // y is simply how high they clicked on the wall
                yy = (_metrics.backWallBottom - y) / _metrics.backWallHeight;
                zz = 1;
                break;

            default:
                throw new Error(clickWall);
            }
        }

        return new ClickLocation(clickWall, new MsoyLocation(xx, yy, zz, 0));
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
        var distH :Number = RoomMetrics.FOCAL + (_metrics.sceneDepth * loc.z);
        var dist0 :Number = (hotSpot.x * mediaScaleX);
        var distN :Number = (contentWidth - hotSpot.x) * mediaScaleX;
        if (loc.x < .5) {
            dist0 *= -1;
        } else {
            distN *= -1;
        }

        var scale0 :Number = RoomMetrics.FOCAL / (distH + dist0);
        var scaleH :Number = RoomMetrics.FOCAL / distH;
        var scaleN :Number = RoomMetrics.FOCAL / (distH + distN);

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

        return new Point(floorInset + (x * floorWidth),
            _metrics.horizonY + ((_metrics.sceneHeight - _metrics.horizonY) -
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
        
        // maybe call the room view, and tell it to find a new z ordering for this target
        if (target.isIncludedInLayout()) {
            adjustZOrder(target as DisplayObject);
        }
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
            if (isNaN(z) || z >= ourZ) {
                break;
            }
            newdex--;
        }

        if (newdex == dex) {
            while (newdex < _parentView.numChildren - 1) {
                z = getZOfChildAt(newdex + 1);
                if (isNaN(z) || z <= ourZ) {
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
        var disp :DisplayObject = _parentView.getChildAt(index);
        if (disp is RoomElement) {
            var re :RoomElement = (disp as RoomElement);
            if (re.isIncludedInLayout()) {
                return re.getLocation().z;
            }
        }
        return NaN; // either not a RoomElement, or not included in layout
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
    
