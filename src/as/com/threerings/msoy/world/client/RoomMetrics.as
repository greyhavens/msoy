package com.threerings.msoy.world.client {

import flash.geom.Point;

import com.threerings.flash.Vector3;
import com.threerings.msoy.world.data.DecorData;
import com.threerings.msoy.world.data.MsoyLocation;

/**
 * Room parameters in screen coordinates, and associated conversion routines.
 *
 * Rooms are measured in two coordinate spaces: room coordinates, and screen coordinates.
 *
 * In room space, the room is an axis-aligned unit cube, with one corner at the origin and
 * the other at [1, 1, 1]. The player's camera is centered on the [0.5, 0.5, z] axis,
 * and the viewport is defined as the square [[0, 0, 0], [1, 1, 0]] (i.e. the 'front wall'
 * of the unit cube), with origin in the lower left. Objects locations are expressed
 * in room space. 
 *
 * In pixel space, the room has a specific height, width, and depth, with origin at upper left.
 * Because the front wall serves as the viewport, and the view is always axis-aligned
 * and looking in the +z direction, conversion into pixel space reduces to projecting
 * all objects onto the front wall, and then converting their x, y coordinates from room space
 * to pixel space.
 *
 * Horizon value changes where the room's "horizon line" is displayed in the viewport.
 * Changes to the horizon can be considered as skewing the room square, such that
 * the back wall shifts up or down. The skew is done during conversion between screen
 * and room coordinate spaces. 
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
     * (so we're shifting the back wall up), 0.5 means it's in the middle of the screen,
     * and 1 means it's at the very bottom (so we're shifting the back wall down).
     */
    public var sceneHorizon :Number;

    /**
     * Camera location, in room coordinates.
     * The camera always looks in the direction of [ 0, 0, 1 ], the positive z axis.
     */
    public var camera :Vector3;

    /** Camera's focal length. */
    public var focal :Number;
    
    /**
     * Floor skew factor. Horizon changes skews the room, as if raising or lowering the
     * room's back wall, and this is the value of this vertical skew offset.
     */
    public var skewoffset :Number;

    /** Floor normal: vector normal to the skewed floor, pointing up. */
    protected var nfloor :Vector3;

    /** Ceiling normal: vector normal to the skewed floor, pointing down. */
    protected var nceiling :Vector3;
        

    /** Read and update metrics from decor data. */
    public function update (data :DecorData) :void
    {
        // I'm using 'this' to make clear which assignments are for public props
        this.focal = DEFAULT_FOCAL;
        this.sceneDepth = data.depth;
        this.sceneWidth = data.width;
        this.sceneHeight = data.height;
        this.sceneHorizon = data.horizon;
        this.camera = new Vector3 (0.5, 0.5, - focal / sceneDepth);

        // calculate floor skew
        var upSkew :Vector3   = new Vector3(0,  0.5, focal / sceneDepth); // at horizon = 0
        var downSkew :Vector3 = new Vector3(0, -0.5, focal / sceneDepth); // at horizon = 1
        var skew :Vector3     = Vector3.interpolate(downSkew, upSkew, sceneHorizon).normalize();

        // make the skew vector stretch from near to far wall, and remember its y-value
        var nskew :Vector3 = skew.multiply(1 / skew.z); 
        this.skewoffset = nskew.y;
        // save its normals
        this.nfloor = new Vector3(nskew.x, nskew.z, -nskew.y).normalize();
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
     * Returns a point in screen coordinates corresponding to the vector in room coordinates.
     */
    // Why doesn't Actionscript support method overloading? What the flash?
    public function roomToScreenV3 (v :Vector3) :Point
    {
        return roomToScreen(v.x, v.y, v.z);
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
            [ ClickLocation.FLOOR,
              l.intersection(camera, LEFT_BOTTOM_NEAR, nfloor) ],
            [ ClickLocation.CEILING,
              l.intersection(camera, RIGHT_TOP_NEAR, nceiling) ],
            [ ClickLocation.LEFT_WALL,
              unskewVector(l.intersection(camera, LEFT_BOTTOM_NEAR, N_RIGHT)) ],
            [ ClickLocation.RIGHT_WALL,
              unskewVector(l.intersection(camera, RIGHT_TOP_NEAR, N_LEFT)) ],
            [ ClickLocation.BACK_WALL,
              unskewVector(l.intersection(camera, CENTER_CENTER_FAR, N_TO_CAMERA)) ] ];

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
        var n :Vector3 = line.cross(N_LEFT).normalize();

        // find where this plane intersects with the constrained ray
        var result :Vector3 = N_UP.intersection(p, camera, n);

        // subtract any room skew factors
        return unskewVector(result);
    }        

    /** Add skewing factor to the vector (used before projecting onto front wall). */
    public function skewVector (v :Vector3) :Vector3
    {
        var yoffset :Number = interpolate(0, skewoffset, v.z);
        return new Vector3 (v.x, v.y + yoffset, v.z);
    }
    
    /** Remove skewing factor from the vector (used on results of line of sight calculation). */
    public function unskewVector (v :Vector3) :Vector3
    {
        var yoffset :Number = interpolate(0, skewoffset, v.z);
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
        return target.subtract(camera).intersection(camera, LEFT_BOTTOM_NEAR, N_TO_CAMERA);
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

    /** Initial focal length of our perspective rendering. */
    public static const DEFAULT_FOCAL :Number = 488; 

    /** Convenience position vectors, in room coordinates. */
    public static const LEFT_BOTTOM_NEAR :Vector3  = new Vector3( 0, 0, 0);
    public static const RIGHT_TOP_NEAR :Vector3    = new Vector3( 1, 1, 0);
    public static const LEFT_BOTTOM_FAR :Vector3  = new Vector3( 0, 0, 1);
    public static const RIGHT_TOP_FAR :Vector3    = new Vector3( 1, 1, 1);
    public static const CENTER_CENTER_FAR :Vector3 = new Vector3(.5,.5, 1);

    /** Convenience normal vectors. */
    public static const N_LEFT :Vector3       = new Vector3(-1, 0, 0);
    public static const N_RIGHT :Vector3      = new Vector3( 1, 0, 0);
    public static const N_UP :Vector3         = new Vector3( 0, 1, 0);
    public static const N_DOWN :Vector3       = new Vector3( 0,-1, 0);
    public static const N_TO_CAMERA :Vector3  = new Vector3( 0, 0,-1);
}
}
