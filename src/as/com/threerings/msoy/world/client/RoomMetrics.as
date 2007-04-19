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

    /** Actual pixel height of the back wall. */
    public var backWallHeight :Number;

    /** Actual pixel width of the back wall. */
    public var backWallWidth :Number;

    /** Y pixel of the top of the back wall. */
    public var backWallTop :Number;

    /** Y pixel of the bottom of the back wall. */
    public var backWallBottom :Number;

    /** Y pixel of the horizon (also its height in pixels from the top of the screen). */
    public var horizonY :Number;

    /** Camera location, in room coordinates.
     *  The camera always looks in the direction of [ 0, 0, 1 ], the positive z axis. */
    public var camera :MsoyLocation;

    public function update (data :DecorData) :void
    {
        this.sceneDepth = nearScale * data.depth;
        this.sceneWidth = nearScale * data.width;
        this.sceneHeight = nearScale * data.height;
        var sky :Number = 1 - data.horizon; // sky height in room units (from 0 to 1).

        // I'm using 'this' to make clear which assignments are for public props
        this.farScale = (sceneDepth == 0) ? 0 : (FOCAL / (FOCAL + sceneDepth));
        this.scaleRange = nearScale - farScale;
        this.backWallHeight = data.height * farScale;
        this.backWallWidth = data.width * farScale;

        this.horizonY = sceneHeight * sky;
        this.backWallTop = horizonY - (backWallHeight * sky);
        this.backWallBottom = backWallTop + backWallHeight;

        this.camera = new MsoyLocation (0.5, 0.5, - FOCAL / sceneDepth, 0);
    }

    /**
     * Returns a point in screen coordinates corresponding to <x, y, z> point in room coordinates.
     */
    public function roomToScreen (x :Number, y :Number, z :Number) :Point
    {
        // raw x, y distances in screen coordinates at z
        var position :Point = roomDistanceToPixelDistance (new Point(x, y), z);

        // pixel offset from the left edge of the stage, of an object at z
        var leftOffset :Number = interpolate(0, (sceneWidth - backWallWidth) / 2, z);
        // pixel offset from the bottom of the screen, of something sitting on the ground at z
        var groundOffset :Number = interpolate(0, sceneHeight - backWallBottom, z);

        var xPosition :Number = leftOffset + position.x;
        var yPosition :Number = sceneHeight - (groundOffset + position.y);

        return new Point(xPosition, yPosition);
    }

    /**
     * Given dx and dy values (as a point) and z value, in room coordinates,
     * converts them to pixel dx and dy.
     */
    public function roomDistanceToPixelDistance (delta :Point, z :Number) :Point
    {
        var scale :Number = scaleAtDepth(z);
        return new Point(delta.x * scale * sceneWidth, delta.y * scale * sceneHeight);
    }

    /**
     * Given dx and dy values (as a point) and a z value, in screen coordinates,
     * converts them to room dx and dy.
     */
    public function pixelDistanceToRoomDistance (delta :Point, z :Number) :Point
    {
        var roomHeightAtDepth :Number = interpolate(sceneHeight, backWallHeight, z);
        var roomWidthAtDepth :Number = interpolate(sceneWidth, backWallWidth, z);
        return new Point(delta.x / roomWidthAtDepth, delta.y / roomHeightAtDepth);
    }

    /**
     * Given z position in room coordinates, it returns a scaling factor for
     * how much a media item needs to be scaled to look correct at that distance.
     */
    public function scaleAtDepth (z :Number) :Number
    {
        // z goes from 1 at far wall to 0 at near wall, so we interpolate between those two.
        return interpolate(nearScale, farScale, z);
    }

    /**
     * Interpolates linearly between two values depending on the z coordinate
     * (z should be in room dimensions, so in [0, 1]).
     */
    public function interpolate (nearValue :Number, farValue :Number, z :Number) :Number
    {
        return (1 - z) * nearValue + z * farValue;
    }


    /** The focal length of our perspective rendering. */
    // This value (488) was chosen so that the standard depth (400) causes layout nearly identical
    // to the original perspective math.  So, it's purely historical, but we could choose a new
    // focal length and even a new standard scene depth.
    // TODO
    public static const FOCAL :Number = 488;
}
}
