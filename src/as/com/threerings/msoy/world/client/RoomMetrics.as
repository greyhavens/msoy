package com.threerings.msoy.world.client {

import flash.geom.Point;

import com.threerings.flash.Vector3;
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

    /**
     * Scene horizon value, in [0, 1], where 0 means horizon is at the topmost line of the view
     * (so we're shifting the floor up), 0.5 means it's in the middle of the screen,
     * and 1 means it's at the very bottom (so we're shifting the floor down).
     */
    public var sceneHorizon :Number;

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
    public var camera :Vector3;

    /**
     * Floor skew vector. Horizon changes skews the room, by raising or lowering the room's back
     * wall, and this vector represents the skew as a vector from the center of the front wall
     * to the center of the back wall. If the horizon is neutral at 0.5, the vector is parallel to
     * the z-axis.
     */
    public var skew :Vector3;

    /** Floor normal: vector normal to the skewed floor, pointing up. */
    protected var nfloor :Vector3;

    /** Ceiling normal: vector normal to the skewed floor, pointing down. */
    protected var nceiling :Vector3;
        

    /** Read and update metrics from decor data. */
    public function update (data :DecorData) :void
    {
        this.sceneDepth = nearScale * data.depth;
        this.sceneWidth = nearScale * data.width;
        this.sceneHeight = nearScale * data.height;
        this.sceneHorizon = data.horizon;
        var sky :Number = 1 - sceneHorizon; // sky height in room units (from 0 to 1).

        // I'm using 'this' to make clear which assignments are for public props
        this.farScale = (sceneDepth == 0) ? 0 : (FOCAL / (FOCAL + sceneDepth));
        this.scaleRange = nearScale - farScale;
        this.backWallHeight = data.height * farScale;
        this.backWallWidth = data.width * farScale;

        this.horizonY = sceneHeight * sky;
        this.backWallTop = horizonY - (backWallHeight * sky);
        this.backWallBottom = backWallTop + backWallHeight;

        this.camera = new Vector3 (0.5, 0.5, - FOCAL / sceneDepth);

        // calculate floor skew
        var upSkew :Vector3   = new Vector3(0,  0.5, FOCAL / sceneDepth); // at horizon = 0
        var downSkew :Vector3 = new Vector3(0, -0.5, FOCAL / sceneDepth); // at horizon = 1
        this.skew = Vector3.interpolate(downSkew, upSkew, sceneHorizon).normalize();
        this.skew = skew.multiply(1 / skew.z); // make it stretch from near to far wall
        
        this.nfloor = new Vector3(skew.x, skew.z, -skew.y).normalize();
        this.nceiling = nfloor.multiply(-1);
    }

    /**
     * Given a screen position, in pixels from upper-left corner, returns a vector
     * from the camera through that pixel position on the front wall,
     * whose z-length extends all the way to the back wall.
     */
    protected function screenToLineOfSight (x :Number, y :Number) :Vector3
    {
        // scale is the ratio of z-distance to the back wall, over z-distance to the front wall
        var scale :Number = (1 - camera.z) / (0 - camera.z);

        // create the vector to the front wall, and multiply it by the scaling ratio
        // to get a vector to the back wall.
        var rx :Number = x / sceneWidth;
        var ry :Number = (sceneHeight - y) / sceneHeight;
        return new Vector3(rx - camera.x, ry - camera.y, 0 - camera.z).multiply(scale);
    }

    /**
     * Returns a point in screen coordinates corresponding to <x, y, z> point in room coordinates.
     */
    public function roomToScreen (x :Number, y :Number, z :Number) :Point
    {
        var v :Vector3 = positionOnFrontWall(new Vector3(x, y, z));

        var xPosition :Number = v.x * sceneWidth;
        var yPosition :Number = sceneHeight - v.y * sceneHeight;
        
        return new Point(xPosition, yPosition);
    }

    /**
     * Given a screen location, draws a line of sight vector and tries to find the closest
     * intersection with any of the walls within the unit cube. Returns a location in room
     * coordinates, with "back wall" as the default click target if no other intersection exists.
     */
    public function screenToWallProjection (x :Number, y :Number) :ClickLocation
    {
        var l :Vector3 = screenToLineOfSight(x, y);
        
        // intersect with the five walls (except for the near wall :)
        var walls :Array = [
            [ ClickLocation.FLOOR, l.intersection(camera, leftBottomNearCorner, nfloor) ],
            [ ClickLocation.CEILING, l.intersection(camera, rightTopNearCorner, nceiling) ],
            [ ClickLocation.LEFT_WALL, l.intersection(camera, leftBottomNearCorner, nRight) ],
            [ ClickLocation.RIGHT_WALL, l.intersection(camera, rightTopNearCorner, nLeft) ],
            [ ClickLocation.BACK_WALL, l.intersection(camera, farCenter, nToCamera) ] ];

        var min :Number = Infinity;
        var minpair :Array = walls[4];
        for each (var pair :Array in walls) {
            var location :int = pair[0];
            var len :Number = pair[1].length();
            if (len <= min && pair[1].z > 0) { // only check points in the scene
                min = len;
                minpair = pair;
            }
        }

        var v :Vector3 = minpair[1];
        
        // because the room is skewed, we need to "unskew" it.
        // it's as easy as projecting back to the flat horizontal plane.
        if (minpair[0] == ClickLocation.FLOOR) { v.y = 0; }
        if (minpair[0] == ClickLocation.CEILING) { v.y = 1; }

        return new ClickLocation(minpair[0], new MsoyLocation (v.x, v.y, v.z, 0));
    }

    /**
     * Given a screen location l, and a positional constraint p, finds a location on the vertical
     * line passing through p that corresponds to the screen location.
     */
    public function screenToYLineProjection (x :Number, y :Number, p :Vector3) :Vector3
    {
        // if we're constraining to y-axis movement only, what we actually want to do is
        // invert the problem - the player's cursor is sweeping a horizontal plane across
        // the scene, skewed based on the mouse pointer, and we intersect that with
        // a vertical line coming from p.

        // find the line of sight vector, and the normal of the plane it defines
        var line :Vector3 = screenToLineOfSight(x, y);
        var n :Vector3 = line.cross(nLeft).normalize();

        // find where this plane intersects with the constrained ray
        var result :Vector3 = nAbsoluteUp.intersection(p, camera, n);

        // unskew the result
        return unskewVector(result);
    }        

    /** Returns a new vector that skews the parameter based on floor skew. */
    public function skewVector (v :Vector3) :Vector3
    {
        var yoffset :Number = interpolate(0, skew.y, v.z);
        return new Vector3 (v.x, v.y + yoffset, v.z);
    }
    
    /** Returns a new vector that skews the parameter based on floor skew. */
    public function unskewVector (v :Vector3) :Vector3
    {
        var yoffset :Number = interpolate(0, skew.y, v.z);
        return new Vector3 (v.x, v.y - yoffset, v.z);
    }

    /**
     * Draws a ray from the camera to the point in the room, and finds where it intersects
     * with the front wall. If the room is skewed, that's taken into account automatically.
     */
    public function positionOnFrontWall (target :Vector3) :Vector3
    {
        target = skewVector(target);
        
        // draw a ray from the camera, and see where it falls on the front wall
        return target.subtract(camera).intersection(camera, leftBottomNearCorner, nToCamera);
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
        var scale :Number = scaleAtDepth(z);
        var roomHeightAtDepth :Number =  sceneHeight * z;
        var roomWidthAtDepth :Number = sceneWidth * z;
        return new Point(delta.x / roomWidthAtDepth, delta.y / roomHeightAtDepth);
    }

    /**
     * Given z position in room coordinates it returns a scaling factor, which converts
     * item height at z distance to item height at the front plane. This can be used immediately
     * to scale media displayed on screen. 
     *
     * Scaling factor at the front wall is 1.0, and decreases as the z distance grows.
     */
    public function scaleAtDepth (z :Number) :Number
    {
        // scaling factor is the proportion of distance from camera to the near wall,
        // over the overall distance from camera to the z point.
        return (0 - camera.z) / (z - camera.z);
    }

    /**
     * Interpolates linearly between two values depending on the z coordinate
     * (z should be in room dimensions, so in [0, 1]).
     */
    public function interpolate (nearValue :Number, farValue :Number, z :Number) :Number
    {
        return (1 - z) * nearValue + z * farValue;
    }

    /** Convenience function: creates a new MsoyLocation object from a Vector3 object. */
    public function toMsoyLocation (v :Vector3) :MsoyLocation
    {
        return new MsoyLocation (v.x, v.y, v.z, 0);
    }

    /** Convenience function: creates a new Vector3 object from a MsoyLocation object. */
    public function toVector3 (loc :MsoyLocation) :Vector3
    {
        return new Vector3 (loc.x, loc.y, loc.z);
    }

    // LEGACY FUNCTIONS (TEMPORARY)
    /*
    public function roomToScreen_legacy (x :Number, y :Number, z :Number) :Point
    {
        var position :Point = roomDistanceToPixelDistance (new Point(x, y), z);
        var leftOffset :Number = interpolate(0, (sceneWidth - backWallWidth) / 2, z);
        var groundOffset :Number = interpolate(0, sceneHeight - backWallBottom, z);
        var xPosition :Number = leftOffset + position.x;
        var yPosition :Number = sceneHeight - (groundOffset + position.y);
        return new Point(xPosition, yPosition);
    }

    public function pixelDistanceToRoomDistance_legacy (delta :Point, z :Number) :Point
    {
        var roomHeightAtDepth :Number = interpolate(sceneHeight, backWallHeight, z);
        var roomWidthAtDepth :Number = interpolate(sceneWidth, backWallWidth, z);
        return new Point(delta.x / roomWidthAtDepth, delta.y / roomHeightAtDepth);
    }
    
    public function scaleAtDepth_legacy (z :Number) :Number
    {
        return interpolate(nearScale, farScale, z);
    }
    */

    /** The focal length of our perspective rendering. */
    public static const FOCAL :Number = 800;

    /** Convenience position vectors, in room coordinates. */
    public static const leftBottomNearCorner :Vector3 = new Vector3(0, 0, 0);
    public static const rightTopNearCorner :Vector3 = new Vector3(1, 1, 0);
    public static const farCenter :Vector3 = new Vector3(0.5, 0.5, 1);

    /** Convenience normal vectors. */
    public static const nLeft :Vector3       = new Vector3(-1, 0, 0);
    public static const nRight :Vector3      = new Vector3( 1, 0, 0);
    public static const nToCamera :Vector3   = new Vector3( 0, 0,-1);
    public static const nAbsoluteUp :Vector3 = new Vector3( 0, 1, 0);
}
}
