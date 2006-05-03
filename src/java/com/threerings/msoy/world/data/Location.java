//
// $Id$

package com.threerings.msoy.world.data;

/**
 * Extends basic the basic Location with a z-coordinate.
 */
public class Location extends com.threerings.whirled.spot.data.Location
{
    /** The body's z position (interpreted by the display system). */
    public int z;

    /** Suitable for unserialization. */
    public Location ()
    {
    }

    /**
     * Constructs a fully-specified Location.
     */
    public Location (int x, int y, int z, byte orient)
    {
        super(x, y, orient);
        this.z = z;
    }

    // documentation inherited
    public boolean equals (Object other)
    {
        // we just go ahead and redefine this whole method
        if (other instanceof Location) {
            Location that = (Location)other;
            return (this.x == that.x) && (this.y == that.y) &&
                (this.z == that.z);
        }
        return false;
    }
}
