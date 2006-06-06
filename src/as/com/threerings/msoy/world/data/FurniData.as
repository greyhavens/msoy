package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.msoy.data.MediaData;

public class FurniData
    implements Streamable
{
    /** The id of this piece of furni. */
    public var id :int;

    /** Info about the media that represents this piece of furni. */
    public var mediaData :MediaData;

    /** The location in the scene. */
    public var loc :MsoyLocation;

    // documentation inherited from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeInt(id);
        out.writeObject(mediaData);
        out.writeObject(loc);
    }

    // documentation inherited from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        id = ins.readInt();
        mediaData = (ins.readObject() as MediaData);
        loc = (ins.readObject() as MsoyLocation);
    }
}
}
