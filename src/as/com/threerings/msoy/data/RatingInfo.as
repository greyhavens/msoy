//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.msoy.data.RatingInfo;

/** The rating summary of some rateable thing. */
public class RatingInfo
    implements Streamable
{
    public var averageRating :Number;
    public var count :int;
    public var myRating :Number;

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        averageRating = ins.readFloat();
        count = ins.readInt();
        myRating = ins.readFloat();
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeFloat(averageRating);
        out.writeInt(count);
        out.writeFloat(myRating);
    }
}
}
