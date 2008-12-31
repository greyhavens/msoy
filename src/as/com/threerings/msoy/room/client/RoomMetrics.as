//
// $Id$

package com.threerings.msoy.room.client {

import flash.geom.Point;

import com.threerings.flash.Vector3;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.room.data.MsoyLocation;

/**
 * Room parameters in screen coordinates, and associated conversion routines.
 *
 * Rooms are measured in three coordinate spaces: world coordinates, room coordinates,
 * and screen coordinates.
 *
 * In room space, the room is an axis-aligned unit cube, with one corner at the origin and
 * the other at [1, 1, 1]. The player's camera is centered on the [0.5, 0.5, z] axis,
 * and the viewport is defined as the square [[0, 0, 0], [1, 1, 0]] (i.e. the 'front wall'
 * of the unit cube), with origin in the lower left. Objects locations are expressed
 * in room space.
 *
 * World space is intermediate between room and screen; in a standard room, all room positions
 * are the same as world positions. However, horizon value can skew the room by shifting the back
 * wall up or down. This modifies how room x and y values map to world position.
 *
 * In pixel space, the room has a specific height, width, and depth, with origin at upper left.
 * Because the front wall serves as the viewport, and the view is always axis-aligned
 * and looking in the +z direction, conversion into pixel space reduces to projecting
 * all objects onto the front wall, and then converting their x, y coordinates from room space
 * to pixel space.
 *
 *
 */
public class RoomMetrics
{
    /** Scene width, in screen coordinates. */
    public var sceneWidth :int

    /** Scene height, in screen coordinates. */
    public var sceneHeight :int;

    /** Scene depth, in screen coordinates. */
    public var sceneDepth :int;

    /**
     * Scene horizon value, in [0, 1], where 0 means horizon is at the topmost line of the view
     * (so we're shifting the back wall up), 0.5 means it's in the middle of the screen,
     * and 1 means it's at the very bottom (so we're shifting the back wall down).
     */
    public var sceneHorizon :Number;

    /**
     * Camera location, in world coordinates.
     * The camera always looks in the direction of [ 0, 0, 1 ], the positive z axis.
     */
    public var camera :Vector3;

    /** Camera's focal length, in screen units. */
    public var focal :Number;

    /**
     * This flag specifies whether this room is being stretched into a trapezoid, making the room's
     * side walls parallel to the player's line of sight. If true, the room will be stretched to
     * fill the player's viewport; otherwise the room will be displayed in perspective.
     */
    public var trapezoidalTransform :Boolean;

    /**
     * Vertical skew factor, in world coordinates. Horizon changes skews the room, as if raising or
     * lowering the room's back wall; this is the skew value as the vertical distance between
     * the center of the far wall in world coordinate space and in room coordinate space.
     */
    protected var vSkewOffset :Number;

    /** Wall definition objects, mapping from wall type IDs to anchor points and normals. */
    protected var walldefs :Array;

    /** Read and update metrics. */
    public function update (decor :Decor) :void
    {
        // I'm using 'this' to make clear which assignments are for public props
        this.focal = Math.max(1, DEFAULT_FOCAL);    // nb: focal length must not be zero
        this.sceneDepth = Math.max(1, decor.depth); // nb: scene depth must not be zero
        this.sceneWidth = decor.width;
        this.sceneHeight = decor.height;
        this.sceneHorizon = decor.horizon;
        this.trapezoidalTransform = decor.hideWalls;
        this.camera = new Vector3 (0.5, 0.5, - focal / sceneDepth);

        // floor and ceiling:

        // calculate floor vectors for maximally and minimally skewed floor, and current skew
        var upSkew :Vector3   = new Vector3(0,  0.5, focal / sceneDepth); // at horizon = 1
        var downSkew :Vector3 = new Vector3(0, -0.5, focal / sceneDepth); // at horizon = 0
        var skew :Vector3     = Vector3.interpolate(downSkew, upSkew, sceneHorizon).normalize();

        // make the skew vector stretch from near to far wall, and remember its y-value
        var nskew :Vector3 = skew.scale(1 / skew.z);
        this.vSkewOffset = nskew.y;

        // save its normals: one of the skewed floor pointing up, the other pointing down
        var nfloor :Vector3 = new Vector3(nskew.x, nskew.z, -nskew.y).normalize();
        var nceiling :Vector3 = nfloor.scale(-1);

        // left and right walls:

        // default wall normals point towards the center of the room
        var nleft :Vector3 = N_RIGHT;
        var nright :Vector3 = N_LEFT;

        if (trapezoidalTransform) {
            // stretch them out!
            var scale :Number = (1 - camera.z) / (0 - camera.z);
            var leftwall :Vector3 = new Vector3(camera.x * (1 - scale), 0, 1);
            var rightwall :Vector3 = new Vector3(camera.x + scale * (1 - camera.x) - 1, 0, 1);
            nleft = leftwall.cross(N_DOWN).normalize();
            nright = rightwall.cross(N_UP).normalize();
        }

        // create wall definitions for this room. room anchor points are specified in room space,
        // and world anchor points and normals are specified in world space.
        this.walldefs = [
            { type: ClickLocation.FLOOR,      roompoint: LEFT_BOTTOM_NEAR, n: nfloor   },
            { type: ClickLocation.CEILING,    roompoint: RIGHT_TOP_FAR,    n: nceiling },
            { type: ClickLocation.LEFT_WALL,  roompoint: LEFT_BOTTOM_NEAR, n: nleft    },
            { type: ClickLocation.RIGHT_WALL, roompoint: RIGHT_TOP_FAR,    n: nright   },
            { type: ClickLocation.FRONT_WALL, roompoint: LEFT_BOTTOM_NEAR, n: N_AWAY   },
            { type: ClickLocation.BACK_WALL,  roompoint: RIGHT_TOP_FAR,    n: N_NEAR   } ];
        // add world anchor points here, and wrap normals
        for each (var def :Object in this.walldefs) {
            def.worldpoint = roomToWorld(new RoomVector(def.roompoint));
            def.worldnormal = new WorldVector(def.n);
        }
    }

    /**
     * Given an <x, y, z> point in room coordinates, projects that point onto the front wall
     * along the line of sight from the camera, and returns its location in screen coordinates.
     */
    public function roomToScreen (x :Number, y :Number, z :Number) :Point
    {
        var roomPosition :RoomVector = new RoomVector(new Vector3(x, y, z));
        var worldPosition :WorldVector = roomToWorld(roomPosition);

        // draw a ray from the camera, and see where it falls on the front wall
        var w :WorldVector = new WorldVector(
            worldPosition.v.subtract(camera).intersection(camera, LEFT_BOTTOM_NEAR, N_NEAR));

        var xPosition :Number = w.v.x * sceneWidth;
        var yPosition :Number = (1 - w.v.y) * sceneHeight;

        return new Point(xPosition, yPosition);
    }

    /**
     * Given a screen location, draws a line of sight vector and tries to find the closest
     * intersection with any of the walls within the unit cube. Returns a location in room
     * coordinates, with "back wall" as the default click target if no other intersection exists.
     */
    public function screenToInnerWallsProjection (x :Number, y :Number) :ClickLocation
    {
        var l :WorldVector = screenToLineOfSight(x, y);

        var mindistance :Number = Infinity;
        var minpoint :WorldVector = null;
        var minwall :int = ClickLocation.BACK_WALL;

        // intersect with the five walls (except for the near wall :)
        for each (var def :Object in walldefs) {
            if (def.type != ClickLocation.FRONT_WALL) {
                var pos :WorldVector = lineOfSightToPlaneProjection(
                    l, def.worldpoint, def.worldnormal);

                if (pos != null && pos.v.length < mindistance) {
                    minpoint = pos;
                    mindistance = pos.v.length;
                    minwall = def.type;
                }
            }
        }

        return new ClickLocation(minwall, (minpoint != null) ?
                                 toMsoyLocation(worldToRoom(minpoint).v) :
                                 new MsoyLocation());
    }

    /**
     * Given a screen location and wall type (one of the ClickLocation.* constants), draws a line
     * of sight vector, and tries to intersect it with that wall. Returns a point of intersection
     * in room coordinates in the z >= 0 half-space, or null if the intersection is invalid (the
     * wall is parallel, or point of intersection lies behind the front wall (in z < 0
     * half-space)).
     */
    public function screenToWallProjection (x :Number, y :Number, wall :int) :ClickLocation
    {
        var def :Object = getWallDef(wall);
        var pos :WorldVector = screenToPlane(x, y, def.worldpoint, def.worldnormal);
        return (pos != null) ?
            new ClickLocation(def.type, toMsoyLocation(worldToRoom(pos).v)) :
            null;
    }

    /**
     * Given a screen location and an anchor point (in room coordinates), draws a line of sight
     * vector, and tries to intersect it with a plane parallel to the view port, passing through
     * the anchor point. Returns a point of intersection in room coordinates in the z >= 0
     * half-space, or null if the intersection is invalid (the wall is parallel, or point of
     * intersection lies behind the front wall (in z < 0 half-space)). The resulting location's
     * /click/ parameter is not meaningful.
     */
    public function screenToWallPlaneProjection (x :Number, y :Number, point :Vector3)
        :ClickLocation
    {
        // convert room point to world coords
        var worldpoint :WorldVector = roomToWorld(new RoomVector(point));

        var def :Object = getWallDef(ClickLocation.BACK_WALL);
        var worldnormal :WorldVector = new WorldVector(def.n);
        var intersection :WorldVector = screenToPlane(x, y, worldpoint, worldnormal);
        return (intersection != null) ?
            new ClickLocation(def.type, toMsoyLocation(worldToRoom(intersection).v)) :
            null;
    }

    /**
     * Given a screen location and an anchor point (in room coordinates), draws a line of sight
     * vector, and tries to intersect it with a plane parallel to the floor, passing through
     * the anchor point. Returns a point of intersection in room coordinates in the z >= 0
     * half-space, or null if the intersection is invalid (the wall is parallel, or point of
     * intersection lies behind the front wall (in z < 0 half-space)). The resulting location's
     * /click/ parameter is not meaningful.
     */
    public function screenToFloorPlaneProjection (x :Number, y :Number, point :Vector3)
        :ClickLocation
    {
        // convert room point to world coords
        var worldpoint :WorldVector = roomToWorld(new RoomVector(point));

        var def :Object = getWallDef(ClickLocation.FLOOR);
        var worldnormal :WorldVector = new WorldVector(def.n);
        var intersection :WorldVector = screenToPlane(x, y, worldpoint, worldnormal);

        return (intersection != null) ?
            new ClickLocation(def.type, toMsoyLocation(worldToRoom(intersection).v)) :
            null;
    }

    /**
     * Given a screen location x, y, and an anchor point p in room coordinates, finds a location on
     * the x-axis-aligned line passing through p, that corresponds to the screen location. Returns
     * the new location in room coordinates.
     */
    public function screenToXLineProjection (x :Number, y :Number, p :Vector3) :Vector3
    {
        return screenSweepingProjection(x, y, p, N_RIGHT, true);
    }

    /**
     * Given a screen location x, y, and an anchor point p in room coordinates, finds a location on
     * the y-axis-aligned line passing through p, that corresponds to the screen location. Returns
     * the new location in room coordinates.
     */
    public function screenToYLineProjection (x :Number, y :Number, p :Vector3) :Vector3
    {
        return screenSweepingProjection(x, y, p, N_UP, false);
    }

    /**
     * Given a screen location x, y, and an anchor point p in room coordinates, finds a location on
     * the z-axis-aligned line passing through p, that corresponds to the screen location. Returns
     * the new location in room coordinates.
     */
    public function screenToZLineProjection (x :Number, y :Number, p :Vector3) :Vector3
    {
        return screenSweepingProjection(x, y, p, N_NEAR, false);
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

    // IMPLEMENTATION DETAILS

    /**
     * Returns wall definition for the specified wall type.
     */
    protected function getWallDef (wall :int) :Object
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
        return def;
    }

    /** Accessor that converts position from room space to world space. */
    protected function roomToWorld (r :RoomVector) :WorldVector
    {
        return new WorldVector(roomWorldConversion(r.v, true));
    }

    /** Accessor that converts position from world space to room space. */
    protected function worldToRoom (w :WorldVector) :RoomVector
    {
        return new RoomVector(roomWorldConversion(w.v, false));
    }

    /**
     * Apply skewing factor to the vector. Boolean specifies whether the projection is
     * from room to world coordinate space, or vice versa.
     */
    protected function roomWorldConversion (v :Vector3, toWorld :Boolean) :Vector3
    {
        var processingCamera :Boolean = (v == camera);
        var yoffset :Number = interpolate(0, toWorld ? vSkewOffset : -vSkewOffset, v.z);
        var xoffset :Number = 0;

        var v :Vector3 = new Vector3 (v.x + xoffset, v.y + yoffset, v.z);
        if (Math.abs(v.y) < 0.001) {
            v.y = 0;  // remove any loss of precision artifacts
        }

        if (trapezoidalTransform && ! processingCamera) {
            // figure out how to adjust the x position. we want to stretch the back wall so that
            // the scene completely fills the player's view cone (in other words, make the side
            // walls be parallel to the player's line of sight at the edges of the screen).

            // get the scale factor for the far wall (it's the same as the proportion between
            // distances to the far wall vs the near wall), and then adjust it for our z position.
            var maxscale :Number = (1 - camera.z) / (0 - camera.z);
            var scale :Number = interpolate(1, maxscale, v.z);
            if (toWorld) {
                v.x = camera.x + (v.x - camera.x) * scale;
            } else {
                v.x = camera.x + (v.x - camera.x) / scale;
            }
        }

        return v;
    }

    /**
     * Given a screen location and a plane definition (as a point and a normal in world
     * coordinates), draws a line of sight vector, and tries to intersect it with that
     * wall. Returns a point of intersection in *world* coordinates in the z >= 0 half-space, or
     * null if the intersection is invalid (the wall is parallel, or point of intersection lies
     * behind the front wall (in z < 0 half-space)). The resulting location's /click/ parameter is
     * not meaningful.
     */
    protected function screenToPlane (
        x :Number, y :Number, point :WorldVector, normal :WorldVector) :WorldVector
    {
        return lineOfSightToPlaneProjection(screenToLineOfSight(x, y), point, normal);
    }

    /**
     * Given a screen position, in pixels from upper-left corner, returns a vector in world
     * coordinates from the camera through that pixel position on the front wall, whose z-length
     * extends all the way to the back wall.
     */
    protected function screenToLineOfSight (x :Number, y :Number) :WorldVector
    {
        // scale is the ratio of z-distance to the back wall, over z-distance to the front wall
        var scale :Number = (1 - camera.z) / (0 - camera.z);

        // create the vector to the front wall, and multiply it by the scaling ratio
        // to get a vector to the back wall in room coords
        var rx :Number = x / sceneWidth;
        var ry :Number = (sceneHeight - y) / sceneHeight;
        var v :Vector3 = new Vector3(rx - camera.x, ry - camera.y, 0 - camera.z).scale(scale);

        return new WorldVector(v);
    }

    /**
     * Given a line of sight vector (in world coords) and a wall defined by a point and a normal
     * vector (also in world coords), tries to intersect that vector with that wall. Returns a
     * point of intersection in world coords in the z >= 0 half-space, or null if the intersection
     * was invalid (wall is either parallel to the vector, or the intersection lies behind the
     * front wall (in the z < 0 half-space)).
     */
    protected function lineOfSightToPlaneProjection (
        l :WorldVector, point :WorldVector, normal :WorldVector) :WorldVector
    {
        var pos :Vector3 = l.v.intersection(camera, point.v, normal.v);
        return (pos.z >= 0 && pos != Vector3.INFINITE) ? new WorldVector(pos) : null;
    }

    /**
     * Given a screen location x and y, and a line defined with the /anchor/ point and the /axis/
     * vector, the function finds:
     * 1. the sweeping plane that looks at x, y, and
     * 2. the intersection point of the sweeping plane with a line defined by /anchor/ and /axis/
     * When the isVertical flag is set, the sweeping plane will be parallel to the +y axis,
     * otherwise it will be parallel to the +x axis. The function returns the intersection
     * point in room coordinates, or Vector.INFINITE if no valid intersection point was found.
     */
    protected function screenSweepingProjection (
        x :Number, y :Number, anchor :Vector3, axis :Vector3, isVertical :Boolean) :Vector3
    {
        var d :Vector3 = isVertical ? N_UP : N_LEFT;
        var sweepNormal :Vector3 = null;
        var sweepAnchor :Vector3 = null;

        // find where the cursor is pointing
        var worldTarget :WorldVector = new WorldVector(camera.add(screenToLineOfSight(x, y).v));
        var roomTarget :RoomVector = worldToRoom(worldTarget);

        if (trapezoidalTransform && isVertical) {
            // the sweeping plane shouldn't be anchored at the camera - it's parallel to the
            // +z axis because of the trapezoidal skew of the room coordinate space
            sweepAnchor = roomTarget.v;
            sweepNormal = N_AWAY.cross(d).normalize();

        } else {
            // this is the standard approach - create a sweeping plane anchored at the camera
            var roomCamera :RoomVector = worldToRoom(new WorldVector(camera));
            var roomLOS :Vector3 = roomTarget.v.subtract(roomCamera.v);
            sweepAnchor = roomCamera.v;
            sweepNormal = roomLOS.cross(d).normalize();
        }

        // find where this plane intersects with the constraint line
        return axis.intersection(anchor, sweepAnchor, sweepNormal);
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

import com.threerings.flash.Vector3;

// These internal wrapper classes specify the vector's coordinate space. This let us use the
// compiler to catch coordinate space conversion mistakes at compile time.

internal class RoomVector
{
    public var v :Vector3;
    public function RoomVector (v :Vector3)
    {
        this.v = v;
    }
}

internal class WorldVector
{
    public var v :Vector3;
    public function WorldVector (v :Vector3)
    {
        this.v = v;
    }
}
