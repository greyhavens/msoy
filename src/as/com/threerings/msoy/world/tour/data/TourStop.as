//
// $Id$

package com.threerings.msoy.world.tour.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.msoy.data.RatingInfo;

/** Represents a destination along the tour. */
public class TourStop
    implements Streamable
{
    public var sceneId :int;
    public var rating :RatingInfo;

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        sceneId = ins.readInt();
        rating = RatingInfo(ins.readObject());
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeInt(sceneId);
        out.writeObject(rating);
    }
}
}
