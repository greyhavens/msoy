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
     * room's back wall, and this is the value of this vertical skew offset in room units.
     */
    public var skewoffset :Number;

    /** Wall definition objects, mapping from wall type IDs to anchor points and normals. */
    public var walldefs :Array; 
    
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
        // save its normals: one of the skewed floor pointing up, the other pointing down
        var nfloor :Vector3 = new Vector3(nskew.x, nskew.z, -nskew.y).normalize();
        var nceiling :Vector3 = nfloor.multiply(-1);

        // wall definitions for this room. 
        this.walldefs = [
            { type: ClickLocation.FLOOR,      point: LEFT_BOTTOM_NEAR, normal: nfloor   },
            { type: ClickLocation.CEILING,    point: RIGHT_TOP_FAR,    normal: nceiling },
            { type: ClickLocation.LEFT_WALL,  point: LEFT_BOTTOM_NEAR, normal: N_RIGHT  },
            { type: ClickLocation.RIGHT_WALL, point: RIGHT_TOP_FAR,    normal: N_LEFT   },
            { type: ClickLocation.FRONT_WALL, point: LEFT_BOTTOM_NEAR, normal: N_AWAY   },
            { type: ClickLocation.BACK_WALL,  point: RIGHT_TOP_FAR,    normal: N_NEAR   } ];
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
        var yPosition :Number = (1 - v.y) * sceneHeight;
        
        return new Point(xPosition, yPosition);
    }

    /**
     * Given a screen location, draws a line of sight vector and tries to find the closest
     * intersection with any of the walls within the unit cube. Returns a location in room
     * coordinates, with "back wall" as the default click target if no other intersection exists.
     */
    public function screenToInnerWallsProjection (x :Number, y :Number) :ClickLocation
    {
        var l :Vector3 = screenToLineOfSight(x, y);

        var mindistance :Number = Infinity;
        var minpoint :Vector3 = Vector3.INFINITE;
        var minwall :int = ClickLocation.BACK_WALL;
        
        // intersect with the five walls (except for the near wall :)
        for each (var def :Object in walldefs) {
            if (def.type != ClickLocation.FRONT_WALL) {
                var pos :Vector3 = lineOfSightToPlaneProjection(l, def.point, def.normal);
                if (pos.length() < mindistance) {
                    minpoint = pos;
                    mindistance = pos.length();
                    minwall = def.type;
                }
            }
        }

        return new ClickLocation(minwall, toMsoyLocation(minpoint));
    }

    /**
     * Given a screen location and wall type (one of the ClickLocation.* constants),
     * draws a line of sight vector, and tries to intersect it with that wall.
     * Returns a point of intersection, or Vector3.INFINITE if the wall is parallel,
     * or point of intersection lies behind the front wall (in z < 0).
     */
    public function screenToWallProjection (x :Number, y :Number, wall :int) :Vector3
    {
        var def :Object = null;
        for each (var o :Object in walldefs) {
            if (o.type == wall) {
                def = o;
            }
        }

        // sanity check
        if (def == null) {
            throw new ArgumentError ("Wall identifier " + wall + " is not supported.");
        }

        return screenToPlaneProjection(x, y, def.point, def.normal);
    }

    /**
     * Given a screen location and a wall location (as a point and a normal in room coordinates),
     * draws a line of sight vector, and tries to intersect it with that wall.
     * Returns a point of intersection, or Vector3.INFINITE if the wall is parallel,
     * or point of intersection lies behind the front wall (in z < 0).
     */
    public function screenToPlaneProjection (x :Number, y :Number, point :Vector3, normal :Vector3)
        :Vector3
    {
        var l :Vector3 = screenToLineOfSight(x, y);
        return lineOfSightToPlaneProjection(l, point, normal);
    }

    /**
     * Given a line of sight vector (unskewed) and a wall defined by a point and a normal vector,
     * tries to intersect that vector with that wall. Returns a point of intersection in the
     * z => 0 half-space, or Vector3.INFINITE if the wall is either parallel to the vector,
     * or the intersection lies behind the front wall (z < 0).
     */
    protected function lineOfSightToPlaneProjection (l :Vector3, point :Vector3, normal :Vector3)
        :Vector3
    {
        var pos :Vector3 = l.intersection(camera, point, normal);
        if (pos.z < 0) {
            return Vector3.INFINITE;
        } else {
            return applySkew(pos);
        }
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
        return applySkew(result);
    }        

    /**
     * Given a screen location l, and a positional constraint p, finds a location on the
     * z-axis-aligned line passing through p that corresponds to the screen location.
     */
    public function screenToZLineProjection (x :Number, y :Number, p :Vector3) :Vector3
    {
        // similarly to the y-location finder, we treat p as defining a ray parallel with
        // the z axis, and player's cursor as sweeping a horizontal plane anchored at the camera.
        // all we need to do is find the intersection.

        // find the line of sight vector, and the normal of the horizontal plane
        var line :Vector3 = screenToLineOfSight(x, y);
        var n :Vector3 = line.cross(N_UP).normalize();

        // find where this plane intersects with the constrained ray
        var result :Vector3 = N_NEAR.intersection(p, camera, n);

        return result;
    }


    /**
     * Draws a ray from the point in the room to the camera, and finds where it intersects
     * with the front wall.
     */
    public function positionOnFrontWall (target :Vector3) :Vector3
    {
        target = applySkew(target, true);
        
        // draw a ray from the camera, and see where it falls on the front wall
        return target.subtract(camera).intersection(camera, LEFT_BOTTOM_NEAR, N_NEAR);
    }
        
    /**
     * Apply skewing factor to the vector. Forward skew is applied when projecting a ray
     * from the screen into room space; reverse when projecting from room back onto screen.
     */
    protected function applySkew (v :Vector3, forward :Boolean = false) :Vector3
    {
        var yoffset :Number = interpolate(0, forward ? skewoffset : -skewoffset, v.z);

        var v :Vector3 = new Vector3 (v.x, v.y + yoffset, v.z);
        if (Math.abs(v.y) < 0.001) {
            v.y = 0;  // remove any loss of precision artifacts
        }
        
        return v;
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
    public static const LEFT_BOTTOM_FAR :Vector3   = new Vector3( 0, 0, 1);
    public static const RIGHT_TOP_FAR :Vector3     = new Vector3( 1, 1, 1);

    /** Convenience normal vectors. */
    public static const N_LEFT :Vector3   = new Vector3(-1, 0, 0);
    public static const N_RIGHT :Vector3  = new Vector3( 1, 0, 0);
    public static const N_UP :Vector3     = new Vector3( 0, 1, 0);
    public static const N_DOWN :Vector3   = new Vector3( 0,-1, 0);
    public static const N_NEAR :Vector3   = new Vector3( 0, 0,-1);
    public static const N_AWAY :Vector3   = new Vector3( 0, 0, 1);
}
}
