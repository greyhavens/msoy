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

import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;


/**
 * This class factors out room layout math that converts between 3D room coordinate space
 * and screen coordinates.
 */
public class RoomLayout {


    //
    //
    // FROM ABSTRACT ROOM VIEW

   
    // RZ: Only used in EditorController.spritePositioning():644
    
    /**
     * Get the y distance represented by the specified number of pixels for the given z coordinate.
     */
    public static function getYDistance (view :AbstractRoomView, z :Number, pixels :int) :Number
    {
        var metrics :RoomMetrics = view.getRoomMetrics();
        
        var scale :Number = metrics.minScale +
            ((MAX_COORD - z) / MAX_COORD) * (MAX_SCALE - metrics.minScale);
        var sheight :Number = metrics.sceneHeight * scale;
        return (pixels / sheight);
    }
    


    /**
     * Turn the screen coordinate into a MsoyLocation, with the orient field set to 0.
     * @param shiftPoint (optional) another global coordinate at which shift
     *                   was held down, to offset the y coordinate of the result.
     *
     * @return a ClickLocation object.
     */
    public static function pointToLocation (
        view :AbstractRoomView,
        globalX :Number, globalY :Number, shiftPoint :Point = null, yOffset :Number = 0)
        :ClickLocation
    {
        var metrics :RoomMetrics = view.getRoomMetrics();
        
        var p :Point;
        var shiftOffset :Number = 0;
        if (shiftPoint == null) {
            p = new Point(globalX, globalY);

        } else {
            shiftOffset = shiftPoint.y - globalY;
            p = shiftPoint;
        }

        p = view.globalToLocal(p);
        var x :Number = p.x;
        var y :Number = p.y;

        var floorWidth :Number, floorInset :Number;
        var xx :Number, yy :Number, zz :Number;
        var scale :Number;
        var clickWall :int;

        // do some partitioning depending on where the y lies
        if (y < metrics.backWallTop) {
            clickWall = ClickLocation.CEILING;
            scale = metrics.minScale +
                (metrics.backWallTop - y) / metrics.backWallTop * (MAX_SCALE - metrics.minScale);

        } else if (y < metrics.backWallBottom) {
            clickWall = ClickLocation.BACK_WALL;
            scale = metrics.minScale;

        } else {
            clickWall = ClickLocation.FLOOR;
            scale = metrics.minScale +
                (y - metrics.backWallBottom) / (metrics.sceneHeight - metrics.backWallBottom) *
                (MAX_SCALE - metrics.minScale);
        }

        // see how wide the floor is at that scale
        floorWidth = (metrics.sceneWidth * scale);
        floorInset = (metrics.sceneWidth - floorWidth) / 2;

        if (x < floorInset || x - floorInset > floorWidth) {
            if (x < floorInset) {
                clickWall = ClickLocation.LEFT_WALL;
                xx = 0;

            } else {
                clickWall = ClickLocation.RIGHT_WALL;
                xx = MAX_COORD;
            }

            // recalculate floorWidth at the minimum scale
            if (scale != metrics.minScale) {
                floorWidth = (metrics.sceneWidth * metrics.minScale);
                floorInset = (metrics.sceneWidth - floorWidth) / 2;
            }

            switch (clickWall) {
            case ClickLocation.LEFT_WALL:
                scale = metrics.minScale + (x / floorInset) * (MAX_SCALE - metrics.minScale);
                break;

            case ClickLocation.RIGHT_WALL:
                scale = metrics.minScale +
                    ((metrics.sceneWidth - x) / floorInset) * (MAX_SCALE - metrics.minScale);
                break;

            default:
                throw new Error(clickWall);
            }

            // TODO: factor in horizon here
            var wallHeight :Number = (metrics.sceneHeight * scale);
            var wallInset :Number = (metrics.sceneHeight - wallHeight) / 2;
            yy = MAX_COORD * (1 - ((y - wallInset) / wallHeight));
            zz = MAX_COORD * ((scale - metrics.minScale) / (MAX_SCALE - metrics.minScale));

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
                    yy = (yOffset / metrics.sceneHeight) +
                        (shiftOffset / (scale * metrics.sceneHeight * view.scaleY));
                    if (yy < 0 || yy > MAX_COORD) {
                        yy = Math.min(MAX_COORD, Math.max(0, yy));
                    }
                } else {
                    yy = 0;
                }
                zz = MAX_COORD * (1 - ((scale - metrics.minScale) / metrics.scaleRange));
                break;

            case ClickLocation.BACK_WALL:
                // y is simply how high they clicked on the wall
                yy = (metrics.backWallBottom - y) / metrics.backWallHeight;
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
    public static function getPerspInfo (
        view :AbstractRoomView,
        sprite :MsoySprite, contentWidth :int, contentHeight :int,
        loc :MsoyLocation) :PerspInfo
    {
        var metrics :RoomMetrics = view.getRoomMetrics();
        
        var hotSpot :Point = sprite.getMediaHotSpot();
        var mediaScaleX :Number = Math.abs(sprite.getMediaScaleX());
        var mediaScaleY :Number = Math.abs(sprite.getMediaScaleY());

        // below, 0 refers to the right side of the source sprite
        // N refers to the left side, and H refers to the location
        // of the hotspot

        // the scale of the object is determined by the z coordinate
        var distH :Number = RoomMetrics.FOCAL + (view.getScene().getDepth() * loc.z);
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

        var logicalY :Number = loc.y + ((contentHeight * mediaScaleY) / metrics.sceneHeight);

        var p0 :Point = projectedLocation(metrics, scale0, loc.x, logicalY);
        var pH :Point = projectedLocation(metrics, scaleH, loc.x, loc.y);
        var pN :Point = projectedLocation(metrics, scaleN, loc.x, logicalY);

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
    protected static function projectedLocation (
        metrics :RoomMetrics,
        scale :Number, x :Number, y :Number) :Point
    {
        // x position depends on logical x and the scale
        var floorWidth :Number = (metrics.sceneWidth * scale);
        var floorInset :Number = (metrics.sceneWidth - floorWidth) / 2;

        return new Point(floorInset + (x * floorWidth),
            metrics.horizonY + (metrics.subHorizonHeight - (y * metrics.sceneHeight)) * scale);
    }
    */
    


    // RZ: called internally (roomToScreenLocation) and AbstractRoomView.locationUpdated():107

    /**
     * Adjust the z order of the specified sprite so that it is drawn according to its logical Z
     * coordinate relative to other sprites.
     */
    public static function adjustZOrder (view :AbstractRoomView, sprite :DisplayObject) :void
    {
        var dex :int;
        try {
            dex = view.getChildIndex(sprite);
        } catch (er :Error) {
            // this can happen if we're resized during editing as we
            // try to reposition a sprite that's still in our data structures
            // but that has been removed as a child.
            return;
        }

        var newdex :int = dex;
        var ourZ :Number = getZOfChildAt(view, dex);
        var z :Number;
        while (newdex > 0) {
            z = getZOfChildAt(view, newdex - 1);
            if (isNaN(z) || z >= ourZ) {
                break;
            }
            newdex--;
        }

        if (newdex == dex) {
            while (newdex < view.numChildren - 1) {
                z = getZOfChildAt(view, newdex + 1);
                if (isNaN(z) || z <= ourZ) {
                    break;
                }
                newdex++;
            }
        }

        if (newdex != dex) {
            view.setChildIndex(sprite, newdex);
        }
    }

    // RZ: only called internally
    
    /**
     * Convenience method to get the logical z coordinate of the child at the specified index.
     */
    protected static function getZOfChildAt (
        view :AbstractRoomView,
        index :int) :Number
    {
        var disp :DisplayObject = view.getChildAt(index);
        if (disp is MsoySprite) {
            var spr :MsoySprite = (disp as MsoySprite);
            if (spr.isIncludedInLayout()) {
                return spr.loc.z;
            }

        } else if (disp is ZOrderable) {
            return (disp as ZOrderable).getZ();
        }

        // if all else fails..
        return NaN;
    }

    

    private static const MAX_SCALE :Number = 1;
    private static const MAX_COORD :Number = 1;




    // RZ: Only used in AbstractRoomView.locationUpdated():104
    
    /**
     * Calculate the scale and x/y position of the specified media according to its logical
     * coordinates.
     */
    public static function positionAndScale (view :AbstractRoomView, sprite :MsoySprite) :void
    {
        var metrics :RoomMetrics = view.getRoomMetrics();
        
        var screen :Point = metrics.roomToScreen(sprite.loc.x, sprite.loc.y, sprite.loc.z);
        var scale :Number = metrics.roomToScreenScale(sprite.loc.z);
        
        sprite.setLocationScale(scale);

        var hotSpot :Point = sprite.getLayoutHotSpot();

        sprite.x = screen.x - hotSpot.x;
        sprite.y = screen.y - hotSpot.y;

        if (sprite.isIncludedInLayout()) {
            adjustZOrder(view, sprite);
        }

    }


    // FROM ROOM CONTROLLER

    // RZ: only used in RoomController.checkMouse():608,614
    
    /**
     * Project room location into screen coordinates.
     * @param target sprite whose data needs to be updated with the projected location.
     */
    public static function roomToScreenLocation (
        view :AbstractRoomView, loc :MsoyLocation, target :DisplayObject) :void
    {
        var metrics :RoomMetrics = view.getRoomMetrics();
        
        var screen :Point = metrics.roomToScreen(loc.x, loc.y, loc.z);
        var scale :Number = metrics.roomToScreenScale(loc.z);
        
        target.x = screen.x;
        target.y = screen.y;
        target.scaleX = scale;
        target.scaleY = scale;

        if (target is ZOrderable) {
            (target as ZOrderable).setZ(loc.z);
            adjustZOrder(view, target);
        }
    }

   
}
}
    
