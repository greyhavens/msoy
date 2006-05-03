package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.util.Equalable;

/**
 * This class is equivalent to its Java class except that the Java superclass
 * has been incorporated.
 */
public class Location
    implements Equalable, Streamable
{
    /** The body's x position (interpreted by the display system). */
    public var x :int;

    /** The body's x position (interpreted by the display system). */
    public var y :int;

    /** The body's x position (interpreted by the display system). */
    public var z :int;

    /** The body's orientation (defined by {@link DirectionCodes}). */
    public var orient :int;

    public function Location (
            x :int = 0, y :int = 0, z :int = 0, orient :int = 0)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.orient = orient;
    }

    // documentation inherited from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeInt(x);
        out.writeInt(y);
        out.writeByte(orient);
        out.writeInt(z);
    }

    // documentation inherited from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        x = ins.readInt();
        y = ins.readInt();
        orient = ins.readByte();
        z = ins.readInt();
    }

    // documentation inherited from interface Equalable
    public function equals (other :Object) :Boolean
    {
        if (other is Location) {
            var that :Location = (other as Location);
            return (this.x == that.x) && (this.y == that.y) &&
                (this.z == that.z);
        }
        return false;
    }

    /**
     * Locations are equivalent if they have the same coordinates and
     * orientation.
     */
    public function equivalent (oloc :Location) :Boolean
    {
        return equals(oloc) && (orient == oloc.orient);
    }
}
}
