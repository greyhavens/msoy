package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.msoy.data.MediaData;

public class FurniData
    implements Streamable
{
    /** Info about the media that represents this piece of furni. */
    public var mediaData :MediaData;

    /** The location in the scene. */
    public var loc :MsoyLocation;

    // documentation inherited from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeField(mediaData);
        out.writeField(loc);
    }

    // documentation inherited from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        mediaData = (ins.readField(MediaData) as MediaData);
        loc = (ins.readField(MsoyLocation) as MsoyLocation);
    }
}
}
