//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.whirled.spot.data.Location;

/**
 * Extends basic the basic Location with a z-coordinate.
 */
public class MsoyLocation
    implements Location
{
    /** The body's x position (interpreted by the display system). */
    public float x;

    /** The body's y position (interpreted by the display system). */
    public float y;

    /** The body's z position (interpreted by the display system). */
    public float z;

    /** The body's orientation (interpreted by the display system). */
    public short orient;

    /** Suitable for unserialization. */
    public MsoyLocation ()
    {
    }

    /**
     * Constructs a fully-specified Location.
     */
    public MsoyLocation (double x, double y, double z, int orient)
    {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        this.orient = (short) orient;
    }

    /**
     * Get the distance between this location and the other.
     */
    public double distance (MsoyLocation that)
    {
        float dx = this.x - that.x;
        float dy = this.y - that.y;
        float dz = this.z - that.z;

        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    // documentation inherited from interface Location
    public Location getOpposite ()
    {
        MsoyLocation l = (MsoyLocation) clone();
        // rotated 180 degrees
        l.orient = (short) ((orient + 180) % 360);
        return l;
    }

    // documentation inherited from interface Location
    public boolean equivalent (Location other)
    {
        return equals(other) &&
            (orient == ((MsoyLocation) other).orient);
    }

    // documentation inherited
    public Object clone ()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            return cnse;
        }
    }

    // documentation inherited
    public boolean equals (Object other)
    {
        if (other instanceof MsoyLocation) {
            MsoyLocation that = (MsoyLocation)other;
            return (this.x == that.x) && (this.y == that.y) &&
                (this.z == that.z);
        }
        return false;
    }

    // documentation inherited
    public int hashCode ()
    {
        return ((int) x) ^ ((int) y) ^ ((int) z);
    }

    public String toString ()
    {
        return "[MsoyLocation(" + x + ", " + y + ", " + z + ") at " +
            orient + " degrees]";
    }
}
