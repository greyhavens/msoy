//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;
import com.threerings.io.Streamable;

/**
 * Updated rating information in response to the user rating something.
 */
public class RatingResult
    implements Streamable
{
    /** The target's current rating. */
    public var rating :Number;

    /** The number of players who have rated the target. */
    public var ratingCount :int;

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        rating = ins.readFloat();
        ratingCount = ins.readInt();
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeFloat(rating);
        out.writeInt(ratingCount);
    }
}

}
