package com.threerings.msoy.world.client {

import flash.geom.Point;

import com.threerings.msoy.world.data.DecorData;
import com.threerings.msoy.world.data.MsoyLocation;

/**
 * Much of this will change soon when I add heights.
 */
public class RoomMetrics
{
    /** Scene width, in pixels. */
    public var sceneWidth :int

    /** Scene height, in pixels. */
    public var sceneHeight :int;

    /** Scene depth, in "pixels" (where 0 means 'infinite'). */
    public var sceneDepth :int;

    /** The scale of media items at z = 1.0 (far wall). */
    public var farScale :Number;

    /** The scale of media items at z = 0.0 (near wall). */
    public const nearScale :Number = 1.0;

    /** Difference between the scale of nearest and farthest objects. */
    public var scaleRange :Number;

    /**
     * Actual pixel height of the back wall.
     */
    public var backWallHeight :Number;

    /** Y pixel of the top of the back wall. */
    public var backWallTop :Number;

    /** Y pixel of the bottom of the back wall. */
    public var backWallBottom :Number;

    /** Y pixel of the horizon. */
    public var horizonY :Number;

    public var subHorizonHeight :Number;

    /** Camera location, in room coordinates.
     *  The camera always looks in the direction of [ 0, 0, 1 ], the positive z axis. */
    public var camera :MsoyLocation;

    public function update (data :DecorData) :void
    {
        this.sceneDepth = data.depth;
        this.sceneWidth = data.width;
        this.sceneHeight = data.height;
        var horizon :Number = 1 - data.horizon;

        // I'm using 'this' to make clear which assignments are for public props
        this.farScale = (sceneDepth == 0) ? 0 : (FOCAL / (FOCAL + sceneDepth));
        this.scaleRange = nearScale - farScale;
        this.backWallHeight = sceneHeight * farScale;

        this.horizonY = sceneHeight * horizon;
        this.backWallTop = horizonY - (backWallHeight * horizon);
        this.backWallBottom = backWallTop + backWallHeight;

        this.subHorizonHeight = sceneHeight - horizonY;

        this.camera = new MsoyLocation (0.5, horizon, - FOCAL / sceneDepth, 0);
    }

    /**
     * Returns a point in screen coordinates corresponding to <x, y, z> point in room coordinates.
     */
    // TODO: Clean this up with the new formula
    public function roomToScreen (x :Number, y :Number, z :Number) :Point
    {
        var scale :Number = roomToScreenScale(z);
        var floorWidth :Number = (sceneWidth * scale);
        var floorInset :Number = (sceneWidth - floorWidth) / 2;
        return new Point(floorInset + (x * floorWidth),
            horizonY + (subHorizonHeight - (y * sceneHeight)) * scale);
    }

    /**
     * Given z position in room coordinates, it returns a scaling factor for
     * how much a media item needs to be scaled to look correct at that distance.
     */
    public function roomToScreenScale (z :Number) :Number
    {
        // z goes from 1 at far wall to 0 at near wall, so we interpolate between those two.
        return z * farScale + (1 - z) * nearScale;
    }

    /** The focal length of our perspective rendering. */
    // This value (488) was chosen so that the standard depth (400) causes layout nearly identical
    // to the original perspective math.  So, it's purely historical, but we could choose a new
    // focal length and even a new standard scene depth.
    // TODO
    public static const FOCAL :Number = 488;
}
}
