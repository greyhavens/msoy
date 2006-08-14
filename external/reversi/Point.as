package {

import flash.utils.IDataInput;
import flash.utils.IDataOutput;
import flash.utils.IExternalizable;

import com.threerings.util.Hashable;

public class Point
    implements Hashable, IExternalizable
{
    /** Our x coordinate. */
    public var x :int;

    /** Our y coordinate. */
    public var y :int;

    public function Point (x :int = 0, y :int = 0)
    {
        this.x = x;
        this.y = y;
    }

    // from Hashable
    public function hashCode () :int
    {
        return x ^ y;
    }

    // from Hashable
    public function equals (other :Object) :Boolean
    {
        if (other is Point) {
            var that :Point = (other as Point);
            return (this.x == that.x && this.y == that.y);
        }
        return false;
    }

    public function toString () :String
    {
        return "Point(" + x + ", " + y + ")";
    }

    // from IExternalizable
    public function readExternal (input :IDataInput) :void
    {
        x = input.readInt();
        y = input.readInt();
    }

    // from IExternalizable
    public function writeExternal (output :IDataOutput) :void
    {
        output.writeInt(x);
        output.writeInt(y);
    }
}
}
