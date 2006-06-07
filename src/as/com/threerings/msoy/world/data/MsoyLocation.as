package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.util.Equalable;

import com.threerings.whirled.spot.data.Location;

/**
 * This class is equivalent to its Java class except that the Java superclass
 * has been incorporated.
 */
public class MsoyLocation
    implements Location
{
    /** The body's x position (interpreted by the display system). */
    public var x :Number;

    /** The body's x position (interpreted by the display system). */
    public var y :Number;

    /** The body's x position (interpreted by the display system). */
    public var z :Number;

    /** The body's orientation (interpreted by the display system). */
    public var orient :int;

    public function MsoyLocation (
            x :int = 0, y :int = 0, z :int = 0, orient :int = 0)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.orient = orient;
    }

    /**
     * Get the distance between this location and the other.
     */
    public function distance (that :MsoyLocation) :Number
    {
        var dx :Number = this.x - that.x;
        var dy :Number = this.y - that.y;
        var dz :Number = this.z - that.z;

        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    // documentation inherited from interface Location
    public function getOpposite () :Location
    {
        var l :MsoyLocation = (clone() as MsoyLocation);
        // and rotate it 180 degrees
        l.orient += 180 * ((l.orient < 180) ? 1 : -1)
        return l;
    }

    // documentation inherited from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(z);
        out.writeShort(orient);
    }

    // documentation inherited from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        x = ins.readInt();
        y = ins.readInt();
        z = ins.readInt();
        orient = ins.readShort();
    }

    // documentation inherited from interface Hashable
    public function equals (other :Object) :Boolean
    {
        if (other is MsoyLocation) {
            var that :MsoyLocation = (other as MsoyLocation);
            return (this.x == that.x) && (this.y == that.y) &&
                (this.z == that.z);
        }
        return false;
    }

    // documentation inherited from interface Hashable
    public function hashCode () :int
    {
        return x ^ y ^ z;
    }

    /**
     * Locations are equivalent if they have the same coordinates and
     * orientation.
     */
    public function equivalent (oloc :Location) :Boolean
    {
        return equals(oloc) &&
            (orient == (oloc as MsoyLocation).orient);
    }

    // documentation inherited from interface Location
    public function clone () :Object
    {
        return new MsoyLocation(x, y, z, orient);
    }

    public function toString () :String
    {
        return "[MsoyLocation(" + x + ", " + y + ", " + z + ") at " +
            orient + " degrees]";
    }
}
}
